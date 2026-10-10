import type { GraphPage } from './types'

// Follow execution identities, never merge policy phases in the calculation graph.
export function ancestors(graph: GraphPage, failureId: string): string[] {
  const failure = graph.executions.find((e) => e.id === failureId)
  if (!failure) return []
  const executions = new Map(graph.executions.map((e) => [e.id, e]))
  const visited = new Set<string>(),
    states = new Set<string>(),
    pending = [failureId]
  while (pending.length) {
    const id = pending.pop()!
    if (visited.has(id)) continue
    visited.add(id)
    for (const edge of graph.edges.filter(
      (e) => e.to === id && BigInt(e.probability.numerator) > 0n,
    )) {
      const from = executions.get(edge.from)
      if (from?.policyId === failure.policyId) {
        states.add(from.stateId)
        pending.push(from.id)
      }
    }
  }
  return graph.nodes.filter((n) => states.has(n.id)).map((n) => n.id)
}
export function stateLayers(graph: GraphPage, policyId: string): string[][] {
  const executions = graph.executions.filter((e) => e.policyId === policyId)
  const ids = new Set(executions.map((e) => e.id))
  const edges = graph.edges.filter(
    (e) => e.kind === 'FORWARD' && ids.has(e.from) && ids.has(e.to),
  )
  const roots = executions.filter(
    (e) => !edges.some((edge) => edge.to === e.id),
  )
  const queue = (roots.length ? roots : executions.slice(0, 1)).map((e) => ({
    id: e.id,
    depth: 0,
  }))
  const seen = new Set<string>(),
    depths = new Map<string, number>()
  while (queue.length) {
    const current = queue.shift()!
    if (seen.has(current.id)) continue
    seen.add(current.id)
    const execution = executions.find((e) => e.id === current.id)!
    if (!depths.has(execution.stateId))
      depths.set(execution.stateId, current.depth)
    edges
      .filter((e) => e.from === current.id)
      .forEach((e) => queue.push({ id: e.to, depth: current.depth + 1 }))
  }
  // Pages may contain disconnected upper results. Keep their states visible without inventing edges.
  executions.forEach((e) => {
    if (!depths.has(e.stateId)) depths.set(e.stateId, 0)
  })
  const layers: string[][] = []
  for (const [id, depth] of depths) (layers[depth] ??= []).push(id)
  return layers
}
