import {
  baseClass,
  baseSlugs,
  baseTexts,
  selectableBaseKeys,
  baseStartingRarity,
} from '../baseRegistry'
import type { WorkbenchBaseKey } from '../baseRegistry'
import { basePropertyLines, topBase } from '../topBases'
import type { ItemCardData } from '../itemCardData'
import { gameName, gameTerm } from '../../../shared/i18n/i18n'
import type { Locale } from '../../../shared/i18n/i18n'
import templates from '../../../shared/i18n/modifierTemplates.json'
import { basePickerMessages } from './basePickerMessages'

export type BaseCategory = 'weapons' | 'armour' | 'accessories' | 'jewels'
export type BaseChoice = {
  key: WorkbenchBaseKey
  baseItemId: string
  name: string
  itemClass: string
  category: BaseCategory
  slug: string
  variantLabel?: string
}

export function baseCategory(itemClass: string): BaseCategory {
  if (/jewel/i.test(itemClass)) return 'jewels'
  if (/amulet|ring|belt/i.test(itemClass)) return 'accessories'
  if (/armour|helmet|glove|boot|shield|buckler|foc/i.test(itemClass))
    return 'armour'
  return 'weapons'
}

export function workbenchBaseChoices(locale: Locale): BaseChoice[] {
  const choices: BaseChoice[] = selectableBaseKeys.map((key) => {
    const itemClass = baseClass(key) === 'Amulet' ? 'Amulets' : baseClass(key)
    const slug = baseSlugs[key].replaceAll(' ', '_')
    const term = slug.replaceAll('-', '_')
    const baseItemId =
      topBase(key)?.id ?? gameTerm(term, locale).data?.itemKey ?? ''
    return {
      key,
      baseItemId,
      slug,
      itemClass,
      category: baseCategory(itemClass),
      name: gameName(
        baseItemId || term,
        baseTexts[key].split('\n').at(-1)!,
        locale,
      ),
    }
  })
  return choices.map((choice) =>
    choices.filter(
      (other) =>
        other.name === choice.name && other.itemClass === choice.itemClass,
    ).length > 1
      ? {
          ...choice,
          variantLabel: basePreview(choice, null, locale)
            .modifiers.map((line) => line.text)
            .join(' · '),
        }
      : choice,
  )
}

/** Display-only preview from existing reviewed source data; no API call or invented roll. */
export function basePreview(
  choice: BaseChoice,
  itemLevel: number | null,
  locale: Locale,
): ItemCardData {
  const base = topBase(choice.key)
  const text = basePickerMessages[locale]
  const properties = basePropertyLines(choice.key, locale).map(
    (line, index) => ({ id: `source-${index}`, text: line }),
  )
  for (const [id, label, value] of [
    ['armour', text.armourValue, base?.armour],
    ['evasion', text.evasionValue, base?.evasion],
    ['energy-shield', text.energyShieldValue, base?.energyShield],
  ] as const) {
    if (value && !properties.some((line) => line.text.includes(label)))
      properties.push({ id, text: `${label}: ${value}` })
  }
  const bindings = templates.definitions as Record<
    string,
    { englishText: string; template: string; values: string[] }
  >
  const implicitId =
    base?.implicitModifierId ??
    (
      {
        solar: 'solar-amulet:implicit:spirit',
        ring: 'iron-ring:implicit:added-physical-damage-to-attacks',
      } as Record<string, string>
    )[choice.key]
  const binding = implicitId ? bindings[implicitId] : undefined
  const localized = templates.templates[locale] as Record<
    string,
    { template: string }
  >
  const implicit = binding
    ? (localized[binding.template]?.template.replace(
        /\{v(\d+)\}/g,
        (match, index: string) => binding.values[Number(index)] ?? match,
      ) ?? binding.englishText)
    : undefined
  return {
    rarity: baseStartingRarity(choice.key),
    name: choice.name,
    base: choice.name,
    itemClass: choice.itemClass,
    itemLevel,
    properties,
    requirements: base
      ? [{ id: 'requirements', text: base.requirements[locale] }]
      : [],
    modifiers: implicit
      ? [{ id: 'implicit', text: implicit, kind: 'implicit' }]
      : [],
    flags: [],
  }
}
