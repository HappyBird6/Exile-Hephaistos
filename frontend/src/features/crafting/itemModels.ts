// Manual DTO mirror of docs/openapi-item.yaml, shared by API validation and UI.
export interface TextLine {
  number: number
  section: number
  raw: string
  kind: 'CONTENT' | 'SEPARATOR' | 'BLANK'
}
export interface RawField {
  key: string
  value: string
  source: TextLine
}
export type ModifierType =
  | 'IMPLICIT'
  | 'ENCHANT'
  | 'RUNE'
  | 'DESECRATED'
  | 'FRACTURED'
  | 'CRAFTED'
  | 'MUTATED'
  | 'EXPLICIT'
export interface Modifier {
  text: string
  type: ModifierType
  source: TextLine
  metadata: TextLine | null
  affix: 'PREFIX' | 'SUFFIX' | null
  tier: number | null
  affixName: string | null
}
export interface Item {
  text: { originalText: string; lines: TextLine[] }
  locale: 'en'
  itemClass: string
  rarity: 'NORMAL' | 'MAGIC' | 'RARE' | 'UNIQUE' | 'UNKNOWN'
  rarityText: string
  nameLines: TextLine[]
  displayName: string
  displayBase: string | null
  itemLevel: number | null
  properties: RawField[]
  requirements: RawField[]
  markedModifiers: TextLine[]
  modifiers: Modifier[]
  flags: TextLine[]
  unparsedLines: TextLine[]
  warnings: { code: string; lineNumber: number }[]
}
