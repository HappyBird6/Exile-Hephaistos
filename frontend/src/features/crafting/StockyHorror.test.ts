import { expect, it, vi } from 'vitest'
import {
  initialFixture,
  jsonResponse,
} from '../../shared/test/craftingFixtures'
import { applyCurrency, concreteInitial } from './workbenchApi'
import type { AppliedItem, ConcreteItem } from './workbenchApi'

const target = 'stocky-mitts:suffix:essence-socketed-augment-effect'
const definitions = {
  ...initialFixture.modifiers,
  [target]: {
    ...initialFixture.modifiers.s!,
    id: target,
    familyIds: ['SoulCore'],
    stats: [{ id: 'socketEffect', min: 60, max: 60 }],
  },
}
const before: ConcreteItem = {
  ...concreteInitial(initialFixture),
  baseItemId: 'Metadata/Items/Armours/Gloves/FourGlovesStr1',
  implicits: [],
  rarity: 'RARE',
  explicits: [
    { modifierId: 'p', values: { life: 17 }, fractured: true },
    { modifierId: 's', values: { strength: 6 } },
  ],
}
const result: AppliedItem = {
  ruleVersion: 'stocky-workbench-horror-v21',
  ledgerVersion: 'stocky-unverified-numeric-assumptions-v11',
  snapshotId: before.snapshotId,
  action: 'ESSENCE_HORROR',
  applied: true,
  reason: '',
  state: {
    ...before,
    explicits: [
      { modifierId: target, values: { socketEffect: 60 } },
      before.explicits[0]!,
    ],
  },
  events: [
    { kind: 'REMOVE', modifierId: 's', values: {}, selectionProbability: 1 },
    {
      kind: 'ADD',
      modifierId: target,
      values: { socketEffect: 60 },
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
      sourceUrl: 'https://poe2db.tw/us/Essence_of_Horror',
      reason: 'Uniform removal assumption',
    },
  ],
}
const apply = () =>
  applyCurrency(
    before,
    'ESSENCE_HORROR',
    definitions,
    new AbortController().signal,
  )

it('rejects percent unit mistakes and an applied Horror result on Solar', async () => {
  for (const value of [0.6, 59, 61]) {
    const wrong = structuredClone(result)
    wrong.state.explicits[0]!.values = { socketEffect: value }
    wrong.events[1]!.values = { socketEffect: value }
    vi.stubGlobal('fetch', () => Promise.resolve(jsonResponse(wrong)))
    await expect(apply()).rejects.toThrow('Your item is unchanged')
  }
  const solar = { ...before, baseItemId: initialFixture.state.baseItemId }
  vi.stubGlobal('fetch', () =>
    Promise.resolve(
      jsonResponse({
        ...result,
        state: { ...result.state, baseItemId: solar.baseItemId },
      }),
    ),
  )
  await expect(
    applyCurrency(
      solar,
      result.action,
      definitions,
      new AbortController().signal,
    ),
  ).rejects.toThrow('Your item is unchanged')
})

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

it('consumes only matching Crystallisation and rejects a substituted Solar target', async () => {
  const omens = ['Omen_of_Dextral_Crystallisation', 'Omen_of_the_Blessed']
  const actual = {
    ...result,
    consumedOmens: [omens[0]!],
    remainingOmens: [omens[1]!],
  }
  vi.stubGlobal('fetch', () => Promise.resolve(jsonResponse(actual)))
  expect(
    (
      await applyCurrency(
        before,
        actual.action,
        definitions,
        new AbortController().signal,
        omens,
      )
    ).consumedOmens,
  ).toEqual([omens[0]])
  const solarId = 'amulet:suffix:of-suturing'
  const invalid = structuredClone(actual)
  invalid.state.explicits[0]!.modifierId = solarId
  invalid.events[1]!.modifierId = solarId
  const both = {
    ...definitions,
    [solarId]: { ...definitions[target], id: solarId },
  }
  vi.stubGlobal('fetch', () => Promise.resolve(jsonResponse(invalid)))
  await expect(
    applyCurrency(
      before,
      actual.action,
      both,
      new AbortController().signal,
      omens,
    ),
  ).rejects.toThrow('Your item is unchanged')
  expect(before.explicits[0]?.fractured).toBe(true)
})
