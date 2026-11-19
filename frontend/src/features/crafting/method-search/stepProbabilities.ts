import type { AutomaticArrow } from './automaticApi'
import type { ProbabilityStep } from './RouteProbability'
import type { Fraction } from '../path-tree/types'
import type { Frame } from '../workbenchHistory'

const one = { numerator: '1', denominator: '1' }
function divideChance(joint: Fraction, conditional: Fraction) {
  const n = BigInt(joint.numerator) * BigInt(conditional.denominator)
  const d = BigInt(joint.denominator) * BigInt(conditional.numerator)
  // An outward-rounded certified upper endpoint can exceed one after division.
  return n >= d ? one : { numerator: String(n), denominator: String(d) }
}

export function stepProbabilities(
  method: AutomaticArrow,
  point: AutomaticArrow['points'][number],
  outcome: AutomaticArrow['points'][number]['outcomes'][number],
  frames: Frame[],
  first: number,
  labels: string[],
): ProbabilityStep[] {
  const last = first + frames.length - 1
  const firstChaos =
    frames
      .at(-1)
      ?.projectedCurrencies?.some((c) => c.expectedUses !== undefined) ?? false
  const fixed = (index: number, chance: Fraction): ProbabilityStep => ({
    first: first + index,
    last: first + index,
    label: labels[index]!,
    probability: { lower: chance, upper: chance },
  })
  if (
    firstChaos &&
    point.complete &&
    method.cycle.length === 1 &&
    frames.length === method.initial.length + 1 &&
    point.outcomes.every(
      (o) =>
        o.conditional?.length &&
        o.conditional.every((c) => c.expectedChaos && !c.preparationOnly),
    )
  ) {
    // Every outcome reaches the lossless Chaos stage, irrespective of preparation rolls.
    return [
      ...method.initial.map((_, index) => fixed(index, one)),
      {
        first: last,
        last,
        label: labels.at(-1)!,
        probability: outcome.probability,
        firstChaos,
      },
    ]
  }
  // A single terminal case with one certified follower law can be factored without
  // double counting distinct rolls that share a preparation state.
  const tail =
    outcome.conditional?.length === 1
      ? outcome.conditional[0]?.finalStepChance
      : undefined
  if (tail && frames.length === 2 && method.executionMode === 'SINGLE_PASS') {
    return [
      {
        first,
        last: first,
        label: labels[0]!,
        probability: {
          lower: divideChance(outcome.probability.lower, tail),
          upper: divideChance(outcome.probability.upper, tail),
        },
      },
      fixed(1, tail),
    ]
  }
  const grouped =
    frames.length === 1 && (frames[0]?.projectedCurrencies?.length ?? 0) > 1
  return [
    {
      first,
      last,
      label: (grouped ? labels : labels.slice(0, frames.length)).join(' → '),
      probability: outcome.probability,
      firstChaos,
    },
  ]
}
