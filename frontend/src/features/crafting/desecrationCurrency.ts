import type { AbyssState } from './AbyssPanel'
import type { Initial } from './craftingApi'
import type { ConcreteItem } from './workbenchApi'
import { validAbyssResponse } from './abyssResponse'
import { rulesetHeaders, verifyRulesetResponse } from './rulesetIdentity'

export const boneNames = {
  Gnawed_Rib: 'Gnawed Rib',
  Preserved_Rib: 'Preserved Rib',
  Ancient_Rib: 'Ancient Rib',
  Gnawed_Jawbone: 'Gnawed Jawbone',
  Preserved_Jawbone: 'Preserved Jawbone',
  Ancient_Jawbone: 'Ancient Jawbone',
  Gnawed_Collarbone: 'Gnawed Collarbone',
  Preserved_Collarbone: 'Preserved Collarbone',
  Ancient_Collarbone: 'Ancient Collarbone',
} as const
export const isBone = (id: string): id is keyof typeof boneNames =>
  Object.hasOwn(boneNames, id)
export const necromancySide = (
  omens: readonly string[],
): 'PREFIX' | 'SUFFIX' | undefined =>
  omens.includes('Omen_of_Sinistral_Necromancy')
    ? 'PREFIX'
    : omens.includes('Omen_of_Dextral_Necromancy')
      ? 'SUFFIX'
      : undefined
export const boneText = {
  en: [
    'Record the observed outcome: choose its prefix/suffix side and, at six modifiers, the removed modifier. No random outcome is generated. Reveal candidates separately.',
    'Observed affix side',
    'Choose…',
    'Prefix',
    'Suffix',
    'Removed modifier (full item only)',
    'Check the currency, item class, level, slots and observed outcome. Only one Necromancy omen can be active. The item has not changed.',
  ],
  ko: [
    '관측한 적용 결과를 기록합니다. 접두·접미를 선택하고, 6옵션이면 제거된 옵션도 선택하세요. 결과를 무작위로 생성하지 않습니다. 후보 드러내기는 별도 단계입니다.',
    '관측한 옵션 종류',
    '선택…',
    '접두',
    '접미',
    '제거된 옵션 (6옵션 아이템만)',
    '화폐·장비 종류·레벨·슬롯과 관측 결과를 확인하고 징조는 접두 또는 접미 Necromancy 하나만 활성화하세요. 아이템은 변경되지 않았습니다.',
  ],
  'zh-CN': [
    '记录观察结果：选择前缀或后缀；满六词缀时选择被移除的词缀。不生成随机结果。候选揭示为单独步骤。',
    '观察到的词缀类型',
    '选择…',
    '前缀',
    '后缀',
    '被移除的词缀（仅满词缀时）',
    '请检查通货、装备类型、等级、槽位和观察结果，仅可启用一个死灵预兆。物品未改变。',
  ],
  'zh-TW': [
    '記錄觀察結果：選擇前綴或後綴；滿六詞綴時選擇被移除的詞綴。不產生隨機結果。候選揭示為獨立步驟。',
    '觀察到的詞綴類型',
    '選擇…',
    '前綴',
    '後綴',
    '被移除的詞綴（僅滿詞綴時）',
    '請檢查通貨、裝備類型、等級、欄位及觀察結果，僅可啟用一個死靈預兆。物品未改變。',
  ],
  ja: [
    '観測した結果を記録します。プレフィックスかサフィックスを選び、6モッドなら除去されたモッドも選択してください。ランダムな結果は生成しません。候補の開示は別の手順です。',
    '観測したモッドの種類',
    '選択…',
    'プレフィックス',
    'サフィックス',
    '除去されたモッド（6モッド時のみ）',
    '通貨、装備の種類、レベル、枠、観測結果を確認し、ネクロマンシーのオーメンは1つだけ有効にしてください。アイテムは変更されていません。',
  ],
  es: [
    'Registra el resultado observado: elige prefijo o sufijo y, con seis modificadores, el modificador eliminado. No se genera un resultado aleatorio. Revela los candidatos por separado.',
    'Tipo de modificador observado',
    'Elegir…',
    'Prefijo',
    'Sufijo',
    'Modificador eliminado (solo objeto lleno)',
    'Comprueba moneda, clase, nivel, espacios y resultado observado. Solo puede estar activo un presagio de nigromancia. El objeto no ha cambiado.',
  ],
} as const

export async function applyBone(
  state: ConcreteItem,
  currency: string,
  side: string,
  removedSlotId: string,
  activeOmens: string[],
  initial: Initial,
  signal: AbortSignal,
  errorMessage: string,
): Promise<AbyssState> {
  const forcedSide = necromancySide(activeOmens)
  if (
    activeOmens.length > 1 ||
    (activeOmens.length === 1 && !forcedSide) ||
    (forcedSide && side && forcedSide !== side)
  )
    throw new Error(errorMessage)
  side = forcedSide ?? side
  // ItemState canonicalizes explicit IDs before Abyss import. Saved observation
  // order can differ, so map the user's chosen modifier to that wire slot.
  const removed = state.explicits.find(
    (_, index) => `explicit-${index}` === removedSlotId,
  )
  const canonical = [...state.explicits].sort((a, b) =>
    a.modifierId < b.modifierId ? -1 : a.modifierId > b.modifierId ? 1 : 0,
  )
  const wireRemoval = removed
    ? `explicit-${canonical.findIndex((m) => m.modifierId === removed.modifierId)}`
    : removedSlotId
  const response = await fetch('/api/v1/crafting/workbench/abyss/desecrate', {
    method: 'POST',
    headers: rulesetHeaders(initial.rulesetIdentity),
    signal,
    body: JSON.stringify({
      state,
      currency,
      side: side || null,
      removedSlotId: wireRemoval || null,
      activeOmens,
    }),
  })
  verifyRulesetResponse(response, initial.rulesetIdentity)
  if (!response.ok) throw new Error(errorMessage)
  const value: unknown = await response.json()
  if (!validAbyssResponse('desecrate', value, state, initial))
    throw new Error(errorMessage)
  const sameModifier = (
    a: ConcreteItem['explicits'][number],
    b: ConcreteItem['explicits'][number],
  ) =>
    a.modifierId === b.modifierId &&
    !!a.fractured === !!b.fractured &&
    Object.keys(a.values).length === Object.keys(b.values).length &&
    Object.entries(a.values).every(([key, v]) => b.values[key] === v)
  const result = value as AbyssState
  const fresh = result.slots.filter((s) => s.desecrated)
  const retained = result.slots.filter((s) => !s.desecrated)
  const expected = state.explicits.filter(
    (_, i) => `explicit-${i}` !== removedSlotId,
  )
  if (
    !state.implicits.every((m) =>
      result.item.implicits.some((n) => sameModifier(m, n)),
    ) ||
    fresh.length !== 1 ||
    fresh[0]!.modifier !== null ||
    fresh[0]!.side !== side ||
    result.offer !== null ||
    (fresh[0]!.minimumModifierLevel ?? null) !==
      (currency.startsWith('Ancient_') ? 40 : null) ||
    retained.length !== expected.length ||
    !expected.every((m) =>
      retained.some((s) => s.modifier !== null && sameModifier(m, s.modifier)),
    )
  )
    throw new Error(errorMessage)
  return result
}
