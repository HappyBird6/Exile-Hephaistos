import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  initialFixture,
  jsonResponse,
} from '../../shared/test/craftingFixtures'
import { applyCurrency, concreteInitial } from './workbenchApi'
import type { AppliedItem, ConcreteItem } from './workbenchApi'
import { supportsConcreteStateShape } from './workbenchStateShape'
import {
  LocalFilmRepository,
  emptyFilms,
  startFilm,
  recordCraft,
  viewFrame,
  verifiedHistoryState,
  historyStorageKey,
} from './workbenchHistory'
const base = 'Metadata/Items/Armours/Gloves/FourGlovesStr1'
const initial = {
  ...initialFixture,
  state: { ...initialFixture.state, baseItemId: base, implicits: [] },
  augmentSockets: 0,
}
const root = concreteInitial(initial)
const result = (before: ConcreteItem, applied = true): AppliedItem => ({
  rulesetIdentity: 'fixture-ruleset',
  snapshotId: before.snapshotId,
  state: { ...before, augmentSockets: applied ? 1 : before.augmentSockets },
  action: 'ARTIFICER',
  applied,
  reason: applied ? '' : 'Socket count unknown or ordinary maximum reached.',
  ruleVersion: 'stocky-workbench-artificer-v26',
  ledgerVersion: 'stocky-unverified-numeric-assumptions-v11',
  events: [],
  assumptions: [],
  consumedOmens: [],
  remainingOmens: [],
})
const apply = (before: ConcreteItem, r: AppliedItem) => {
  vi.stubGlobal('fetch', async () => jsonResponse(r))
  return applyCurrency(
    before,
    'ARTIFICER',
    initial.modifiers,
    new AbortController().signal,
    [],
    'fixture-ruleset',
  )
}
describe('ordinary Stocky Artificer boundary', () => {
  afterEach(() => vi.unstubAllGlobals())
  it('fresh explicit zero does not invent sockets for a legacy initial', () => {
    expect(root.augmentSockets).toBe(0)
    expect(
      concreteInitial({ ...initial, augmentSockets: undefined }).augmentSockets,
    ).toBeUndefined()
    expect(verifiedHistoryState(root, initial)).toBe(true)
    expect(
      verifiedHistoryState({ ...root, augmentSockets: undefined }, initial),
    ).toBe(true)
  })
  it('accepts deterministic zero to one with unchanged affixes', async () => {
    expect((await apply(root, result(root))).state.augmentSockets).toBe(1)
  })
  it.each([undefined, 1])(
    'accepts refusal for unknown or full sockets %s',
    async (n) => {
      const state = { ...root, augmentSockets: n },
        r = result(state, false)
      expect((await apply(state, r)).applied).toBe(false)
    },
  )
  it('rejects forged changes and extra or exceptional socket state', async () => {
    for (const n of [-1, 2, 1.5, '1'])
      expect(supportsConcreteStateShape({ ...root, augmentSockets: n })).toBe(
        false,
      )
    expect(supportsConcreteStateShape({ ...root, sockets: [] })).toBe(false)
    const r = result(root)
    r.state.rarity = 'MAGIC'
    await expect(apply(root, r)).rejects.toThrow()
    await expect(
      apply(
        { ...root, augmentSockets: undefined },
        result({ ...root, augmentSockets: undefined }),
      ),
    ).rejects.toThrow()
  })
  it('rejects a normal craft that erases known sockets', async () => {
    const before = { ...root, augmentSockets: 1 },
      r = {
        ...result(before),
        action: 'ANNULMENT',
        applied: false,
        state: { ...before, augmentSockets: null },
      } as AppliedItem
    vi.stubGlobal('fetch', async () => jsonResponse(r))
    await expect(
      applyCurrency(
        before,
        'ANNULMENT',
        initial.modifiers,
        new AbortController().signal,
        [],
        'fixture-ruleset',
      ),
    ).rejects.toThrow('Augment Sockets')
  })
  it('restores sockets and preserves the original future when crafting from zero', () => {
    const repo = new LocalFilmRepository(localStorage),
      r = result(root)
    const original = recordCraft(
      startFilm(emptyFilms(), root, 'source', 'fixture-ruleset'),
      root,
      r,
      'unused',
    )
    const bytes = JSON.stringify(original.films[0])
    const branched = recordCraft(viewFrame(original, 0), root, r, 'branch')
    repo.save(branched)
    const saved = localStorage.getItem(historyStorageKey),
      loaded = repo.load()
    expect(loaded.films).toHaveLength(2)
    expect(JSON.stringify(loaded.films[0])).toBe(bytes)
    expect(loaded.films[1]!.frames[1]!.state.augmentSockets).toBe(1)
    expect(localStorage.getItem(historyStorageKey)).toBe(saved)
  })
})
