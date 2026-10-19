import { expect, it } from 'vitest'
import { operationGroups, operationKind } from './operationGrouping'
import type { AutomaticArrow } from './automaticApi'
const method = (
  currency: string,
  omens: string[] = [],
  source = 0,
): AutomaticArrow => ({
  id: currency + omens.join('-') + source,
  source,
  initial: [],
  cycle: [{ currency, omens, purpose: 'GOAL_ACQUISITION' }],
  executionMode: 'SINGLE_PASS',
  points: [],
  reasons: [],
  finished: true,
})
it.each(['EXALTED', 'CHAOS'])(
  'groups %s omen variants without changing method identity or probability data',
  (currency) => {
    const original = [
      method(currency),
      method(currency, ['DEXTRAL_ERASURE']),
      method('GREATER_' + currency),
    ]
    const groups = operationGroups(original)
    expect(groups).toHaveLength(1)
    expect(groups[0]!.variants).toEqual(original)
    expect(groups[0]!.variants[1]).toBe(original[1])
  },
)
it('keeps different states, purposes and preparation sequences separate', () => {
  const first = method('CHAOS'),
    second = method('CHAOS', [], 1),
    prepared = {
      ...method('CHAOS'),
      id: 'prepared',
      initial: [
        {
          currency: 'EXALTED',
          omens: [],
          purpose: 'TEMPORARY_AFFIX_FOR_REMOVAL',
        },
      ],
    }
  const different = {
    ...method('CHAOS'),
    id: 'other-purpose',
    cycle: [{ currency: 'CHAOS', omens: [], purpose: 'HIGH_TIER_PREPARATION' }],
  }
  expect(
    operationGroups([first, second, prepared, different, method('REGAL')]),
  ).toHaveLength(5)
  expect(operationKind(first)).toBe('replace')
  expect(operationKind(prepared)).toBe('prepare')
  expect(operationKind(method('EXALTED'))).toBe('add')
})
