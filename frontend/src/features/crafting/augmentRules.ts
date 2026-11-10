import rules from './augmentCrafting.json'
import type { ConcreteItem } from './workbenchApi'
import type { Definition } from './craftingApi'

type Installed = NonNullable<ConcreteItem['installedAugments']>
type Effect = {
  craftedBonus?: number
  suffixBonus?: number
  maximumQuality?: number
  runeEffectPercent?: number
  idolBonded?: boolean
  marker?: string
  pool?: string
}
const effects = rules.effects as Record<string, Effect>
export const augmentRule = (id: string): Effect => effects[id] ?? {}
export const socketBound = (id: string): boolean =>
  (rules.socketBound as readonly string[]).includes(id)
export const socketIsBound = (item: ConcreteItem, slot: number) =>
  (item.installedAugments ?? []).some(
    (a) => a.slotIndex === slot && socketBound(a.augmentId),
  )
export const socketSuffixBonus = (installed: Installed = []) =>
  installed.reduce(
    (sum, a) => sum + (augmentRule(a.augmentId).suffixBonus ?? 0),
    0,
  )
export const craftedLimit = (item: ConcreteItem) =>
  1 +
  (item.installedAugments ?? []).reduce(
    (sum, a) => sum + (augmentRule(a.augmentId).craftedBonus ?? 0),
    0,
  )
export const augmentQualityMaximum = (item: ConcreteItem) =>
  Math.max(
    20,
    ...(item.installedAugments ?? []).map(
      (a) => augmentRule(a.augmentId).maximumQuality ?? 0,
    ),
  )
export const isCraftedModifier = (
  modifier: ConcreteItem['explicits'][number],
  definition?: Definition,
) =>
  modifier.crafted === true ||
  definition?.tags?.includes('crafted') === true ||
  (rules.craftedModifierIds as readonly string[]).includes(modifier.modifierId)
export const craftedCount = (
  item: ConcreteItem,
  definitions: Record<string, Definition>,
) =>
  item.explicits.filter((m) => isCraftedModifier(m, definitions[m.modifierId]))
    .length
export const hasIdolBonded = (item: ConcreteItem) =>
  (item.installedAugments ?? []).some(
    (a) => augmentRule(a.augmentId).idolBonded === true,
  )
export const structuralAugment = (id: string) => {
  const rule = augmentRule(id)
  return !!(
    rule.craftedBonus ||
    rule.suffixBonus ||
    rule.maximumQuality ||
    rule.pool ||
    rule.marker
  )
}
export const visibleAugmentText = (text: string) =>
  text
    .replace(
      /,?\s*(?:local maximum mods allowed \+|display additional socketable mods for idols) \[[^\]]+\]/g,
      '',
    )
    .trim()

/** Source weights are gated by the installed pool Augment; retained modifiers themselves stay intact. */
export function effectiveDefinitions(
  state: ConcreteItem,
  definitions: Record<string, Definition>,
  all = false,
): Record<string, Definition> {
  const available = (
    rules.pools as Record<string, Record<string, Record<string, number>>>
  )[state.baseItemId]
  if (!available) return definitions
  const active = new Set(
    (state.installedAugments ?? []).map((a) => augmentRule(a.augmentId).pool),
  )
  const weights: Record<string, number> = {}
  for (const [pool, modifiers] of Object.entries(available))
    for (const [id, weight] of Object.entries(modifiers))
      weights[id] = all || active.has(pool) ? weight : 0
  return Object.fromEntries(
    Object.entries(definitions).map(([id, definition]) => [
      id,
      weights[id] == null ? definition : { ...definition, weight: weights[id] },
    ]),
  )
}

export const craftedLimitMessages = {
  en: 'The item has reached its Crafted modifier limit.',
  ko: '이 아이템에 추가할 수 있는 제작 속성 수에 도달했습니다.',
  'zh-CN': '此物品已达到工艺词缀数量上限。',
  'zh-TW': '此物品已達到工藝詞綴數量上限。',
  ja: 'このアイテムはクラフトモッド数の上限に達しています。',
  es: 'El objeto ha alcanzado el límite de modificadores fabricados.',
  ru: 'Предмет достиг предела созданных модификаторов.',
  th: 'ไอเทมนี้มีม็อดคราฟต์ถึงจำนวนสูงสุดแล้ว',
} as const
