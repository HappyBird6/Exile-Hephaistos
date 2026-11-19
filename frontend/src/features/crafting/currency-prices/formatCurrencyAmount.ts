import { decimalFraction } from './decimalAmount'
import type { Fraction } from '../path-tree/types'

type Coin = 'Exalted_Orb' | 'Divine_Orb'

/** Round only at presentation. Search quantities arrive as exact rational bounds. */
export function formatCurrencyAmount(
  value: number | Fraction | null,
  coin: Coin,
  locale: string,
) {
  if (value === null || (typeof value === 'number' && !Number.isFinite(value)))
    return '—'
  const digits = coin === 'Divine_Orb' ? 2 : 0
  const format = new Intl.NumberFormat(locale, {
    maximumFractionDigits: digits,
  })
  const fraction = typeof value === 'number' ? decimalFraction(value) : value
  const scale = 10n ** BigInt(digits)
  const denominator = BigInt(fraction.denominator)
  const units =
    (BigInt(fraction.numerator) * scale + denominator - 1n) / denominator
  const whole = units / scale
  const remainder = units % scale
  if (!remainder) return format.format(whole)
  return (
    format.format(whole) +
    format
      .formatToParts(Number(remainder) / Number(scale))
      .filter((part) => part.type === 'decimal' || part.type === 'fraction')
      .map((part) => part.value)
      .join('')
  )
}

export function formatExpectedUses(
  value: number,
  currencyId: string,
  locale: string,
) {
  if (currencyId === 'Divine_Orb' || currencyId === 'Exalted_Orb')
    return formatCurrencyAmount(value, currencyId, locale)
  if (
    currencyId === 'Greater_Exalted_Orb' ||
    currencyId === 'Perfect_Exalted_Orb'
  )
    return formatCurrencyAmount(value, 'Exalted_Orb', locale)
  return value.toLocaleString(locale, { maximumFractionDigits: 2 })
}
