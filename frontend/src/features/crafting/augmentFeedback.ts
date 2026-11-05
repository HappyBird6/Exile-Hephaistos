import { augments } from './socketAugments'
import type { Locale } from '../../shared/i18n/i18n'
import { uiText } from '../../shared/i18n/i18n'

// PoE2DB Rune categories, limited to classes present in the reviewed catalog.
const groups = [
  [
    'Bows',
    'Crossbows',
    'One Hand Maces',
    'Two Hand Maces',
    'Quarterstaves',
    'Spears',
    'Talismans',
  ],
  ['Wands', 'Staves'],
  ['Gloves', 'Boots', 'Helmets', 'Body Armours', 'Shields', 'Bucklers'],
] as const
const labels: Record<Locale, readonly string[]> = {
  en: ['Martial Weapons', 'Wand or Staff', 'Armour'],
  ko: ['무도 무기', '마법봉 또는 지팡이', '방어구'],
  'zh-CN': ['武术武器', '法杖或长杖', '护甲'],
  'zh-TW': ['武術武器', '法杖或長杖', '護甲'],
  ja: ['武器', 'ワンドまたはスタッフ', '防具'],
  es: ['Armas marciales', 'Varita o báculo', 'Armadura'],
}

export function augmentFeedbackLines(
  id: string,
  locale: Locale,
): string[] | undefined {
  const entry = augments.find((a) => a.id === id)
  if (!entry) return undefined
  return groups.flatMap((classes, index) => {
    const effects = new Map<string, string[]>()
    for (const itemClass of classes) {
      const text =
        entry.effects[itemClass]?.[locale] ?? entry.effects[itemClass]?.en
      if (text) effects.set(text, [...(effects.get(text) ?? []), itemClass])
    }
    return [...effects].map(
      ([effect, parts]) =>
        `${labels[locale][index]}${effects.size > 1 || parts.length < classes.length ? ` (${parts.map((part) => uiText(part, locale)).join(', ')})` : ''}: ${effect}`,
    )
  })
}
