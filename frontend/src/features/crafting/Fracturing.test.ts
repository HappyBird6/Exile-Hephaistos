import { describe, expect, it, vi } from 'vitest'
import {
  initialFixture,
  jsonResponse,
} from '../../shared/test/craftingFixtures'
import { applyCurrency, concreteInitial } from './workbenchApi'
import type { AppliedItem, ConcreteItem } from './workbenchApi'
import {
  LocalFilmRepository,
  startFilm,
  emptyFilms,
  recordCraft,
  currentFrame,
  verifiedHistoryState,
} from './workbenchHistory'

const initial = {
  ...initialFixture,
  modifiers: {
    ...initialFixture.modifiers,
    p2: {
      ...initialFixture.modifiers.p!,
      id: 'p2',
      familyIds: ['OtherPrefix'],
    },
    s2: {
      ...initialFixture.modifiers.s!,
      id: 's2',
      familyIds: ['OtherSuffix'],
    },
  },
}
const rare: ConcreteItem = {
  ...concreteInitial(initial),
  rarity: 'RARE',
  explicits: [
    { modifierId: 'p', values: { life: 17 } },
    { modifierId: 'p2', values: { life: 19 } },
    { modifierId: 's', values: { strength: 6 } },
    { modifierId: 's2', values: { strength: 8 } },
  ],
}
const fractured: ConcreteItem = {
  ...rare,
  explicits: rare.explicits.map((m, i) => ({ ...m, fractured: i === 0 })),
}
function result(
  state: ConcreteItem,
  action: AppliedItem['action'] = 'FRACTURING',
): AppliedItem {
  return {
    rulesetIdentity: 'fixture-ruleset',
    ruleVersion: 'solar-workbench-fracture-v3',
    ledgerVersion: 'solar-uniform-assumptions-v1',
    snapshotId: rare.snapshotId,
    state,
    action,
    applied: true,
    reason: '',
    consumedOmens: [],
    remainingOmens: [],
    assumptions: [],
    events:
      action === 'FRACTURING'
        ? [
            {
              kind: 'FRACTURE',
              modifierId: 'p',
              values: { life: 17 },
              selectionProbability: 0.25,
            },
          ]
        : [],
  }
}
const apply = (state: ConcreteItem, action: AppliedItem['action']) =>
  applyCurrency(
    state,
    action,
    initial.modifiers,
    new AbortController().signal,
    [],
    'fixture-ruleset',
  )
describe('Fracturing contract and stored films', () => {
  it('accepts exactly one unchanged locked instance and restores it from history', async () => {
    vi.stubGlobal('fetch', () =>
      Promise.resolve(jsonResponse(result(fractured))),
    )
    const applied = await apply(rare, 'FRACTURING')
    expect(applied.state.explicits[0]?.fractured).toBe(true)
    const films = recordCraft(
      startFilm(emptyFilms(), rare, 'root', 'fixture-ruleset'),
      rare,
      applied,
      'unused',
    )
    const repository = new LocalFilmRepository(localStorage)
    repository.save(films)
    const restored = currentFrame(repository.load())!.state
    expect(restored).toEqual(fractured)
    expect(verifiedHistoryState(restored, initial)).toBe(true)
  })
  it('rejects altered or removed locks and invented additional fractures from ordinary currency', async () => {
    for (const next of [
      {
        ...fractured,
        explicits: fractured.explicits.map((m, i) =>
          i === 0 ? { ...m, values: { life: 18 } } : m,
        ),
      },
      {
        ...fractured,
        explicits: fractured.explicits.map((m) => ({ ...m, fractured: false })),
      },
      {
        ...fractured,
        explicits: fractured.explicits.map((m, i) => ({
          ...m,
          fractured: i < 2,
        })),
      },
    ]) {
      vi.stubGlobal('fetch', () =>
        Promise.resolve(jsonResponse(result(next, 'DIVINE'))),
      )
      await expect(apply(fractured, 'DIVINE')).rejects.toThrow()
    }
  })
  it('rejects mismatched fracture evidence and multiple stored locks', async () => {
    vi.stubGlobal('fetch', () =>
      Promise.resolve(
        jsonResponse({
          ...result(fractured),
          events: [
            {
              kind: 'FRACTURE',
              modifierId: 's',
              values: { strength: 6 },
              selectionProbability: 0.25,
            },
          ],
        }),
      ),
    )
    await expect(apply(rare, 'FRACTURING')).rejects.toThrow()
    expect(
      verifiedHistoryState(
        {
          ...fractured,
          explicits: fractured.explicits.map((m) => ({
            ...m,
            fractured: true,
          })),
        },
        initial,
      ),
    ).toBe(false)
  })
})
