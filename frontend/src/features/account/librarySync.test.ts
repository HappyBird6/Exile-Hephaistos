import { beforeEach, afterEach, expect, it, vi } from 'vitest'
import { IDBFactory } from 'fake-indexeddb'
import { webcrypto } from 'node:crypto'
import { loadAccount } from './auth'
import {
  syncLibrary,
  browserMergeStatus,
  decideBrowserMerge,
} from './librarySync'
import { IndexedFilmRepository } from '../crafting/session-library/IndexedFilmRepository'
import { concreteInitial } from '../crafting/workbenchApi'
import { initialFixture } from '../../shared/test/craftingFixtures'
import { emptyFilms, startFilm } from '../crafting/workbenchHistory'
import type { Document } from './libraryPlan'
import { canonical, documentKey, planSync, mergeFilms } from './libraryPlan'
const owner = 'account-one'
let generation = 0
let batchSize: number | null = null
let remote: Document[],
  writes: number,
  reads: number,
  loseResponse: boolean,
  failNetwork: boolean
beforeEach(async () => {
  generation = 0
  batchSize = null
  localStorage.clear()
  remote = []
  writes = 0
  reads = 0
  loseResponse = false
  failNetwork = false
  vi.stubGlobal('indexedDB', new IDBFactory())
  vi.stubGlobal('crypto', webcrypto)
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const path = String(input)
      if (path.endsWith('/me'))
        return Response.json({
          enabled: true,
          user: { id: owner, displayName: 'one' },
          csrf: 'token',
        })
      if (failNetwork) throw Error('offline')
      expect((init?.headers as Record<string, string>)['X-Account-Id']).toBe(
        owner,
      )
      if (path.endsWith('/library'))
        return Response.json(
          remote.map((d) => ({
            kind: d.kind,
            id: d.id,
            version: d.version,
            deleted: d.content === null,
          })),
          {
            headers: {
              'X-Session-Generation': String(generation),
              ...(batchSize === null
                ? {}
                : { 'X-Library-Read-Batch-Size': String(batchSize) }),
            },
          },
        )
      if (path.endsWith('/library/read-batch')) {
        reads++
        const body = JSON.parse(String(init?.body))
        expect(body.generation).toBe(generation)
        expect(body.references.length).toBeLessThanOrEqual(batchSize!)
        return Response.json(
          body.references.map((ref: Document) =>
            remote.find((d) => documentKey(d) === documentKey(ref)),
          ),
        )
      }
      const key = decodeURIComponent(path.split('/library/')[1]!)
      const existing = remote.find((d) => documentKey(d) === key)
      if (init?.method === 'PUT') {
        writes++
        const body = JSON.parse(String(init.body))
        if (body.version !== (existing?.version ?? 0))
          return new Response(null, { status: 409 })
        const [kind, id] = key.split('/') as [Document['kind'], string]
        const next = {
          kind,
          id,
          version: body.version + 1,
          content: body.content,
        } as Document
        remote = remote.filter((d) => documentKey(d) !== key).concat(next)
        if (loseResponse) {
          loseResponse = false
          throw Error('response lost')
        }
        return Response.json(next)
      }
      reads++
      return existing
        ? Response.json(existing)
        : new Response(null, { status: 404 })
    }),
  )
  await loadAccount()
})
afterEach(() => vi.unstubAllGlobals())
async function seed(scope: string | null = owner) {
  const repo = new IndexedFilmRepository(localStorage, indexedDB, scope)
  const history = startFilm(
    emptyFilms(),
    concreteInitial(initialFixture),
    'original',
    'fixture-ruleset',
  )
  await repo.load()
  await repo.save(history)
  await repo.close()
  return history
}
async function edit(title: string) {
  const repo = new IndexedFilmRepository(localStorage, indexedDB, owner)
  const history = await repo.load()
  await repo.save({
    ...history,
    films: history.films.map((f) => ({ ...f, title })),
  })
  await repo.close()
}
async function films(scope: string | null = owner) {
  const repo = new IndexedFilmRepository(localStorage, indexedDB, scope)
  try {
    return (await repo.load()).films
  } finally {
    await repo.close()
  }
}
it('uploads only changed documents, retries a lost response without duplication and keeps offline changes', async () => {
  await seed()
  loseResponse = true
  await expect(syncLibrary(owner)).rejects.toThrow('response lost')
  expect(remote).toHaveLength(1)
  await syncLibrary(owner)
  expect(writes).toBe(1)
  const oldReads = reads
  await syncLibrary(owner)
  expect(reads).toBe(oldReads)
  expect(writes).toBe(1)
  failNetwork = true
  await edit('offline edit')
  await expect(syncLibrary(owner)).rejects.toThrow('offline')
  expect((await films())[0]?.title).toBe('offline edit')
  failNetwork = false
  await syncLibrary(owner)
  expect(remote[0]?.content?.title).toBe('offline edit')
  expect(remote).toHaveLength(1)
})
it('forks a conflicting local edit once and preserves the remote original', async () => {
  await seed()
  await syncLibrary(owner)
  const original = remote[0]!
  remote = [
    {
      ...original,
      version: 2,
      content: { ...original.content!, title: 'other device' },
    },
  ]
  await edit('this device')
  await syncLibrary(owner)
  expect(remote).toHaveLength(2)
  expect(remote.find((d) => d.id === 'original')?.content?.title).toBe(
    'other device',
  )
  expect((await films()).map((f) => f.title).sort()).toEqual([
    'other device',
    'this device',
  ])
  await syncLibrary(owner)
  expect(remote).toHaveLength(2)
})
it('applies remote deletion without resurrecting a stale saved session', async () => {
  await seed()
  await syncLibrary(owner)
  remote = [{ ...remote[0]!, version: 2, content: null }]
  await syncLibrary(owner)
  expect(await films()).toEqual([])
  await syncLibrary(owner)
  expect(writes).toBe(1)
})
it('asks before merging, preserves skipped guest data and deduplicates repeated imports', async () => {
  const guest = await seed(null)
  expect((await browserMergeStatus(owner)).needed).toBe(true)
  await decideBrowserMerge(owner, false)
  expect(await films()).toEqual([])
  expect(await films(null)).toEqual(guest.films)
  await decideBrowserMerge(owner, true)
  await decideBrowserMerge(owner, true)
  expect(await films()).toHaveLength(1)
  expect(await films(null)).toEqual(guest.films)
  await syncLibrary(owner)
  expect(remote).toHaveLength(1)
})
it('isolates browser and other account caches and rejects stale tab writes', async () => {
  await seed()
  await seed('account-two')
  const a = new IndexedFilmRepository(localStorage, indexedDB, owner),
    b = new IndexedFilmRepository(localStorage, indexedDB, owner)
  const first = await a.load()
  await b.load()
  await a.save({
    ...first,
    films: first.films.map((f) => ({ ...f, title: 'updated' })),
  })
  await expect(b.save(first)).rejects.toThrow('another tab')
  expect((await films('account-two'))[0]?.title).toBeUndefined()
  expect(await films(null)).toEqual([])
  await a.close()
  await b.close()
})
it('compares content independently of key order and never treats unobserved remote items as local deletions', () => {
  expect(canonical({ b: 1, a: 2 })).toBe(canonical({ a: 2, b: 1 }))
  const doc = {
    kind: 'session',
    id: 'test',
    version: 1,
    content: { id: 'test' },
  } as Document
  expect(planSync([], [], [doc])).toEqual({ writes: [], next: [doc] })
})

