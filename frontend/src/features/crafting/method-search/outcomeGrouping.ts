import type { Selection } from './supportStorage'
import type { AutomaticArrow, AutomaticView } from './automaticApi'
import type { Bounds } from './types'
import { methodPoint } from './methodObservation'
export type OutcomeGroup = {
  id: string
  method: AutomaticArrow
  point: AutomaticArrow['points'][number]
}
export function exaltedGroups(
  view: AutomaticView,
  cycles: number,
): OutcomeGroup[] {
  return view.methods.flatMap((method) => {
    const point = methodPoint(method, cycles)
    if (
      !point ||
      method.cycle.length !== 1 ||
      method.initial.some((a) => a.currency.includes('ESSENCE')) ||
      !/^(GREATER_|PERFECT_)?EXALTED$/.test(method.cycle[0]!.currency)
    )
      return []
    const targets = new Set(
      point.outcomes.filter((o) => o.target !== null).map((o) => o.target),
    )
    return targets.size > 1 ? [{ id: `group:${method.id}`, method, point }] : []
  })
}
const gcd = (a: bigint, b: bigint): bigint => (b === 0n ? a : gcd(b, a % b))
export function sumBounds(values: Bounds[]): Bounds {
  const add = (key: 'lower' | 'upper') => {
    let n = 0n,
      d = 1n
    for (const value of values) {
      const f = value[key]
      const x = BigInt(f.numerator),
        y = BigInt(f.denominator)
      n = n * y + x * d
      d *= y
      const g = gcd(n, d)
      n /= g
      d /= g
    }
    return { numerator: String(n), denominator: String(d) }
  }
  const lower = add('lower'),
    upper = add('upper')
  const n =
    BigInt(upper.numerator) * BigInt(lower.denominator) -
    BigInt(lower.numerator) * BigInt(upper.denominator)
  const d = BigInt(upper.denominator) * BigInt(lower.denominator),
    g = gcd(n, d)
  return {
    lower,
    upper,
    exact: values.every((v) => v.exact),
    confirmedPercent: null,
    arithmeticWidth: { numerator: String(n / g), denominator: String(d / g) },
  }
}
export function groupMass(group: OutcomeGroup) {
  const seen = new Set<string>(),
    byKind = new Map<string, Bounds[]>()
  for (const o of group.point.outcomes) {
    const key = `${o.target}:${o.kind}`
    if (seen.has(key)) throw new Error('Duplicate method outcome')
    seen.add(key)
    byKind.set(o.kind, [...(byKind.get(o.kind) ?? []), o.probability])
  }
  return [...byKind].map(([kind, values]) => ({
    kind,
    probability: sumBounds(values),
  }))
}
export function graphProjection(
  view: AutomaticView,
  cycles: number,
  expanded: ReadonlySet<string>,
  route: Selection[] = [],
) {
  const groups = exaltedGroups(view, cycles),
    byMethod = new Map(groups.map((g) => [g.method.id, g]))
  const nodes = new Set<number>([0]),
    visibleGroups = new Set<string>()
  const edges: {
    from: string
    to: string
    method: AutomaticArrow
    target: number | null
    kind: string
  }[] = []
  const queue = [0]
  for (let index = 0; index < queue.length; index++) {
    const source = queue[index]!
    for (const method of view.methods.filter((m) => m.source === source)) {
      const point = methodPoint(method, cycles)
      if (!point) continue
      const group = byMethod.get(method.id)
      if (group) {
        visibleGroups.add(group.id)
        edges.push({
          from: `state:${source}`,
          to: group.id,
          method,
          target: null,
          kind: 'GROUP',
        })
      }
      for (const outcome of point.outcomes) {
        if (
          group &&
          !expanded.has(group.id) &&
          !route.some(
            (e) =>
              e.method === method.id &&
              e.target === outcome.target &&
              e.kind === outcome.kind,
          )
        )
          continue
        if (outcome.target === null || outcome.kind === 'DEAD') continue
        edges.push({
          from: group?.id ?? `state:${source}`,
          to: `state:${outcome.target}`,
          method,
          target: outcome.target,
          kind: outcome.kind,
        })
        if (!nodes.has(outcome.target)) {
          nodes.add(outcome.target)
          queue.push(outcome.target)
        }
      }
    }
  }
  return {
    nodes: view.nodes.filter((n) => nodes.has(n.id)),
    groups: groups.filter((g) => visibleGroups.has(g.id)),
    edges,
  }
}
