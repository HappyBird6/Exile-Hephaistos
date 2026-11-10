import { observedAugmentEffects } from './augmentEffects'
import { socketIsBound, visibleAugmentText } from './augmentRules'
import data from './augments.json'
import display from '../../shared/i18n/augmentDisplay.json'
import { basePolicy } from './baseRegistry'
import { getLocale, translate } from '../../shared/i18n/i18n'
import type { ItemCardLine } from './itemCardData'
import { rulesetHeaders, verifyRulesetResponse } from './rulesetIdentity'
import type { AppliedItem, ConcreteItem } from './workbenchApi'

type Entry = {
  id: string
  names: Record<string, string>
  effects: Record<string, Record<string, string>>
  image: string
  selectionEnabled: boolean
  kind: string
  properties: Record<string, string[]>
  limit?: { count: number; group: string }
  legacyLimitExempt?: boolean
}
export const augments: readonly Entry[] = data.map((entry) => {
  const local = display[entry.id as keyof typeof display]
  return {
    ...entry,
    names: { ...entry.names, ru: local.ru.name, th: local.th.name },
    properties: {
      ...entry.properties,
      ru: local.ru.properties,
      th: local.th.properties,
    },
    effects: Object.fromEntries(
      Object.entries(entry.effects).map(([itemClass, effects]) => [
        itemClass,
        {
          ...effects,
          ru: (local.ru.effects as Record<string, string>)[itemClass]!,
          th: (local.th.effects as Record<string, string>)[itemClass]!,
        },
      ]),
    ),
  }
})
export const selectableAugments = augments.filter((a) => a.selectionEnabled)
export const isSelectableAugment = (id: string) =>
  selectableAugments.some((a) => a.id === id)
