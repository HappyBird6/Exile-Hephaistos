import type { AutomaticNode } from './automaticApi'
import type { PathTreeProps } from '../path-tree/PathTree'
import type { AttributeRendering } from './compactAttribute'
import type { Item } from '../path-tree/types'

/** Project whole observed tuples; scalar attribute columns must never fabricate a hybrid. */
export function jointAttributeRenderings(
  node: AutomaticNode,
  key: string,
  nodes: AutomaticNode[],
  locale: Parameters<PathTreeProps['presentItem']>[1],
  present: PathTreeProps['presentItem'],
): AttributeRendering[] {
  const groups = new Map<string, Map<string, Item['explicits'][number]>>(),
    tiers = new Map<string, number>(),
    seen = new Set<string>()
  function collect(current: AutomaticNode, field: string) {
    const cursor = `${current.id}:${field}`
    if (seen.has(cursor)) return
    seen.add(cursor)
    const options = current.attributes?.[field] ?? [],
      ids = new Set([field, ...options.map((o) => o.modifierId)])
    options.forEach((o) => tiers.set(o.modifierId, o.tier))
    for (const item of [
      current.item,
      ...(current.members ?? []).map((m) => m.item),
    ])
      for (const m of item.explicits) {
        if (!ids.has(m.modifierId) || Object.keys(m.values).length < 2) continue
        const group =
          groups.get(m.modifierId) ??
          new Map<string, Item['explicits'][number]>()
        group.set(
          JSON.stringify(
            Object.entries(m.values).sort(([a], [b]) => a.localeCompare(b)),
          ),
          m,
        )
        groups.set(m.modifierId, group)
      }
    for (const origin of current.joint ?? []) {
      const sourceKey = origin.retained[field],
        source = nodes.find((n) => n.id === origin.source)
      if (sourceKey && source) collect(source, sourceKey)
    }
  }
  collect(node, key)
  const result: AttributeRendering[] = []
  for (const [id, group] of groups) {
    const points = [...group.values()],
      keys = Object.keys(points[0]!.values).sort()
    points.sort((a, b) => {
      for (const stat of keys) {
        const delta = a.values[stat]! - b.values[stat]!
        if (delta) return delta
      }
      return 0
    })
    // Only proven monotone tuple sequences may be displayed as a range. Otherwise
    // preserve each observed tuple so crossed marginal minima cannot become an item.
    const monotone = points.every(
        (p, i) =>
          i === 0 ||
          keys.every((stat) => p.values[stat]! >= points[i - 1]!.values[stat]!),
      ),
      pairs = monotone
        ? [[points[0]!, points.at(-1)!]]
        : points.map((p) => [p, p])
    const render = (m: Item['explicits'][number]) =>
      present(
        {
          ...node.item,
          explicits: node.item.explicits.map((old) =>
            old.modifierId === key ? m : old,
          ),
        },
        locale,
      ).modifiers.find((line) => line.id === id)?.text
    for (const pair of pairs) {
      const low = render(pair[0]!),
        high = render(pair[1]!)
      if (low && high)
        result.push({
          label: tiers.get(id) ? `T${tiers.get(id)}` : '?',
          low,
          high,
        })
    }
  }
  return result.sort(
    (a, b) => Number(a.label.slice(1)) - Number(b.label.slice(1)),
  )
}
