import type {
  AutomaticArrow,
  AutomaticNode,
  AutomaticView,
} from './automaticApi'
import { methodPoint } from './methodObservation'
import { canonical } from '../path-tree/validation'

export type StateBundle = { id: string; depth: number; nodes: AutomaticNode[] }
export type BundleEdge = {
  id: string
  from: string
  to: string
  variants: {
    method: AutomaticArrow
    outcomes: AutomaticArrow['points'][number]['outcomes']
  }[]
}
// Only display equivalence: every concrete state and source-conditional method survives.
export function stateProjection(view: AutomaticView, cycles: number) {
  const nodeById = new Map(view.nodes.map((n) => [n.id, n]))
  const fixed = new Set<string>(),
    random = new Set<string>()
  const modifierKey = (
    n: AutomaticNode,
    m: AutomaticNode['item']['explicits'][number],
  ) => canonical({ modifier: m, roll: n.rolls?.[m.modifierId] ?? null })
  for (const method of view.methods) {
    const hasEssence = method.initial
      .concat(method.cycle)
      .some((a) => a.currency.includes('ESSENCE'))
    const source = nodeById.get(method.source)!
    const targets = (methodPoint(method, cycles)?.outcomes ?? []).flatMap(
      (o) => (o.target === null ? [] : [nodeById.get(o.target)!]),
    )
    for (const target of targets) {
      for (const m of target.item.explicits) {
        const key = modifierKey(target, m)
        if (
          source.item.explicits.some(
            (before) => modifierKey(source, before) === key,
          )
        )
          continue
        // A preparation Essence does not make the later random additions deterministic.
        const guaranteed =
          hasEssence &&
          targets.every((n) =>
            n.item.explicits.some((other) => modifierKey(n, other) === key),
          )
        ;(guaranteed ? fixed : random).add(key)
      }
    }
  }
  const bundles = new Map<string, StateBundle>(),
    owner = new Map<number, string>()
  for (const n of view.nodes) {
    const key =
      n.id === 0
        ? 'start'
        : canonical({
            depth: n.depth,
            quality40: n.quality40,
            abstractQuality: n.abstractQuality ?? (n.quality40 ? 40 : 0),
            item: {
              ...n.item,
              explicits: n.item.explicits
                .map((m) =>
                  (n.rolls?.[m.modifierId] || random.has(modifierKey(n, m))) &&
                  !fixed.has(modifierKey(n, m))
                    ? {
                        stats: Object.keys(m.values).sort(),
                        fractured: m.fractured,
                      }
                    : { fixed: modifierKey(n, m) },
                )
                .sort((a, b) => canonical(a).localeCompare(canonical(b))),
            },
          })
    let bundle = bundles.get(key)
    if (!bundle) {
      bundle = { id: `state:${n.id}`, depth: n.depth, nodes: [] }
      bundles.set(key, bundle)
    }
    bundle.nodes.push(n)
    owner.set(n.id, bundle.id)
  }
  const edges = new Map<string, BundleEdge>()
  for (const method of view.methods) {
    const point = methodPoint(method, cycles)
    if (!point) continue
    const byTarget = new Map<string, typeof point.outcomes>()
    for (const o of point.outcomes) {
      if (o.target === null) continue
      const to = owner.get(o.target)!
      byTarget.set(to, [...(byTarget.get(to) ?? []), o])
    }
    for (const [to, outcomes] of byTarget) {
      const from = owner.get(method.source)!
      const key = canonical({
        from,
        to,
        initial: method.initial,
        cycle: method.cycle,
        mode: method.executionMode ?? null,
      })
      let edge = edges.get(key)
      if (!edge) {
        edge = { id: `edge:${edges.size}`, from, to, variants: [] }
        edges.set(key, edge)
      }
      edge.variants.push({ method, outcomes })
    }
  }
  // Only show root-reachable bundles; membership is never expanded into tier nodes.
  const reachable = new Set([owner.get(0)!])
  let changed = true
  while (changed) {
    changed = false
    for (const edge of edges.values())
      if (reachable.has(edge.from) && !reachable.has(edge.to)) {
        reachable.add(edge.to)
        changed = true
      }
  }
  return {
    bundles: [...bundles.values()].filter((b) => reachable.has(b.id)),
    edges: [...edges.values()].filter((e) => reachable.has(e.from)),
    owner,
  }
}
