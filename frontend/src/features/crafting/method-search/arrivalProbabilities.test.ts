import { expect, it } from 'vitest'
import { arrivalProbabilities } from './arrivalProbabilities'
import type { BundleEdge } from './stateProjection'
import type { AutomaticArrow } from './automaticApi'
const bounds = (n: string, d = '10000') => ({
  lower: { numerator: n, denominator: d },
  upper: { numerator: n, denominator: d },
  arithmeticWidth: { numerator: '0', denominator: '1' },
  exact: true,
  confirmedPercent: null,
})
const edge = (n: string, d = '10000'): BundleEdge => {
  const action = {
    currency: 'EXALTED',
    omens: ['DEXTRAL_EXALTATION'],
    purpose: 'GOAL_ACQUISITION',
  }
  const method: AutomaticArrow = {
    id: 'one',
    source: 4,
    initial: [],
    cycle: [action],
    executionMode: 'SINGLE_PASS',
    reasons: [],
    finished: true,
    points: [
      {
        cycles: 1,
        complete: true,
        outcomes: [],
        initialUses: {},
        cycleUses: {},
      },
    ],
  }
  return {
    id: 'edge',
    from: 'from',
    to: 'to',
    preferred: ['one', 'two'],
    variants: [
      {
        method,
        outcomes: [{ target: 10, kind: 'GOAL', probability: bounds('89') }],
      },
      {
        method: { ...method, id: 'two', source: 6 },
        outcomes: [{ target: 12, kind: 'GOAL', probability: bounds(n, d) }],
      },
    ],
  }
}
it('displays equal exact conditional chances once without adding starting-item probabilities', () => {
  const rows = arrivalProbabilities(edge('178', '20000'), 100)
  expect(rows).toHaveLength(1)
  expect(rows[0]!.sources).toEqual([4, 6])
  expect(rows[0]!.probability.lower).toEqual({
    numerator: '89',
    denominator: '10000',
  })
})
it('does not group different exact chances merely because both round to 0.89 percent', () =>
  expect(arrivalProbabilities(edge('8901', '1000000'), 100)).toHaveLength(2))
