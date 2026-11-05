import data from './augments.json'
import { basePolicy } from './baseRegistry'
import { getLocale } from '../../shared/i18n/i18n'
import { rulesetHeaders, verifyRulesetResponse } from './rulesetIdentity'
import type { AppliedItem, ConcreteItem } from './workbenchApi'

type Entry = {
  id: string
  names: Record<string, string>
  effects: Record<string, Record<string, string>>
  image: string
  selectionEnabled: boolean
}
export const augments: readonly Entry[] = data
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
  return effects?.[getLocale()] ?? effects?.en
}
export function canInstallAugment(state: ConcreteItem, id?: string): boolean {
  return Boolean(
    id &&
    isSelectableAugment(id) &&
    augmentEffect(id, state.baseItemId) &&
    (state.augmentSockets ?? 0) > 0 &&
    !state.conditions.length &&
    !state.observations &&
    !state.desecrated?.length,
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
  return item.installedAugments.every((a) => {
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
}
export async function installAugment(
  state: ConcreteItem,
  slot: number,
  id: string,
  identity: string,
  signal: AbortSignal,
): Promise<AppliedItem> {
  if (
    !canInstallAugment(state, id) ||
    state.augmentSockets == null ||
    slot < 0 ||
    slot >= state.augmentSockets
  )
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
                  !(k === 'fractured' && v === false) &&
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
  en: [
    'Augments',
    'Socket',
    'Empty',
    'Base socket effects; Bonded and final scaled totals are not calculated.',
    'Unsupported Augment or item state.',
    'Workbench socket limit: ordinary maximum +1.',
  ],
  ko: [
    '증강물',
    '소켓',
    '비어 있음',
    '기본 소켓 효과입니다. 결속 효과와 최종 배율 합계는 계산하지 않습니다.',
    '지원하지 않는 증강물 또는 아이템 상태입니다.',
    '작업대 소켓 상한: 일반 최대 +1.',
  ],
  'zh-CN': [
    '增强物',
    '插槽',
    '空',
    '基础插槽效果；不计算绑定效果或最终倍率。',
    '不支持的增强物或物品状态。',
    '工作台插槽上限：通常上限 +1。',
  ],
  'zh-TW': [
    '增幅物',
    '插槽',
    '空',
    '基礎插槽效果；不計算綁定效果或最終倍率。',
    '不支援的增幅物或物品狀態。',
    '工作台插槽上限：通常上限 +1。',
  ],
  ja: [
    '増強アイテム',
    'ソケット',
    '空',
    '基本ソケット効果。結合効果と最終倍率は計算しません。',
    '未対応の増強アイテムまたは状態です。',
    '作業台のソケット上限：通常上限 +1。',
  ],
  es: [
    'Aumentos',
    'Engarce',
    'Vacío',
    'Efectos básicos; no se calculan efectos vinculados ni totales escalados.',
    'Aumento o estado de objeto no compatible.',
    'Límite de engarces: máximo normal +1.',
  ],
} as const
export function socketText() {
  const m = messages[getLocale()]
  return {
    tab: m[0],
    socket: m[1],
    empty: m[2],
    effects: m[3],
    unsupported: m[4],
    policy: m[5],
  }
}
