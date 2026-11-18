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
  const visited = new Set([plan.root.source])
  const queue: { method: AutomaticArrow; route: Selection[] }[] = [
    { method: plan.root, route: [] },
  ]
  let fallback: Selection[] = []
  for (let i = 0; i < queue.length; i++) {
    const { method, route } = queue[i]!
    if (route.length >= 64) continue
    const outcomes = [
      ...(methodPoint(method, plan.cycles)?.outcomes ?? []),
    ].sort((a, b) => Number(compare(b.probability.lower, a.probability.lower)))
    for (const outcome of outcomes) {
      if (
        outcome.target === null ||
        BigInt(outcome.probability.upper.numerator) === 0n
      )
        continue
      const path = [
        ...route,
        {
          source: method.source,
          method: method.id,
          target: outcome.target,
          kind: outcome.kind,
        },
      ]
      if (outcome.kind === 'GOAL') return path
      if (path.length > fallback.length) fallback = path
      const next = methods.get(plan.decisions.get(outcome.target) ?? '')
      if (
        outcome.kind === 'CHECKPOINT' &&
        next &&
        !visited.has(outcome.target)
      ) {
        visited.add(outcome.target)
        queue.push({ method: next, route: path })
      }
    }
  }
  return fallback
}