it('does not invalidate the active repository when syncing only its own edits', async () => {
  const repo = new IndexedFilmRepository(localStorage, indexedDB, owner)
  let history = await repo.load()
  history = startFilm(
    history,
    concreteInitial(initialFixture),
    'z-session',
    'fixture-ruleset',
  )
  history = startFilm(
    history,
    concreteInitial(initialFixture),
    'a-session',
    'fixture-ruleset',
  )
  await repo.save(history)
  await syncLibrary(owner)
  history = {
    ...history,
    films: history.films.map((f, i) =>
      i === 0 ? { ...f, title: 'first edit' } : f,
    ),
  }
  await repo.save(history)
  await syncLibrary(owner)
  history = {
    ...history,
    films: history.films.map((f, i) =>
      i === 0 ? { ...f, title: 'second edit' } : f,
    ),
  }
  await expect(repo.save(history)).resolves.toBeUndefined()
  await syncLibrary(owner)
  expect(remote.find((d) => d.id === 'z-session')?.content?.title).toBe(
    'second edit',
  )
  await repo.close()
})
it('recovers an acknowledged-on-server write before uploading newer local changes', async () => {
  await seed()
  loseResponse = true
  await expect(syncLibrary(owner)).rejects.toThrow()
  await edit('newer local edit')
  await syncLibrary(owner)
  expect(remote).toHaveLength(1)
  expect(remote[0]?.content?.title).toBe('newer local edit')
  expect(remote[0]?.version).toBe(2)
})
it('preserves full base favorites and their removals across account sync', async () => {
  const {
    saveBaseFavorite,
    removeBaseFavorite,
    baseFavoritesKey,
    readBaseFavorites,
  } = await import('../crafting/base-favorites/repository')
  const { scopedStorage } = await import('./storageScope')
  const history = await seed()
  const id = saveBaseFavorite(history.films[0]!)
  await syncLibrary(owner)
  expect(remote.filter((d) => d.kind === 'base-favorite')).toHaveLength(1)
  expect(
    readBaseFavorites(
      scopedStorage(localStorage, owner).getItem(baseFavoritesKey),
    )[0]?.frames[0]?.state,
  ).toEqual(history.films[0]?.frames[0]?.state)
  removeBaseFavorite(id)
  await syncLibrary(owner)
  expect(remote.find((d) => d.kind === 'base-favorite')?.content).toBeNull()
})

