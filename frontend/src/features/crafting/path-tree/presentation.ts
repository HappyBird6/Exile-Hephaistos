import type { Definition } from '../craftingApi'
import type { ItemCardData } from '../itemCardData'
import { localizedModifierText } from '../localizedModifiers'
import type { Locale } from '../../../shared/i18n/i18n'
import type { Item } from './types'
import { pathTreeMessages } from './messages'

export type ItemPresentation = (item: Item, locale: Locale) => ItemCardData
// Definitions come from the same frozen starting catalog. No stat names or numeric rules here.
export function createItemPresentation(
  base: { name: string; itemClass: string },
  definitions: Record<string, Definition>,
): ItemPresentation {
  return (item, locale) => ({
    name: base.name,
    base: base.name,
    itemClass: base.itemClass,
    rarity: item.rarity,
    itemLevel: item.itemLevel,
    properties: [],
    requirements: [],
    flags: [],
    modifiers: [
      ...item.implicits.map((m) => ({ ...m, kind: 'implicit' as const })),
      ...item.explicits.map((m) => ({ ...m, kind: 'explicit' as const })),
    ].map((m) => ({
      id: m.modifierId,
      kind: m.kind,
      fractured: m.fractured,
      text: definitions[m.modifierId]
        ? localizedModifierText(definitions[m.modifierId]!, m.values, locale)
        : pathTreeMessages[locale].unavailable,
    })),
  })
}
