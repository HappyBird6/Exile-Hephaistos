import { getLocale } from '../../../shared/i18n/i18n'

/** Presentation only. Never use these strings as API rolls or saved item values. */
export function formatAttributeNumber(
  value: number,
  locale = getLocale(),
): string {
  return new Intl.NumberFormat(locale, { maximumFractionDigits: 1 }).format(
    Math.abs(value) < 0.05 ? 0 : value,
  )
}

const escapePattern = (value: string) =>
  value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

/** Round decimal spans, keeping integers, units, ranges and surrounding text intact. */
export function formatAttributeText(
  text: string,
  locale = getLocale(),
  outputLocale = locale,
): string {
  const formatter = new Intl.NumberFormat(locale)
  const parts = formatter.formatToParts(12345.6)
  const decimal = parts.find((part) => part.type === 'decimal')!.value
  const group = parts.find((part) => part.type === 'group')?.value
  const grouping = group ? `(?:${escapePattern(group)}\\d{3})*` : ''
  const pattern = new RegExp(
    `(?<![\\p{L}\\p{N}_])([+-]?)(\\d+${grouping})${escapePattern(decimal)}(\\d+)(?!\\d)`,
    'gu',
  )
  return text.replace(
    pattern,
    (_match, sign: string, whole: string, fraction: string) => {
      const digits = group ? whole.replaceAll(group, '') : whole
      const tenths =
        BigInt(digits) * 10n +
        BigInt(fraction[0]!) +
        (Number(fraction[1] ?? 0) >= 5 ? 1n : 0n)
      const integer = new Intl.NumberFormat(outputLocale, {
        maximumFractionDigits: 0,
      }).format(tenths / 10n)
      const remainder = tenths % 10n
      return (
        (sign === '-' && tenths === 0n ? '' : sign) +
        integer +
        (remainder
          ? new Intl.NumberFormat(outputLocale)
              .formatToParts(1.1)
              .find((part) => part.type === 'decimal')!.value + remainder
          : '')
      )
    },
  )
}
