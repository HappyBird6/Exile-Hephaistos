import catalog from '../../shared/i18n/modifierTemplates.json'
import { getLocale } from '../../shared/i18n/i18n'
import type { Locale } from '../../shared/i18n/i18n'
import type { Definition } from './craftingApi'
import { rolledText } from './workbenchApi'
import {
  formatAttributeNumber,
  formatAttributeText,
} from './numeric-display/attributeNumbers'

type Binding = {
  stats: NonNullable<Definition['stats']>
  englishText: string
  values: string[]
  template: string
  valueStats?: { id: string; divisor: number }[]
}
type Template = { template: string; name: string }
const bindings: Record<string, Binding> = catalog.definitions
const templates: Record<Locale, Record<string, Template>> = catalog.templates
const range = /\((-?\d+(?:\.\d+)?)\s*[\u2014\u2013?-]\s*(-?\d+(?:\.\d+)?)\)/
function verifiedTemplate(definition: Definition, locale: Locale) {
  const binding = bindings[definition.id]
  if (
    !binding ||
    binding.englishText !== definition.text ||
    binding.stats.length !== (definition.stats ?? []).length ||
    !binding.stats.every((stat, index) => {
      const actual = definition.stats?.[index]
      return (
        actual?.id === stat.id &&
        actual.min === stat.min &&
        actual.max === stat.max
      )
    })
  )
    return undefined
  const translation =
    templates[locale][binding.template] ??
    (definition.id.startsWith('reveal:')
      ? templates.en[binding.template]
      : undefined)
  return translation ? { binding, translation } : undefined
}

// No localized string is fed to the API, catalog, grouping keys or saved films.
// Only source-correlated display spans are projected; canonical rolls remain unchanged.
export function localizedModifierText(
  definition: Definition,
  values?: Record<string, number>,
  locale = getLocale(),
): string {
  const english = values ? rolledText(definition, values) : definition.text
  const verified = verifiedTemplate(definition, locale)
  if (!verified) return formatAttributeText(english, 'en')
  const { binding, translation } = verified
  const numbers = binding.values.map((value) =>
    formatAttributeText(value, 'en', locale),
  )
  if (values && binding.valueStats) {
    if (!binding.valueStats.every((stat) => Number.isFinite(values[stat.id])))
      return english
    binding.valueStats.forEach((stat, i) => {
      const value = values[stat.id]! / stat.divisor
      numbers[i] =
        (numbers[i]?.startsWith('+') && value >= 0 ? '+' : '') +
        formatAttributeNumber(value, locale)
    })
    return translation.template.replace(
      /\{v(\d+)\}/g,
      (match, index: string) => numbers[Number(index)] ?? match,
    )
  }
  if (values && /^(ruby|emerald|sapphire|diamond):/.test(definition.id)) {
    numbers.forEach((n, i) => {
      const stat = definition.stats?.[i]
      if (stat && Number.isSafeInteger(values[stat.id]))
        numbers[i] =
          (n.startsWith('+') ? '+' : '') +
          formatAttributeNumber(values[stat.id]!, locale)
    })
    return translation.template.replace(
      /\{v(\d+)\}/g,
      (match, index: string) => numbers[Number(index)] ?? match,
    )
  }
  if (values) {
    if (english === definition.text) {
      // Fixed stats retain their published display values; no conversion is inferred.
    } else {
      const stat =
        definition.stats?.length === 1 ? definition.stats[0] : undefined
      const match = definition.text.match(range)
      if (
        stat &&
        match &&
        numbers.length === 1 &&
        Number(match[1]) === stat.min &&
        Number(match[2]) === stat.max &&
        Number.isFinite(values[stat.id])
      ) {
        numbers[0] = formatAttributeText(
          binding.values[0]!.replace(range, String(values[stat.id]!)),
          'en',
          locale,
        )
      } else {
        // Unknown multi-stat display conversions remain raw source units. Localize
        // only the verified affix name, retaining canonical stat IDs and values.
        return english.startsWith(`${definition.name}:`)
          ? `${translation.name}${english.slice(definition.name.length)}`
          : english
      }
    }
  }
  return translation.template.replace(
    /\{v(\d+)\}/g,
    (match, index: string) => numbers[Number(index)] ?? match,
  )
}

export function modifierTranslationStatus(
  definition: Definition,
  locale = getLocale(),
) {
  return locale === 'en' || verifiedTemplate(definition, locale)
    ? 'VERIFIED'
    : 'ENGLISH_FALLBACK'
}

/** Mark the edited value in its verified effect sentence, without internal IDs. */
export function localizedModifierValueLabel(
  definition: Definition,
  statId: string,
  valueLabel: string,
  locale = getLocale(),
): string {
  valueLabel = formatAttributeText(valueLabel, locale)
  const verified = verifiedTemplate(definition, locale)
  const index = definition.stats?.findIndex((stat) => stat.id === statId) ?? -1
  if (verified) {
    const { binding, translation } = verified
    const valueIndex = binding.valueStats
      ? binding.valueStats.findIndex((stat) => stat.id === statId)
      : binding.values.length === definition.stats?.length
        ? index
        : -1
    if (valueIndex >= 0)
      return translation.template.replace(/\{v(\d+)\}/g, (match, i: string) =>
        Number(i) === valueIndex
          ? `[${valueLabel}]`
          : formatAttributeText(
              binding.values[Number(i)] ?? match,
              'en',
              locale,
            ),
      )
  }
  const ranges = [
    ...definition.text.matchAll(
      /\((-?\d+(?:\.\d+)?)\s*[\u2014\u2013?-]\s*(-?\d+(?:\.\d+)?)\)/g,
    ),
  ]
  if (
    ranges.length === definition.stats?.length &&
    ranges[index] &&
    ranges.every(
      (match, i) =>
        Number(match[1]) === definition.stats?.[i]?.min &&
        Number(match[2]) === definition.stats?.[i]?.max,
    )
  ) {
    const match = ranges[index]!
    return (
      definition.text.slice(0, match.index) +
      `[${valueLabel}]` +
      definition.text.slice(match.index + match[0].length)
    )
  }
  return `${localizedModifierText(definition, undefined, locale)} — ${valueLabel}`
}

export function modifierRollDivisor(
  definition: Definition,
  statId: string,
): number {
  return (
    verifiedTemplate(definition, 'en')?.binding.valueStats?.find(
      (s) => s.id === statId,
    )?.divisor ?? 1
  )
}

// Correct binary floating-point noise only; do not coerce an unsupported fractional roll.
export function storedModifierRoll(value: number, divisor: number): number {
  const scaled = value * divisor
  const integer = Math.round(scaled)
  return Math.abs(scaled - integer) < 1e-9 ? integer : scaled
}
