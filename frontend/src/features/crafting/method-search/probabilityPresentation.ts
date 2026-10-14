import type { Fraction } from '../path-tree/types'
import { percent } from '../path-tree/validation'
import type { Bounds } from './types'

function fixed(value: Fraction, multiplier: bigint, round: boolean) {
  const numerator = BigInt(value.numerator) * multiplier,
    denominator = BigInt(value.denominator)
  const units = round
    ? (2n * numerator + denominator) / (2n * denominator)
    : numerator / denominator
  return `${units / 100n}.${(units % 100n).toString().padStart(2, '0')}`
}
export function probabilityText(value: Fraction, bounds?: Bounds) {
  if (!bounds) return percent(value)
  const lower = fixed(bounds.lower, 10000n, true),
    upper = fixed(bounds.upper, 10000n, true)
  return lower === upper ? `${lower}%` : `${lower}% – ${upper}%`
}
export function quantityText(value: Fraction, bounds?: Bounds) {
  const lower = fixed(bounds?.lower ?? value, 100n, false),
    upper = fixed(bounds?.upper ?? value, 100n, false)
  return lower === upper ? lower : `${lower} – ${upper}`
}