it('deduplicates import paths without dropping the incoming favorite flag', async () => {
  const history = await seed()
  const original = history.films[0]!
  const incoming = {
    ...original,
    id: 'different-browser-id',
    favorite: true,
    favoriteAt: '2026-11-12T00:00:00Z',
  }
  const merged = mergeFilms([original], [incoming])
  expect(merged).toHaveLength(1)
  expect(merged[0]?.id).toBe(original.id)
  expect(merged[0]?.favorite).toBe(true)
  expect(original.favorite).toBeUndefined()
})

it('discards unacknowledged offline sessions after an account-wide clear', async () => {
  await seed()
  await syncLibrary(owner)
  await edit('offline stale')
  generation = 1
  remote = remote.map((d) => ({ ...d, version: d.version + 1, content: null }))
  await syncLibrary(owner)
  expect(await films()).toEqual([])
  expect(writes).toBe(1)
})

it('keeps a newer local edit made while a remote index is in flight', async () => {
  await seed()
  await syncLibrary(owner)
  await edit('upload checkpoint')
  const originalFetch = vi.mocked(fetch).getMockImplementation()!
  let changed = false
  vi.mocked(fetch).mockImplementation(async (input, init) => {
    const response = await originalFetch(input, init)
    if (String(input).endsWith('/library') && !changed) {
      changed = true
      await edit('continued crafting')
    }
    return response
  })
  await syncLibrary(owner)
  expect((await films())[0]?.title).toBe('continued crafting')
  await syncLibrary(owner)
  expect(remote).toHaveLength(1)
  expect(remote[0]?.content?.title).toBe('continued crafting')
})

it('forks on retry when another device saves after the index was read', async () => {
  await seed()
  await syncLibrary(owner)
  await edit('this device')
  const originalFetch = vi.mocked(fetch).getMockImplementation()!
  let raced = false
  vi.mocked(fetch).mockImplementation(async (input, init) => {
    if (init?.method === 'PUT' && !raced) {
      raced = true
      remote = [
        {
          ...remote[0]!,
          version: 2,
          content: { ...remote[0]!.content!, title: 'other device' },
        },
      ]
    }
    return originalFetch(input, init)
  })
  await expect(syncLibrary(owner)).rejects.toMatchObject({ status: 409 })
  expect((await films())[0]?.title).toBe('this device')
  await syncLibrary(owner)
  await syncLibrary(owner)
  expect(remote.map((d) => d.content?.title).sort()).toEqual([
    'other device',
    'this device',
  ])
})

it('does not create extra conflict copies when the fork response is lost', async () => {
  await seed()
  await syncLibrary(owner)
  remote = [
    {
      ...remote[0]!,
      version: 2,
      content: { ...remote[0]!.content!, title: 'other device' },
    },
  ]
  await edit('this device')
  loseResponse = true
  await expect(syncLibrary(owner)).rejects.toThrow('response lost')
  await syncLibrary(owner)
  await syncLibrary(owner)
  expect(remote).toHaveLength(2)
  expect((await films()).map((f) => f.title).sort()).toEqual([
    'other device',
    'this device',
  ])
})

