import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  initialFixture,
  fixtureFetch,
} from '../../shared/test/craftingFixtures'
import { applyCurrency, concreteInitial } from './workbenchApi'
import { supportsConcreteStateShape } from './workbenchStateShape'
import {
  LocalFilmRepository,
  emptyFilms,
  startFilm,
  verifiedHistoryState,
  historyStorageKey,
} from './workbenchHistory'

const root = concreteInitial(initialFixture)
describe('lossless unsupported state boundary', () => {
  afterEach(() => vi.unstubAllGlobals())
  it.each(['quality', 'qualityType', 'socketCount', 'sockets', 'augments'])(
    'preserves stored %s and blocks crafting before network use',
    async (field) => {
      const state = { ...root, [field]: 1 }
      const fetch = vi.fn()
      vi.stubGlobal('fetch', fetch)
      expect(supportsConcreteStateShape(state)).toBe(false)
      expect(verifiedHistoryState(state, initialFixture)).toBe(false)
      await expect(
        applyCurrency(
          state,
          'TRANSMUTATION',
          initialFixture.modifiers,
          new AbortController().signal,
        ),
      ).rejects.toThrow('unsupported properties')
      expect(fetch).not.toHaveBeenCalled()
      const repository = new LocalFilmRepository(localStorage)
      repository.save(startFilm(emptyFilms(), state, 'future'))
      const bytes = localStorage.getItem(historyStorageKey)
      const loaded = repository.load()
      expect(loaded.films[0]!.frames[0]!.state).toEqual(state)
      expect(localStorage.getItem(historyStorageKey)).toBe(bytes)
    },
  )
  it('rejects extra modifier properties but keeps legacy item/modifier fields valid', () => {
    const state = {
      ...root,
      implicits: root.implicits.map((m) => ({ ...m, socketBound: true })),
    }
    expect(supportsConcreteStateShape(state)).toBe(false)
    expect(verifiedHistoryState(state, initialFixture)).toBe(false)
    expect(supportsConcreteStateShape({ ...root, modifierIds: [] })).toBe(true)
    expect(verifiedHistoryState(root, initialFixture)).toBe(true)
  })
  it('does not accept a server result that projects in unsupported socket state', async () => {
    vi.stubGlobal('fetch', async (_input: unknown, options: RequestInit) => {
      const response = await fixtureFetch(
        '/api/v1/crafting/workbench/apply',
        options,
      )
      const result = await response.json()
      result.state.socketCount = 1
      return new Response(JSON.stringify(result), { status: 200 })
    })
    await expect(
      applyCurrency(
        root,
        'TRANSMUTATION',
        initialFixture.modifiers,
        new AbortController().signal,
      ),
    ).rejects.toThrow()
  })
})
