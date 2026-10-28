// Traverse only outgoing display edges; merging paths never pulls in parents.
export function downstream(
  start: string | null,
  edges: readonly { id: string; from: string; to: string }[],
) {
  const nodes = new Set<string>(),
    links = new Set<string>()
  const outgoing = new Map<string, (typeof edges)[number][]>()
  for (const edge of edges) {
    const rows = outgoing.get(edge.from) ?? []
    rows.push(edge)
    outgoing.set(edge.from, rows)
  }
  const pending = start === null ? [] : [start]
  while (pending.length) {
    const id = pending.pop()!
    if (nodes.has(id)) continue
    nodes.add(id)
    for (const edge of outgoing.get(id) ?? []) {
      links.add(edge.id)
      if (!nodes.has(edge.to)) pending.push(edge.to)
    }
  }
  return { nodes, links }
}
