import type { AutomaticView } from './automaticApi'
export type CalculationState =
  | 'preparing'
  | 'calculating'
  | 'queued'
  | 'stopping'
  | 'resuming'
  | 'stopped'
  | 'restored'
  | 'paused'
  | 'partial'
  | 'complete'
  | 'error'
  | 'expired'
export function calculationState(
  view: AutomaticView | null,
  enabled: boolean,
  advancing: boolean,
  phase: 'STOP' | 'RESUME' | null,
  failed: boolean,
  expired: boolean,
): CalculationState {
  if (phase === 'STOP') return 'stopping'
  if (phase === 'RESUME') return 'resuming'
  if (expired) return 'expired'
  if (failed) return 'error'
  if (!view) return 'preparing'
  if (view.status === 'CANCELLED') return 'stopped'
  if (view.status === 'PAUSED') return 'paused'
  if (view.status === 'COMPLETE' && view.complete) return 'complete'
  if (view.status === 'PARTIAL' || view.status === 'COMPLETE') return 'partial'
  if (!enabled) return 'restored'
  return advancing ? 'calculating' : 'queued'
}
