import type { BundleEdge } from './stateProjection'
import { sumBounds } from './outcomeGrouping'
import { methodPoint } from './methodObservation'
const fraction = (f: { numerator: string; denominator: string }) => {
  let n = BigInt(f.numerator),
    d = BigInt(f.denominator)
  const gcd = (a: bigint, b: bigint): bigint => (b ? gcd(b, a % b) : a)
  const g = gcd(n, d)
  n /= g
  d /= g
  return [String(n), String(d)]
}
/** Display an identical certified conditional interval once; never add alternatives. */
export function arrivalProbabilities(edge: BundleEdge, cycles: number) {
  const groups = new Map<
    string,
    {
      method: BundleEdge['variants'][number]['method']
      probability: ReturnType<typeof sumBounds>
      sources: number[]
    }
  >()
  for (const { method, outcomes } of edge.variants.filter((v) =>
    edge.preferred.includes(v.method.id),
  )) {
    const probability = sumBounds(outcomes.map((o) => o.probability))
    const key = JSON.stringify([
      method.initial,
      method.cycle,
      methodPoint(method, cycles)?.cycles,
      fraction(probability.lower),
      fraction(probability.upper),
    ])
    const previous = groups.get(key)
    if (previous) previous.sources.push(method.source)
    else groups.set(key, { method, probability, sources: [method.source] })
  }
  return [...groups.values()]
}