it('does not apply an old account response after switching accounts', async () => {
  await seed()
  await syncLibrary(owner)
  await seed('account-two')
  const originalFetch = vi.mocked(fetch).getMockImplementation()!
  vi.mocked(fetch).mockImplementation(async (input, init) => {
    if (String(input).endsWith('/me'))
      return Response.json({
        enabled: true,
        user: { id: 'account-two', displayName: 'two' },
        csrf: 'two-token',
      })
    const response = await originalFetch(input, init)
    if (String(input).endsWith('/library')) await loadAccount()
    return response
  })
  await expect(syncLibrary(owner)).rejects.toThrow('ACCOUNT_CHANGED')
  expect((await films('account-two'))[0]?.title).toBeUndefined()
  expect(remote).toHaveLength(1)
  expect(writes).toBe(1)
})

it('remembers a skipped browser merge after signing in again', async () => {
  const guest = await seed(null)
  await decideBrowserMerge(owner, false)
  await loadAccount()
  await syncLibrary(owner)
  expect((await browserMergeStatus(owner)).needed).toBe(false)
  expect(await films(null)).toEqual(guest.films)
  expect(remote).toEqual([])
})

it('clears only guest sessions while keeping base favorites and account sessions', async () => {
  const { clearSessions } = await import('./librarySync')
  const { baseFavoritesKey } =
    await import('../crafting/base-favorites/repository')
  const guest = await seed(null)
  await seed()
  localStorage.setItem(baseFavoritesKey, JSON.stringify(guest.films))
  await clearSessions(null)
  expect(await films(null)).toEqual([])
  expect(await films()).toHaveLength(1)
  expect(JSON.parse(localStorage.getItem(baseFavoritesKey)!)).toEqual(
    guest.films,
  )
})

it('honors a bulk-clear generation even when the server has reclaimed all session tombstones', async () => {
  await seed()
  await syncLibrary(owner)
  await edit('stale offline content')
  generation = 1
  remote = []
  await syncLibrary(owner)
  expect(await films()).toEqual([])
  await syncLibrary(owner)
  expect(remote).toEqual([])
  expect(writes).toBe(1)
  await seed()
  await syncLibrary(owner)
  expect(remote).toHaveLength(1)
  expect(writes).toBe(2)
})

it('batches cold reads, reuses unchanged documents and reads only changed versions', async () => {
  batchSize = 5
  const base = (await seed(null)).films[0]!
  remote = Array.from({ length: 12 }, (_, i) => ({
    kind: 'session',
    id: `remote-${i}`,
    version: 1,
    content: { ...base, id: `remote-${i}` },
  }))
  await syncLibrary(owner)
  expect(reads).toBe(3)
  expect(writes).toBe(0)
  expect(await films()).toHaveLength(12)
  await syncLibrary(owner)
  expect(reads).toBe(3)
  remote[0] = {
    ...remote[0]!,
    version: 2,
    content: { ...remote[0]!.content!, title: 'changed' },
  }
  await syncLibrary(owner)
  expect(reads).toBe(4)
  expect((await films()).find((f) => f.id === 'remote-0')?.title).toBe(
    'changed',
  )
})

it('does not publish a partially read library when a later batch fails', async () => {
  batchSize = 5
  const base = (await seed(null)).films[0]!
  remote = Array.from({ length: 8 }, (_, i) => ({
    kind: 'session',
    id: `remote-${i}`,
    version: 1,
    content: { ...base, id: `remote-${i}` },
  }))
  const originalFetch = fetch
  let batches = 0
  vi.stubGlobal(
    'fetch',
    async (input: RequestInfo | URL, init?: RequestInit) => {
      if (String(input).endsWith('/read-batch') && ++batches === 2)
        throw Error('interrupted batch')
      return originalFetch(input, init)
    },
  )
  await expect(syncLibrary(owner)).rejects.toThrow('interrupted batch')
  expect(await films()).toHaveLength(0)
  expect(writes).toBe(0)
  vi.stubGlobal('fetch', originalFetch)
  await syncLibrary(owner)
  expect(await films()).toHaveLength(8)
  expect(writes).toBe(0)
})
