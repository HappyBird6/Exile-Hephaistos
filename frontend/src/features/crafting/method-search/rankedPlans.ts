import type { Selection } from './supportStorage'
import { compare } from '../path-tree/validation'
import type { AutomaticView, AutomaticArrow } from './automaticApi'
import type { Bounds } from './types'
import type { PolicyCandidate } from './policyCandidates'
import { methodPoint } from './methodObservation'
import { sumBounds } from './outcomeGrouping'
export type Plan = {
  id: string
  root: AutomaticArrow
  success?: Bounds | undefined
  cycles: number
  decisions: Map<number, string>
  candidate?: PolicyCandidate
}
export function rankedPlans(view: AutomaticView, cycles: number) {
  const methods = new Map(view.methods.map((m) => [m.id, m]))
  const plans: Plan[] = []
  for (const c of view.candidates ?? []) {
    const decisions = new Map(c.decisions.map((d) => [d.node, d.method]))
    const root = methods.get(decisions.get(0) ?? '')
    if (root)
      plans.push({
        id: c.id,
        root,
        success: c.success,
        cycles: c.cycles,
        decisions,
        candidate: c,
      })
  }
  // Frozen jobs predate policy projections. Their scores cover the stored method only.
  if (view.candidates === undefined)
    for (const root of view.methods.filter((m) => m.source === 0)) {
      const point = methodPoint(root, cycles)
      const terminal =
        point?.complete &&
        point.outcomes.length > 0 &&
        point.outcomes.every((o) =>
          ['GOAL', 'REBASE', 'DEAD', 'LOSS'].includes(o.kind),
        )
      plans.push({
        id: root.id,
        root,
        cycles,
        decisions: new Map([[0, root.id]]),
        success: terminal
          ? sumBounds(
              point.outcomes
                .filter((o) => o.kind === 'GOAL')
                .map((o) => o.probability),
            )
          : undefined,
      })
    }
  return plans.sort((a, b) => {
    if (!a.success || !b.success)
      return (
        Number(!!b.success) - Number(!!a.success) || a.id.localeCompare(b.id)
      )
    const difference =
      compare(b.success.lower, a.success.lower) ||
      compare(b.success.upper, a.success.upper)
    return difference > 0n ? 1 : difference < 0n ? -1 : a.id.localeCompare(b.id)
  })
}

/** A selectable example from this policy's observations. Its branch odds do not rank the policy. */
export function previewRoute(view: AutomaticView, plan: Plan) {
  const methods = new Map(view.methods.map((method) => [method.id, method]))
  const visited = new Set<number>(),
    route: Selection[] = []
  let method: AutomaticArrow | undefined = plan.root
  while (method && route.length < 64 && !visited.has(method.source)) {
    visited.add(method.source)
    const outcomes = methodPoint(method, plan.cycles)?.outcomes ?? []
    const priority = (kind: string) =>
      kind === 'GOAL' ? 2 : kind === 'CHECKPOINT' ? 1 : 0
    let best: (typeof outcomes)[number] | undefined
    for (const o of outcomes) {
      if (o.target === null || visited.has(o.target)) continue
      if (
        !best ||
        priority(o.kind) > priority(best.kind) ||
        (priority(o.kind) === priority(best.kind) &&
          compare(o.probability.lower, best.probability.lower) > 0n)
      )
        best = o
    }
    if (!best || best.target === null) break
    route.push({
      source: method.source,
      method: method.id,
      kind: best.kind,
      target: best.target,
    })
    if (best.kind !== 'CHECKPOINT') break
    method = methods.get(plan.decisions.get(best.target) ?? '')
  }
  return route
}
