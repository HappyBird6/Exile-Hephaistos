import type { AutomaticArrow } from './automaticApi'
export function isRepeatable(method: AutomaticArrow) {
  // Compatibility for older read-only fixtures/servers: never infer a repeat
  // from an Exalted action. Current servers certify this from first-pass mass.
  return (
    method.executionMode === 'REPEATABLE' ||
    (!method.executionMode && method.cycle.every((a) => a.currency === 'CHAOS'))
  )
}
export function methodPoint(method: AutomaticArrow, cycles: number) {
  return method.points.find(
    (p) => p.cycles === (isRepeatable(method) ? cycles : 1),
  )
}
