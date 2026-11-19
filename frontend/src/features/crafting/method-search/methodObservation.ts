import type { Locale } from '../../../shared/i18n/i18n'
import { sessionNotationMessages } from '../session-notation/messages'
import { quantityText } from './probabilityPresentation'
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

export function chaosMeanLabel(
  point: AutomaticArrow['points'][number] | undefined,
  locale: Locale,
) {
  const mean = point?.cycleUses.CHAOS
  if (
    !mean ||
    !point.outcomes.some((o) => o.conditional?.some((c) => c.expectedChaos))
  )
    return undefined
  return sessionNotationMessages[locale].expectedUses(
    quantityText(mean.lower, mean),
  )
}
