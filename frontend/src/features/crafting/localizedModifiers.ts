import catalog from '../../shared/i18n/modifierTemplates.json'
import { getLocale, formatNumber } from '../../shared/i18n/i18n'
import type { Locale } from '../../shared/i18n/i18n'
import type { Definition } from './craftingApi'
import { rolledText } from './workbenchApi'

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
  const translation = templates[locale][binding.template]
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
  if (!verified) return english
  const { binding, translation } = verified
  const numbers = [...binding.values]
  if (values && binding.valueStats) {
    if (!binding.valueStats.every((stat) => Number.isFinite(values[stat.id])))
      return english
    binding.valueStats.forEach((stat, i) => {
      const value = values[stat.id]! / stat.divisor
      numbers[i] =
        (numbers[i]?.startsWith('+') && value >= 0 ? '+' : '') +
        formatNumber(value, { maximumFractionDigits: 20 }, locale)
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
          formatNumber(values[stat.id]!, { maximumFractionDigits: 20 }, locale)
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
        numbers[0] = numbers[0]!.replace(
          range,
          formatNumber(values[stat.id]!, { maximumFractionDigits: 20 }, locale),
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
