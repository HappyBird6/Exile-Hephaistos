import { expect, it, vi } from 'vitest'
import {
  initialFixture,
  jsonResponse,
} from '../../shared/test/craftingFixtures'
import { applyCurrency, concreteInitial } from './workbenchApi'
import type { AppliedItem, ConcreteItem } from './workbenchApi'

const target = 'amulet:prefix:alloy-maximum-runic-ward'
const definitions = {
  ...initialFixture.modifiers,
  [target]: {
    ...initialFixture.modifiers.p!,
    id: target,
    sourceUrl:
      'https://poe2db.tw/us/hover?s=Data%5CMods%2FAlloyMaximumRunicWardPercent1',
    familyIds: ['RunicWardPercent'],
    stats: [{ id: 'maximum_ward_+%', min: 6, max: 10 }],
  },
}
const before: ConcreteItem = {
  ...concreteInitial(initialFixture),
  baseItemId: 'Metadata/Items/Amulets/FourAmulet9',
  rarity: 'RARE',
  explicits: [
    { modifierId: 'p', values: { life: 17 } },
    { modifierId: 's', values: { strength: 6 }, fractured: true },
  ],
}
const result: AppliedItem = {
  rulesetIdentity: 'fixture-ruleset',
  ruleVersion: 'solar-workbench-runic-alloy-v15',
  ledgerVersion: 'solar-uniform-assumptions-v9',
  snapshotId: before.snapshotId,
  action: 'RUNIC_ALLOY',
  applied: true,
  reason: '',
  state: {
    ...before,
    explicits: [
      { modifierId: target, values: { 'maximum_ward_+%': 8 } },
      before.explicits[1]!,
    ],
  },
  events: [
    { kind: 'REMOVE', modifierId: 'p', values: {}, selectionProbability: 1 },
    {
      kind: 'ADD',
      modifierId: target,
      values: { 'maximum_ward_+%': 8 },
      selectionProbability: 1,
    },
  ],
  consumedOmens: [],
  remainingOmens: ['Omen_of_Sinistral_Crystallisation', 'Omen_of_the_Blessed'],
  assumptions: [
    {
      id: 'uniform-integer-roll-v1',
      candidateUnit: 'maximum_ward_+%',
      n: 5,
      candidates: [],
      min: 6,
      max: 10,
      sourceUrl:
        'https://poe2db.tw/us/hover?s=Data%5CMods%2FAlloyMaximumRunicWardPercent1',
      reason: 'Uniform source integers',
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
    'RUNIC_ALLOY',
    definitions,
    new AbortController().signal,
    ['Omen_of_Sinistral_Crystallisation', 'Omen_of_the_Blessed'],
    'fixture-ruleset',
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
      consumedOmens: ['Omen_of_Sinistral_Crystallisation'],
      remainingOmens: ['Omen_of_the_Blessed'],
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
