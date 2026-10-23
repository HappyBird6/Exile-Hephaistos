import type {
  AutomaticAction,
  AutomaticArrow,
  AutomaticView,
} from './automaticApi'
import { methodPoint } from './methodObservation'
import { sumBounds } from './outcomeGrouping'
export function operationKind(method: AutomaticArrow) {
  const currency = method.cycle.at(-1)?.currency ?? ''
  if (/^(GREATER_|PERFECT_)?EXALTED$/.test(currency)) return 'add'
  if (/^(GREATER_|PERFECT_)?CHAOS$/.test(currency))
    return method.initial.length ? 'prepare' : 'replace'
  return 'other'
}
// Compare completed whole methods, including Transmutation before Regal. Never
// use rounded labels or a conditional probability for the final action alone.
export function additionOrder(
  methods: AutomaticArrow[],
  cycles: number,
  recommendations: AutomaticView['recommendations'] = [],
) {
  const preferred = new Set(
    recommendations.flatMap((r) =>
      r.cycles === cycles && r.strategyStatus === 'ACTION_CERTIFIED'
        ? r.strategyMethods
        : [],
    ),
  )
  const scores = methods.map((method) => {
    const point = methodPoint(method, cycles)
    if (
      !point?.complete ||
      point.outcomes.some((o) => ['UNRESOLVED', 'ACTIVE'].includes(o.kind))
    )
      return null
    return sumBounds(
      point.outcomes.filter((o) => o.kind === 'GOAL').map((o) => o.probability),
    )
  })
  const compare = (
    a: { numerator: string; denominator: string },
    b: { numerator: string; denominator: string },
  ) =>
    BigInt(a.numerator) * BigInt(b.denominator) -
    BigInt(b.numerator) * BigInt(a.denominator)
  // A stable topological order only promotes a method when its lower bound is
  // above the other's upper bound. Overlapping or unfinished laws stay choices.
  const remaining = methods.map((_, i) => i),
    ordered: AutomaticArrow[] = []
  while (remaining.length) {
    const next = remaining.findIndex(
      (i) =>
        !remaining.some((j) => {
          const a = scores[j],
            b = scores[i]
          const left = methods[j]!,
            right = methods[i]!
          const lp = methodPoint(left, cycles),
            rp = methodPoint(right, cycles)
          const law = (p: NonNullable<typeof lp>) =>
            JSON.stringify(
              p.outcomes
                .map((o) => [
                  o.target,
                  o.kind,
                  o.probability.lower,
                  o.probability.upper,
                ])
                .sort((x, y) =>
                  JSON.stringify(x).localeCompare(JSON.stringify(y)),
                ),
            )
          const plain = (m: AutomaticArrow) =>
            m.initial.concat(m.cycle).every((a) => !a.omens.length)
          // Complete identical laws imply identical successor behavior. Apply this
          // before recommendation tie-breaking so the relation stays acyclic.
          if (
            a &&
            b &&
            lp &&
            rp &&
            law(lp) === law(rp) &&
            plain(left) !== plain(right)
          )
            return plain(left)
          if (preferred.has(left.id) !== preferred.has(right.id))
            return preferred.has(left.id)
          return !!(a && b && compare(a.lower, b.upper) > 0n)
        }),
    )
    const [index] = remaining.splice(next, 1)
    ordered.push(methods[index!]!)
  }
  return ordered
}
export function operationSignature(method: AutomaticArrow) {
  const signature = (actions: AutomaticAction[]) =>
    actions.map((a) => [
      a.currency.replace(/^(GREATER_|PERFECT_)/, ''),
      a.purpose,
    ])
  return operationKind(method) === 'other' &&
    method.cycle.at(-1)?.currency !== 'REGAL'
    ? method.id
    : JSON.stringify([signature(method.initial), signature(method.cycle)])
}
export function operationGroups(
  methods: AutomaticArrow[],
  cycles = 100,
  recommendations: AutomaticView['recommendations'] = [],
) {
  const groups = new Map<string, AutomaticArrow[]>()
  for (const method of methods) {
    const key = JSON.stringify([method.source, operationSignature(method)])
    groups.set(key, [...(groups.get(key) ?? []), method])
  }
  return [...groups].map(([id, variants]) => ({
    id,
    variants: /^(GREATER_|PERFECT_)?(REGAL|EXALTED)$/.test(
      variants[0]?.cycle.at(-1)?.currency ?? '',
    )
      ? additionOrder(variants, cycles, recommendations)
      : variants,
  }))
}
