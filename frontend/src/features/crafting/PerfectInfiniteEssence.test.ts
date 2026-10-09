import { expect, it, vi } from 'vitest'
import {
  initialFixture,
  jsonResponse,
} from '../../shared/test/craftingFixtures'
import { applyCurrency, concreteInitial } from './workbenchApi'
import type { AppliedItem, ConcreteItem } from './workbenchApi'

const targets = ['strength', 'dexterity', 'intelligence'].map(
  (s) => `amulet:suffix:essence-percent-${s}`,
)
const definitions = {
  ...initialFixture.modifiers,
  ...Object.fromEntries(
    targets.map((id, i) => [
      id,
      {
        ...initialFixture.modifiers.s!,
        id,
        familyIds: [`percentage-${i}`],
        stats: [{ id: 'percent', min: 7, max: 10 }],
      },
    ]),
  ),
}
const before: ConcreteItem = {
  ...concreteInitial(initialFixture),
  rarity: 'RARE',
  explicits: [
    { modifierId: 'p', values: { life: 17 }, fractured: true },
    { modifierId: 's', values: { strength: 6 } },
  ],
}
const result: AppliedItem = {
  rulesetIdentity: 'fixture-ruleset',
  ruleVersion: 'solar-workbench-perfect-infinite-v12',
  ledgerVersion: 'solar-uniform-assumptions-v6',
  snapshotId: before.snapshotId,
  action: 'PERFECT_ESSENCE_INFINITE',
  applied: true,
  reason: '',
  state: {
    ...before,
    explicits: [
      { modifierId: targets[0]!, values: { percent: 9 } },
      before.explicits[0]!,
    ],
  },
  events: [
    { kind: 'REMOVE', modifierId: 's', values: {}, selectionProbability: 1 },
    {
      kind: 'ADD',
      modifierId: targets[0]!,
      values: { percent: 9 },
      selectionProbability: 1 / 3,
    },
  ],
  consumedOmens: ['Omen_of_Dextral_Crystallisation'],
  remainingOmens: ['Omen_of_the_Blessed'],
  assumptions: [
    {
      id: 'uniform-removal-v1',
      candidateUnit: 'eligible explicit modifier instance',
      n: 1,
      candidates: ['s'],
      min: null,
      max: null,
      sourceUrl: 'https://poe2db.tw/us/Omen_of_Dextral_Crystallisation',
      reason: 'Uniform removal',
    },
    {
      id: 'uniform-essence-choice-v1',
      candidateUnit: 'fixed essence modifier outcome',
      n: 3,
      candidates: targets,
      min: null,
      max: null,
      sourceUrl: 'https://poe2db.tw/us/Perfect_Essence_of_the_Infinite',
      reason: 'Uniform choice',
    },
  ],
}
const apply = () =>
  applyCurrency(
    before,
    'PERFECT_ESSENCE_INFINITE',
    definitions,
    new AbortController().signal,
    ['Omen_of_Dextral_Crystallisation', 'Omen_of_the_Blessed'],
    'fixture-ruleset',
  )

it('verifies the sourced three-result replacement with restricted removal and exact omen consumption', async () => {
  vi.stubGlobal('fetch', () => Promise.resolve(jsonResponse(result)))
  expect((await apply()).state).toEqual(result.state)
})

it('rejects wrong choice probability or ledger, invalid removal, omitted consumption and changed preserved rolls', async () => {
  for (const invalid of [
    {
      ...result,
      events: [
        result.events[0]!,
        { ...result.events[1]!, selectionProbability: 1 },
      ],
    },
    { ...result, assumptions: [result.assumptions[0]!] },
    {
      ...result,
      assumptions: [
        result.assumptions[0]!,
        {
          ...result.assumptions[1]!,
          candidates: [targets[0]!, targets[0]!, targets[1]!],
        },
      ],
    },
    {
      ...result,
      events: [{ ...result.events[0]!, modifierId: 'p' }, result.events[1]!],
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
          { ...before.explicits[0]!, values: { life: 18 } },
        ],
      },
    },
  ]) {
    vi.stubGlobal('fetch', () => Promise.resolve(jsonResponse(invalid)))
    await expect(apply()).rejects.toThrow('Your item is unchanged')
  }
})
