import {
  LocalFilmRepository,
  validateFilms,
  validStoredFilm,
} from '../workbenchHistory'
import type { Films, Film, FilmRepository } from '../workbenchHistory'

type Metadata = Omit<Films, 'films'> & { order: string[]; revision: number }
const request = <T>(value: IDBRequest<T>) =>
  new Promise<T>((resolve, reject) => {
    value.onsuccess = () => resolve(value.result)
    value.onerror = () => reject(value.error)
  })
const completed = (tx: IDBTransaction) =>
  new Promise<void>((resolve, reject) => {
    tx.oncomplete = () => resolve()
    tx.onabort = () =>
      reject(tx.error ?? new Error('History transaction aborted'))
    tx.onerror = () =>
      reject(tx.error ?? new Error('History transaction failed'))
  })

/** One atomic checkpoint, with per-film writes and optimistic concurrency between browser tabs.
 * The legacy localStorage bytes remain untouched after migration; a failed migration never marks
 * the new store as initialized. No remote account or server storage is involved.
 */
export class IndexedFilmRepository implements FilmRepository {
  private database: Promise<IDBDatabase> | undefined
  private revision = 0
  private previous = new Map<string, Film>()
  private fallback: LocalFilmRepository | undefined
  constructor(
    private storage: Pick<Storage, 'getItem' | 'setItem'> = localStorage,
    private factory = globalThis.indexedDB,
  ) {
    // Older/private browsers without the API keep the existing persistence contract.
    if (!factory) this.fallback = new LocalFilmRepository(storage)
  }
  loadSyncFallback() {
    return this.fallback?.load()
  }
  private open() {
    if (this.database) return this.database
    const pending = new Promise<IDBDatabase>((resolve, reject) => {
      const opening = this.factory.open('hephaistos-workbench-history', 1)
      let failed = false
      opening.onupgradeneeded = () => {
        opening.result.createObjectStore('films', { keyPath: 'id' })
        opening.result.createObjectStore('metadata')
      }
      opening.onsuccess = () => {
        const db = opening.result
        if (failed) {
          db.close()
          return
        }
        db.onversionchange = () => db.close()
        resolve(db)
      }
      opening.onerror = () => {
        failed = true
        reject(opening.error)
      }
      opening.onblocked = () => {
        failed = true
        reject(new Error('History database upgrade blocked'))
      }
    })
    this.database = pending
    // A temporary browser storage failure must not poison the user's Retry action.
    void pending.catch(() => {
      if (this.database === pending) this.database = undefined
    })
    return pending
  }
  async load(): Promise<Films> {
    if (this.fallback) return this.fallback.load()
    const db = await this.open()
    // A readwrite transaction makes competing first migrations mutually exclusive.
    const tx = db.transaction(['films', 'metadata'], 'readwrite')
    const done = completed(tx)
    try {
      const meta = (await request(
        tx.objectStore('metadata').get('current'),
      )) as Metadata | undefined
      let history: Films
      if (!meta) {
        history = new LocalFilmRepository(this.storage).load()
        validateFilms(history)
        for (const film of history.films) tx.objectStore('films').put(film)
        const { films, ...rest } = history
        tx.objectStore('metadata').put(
          { ...rest, order: films.map((f) => f.id), revision: 1 },
          'current',
        )
        this.revision = 1
      } else {
        const films = (await request(
          tx.objectStore('films').getAll(),
        )) as Film[]
        const byId = new Map(films.map((f) => [f.id, f]))
        const { order, revision, ...rest } = meta
        history = { ...rest, films: order.map((id) => byId.get(id)!) }
        validateFilms(history)
        this.revision = revision
      }
      await done
      this.previous = new Map(history.films.map((f) => [f.id, f]))
      return history
    } catch (error) {
      try {
        tx.abort()
      } catch {
        /* May already be aborted by IndexedDB. */
      }
      await done.catch(() => {})
      throw error
    }
  }
  async save(history: Films): Promise<void> {
    if (this.fallback) return this.fallback.save(history)
    const changed = history.films.filter((f) => this.previous.get(f.id) !== f)
    if (
      !changed.every(validStoredFilm) ||
      new Set(history.films.map((f) => f.id)).size !== history.films.length
    )
      throw new Error('Invalid history; previous checkpoint retained')
    const db = await this.open()
    const tx = db.transaction(['films', 'metadata'], 'readwrite')
    const done = completed(tx)
    try {
      const meta = (await request(
        tx.objectStore('metadata').get('current'),
      )) as Metadata | undefined
      if (meta?.revision !== this.revision)
        throw new Error('History changed in another tab; reload before saving')
      const ids = new Set(history.films.map((f) => f.id))
      for (const id of this.previous.keys())
        if (!ids.has(id)) tx.objectStore('films').delete(id)
      for (const film of changed) tx.objectStore('films').put(film)
      const { films, ...rest } = history
      const revision = this.revision + 1
      tx.objectStore('metadata').put(
        { ...rest, order: films.map((f) => f.id), revision },
        'current',
      )
      await done
      this.revision = revision
      this.previous = new Map(films.map((f) => [f.id, f]))
    } catch (error) {
      try {
        tx.abort()
      } catch {
        /* Keep the committed checkpoint intact. */
      }
      await done.catch(() => {})
      throw error
    }
  }
  async close() {
    ;(await this.database)?.close()
  }
}
