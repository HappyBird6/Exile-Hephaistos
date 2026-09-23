import type { ParsedItemText, TextLine } from './itemTextApi'

export type ModifierKind =
  | 'implicit'
  | 'enchant'
  | 'rune'
  | 'desecrated'
  | 'fractured'
  | 'crafted'
  | 'explicit'
  | 'unknown'
export interface ItemCardLine {
  id: string
  text: string
  kind?: ModifierKind
  // Supplied by a future verified catalog/engine adapter, never inferred from prose.
  detail?: string
}
export interface ItemCardData {
  rarity: ParsedItemText['rarity']
  name: string
  base: string | null
  itemClass: string
  itemLevel: number | null
  properties: readonly ItemCardLine[]
  requirements: readonly ItemCardLine[]
  modifiers: readonly ItemCardLine[]
  flags: readonly ItemCardLine[]
}

// A display-only exclusion: the untouched source remains in parsing details.
export function isTradePrice(text: string): boolean {
  return /^\s*(?:[~-]\s*)?(?:b\/o|price)\s+\d+(?:[.,]\d+)?\s+\S/i.test(text)
}
function fromLine(line: TextLine): ItemCardLine {
  return { id: `line-${line.number}`, text: line.raw }
}
function modifier(line: TextLine): ItemCardLine {
  const marker =
    /\((implicit|enchant|rune|desecrated|fractured|crafted)\)$/.exec(
      line.raw.trim(),
    )?.[1]
  const kind: ModifierKind =
    marker === 'implicit' ||
    marker === 'enchant' ||
    marker === 'rune' ||
    marker === 'desecrated' ||
    marker === 'fractured' ||
    marker === 'crafted'
      ? marker
      : 'unknown'
  return { ...fromLine(line), kind }
}
export function toItemCard(item: ParsedItemText): ItemCardData {
  return {
    rarity: item.rarity,
    name: item.displayName,
    base: item.displayBase,
    itemClass: item.itemClass,
    itemLevel: item.itemLevel,
    properties: item.properties
      .filter((field) => !['Item Level', '아이템 레벨'].includes(field.key))
      .map((field) => fromLine(field.source)),
    requirements: item.requirements.map((field) => fromLine(field.source)),
    // Unknown lines stay neutral and in original order; they may be flavour or metadata.
    modifiers: [...item.markedModifiers, ...item.unparsedLines]
      .sort((a, b) => a.number - b.number)
      .map(modifier),
    flags: item.flags.map(fromLine),
  }
}
