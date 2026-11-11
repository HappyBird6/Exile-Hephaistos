import { expect, it, vi } from 'vitest'
import {
  initialFixture,
  jsonResponse,
} from '../../shared/test/craftingFixtures'
import { applyCurrency, concreteInitial } from './workbenchApi'
import type { AppliedItem, ConcreteItem } from './workbenchApi'

const omen = 'Omen_of_Greater_Exaltation'
const before: ConcreteItem = {
  ...concreteInitial(initialFixture),
  rarity: 'RARE',
  explicits: [],
}
const additions = [
  { modifierId: 'p', values: { life: 17 } },
  { modifierId: 's', values: { strength: 6 } },
]
const result: AppliedItem = {
  rulesetIdentity: 'fixture-ruleset',
  ruleVersion: 'solar-workbench-double-exalt-v4',
  ledgerVersion: 'solar-uniform-assumptions-v2',
  snapshotId: before.snapshotId,
  state: { ...before, explicits: additions },
  action: 'EXALTED',
  applied: true,
  reason: '',
  events: additions.map((m) => ({
    ...m,
    kind: 'ADD',
    selectionProbability: 0.1,
  })),
  consumedOmens: [omen],
  remainingOmens: [],
  assumptions: [],
}
const apply = () =>
  applyCurrency(
    before,
    'EXALTED',
    initialFixture.modifiers,
    new AbortController().signal,
    [omen],
    'fixture-ruleset',
  )

it('accepts two verified additions with one consumed Greater Exaltation', async () => {
  vi.stubGlobal('fetch', () =>
    Promise.resolve(
      jsonResponse({ ...result, events: [...result.events].reverse() }),
    ),
  )
  expect((await apply()).state.explicits).toHaveLength(2)
})

it('rejects a partial addition, mismatched event or missing omen consumption', async () => {
  for (const invalid of [
    { ...result, state: { ...before, explicits: additions.slice(0, 1) } },
    { ...result, events: result.events.slice(0, 1) },
    { ...result, consumedOmens: [], remainingOmens: [omen] },
    { ...result, events: [result.events[0], result.events[0]] },
  ]) {
    vi.stubGlobal('fetch', () => Promise.resolve(jsonResponse(invalid)))
    await expect(apply()).rejects.toThrow('Your item is unchanged')
  }
})

it('accepts a directional single-slot fallback, retaining Greater, and rejects wrong-side or consumed-Greater responses', async () => {
  const direction = 'Omen_of_Sinistral_Exaltation'
  const definitions = {
    ...initialFixture.modifiers,
    p2: {
      ...initialFixture.modifiers.p!,
      id: 'p2',
      familyIds: ['second-life'],
    },
    p3: { ...initialFixture.modifiers.p!, id: 'p3', familyIds: ['third-life'] },
  }
  const item = {
    ...before,
    explicits: [additions[0]!, { modifierId: 'p2', values: { life: 17 } }],
  }
  const added = { modifierId: 'p3', values: { life: 17 } }
  const response = {
    ...result,
    state: { ...item, explicits: [...item.explicits, added] },
    events: [{ ...added, kind: 'ADD', selectionProbability: 0.5 }],
    consumedOmens: [direction],
    remainingOmens: [omen],
  }
  const run = () =>
    applyCurrency(
      item,
      'EXALTED',
      definitions,
      new AbortController().signal,
      [omen, direction],
      'fixture-ruleset',
    )
  vi.stubGlobal('fetch', () => Promise.resolve(jsonResponse(response)))
  expect((await run()).remainingOmens).toEqual([omen])
  for (const invalid of [
    { ...response, consumedOmens: [omen, direction], remainingOmens: [] },
    {
      ...response,
      state: { ...item, explicits: [...item.explicits, additions[1]!] },
      events: [{ ...additions[1]!, kind: 'ADD', selectionProbability: 0.5 }],
    },
  ]) {
    vi.stubGlobal('fetch', () => Promise.resolve(jsonResponse(invalid)))
    await expect(run()).rejects.toThrow('Your item is unchanged')
  }
})
