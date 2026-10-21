import type { AutomaticAction, AutomaticArrow } from './automaticApi'
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
function regalOrder(methods: AutomaticArrow[], cycles: number) {
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
          return a && b && compare(a.lower, b.upper) > 0n
        }),
    )
    const [index] = remaining.splice(next, 1)
    ordered.push(methods[index!]!)
  }
  return ordered
}
export function operationGroups(methods: AutomaticArrow[], cycles = 100) {
  const groups = new Map<string, AutomaticArrow[]>()
  for (const method of methods) {
    const signature = (actions: AutomaticAction[]) =>
      actions.map((a) => [
        a.currency.replace(/^(GREATER_|PERFECT_)/, ''),
        a.purpose,
      ])
    const key =
      operationKind(method) === 'other' &&
      method.cycle.at(-1)?.currency !== 'REGAL'
        ? method.id
        : JSON.stringify([
            method.source,
            signature(method.initial),
            signature(method.cycle),
          ])
    groups.set(key, [...(groups.get(key) ?? []), method])
  }
  return [...groups].map(([id, variants]) => ({
    id,
    variants:
      variants[0]?.cycle.at(-1)?.currency === 'REGAL'
        ? regalOrder(variants, cycles)
        : variants,
  }))
}
