import { expect, it, vi } from 'vitest'
import {
  initialFixture,
  jsonResponse,
} from '../../shared/test/craftingFixtures'
import { applyCurrency, concreteInitial } from './workbenchApi'
import type { AppliedItem, ConcreteItem } from './workbenchApi'

const target = 'amulet:prefix:essence-maximum-quality'
const definitions = {
  ...initialFixture.modifiers,
  [target]: {
    ...initialFixture.modifiers.p!,
    id: target,
    familyIds: ['LocalMaximumQuality'],
    stats: [{ id: 'maximum_quality', min: 20, max: 20 }],
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
  ruleVersion: 'solar-workbench-breach-essence-v14',
  ledgerVersion: 'solar-uniform-assumptions-v8',
  snapshotId: before.snapshotId,
  action: 'ESSENCE_BREACH',
  applied: true,
  reason: '',
  state: {
    ...before,
    explicits: [
      { modifierId: target, values: { maximum_quality: 20 } },
      before.explicits[1]!,
    ],
  },
  events: [
    { kind: 'REMOVE', modifierId: 'p', values: {}, selectionProbability: 1 },
    {
      kind: 'ADD',
      modifierId: target,
      values: { maximum_quality: 20 },
      selectionProbability: 1,
    },
  ],
  consumedOmens: ['Omen_of_Sinistral_Crystallisation'],
  remainingOmens: ['Omen_of_the_Blessed'],
  assumptions: [
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
    'ESSENCE_BREACH',
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
      assumptions: [{ ...result.assumptions[0]!, candidates: ['s'] }],
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
