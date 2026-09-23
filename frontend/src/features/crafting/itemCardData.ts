import type {
  ParsedItemText,
  TextLine,
  ModifierKind as ParsedModifierKind,
} from './itemTextApi'

export type ModifierKind = Lowercase<ParsedModifierKind> | 'unknown'
export interface ItemCardLine {
  id: string
  text: string
  kind?: ModifierKind
  detail?: string | undefined
  affixLabel?: string | undefined
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

// Display-only exclusion; the untouched source remains in parsing evidence.
export function isTradePrice(text: string): boolean {
  return /^\s*(?:[~-]\s*)?(?:b\/o|price)\s+\d+(?:[.,]\d+)?\s+\S/i.test(text)
}
function fromLine(line: TextLine): ItemCardLine {
  return { id: `line-${line.number}`, text: line.raw }
}
const kinds: Record<ParsedModifierKind, ModifierKind> = {
  IMPLICIT: 'implicit',
  ENCHANT: 'enchant',
  RUNE: 'rune',
  DESECRATED: 'desecrated',
  FRACTURED: 'fractured',
  CRAFTED: 'crafted',
  MUTATED: 'mutated',
  EXPLICIT: 'explicit',
}
export function toItemCard(item: ParsedItemText): ItemCardData {
  const metadataLines = new Set(
    item.modifiers.flatMap((mod) =>
      mod.metadata ? [mod.metadata.number] : [],
    ),
  )
  const lines = [
    ...item.modifiers.map((mod) => ({
      number: mod.source.number,
      line: {
        id: `line-${mod.source.number}`,
        text: mod.text,
        kind: kinds[mod.kind],
        detail: mod.metadata
          ? `${mod.metadata.raw}\n${mod.source.raw}`
          : mod.source.raw !== mod.text
            ? mod.source.raw
            : undefined,
        affixLabel: mod.affix
          ? `${mod.affix === 'PREFIX' ? 'P' : 'S'}${mod.tier ?? '?'}`
          : undefined,
      },
    })),
    ...item.unparsedLines
      .filter((line) => !metadataLines.has(line.number))
      .map((line) => ({
        number: line.number,
        line: { ...fromLine(line), kind: 'unknown' as const },
      })),
  ].sort((a, b) => a.number - b.number)
  return {
    rarity: item.rarity,
    name: item.displayName,
    base: item.displayBase,
    itemClass: item.itemClass,
    itemLevel: item.itemLevel,
    properties: item.properties
      .filter((field) => field.key !== 'Item Level')
      .map((field) => fromLine(field.source)),
    requirements: item.requirements.map((field) => fromLine(field.source)),
    modifiers: lines.map((entry) => entry.line),
    flags: item.flags.map(fromLine),
  }
}
