import { repeatContract } from '../../../../shared/contracts/public'
import { chaosRepeatMaximumUsesLimit } from './types'

/** Parse decimal count input only; null must block the caller's execution path. */
export function parseChaosRepeatMaximumUses(raw: string): number | null {
  if (raw === '' || /[^0-9]/.test(raw)) return null
  const value = Number(raw)
  return Number.isSafeInteger(value) &&
    value >= repeatContract.minimumUses &&
    value <= chaosRepeatMaximumUsesLimit
    ? value
    : null
}
