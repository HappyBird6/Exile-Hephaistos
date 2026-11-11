import { IDBFactory, IDBObjectStore } from 'fake-indexeddb'
import { expect, it, vi } from 'vitest'
import { IndexedFilmRepository } from './IndexedFilmRepository'
import {
  emptyFilms,
  startFilm,
  historyStorageKey,
  LocalFilmRepository,
} from '../workbenchHistory'
import { initialFixture } from '../../../shared/test/craftingFixtures'
import { concreteInitial } from '../workbenchApi'
import { queueSessionDeletion, restoreDeletedSession } from './deletionUndo'
const history = () =>
  ['one', 'two'].reduce(
    (h, id) =>
      startFilm(
        h,
        concreteInitial(initialFixture),
        id,
        initialFixture.rulesetIdentity,
        'Exact original clipboard',
      ),
    emptyFilms(),
  )
it('migrates once, preserves original bytes and roundtrips the deletion deadline and source text', async () => {
  const factory = new IDBFactory()
  const original = queueSessionDeletion(history(), 'two', 1000)!
  new LocalFilmRepository(localStorage).save(original)
  const raw = localStorage.getItem(historyStorageKey)
  const repo = new IndexedFilmRepository(localStorage, factory)
  expect(await repo.load()).toEqual(original)
  await repo.save(restoreDeletedSession(original, 2000))
  expect(localStorage.getItem(historyStorageKey)).toBe(raw)
  const reloaded = await new IndexedFilmRepository(localStorage, factory).load()
  expect(reloaded.films).toHaveLength(2)
  expect(reloaded.films[1]?.sourceDocument).toBe('Exact original clipboard')
})
it('rejects a stale tab transaction without overwriting the newer saved session', async () => {
  const factory = new IDBFactory()
  const first = new IndexedFilmRepository(localStorage, factory),
    second = new IndexedFilmRepository(localStorage, factory)
  const [a, b] = await Promise.all([first.load(), second.load()])
  await first.save({ ...a, films: history().films })
  await expect(second.save({ ...b, cursor: 0 })).rejects.toThrow('another tab')
  expect((await second.load()).films).toHaveLength(2)
})
it('writes only changed films and atomically retains the previous checkpoint on quota failure', async () => {
  const repo = new IndexedFilmRepository(localStorage, new IDBFactory())
  await repo.load()
  const original = history()
  await repo.save(original)
  const writes = vi.spyOn(IDBObjectStore.prototype, 'put')
  await repo.save({ ...original, active: 'one' })
  expect(writes).toHaveBeenCalledTimes(1) // cursor metadata only, no history serialization
  const changed = {
    ...original,
    films: original.films.map((f, i) => (i ? f : { ...f, title: 'renamed' })),
  }
  writes.mockImplementationOnce(() => {
    throw new DOMException('full', 'QuotaExceededError')
  })
  await expect(repo.save(changed)).rejects.toThrow('full')
  writes.mockRestore()
  const restored = await repo.load()
  expect(restored.active).toBe('one')
  expect(restored.films[0]?.title).toBeUndefined()
})
it('does not mark an invalid legacy migration complete', async () => {
  const factory = new IDBFactory()
  localStorage.setItem(historyStorageKey, '{broken')
  const repo = new IndexedFilmRepository(localStorage, factory)
  await expect(repo.load()).rejects.toThrow()
  expect(localStorage.getItem(historyStorageKey)).toBe('{broken')
  localStorage.setItem(historyStorageKey, JSON.stringify(history()))
  expect((await repo.load()).films).toHaveLength(2)
})

it('can retry after a temporary database open failure without losing the legacy history', async () => {
  const factory = new IDBFactory()
  new LocalFilmRepository(localStorage).save(history())
  const open = vi.spyOn(factory, 'open').mockImplementationOnce(() => {
    throw new DOMException('temporarily unavailable', 'UnknownError')
  })
  const repo = new IndexedFilmRepository(localStorage, factory)
  await expect(repo.load()).rejects.toThrow('temporarily unavailable')
  expect((await repo.load()).films).toHaveLength(2)
  expect(open).toHaveBeenCalledTimes(2)
  open.mockRestore()
  await repo.close()
})
