import { expect, it, vi } from 'vitest'
import {
  initialFixture,
  jsonResponse,
} from '../../shared/test/craftingFixtures'
import { applyCurrency, concreteInitial } from './workbenchApi'
import type { AppliedItem, ConcreteItem } from './workbenchApi'

const target = 'amulet:suffix:of-suturing'
const definitions = {
  ...initialFixture.modifiers,
  [target]: {
    ...initialFixture.modifiers.s!,
    id: target,
    familyIds: ['LifeRecoup'],
    stats: [{ id: 'recoup', min: 19, max: 21 }],
  },
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
  ruleVersion: 'solar-workbench-hysteria-essence-v10',
  ledgerVersion: 'solar-uniform-assumptions-v4',
  snapshotId: before.snapshotId,
  action: 'ESSENCE_HYSTERIA',
  applied: true,
  reason: '',
  state: {
    ...before,
    explicits: [
      { modifierId: target, values: { recoup: 20 } },
      before.explicits[0]!,
    ],
  },
  events: [
    { kind: 'REMOVE', modifierId: 's', values: {}, selectionProbability: 1 },
    {
      kind: 'ADD',
      modifierId: target,
      values: { recoup: 20 },
      selectionProbability: 1,
    },
  ],
  consumedOmens: [],
  remainingOmens: [],
  assumptions: [
    {
      id: 'uniform-removal-v1',
      candidateUnit: 'eligible explicit modifier instance',
      n: 1,
      candidates: ['s'],
      min: null,
      max: null,
      sourceUrl: 'https://poe2db.tw/us/Essence_of_Hysteria',
      reason: 'Uniform removal assumption',
    },
  ],
}
const apply = () =>
  applyCurrency(
    before,
    'ESSENCE_HYSTERIA',
    definitions,
    new AbortController().signal,
    [],
    'fixture-ruleset',
  )

it('accepts the canonical reordered replacement while preserving a fractured roll', async () => {
  vi.stubGlobal('fetch', () => Promise.resolve(jsonResponse(result)))
  expect((await apply()).state.explicits).toEqual(result.state.explicits)
})

it('rejects wrong removal evidence, invented events, missing assumptions and changed old rolls', async () => {
  for (const invalid of [
    { ...result, events: [...result.events].reverse() },
    { ...result, assumptions: [] },
    {
      ...result,
      assumptions: [{ ...result.assumptions[0]!, candidates: ['p'] }],
    },
    {
      ...result,
      events: [
        { ...result.events[0]!, selectionProbability: 0.5 },
        result.events[1]!,
      ],
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
