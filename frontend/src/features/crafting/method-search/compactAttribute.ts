import type { ItemCardLine } from '../itemCardData'
import type { Locale } from '../../../shared/i18n/i18n'

export type AttributeRendering = {
  label: string
  low: string
  high: string
  chance?: string
  chanceKey?: string
}
const number = /[+-]?\d[\d,.]*/g
const numbers = (text: string) =>
  [...text.matchAll(number)].map((m) => ({ value: m[0], offset: m.index! }))

/** Keep the localized property once; only the changing numeric columns become tier rows. */
export function compactAttribute(
  line: ItemCardLine,
  variants: AttributeRendering[],
  locale?: Locale,
): ItemCardLine {
  if (!variants.length) return line
  const texts = variants.flatMap((v) => [v.low, v.high])
  const skeleton = texts[0]!.replace(number, '#')
  if (texts.some((t) => t.replace(number, '#') !== skeleton)) return line
  const columns = texts.map(numbers)
  const varying = columns[0]!
    .map((_, i) => i)
    .filter((i) => new Set(columns.map((c) => c[i]!.value)).size > 1)
  if (!varying.length) {
    const result = { ...line, text: variants[0]!.low }
    if (new Set(variants.map((v) => v.label)).size > 1) delete result.affixLabel
    return result
  }
  let index = 0
  let title = variants[0]!.low
    .replace(number, (value) => (varying.includes(index++) ? '\uFFF0' : value))
    .replace(/\uFFF0\s*%?/g, '')
    .replace(/\s+/g, ' ')
    .trim()
  if (locale === 'en' || locale === undefined)
    title = title.replace(/^to\s+/i, '')
  if (locale === 'es' || locale === undefined)
    title = title.replace(/^a\s+(?:la\s+|los\s+|las\s+)?/i, '')
  if (!title) return line
  const rows = variants.map((v) => {
    const low = numbers(v.low),
      high = numbers(v.high)
    const value = varying
      .map((i) => {
        const a = low[i]!.value,
          b = high[i]!.value
        const unit = /^\s*%/.test(v.low.slice(low[i]!.offset + a.length))
          ? '%'
          : ''
        return (
          (a === b
            ? a
            : `${a}–${a.startsWith('+') ? b.replace(/^\+/, '') : b}`) + unit
        )
      })
      .join(' / ')
    return {
      label: v.label,
      value,
      low: v.low,
      high: v.high,
      ...(v.chance === undefined ? {} : { chance: v.chance }),
      ...(v.chanceKey === undefined ? {} : { chanceKey: v.chanceKey }),
    }
  })
  const rest = { ...line }
  delete rest.detail
  delete rest.affixLabel
  return {
    ...rest,
    text: title,
    tierRows: rows.filter(
      (row, i) =>
        rows.findIndex(
          (other) => JSON.stringify(other) === JSON.stringify(row),
        ) === i,
    ),
  }
}
export function tierLabel(label: string | undefined) {
  const match = label?.match(/(\d+)\s*$/)
  return match ? `T${match[1]}` : (label ?? '—')
}

export function attributeHeading(text: string) {
  return text
    .replace(number, '')
    .replace(/[%–~?]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
    .replace(/^to\s+/i, '')
    .replace(/^a\s+(?:la\s+|los\s+|las\s+)?/i, '')
}
