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
