import type { AutomaticView } from './automaticApi'
import type { Selection } from './supportStorage'
export function routeThrough(route: Selection[], edge: Selection): Selection[] {
  const last = route.at(-1)
  if (last?.target === edge.source) return [...route, edge].slice(0, 64)
  const index = route.findIndex((e) => e.source === edge.source)
  return [...(index >= 0 ? route.slice(0, index) : []), edge]
}
export function validRoute(
  view: AutomaticView,
  route: Selection[],
  cycles: number,
) {
  return route.every(
    (e, i) =>
      (i === 0 || route[i - 1]!.target === e.source) &&
      view.methods.some(
        (m) =>
          m.id === e.method &&
          m.source === e.source &&
          m.points.some(
            (p) =>
              p.cycles === cycles &&
              p.outcomes.some(
                (o) => o.target === e.target && o.kind === e.kind,
              ),
          ),
      ),
  )
}