export const augmentMaterials = augments.map((a) => ({
  id: a.id,
  name: a.names.en!,
  image: a.image,
  category: 'Augments' as const,
}))
export const augmentName = (id: string) => {
  const a = augments.find((a) => a.id === id)
  return a?.names[getLocale()] ?? a?.names.en ?? id
}
export function augmentEffect(id: string, base: string): string | undefined {
  const a = augments.find((a) => a.id === id)
  const effects = a?.effects[basePolicy(base)?.itemClass ?? '']
  const text = effects?.[getLocale()] ?? effects?.en
  return text ? visibleAugmentText(text) : text
}
export function withinAugmentLimit(
  installed: NonNullable<ConcreteItem['installedAugments']>,
  id: string,
): boolean {
  const limit = augments.find((a) => a.id === id)?.limit
  return (
    !limit ||
    installed.filter(
      (a) =>
        augments.find((entry) => entry.id === a.augmentId)?.limit?.group ===
        limit.group,
    ).length <= limit.count
  )
}
export function canInstallAugment(
  state: ConcreteItem,
  id?: string,
  slot?: number,
): boolean {
  if (!id) return false
  if (slot == null)
    return Array.from(
      { length: state.augmentSockets ?? 0 },
      (_, index) => index,
    ).some((index) => canInstallAugment(state, id, index))
  return (
    isSelectableAugment(id) &&
    Boolean(augmentEffect(id, state.baseItemId)) &&
    Number.isInteger(slot) &&
    slot >= 0 &&
    slot < (state.augmentSockets ?? 0) &&
    !socketIsBound(state, slot) &&
    !state.conditions.length &&
    !state.observations &&
    !state.desecrated?.length &&
    withinAugmentLimit(
      [
        ...(state.installedAugments ?? []).filter((a) => a.slotIndex !== slot),
        { slotIndex: slot, augmentId: id },
      ],
      id,
    )
  )
}
export function augmentShape(item: Record<string, unknown>): boolean {
  if (item.installedAugments == null) return true
  if (
    !Array.isArray(item.installedAugments) ||
    item.installedAugments.length > 3
  )
    return false
  const slots = new Set<number>()
  const valid = item.installedAugments.every((a) => {
    if (
      !a ||
      typeof a !== 'object' ||
      Object.keys(a).length !== 2 ||
      !Number.isInteger(a.slotIndex) ||
      a.slotIndex < 0 ||
      typeof item.augmentSockets !== 'number' ||
      a.slotIndex >= item.augmentSockets ||
      slots.has(a.slotIndex) ||
      typeof a.augmentId !== 'string' ||
      typeof item.baseItemId !== 'string' ||
      !augmentEffect(a.augmentId, item.baseItemId)
    )
      return false
    slots.add(a.slotIndex)
    return true
  })
  return (
    valid &&
    item.installedAugments.every(
      (a) =>
        augments.find((entry) => entry.id === a.augmentId)?.legacyLimitExempt ||
        withinAugmentLimit(
          item.installedAugments as NonNullable<
            ConcreteItem['installedAugments']
          >,
          a.augmentId,
        ),
    )
  )
}
export async function installAugment(
  state: ConcreteItem,
  slot: number,
  id: string,
  identity: string,
  signal: AbortSignal,
): Promise<AppliedItem> {
  if (!canInstallAugment(state, id, slot))
    throw new Error(socketText().unsupported)
  const response = await fetch('/api/v1/crafting/workbench/augments/install', {
    method: 'POST',
    headers: rulesetHeaders(identity),
    body: JSON.stringify({ state, slotIndex: slot, augmentId: id }),
    signal,
  })
  verifyRulesetResponse(response, identity)
  if (!response.ok) throw new Error(socketText().unsupported)
  const result = (await response.json()) as AppliedItem
  const installed = [
    ...(state.installedAugments ?? []).filter((a) => a.slotIndex !== slot),
    { slotIndex: slot, augmentId: id },
  ].sort((a, b) => a.slotIndex - b.slotIndex)
  const expected = { ...state, installedAugments: installed }
  const stable = (v: unknown): unknown =>
    Array.isArray(v)
      ? v.map(stable)
      : v && typeof v === 'object'
        ? Object.fromEntries(
            Object.entries(v)
              .filter(
                ([k, v]) =>
                  v != null &&
                  !(['fractured', 'crafted'].includes(k) && v === false) &&
                  !(k === 'desecrated' && Array.isArray(v) && !v.length),
              )
              .sort(([a], [b]) => a.localeCompare(b))
              .map(([k, v]) => [k, stable(v)]),
          )
        : v
  const normalized = (s: ConcreteItem) => JSON.stringify(stable(s))
  if (
    result.rulesetIdentity !== identity ||
    !result.applied ||
    result.action !== 'INSTALL_AUGMENT' ||
    normalized(result.state) !== normalized(expected) ||
    result.events.length ||
    result.consumedOmens.length ||
    !augmentShape(result.state as unknown as Record<string, unknown>)
  )
    throw new Error(socketText().unsupported)
  return result
}
const messages = {
  en: ['Augments', 'Socket', 'Empty', 'Unsupported Augment or item state.'],
  ko: [
    '증강물',
    '소켓',
    '비어 있음',
    '지원하지 않는 증강물 또는 아이템 상태입니다.',
  ],
  'zh-CN': ['增强物', '插槽', '空', '不支持的增强物或物品状态。'],
  'zh-TW': ['增幅物', '插槽', '空', '不支援的增幅物或物品狀態。'],
  ja: [
    '増強アイテム',
    'ソケット',
    '空',
    '未対応の増強アイテムまたは状態です。',
  ],
  es: [
    'Aumentos',
    'Engarce',
    'Vacío',
    'Aumento o estado de objeto no compatible.',
  ],

  ru: [
    'Дополнения',
    'Розетка',
    'Пустой',
    'Неподдерживаемое улучшение или состояние предмета.',
  ],
  th: [
    'ไอเทมเสริม',
    'ช่องใส่',
    'ว่างเปล่า',
    'Augment หรือสถานะรายการไม่รองรับ',
  ],
} as const
export function socketText() {
  const m = messages[getLocale()]
  return { tab: m[0], socket: m[1], empty: m[2], unsupported: m[3] }
}

/** Unknown identity is a display label; the observed effect stays independent of affix rolls. */
export function unidentifiedRuneEffects(state: ConcreteItem): ItemCardLine[] {
  const itemClass = basePolicy(state.baseItemId)?.itemClass ?? ''
  return observedAugmentEffects(state).map((source, index) => {
    const text = source
      .replace(/^\{.*?}\s*/, '')
      .replace(/\s*\(rune\)$/, '')
      .trim()
    const matches = augments.filter(
      (entry) => entry.effects[itemClass]?.en === text,
    )
    const translations = new Set(
      matches
        .map((entry) => entry.effects[itemClass]?.[getLocale()])
        .filter(Boolean),
    )
    const effect = translations.size === 1 ? [...translations][0]! : text
    return {
      id: `observed-augment-${index}`,
      text: `${translate('base.unidentified_rune')}: ${effect}`,
      kind: 'rune',
    }
  })
}
