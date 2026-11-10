import display from '../../../shared/i18n/modifierTemplates.json'
import type { Definition } from '../craftingApi'

type Binding = {
  englishText: string
  stats: NonNullable<Definition['stats']>
  values: string[]
  template: string
  valueStats?: { id: string; divisor: number }[]
}
const bindings: Record<string, Binding> = display.definitions
const templates: Record<string, { template: string }> = display.templates.en
export const visibleClipboardText = (text: string) =>
  text
    .split('\n')
    .filter(
      (line) =>
        !/^local (?:weapon implicit hidden added .* damage|display grants spear throw skill) \[(?:-?\d+|\{v\d+\})\]$/.test(
          line,
        ),
    )
    .join('\n')
const range = /\((-?\d+(?:\.\d+)?)\s*[—–-]\s*(-?\d+(?:\.\d+)?)\)/g

export function hasVerifiedClipboardValues(definition: Definition): boolean {
  const binding = bindings[definition.id]
  return (
    !!binding &&
    binding.englishText === definition.text &&
    binding.stats.length === definition.stats?.length &&
    binding.stats.every((stat, i) => {
      const actual = definition.stats?.[i]
      return (
        actual?.id === stat.id &&
        actual.min === stat.min &&
        actual.max === stat.max
      )
    }) &&
    Array.isArray(binding.valueStats)
  )
}

/** English advanced clipboard syntax, without the screen's one-decimal rounding. */
export function modifierClipboardText(
  definition: Definition,
  values: Record<string, number>,
  observed = false,
): string {
  const binding = bindings[definition.id]
  const verified =
    binding &&
    binding.englishText === definition.text &&
    binding.stats.length === definition.stats?.length &&
    binding.stats.every((stat, i) => {
      const actual = definition.stats?.[i]
      return (
        actual?.id === stat.id &&
        actual.min === stat.min &&
        actual.max === stat.max
      )
    })
  const converters = verified
    ? (binding.valueStats ??
      (/^(ruby|emerald|sapphire|diamond):/.test(definition.id) &&
      !definition.tags?.includes('unscalable')
        ? definition.stats?.map((stat) => ({ id: stat.id, divisor: 1 }))
        : undefined))
    : undefined
  if (verified && converters) {
    return visibleClipboardText(templates[binding.template]!.template)
      .replace(/\{v(\d+)\}/g, (_match, index: string) => {
        const i = Number(index),
          converter = converters[i]
        if (
          !converter ||
          !(observed
            ? Number.isFinite(values[converter.id])
            : Number.isSafeInteger(values[converter.id]))
        )
          throw new Error('Missing verified clipboard roll')
        const amount = values[converter.id]! / converter.divisor
        // Per-minute units need at least hundredths to distinguish adjacent integer rolls.
        // Other non-terminating decimals retain their full numeric precision.
        const decimal = String(amount)
        const [whole, fraction = ''] = decimal.split('.')
        const text =
          Math.abs(converter.divisor) === 60 && fraction.length < 2
            ? `${whole}.${fraction.padEnd(2, '0')}`
            : decimal
        const source = binding.values[i] ?? ''
        const bounds = [...source.matchAll(range)][0]
        return (
          (source.startsWith('+') && amount >= 0 ? '+' : '') +
          text +
          (bounds && !observed ? `(${bounds[1]}-${bounds[2]})` : '')
        )
      })
      .replace(/\b1(\([^)]*\))? seconds\b/g, '1$1 second')
  }
  const represented = new Set<string>()
  const text = definition.text.replace(
    range,
    (original, low: string, high: string) => {
      const candidates =
        definition.stats?.filter(
          (stat) => stat.min === Number(low) && stat.max === Number(high),
        ) ?? []
      if (candidates.length !== 1) return original
      const stat = candidates[0]!
      represented.add(stat.id)
      if (
        !(observed
          ? Number.isFinite(values[stat.id])
          : Number.isSafeInteger(values[stat.id]))
      )
        throw new Error('Missing clipboard roll')
      return `${values[stat.id]}${observed ? '' : `(${low}-${high})`}`
    },
  )
  if (
    definition.stats?.some(
      (stat) => stat.min !== stat.max && !represented.has(stat.id),
    )
  )
    throw new Error('No verified clipboard conversion')
  return visibleClipboardText(text)
}

/** Recognized sentences may still lack enough information to recover original integer rolls. */
export function recognizedClipboardSentence(
  text: string,
  definitions: Record<string, Definition>,
): boolean {
  const shape = (value: string) =>
    value
      .replace(/(?<=\d)\((-?\d+(?:\.\d+)?)\s*[—–-]\s*(-?\d+(?:\.\d+)?)\)/g, '')
      .replace(/\((-?\d+(?:\.\d+)?)\s*[—–-]\s*(-?\d+(?:\.\d+)?)\)/g, '#')
      .replace(/\{v\d+\}|[+-]?\d+(?:\.\d+)?/g, '#')
      .replace(/[+]#/g, '#')
      .replace(/\s+/g, ' ')
      .trim()
      .toLowerCase()
  const observed = shape(text)
  return Object.values(definitions).some((definition) => {
    const binding = bindings[definition.id]
    const sources = [
      definition.text,
      binding ? (templates[binding.template]?.template ?? '') : '',
    ]
    return sources.some((source) =>
      source.split('\n').some((line) => shape(line) === observed),
    )
  })
}
