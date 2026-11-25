import type { Locale } from '../../shared/i18n/i18n'
import { isCraftedModifier } from './augmentRules'
import type { Definition } from './craftingApi'
import type { ConcreteItem } from './workbenchApi'
import type {
  Item,
  TextLine,
  ModifierType as ApiModifierType,
} from './itemModels'

export type ModifierKind = Lowercase<ApiModifierType> | 'unknown'
const explicitKinds = new Set<ModifierKind>([
  'explicit',
  'fractured',
  'crafted',
  'desecrated',
  'mutated',
])
export function isExplicitOption(kind: ModifierKind | undefined) {
  return kind !== undefined && explicitKinds.has(kind)
}
export interface ItemCardLine {
  sourceLabel?: string | undefined
  unrevealedDesecrated?: boolean
  numericLocale?: Locale

  junkSide?: 'PREFIX' | 'SUFFIX' | undefined
  affixType?: 'NONE' | 'PREFIX' | 'SUFFIX' | undefined

  attributeKey?: string
  tierRows?: {
    label: string
    value: string
    chance?: string
    chanceKey?: string
    low?: string
    high?: string
  }[]

  id: string
  text: string
  kind?: ModifierKind
  detail?: string | undefined
  affixLabel?: string | undefined
  removalCandidate?: boolean | undefined
  fractured?: boolean | undefined
  lastCurrencyAffected?: boolean | undefined
}
export interface ItemCardData {
  rarity: Item['rarity']
  name: string
  base: string | null
  itemClass: string
  itemLevel: number | null
  properties: readonly ItemCardLine[]
  requirements: readonly ItemCardLine[]
  modifiers: readonly ItemCardLine[]
  flags: readonly ItemCardLine[]
}

/** The stored slot also identifies a revealed desecrated modifier in older sessions. */
export function stateModifierAffix(
  item: ConcreteItem,
  modifier: ConcreteItem['explicits'][number],
  definition: Definition | undefined,
  implicit: boolean,
): Pick<ItemCardLine, 'kind' | 'affixType'> {
  if (implicit) return { kind: 'implicit' }
  const slot = item.desecrated?.find(
    (s) => s.modifierId === modifier.modifierId,
  )
  return {
    kind: slot
      ? 'desecrated'
      : isCraftedModifier(modifier, definition)
        ? 'crafted'
        : 'explicit',
    affixType: slot?.side ?? definition?.affixType,
  }
}

// Display-only exclusion; the untouched source remains in parsing evidence.
export function isTradePrice(text: string): boolean {
  return /^\s*(?:[~-]\s*)?(?:b\/o|price)\s+\d+(?:[.,]\d+)?\s+\S/i.test(text)
}
function fromLine(line: TextLine): ItemCardLine {
  return { id: `line-${line.number}`, text: line.raw, numericLocale: 'en' }
}
const kinds: Record<ApiModifierType, ModifierKind> = {
  IMPLICIT: 'implicit',
  ENCHANT: 'enchant',
  RUNE: 'rune',
  DESECRATED: 'desecrated',
  FRACTURED: 'fractured',
  CRAFTED: 'crafted',
  MUTATED: 'mutated',
  EXPLICIT: 'explicit',
}
export function toItemCard(item: Item): ItemCardData {
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
        numericLocale: 'en' as const,
        kind: kinds[mod.type],
        detail: mod.metadata
          ? `${mod.metadata.raw}\n${mod.source.raw}`
          : mod.source.raw !== mod.text
            ? mod.source.raw
            : undefined,
        affixType: mod.affix ?? undefined,
        affixLabel:
          mod.affix && isExplicitOption(kinds[mod.type])
            ? `T${mod.tier ?? '?'}`
            : undefined,
      },
    })),
    ...item.unparsedLines
      .filter((line) => !metadataLines.has(line.number))
      .map((line) => ({
        number: line.number,
        line: {
          id: `line-${line.number}`,
          text: line.raw,
          kind: 'unknown' as const,
        },
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
