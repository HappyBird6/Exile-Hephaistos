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
  const fraction = typeof value === 'number' ? decimalFraction(value) : value
  const numerator = BigInt(fraction.numerator)
  const denominator = BigInt(fraction.denominator)
  const thousands = numerator > 1000n * denominator
  const displayDenominator = thousands ? 1000n * denominator : denominator
  const digits = coin === 'Divine_Orb' ? 2 : Number(thousands)
  const format = new Intl.NumberFormat(locale, {
    maximumFractionDigits: digits,
  })
  const scale = 10n ** BigInt(digits)
  const units =
    (numerator * scale + displayDenominator - 1n) / displayDenominator
  const whole = units / scale
  const remainder = units % scale
  let amount = format.format(whole)
  if (remainder) {
    amount += format
      .formatToParts(Number(remainder) / Number(scale))
      .filter((part) => part.type === 'decimal' || part.type === 'fraction')
      .map((part) => part.value)
      .join('')
  }
  return thousands ? amount + 'k' : amount
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
  return value.toLocaleString(
    locale,
    value > 0 && value < 1
      ? { maximumSignificantDigits: 2 }
      : { maximumFractionDigits: 2 },
  )
}
