import { expect, it, vi } from 'vitest'
import {
  initialFixture,
  jsonResponse,
} from '../../shared/test/craftingFixtures'
import { applyCurrency, concreteInitial } from './workbenchApi'
import type { AppliedItem, ConcreteItem } from './workbenchApi'

const target = 'amulet:prefix:essence-abyssal-mark'
const definitions = {
  ...initialFixture.modifiers,
  'amulet:suffix:essence-abyssal-mark': {
    ...initialFixture.modifiers.s!,
    id: 'amulet:suffix:essence-abyssal-mark',
    familyIds: ['EssenceAbyss'],
    stats: [{ id: 'essence_abyss_guaranteed_pick', min: 1, max: 1 }],
  },
  [target]: {
    ...initialFixture.modifiers.p!,
    id: target,
    familyIds: ['EssenceAbyss'],
    stats: [{ id: 'essence_abyss_guaranteed_pick', min: 1, max: 1 }],
  },
}
const before: ConcreteItem = {
  ...concreteInitial(initialFixture),
  rarity: 'RARE',
  explicits: [
    { modifierId: 'p', values: { life: 17 } },
    { modifierId: 's', values: { strength: 6 }, fractured: true },
  ],
}
const result: AppliedItem = {
  ruleVersion: 'solar-workbench-abyss-essence-v16',
  ledgerVersion: 'solar-uniform-assumptions-v10',
  snapshotId: before.snapshotId,
  action: 'ESSENCE_ABYSS',
  applied: true,
  reason: '',
  state: {
    ...before,
    explicits: [
      { modifierId: target, values: { essence_abyss_guaranteed_pick: 1 } },
      before.explicits[1]!,
    ],
  },
  events: [
    { kind: 'REMOVE', modifierId: 'p', values: {}, selectionProbability: 1 },
    {
      kind: 'ADD',
      modifierId: target,
      values: { essence_abyss_guaranteed_pick: 1 },
      selectionProbability: 1 / 2,
    },
  ],
  consumedOmens: ['Omen_of_Sinistral_Crystallisation'],
  remainingOmens: ['Omen_of_the_Blessed'],
  assumptions: [
    {
      id: 'uniform-essence-choice-v1',
      candidateUnit: 'fixed essence modifier outcome',
      n: 2,
      candidates: [target, 'amulet:suffix:essence-abyssal-mark'],
      min: null,
      max: null,
      sourceUrl: 'https://poe2db.tw/us/Essence_of_the_Abyss',
      reason: 'Uniform among sourced affixes',
    },
    {
      id: 'uniform-removal-v1',
      candidateUnit: 'eligible explicit modifier instance',
      n: 1,
      candidates: ['p'],
      min: null,
      max: null,
      sourceUrl: 'https://poe2db.tw/us/Omen_of_Sinistral_Crystallisation',
      reason: 'Uniform removal',
    },
  ],
}
const apply = () =>
  applyCurrency(
    before,
    'ESSENCE_ABYSS',
    definitions,
    new AbortController().signal,
    ['Omen_of_Sinistral_Crystallisation', 'Omen_of_the_Blessed'],
  )

it('accepts a guaranteed prefix replacement with exact preservation and matching omen consumption', async () => {
  vi.stubGlobal('fetch', () => Promise.resolve(jsonResponse(result)))
  expect((await apply()).state).toEqual(result.state)
})

it('rejects invented choices, wrong removal ledger, changed fracture and missing omen consumption', async () => {
  for (const invalid of [
    {
      ...result,
      events: [
        result.events[0]!,
        { ...result.events[1]!, selectionProbability: 1 / 3 },
      ],
    },
    {
      ...result,
      events: [{ ...result.events[0]!, modifierId: 's' }, result.events[1]!],
    },
    {
      ...result,
      assumptions: [
        result.assumptions[0]!,
        { ...result.assumptions[1]!, candidates: ['s'] },
      ],
    },
    {
      ...result,
      consumedOmens: [],
      remainingOmens: [...result.consumedOmens, ...result.remainingOmens],
    },
    {
      ...result,
      state: {
        ...result.state,
        explicits: [
          result.state.explicits[0]!,
          { ...before.explicits[1]!, values: { strength: 7 } },
        ],
      },
    },
  ]) {
    vi.stubGlobal('fetch', () => Promise.resolve(jsonResponse(invalid)))
    await expect(apply()).rejects.toThrow('Your item is unchanged')
  }
})
