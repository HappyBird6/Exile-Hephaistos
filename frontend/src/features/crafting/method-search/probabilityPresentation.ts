import type { Locale } from '../../../shared/i18n/i18n'
import { priceMessages } from '../currency-prices/messages'
import type { CraftingCost } from '../currency-prices/cost'
import { formatCurrencyAmount } from '../currency-prices/formatCurrencyAmount'
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
export function probabilityText(
  value: Fraction,
  bounds?: Pick<Bounds, 'lower' | 'upper'>,
) {
  if (!bounds) return percent(value)
  const lower = fixed(bounds.lower, 10000n, true),
    upper = fixed(bounds.upper, 10000n, true)
  if (upper === '0.00' && BigInt(bounds.upper.numerator) > 0n) {
    return BigInt(bounds.lower.numerator) > 0n ? '<0.01%' : '0.00% – <0.01%'
  }
  return lower === upper ? `${lower}%` : `${lower}% – ${upper}%`
}
export function quantityText(
  value: Fraction,
  bounds?: Pick<Bounds, 'lower' | 'upper'>,
) {
  const lower = fixed(bounds?.lower ?? value, 100n, false),
    upper = fixed(bounds?.upper ?? value, 100n, false)
  return lower === upper ? lower : `${lower} – ${upper}`
}

export function materialQuantityText(
  material: string,
  locale: string,
  value: Fraction,
  bounds?: Pick<Bounds, 'lower' | 'upper'>,
) {
  const coin = ['EXALTED', 'GREATER_EXALTED', 'PERFECT_EXALTED'].includes(
    material,
  )
    ? 'Exalted_Orb'
    : material === 'DIVINE'
      ? 'Divine_Orb'
      : null
  if (!coin) return quantityText(value, bounds)
  const lower = formatCurrencyAmount(bounds?.lower ?? value, coin, locale)
  const upper = formatCurrencyAmount(bounds?.upper ?? value, coin, locale)
  return lower === upper ? lower : lower + ' – ' + upper
}

export function planTitle(
  cost: CraftingCost,
  locale: Locale,
  probability: string,
) {
  if (cost.retryMaterials?.length)
    return priceMessages[locale].unknownUses + ' · ' + probability
  const exalted =
    cost.incomplete && !cost.exalted
      ? '—'
      : (cost.incomplete ? '≥ ' : '') +
        formatCurrencyAmount(cost.exalted, 'Exalted_Orb', locale)
  return `${exalted} ex · ${probability}`
}
