import type { AbyssState } from './AbyssPanel'
import type { Initial } from './craftingApi'
import type { ConcreteItem } from './workbenchApi'
import { validAbyssResponse } from './abyssResponse'
import { rulesetHeaders, verifyRulesetResponse } from './rulesetIdentity'

export const boneNames = {
  Preserved_Cranium: 'Preserved Cranium',
  Altered_Collarbone: 'Altered Collarbone',
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
    'Record the observed outcome: choose its prefix/suffix side and, when full, the removed modifier. No random outcome is generated. Choose the reveal modifier separately.',
    'Observed affix side',
    'Choose…',
    'Prefix',
    'Suffix',
    'Removed modifier (full item only)',
    'Check the currency, item class, level, slots and observed outcome. Only one Necromancy omen can be active. The item has not changed.',
  ],
  ko: [
    '관측한 적용 결과를 기록합니다. 접두·접미를 선택하고, 슬롯이 가득 차면 제거된 옵션도 선택하세요. 결과를 무작위로 생성하지 않습니다. 드러낼 옵션은 별도로 선택합니다.',
    '관측한 옵션 종류',
    '선택…',
    '접두',
    '접미',
    '제거된 옵션 (슬롯이 가득 찬 경우)',
    '화폐·장비 종류·레벨·슬롯과 관측 결과를 확인하고 징조 조합을 확인하세요. Necromancy는 접두 또는 접미 하나만 활성화할 수 있습니다. 아이템은 변경되지 않았습니다.',
  ],
  'zh-CN': [
    '记录观察结果：选择前缀或后缀；词缀已满时选择被移除的词缀。不生成随机结果。候选揭示为单独步骤。',
    '观察到的词缀类型',
    '选择…',
    '前缀',
    '后缀',
    '被移除的词缀（仅满词缀时）',
    '请检查通货、装备类型、等级、槽位和观察结果，仅可启用一个死灵预兆。物品未改变。',
  ],
  'zh-TW': [
    '記錄觀察結果：選擇前綴或後綴；詞綴已滿時選擇被移除的詞綴。不產生隨機結果。候選揭示為獨立步驟。',
    '觀察到的詞綴類型',
    '選擇…',
    '前綴',
    '後綴',
    '被移除的詞綴（僅滿詞綴時）',
    '請檢查通貨、裝備類型、等級、欄位及觀察結果，僅可啟用一個死靈預兆。物品未改變。',
  ],
  ja: [
    '観測した結果を記録します。プレフィックスかサフィックスを選び、モッド枠が埋まっている場合は除去されたモッドも選択してください。ランダムな結果は生成しません。候補の開示は別の手順です。',
    '観測したモッドの種類',
    '選択…',
    'プレフィックス',
    'サフィックス',
    '除去されたモッド（モッド枠が埋まっている時のみ）',
    '通貨、装備の種類、レベル、枠、観測結果を確認し、ネクロマンシーのオーメンは1つだけ有効にしてください。アイテムは変更されていません。',
  ],
  es: [
    'Registra el resultado observado: elige prefijo o sufijo y, con los espacios llenos, el modificador eliminado. No se genera un resultado aleatorio. Revela los candidatos por separado.',
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
  observedSides?: ('PREFIX' | 'SUFFIX')[],
): Promise<AbyssState> {
  const putrefaction = activeOmens.includes('Omen_of_Putrefaction')
  if (
    putrefaction &&
    (activeOmens.length !== 1 ||
      !observedSides?.length ||
      observedSides.length > 6)
  )
    throw new Error(errorMessage)
  const poolNames: Record<string, string> = {
    Omen_of_the_Liege: 'AMANAMU',
    Omen_of_the_Sovereign: 'ULAMAN',
    Omen_of_the_Blackblooded: 'KURGAL',
  }
  const poolOmens = activeOmens.filter((id) => !!poolNames[id])
  const sideOmens = activeOmens.filter(
    (id) =>
      id === 'Omen_of_Sinistral_Necromancy' ||
      id === 'Omen_of_Dextral_Necromancy',
  )
  const forcedSide = necromancySide(activeOmens)
  if (
    sideOmens.length > 1 ||
    poolOmens.length > 1 ||
    (!putrefaction &&
      activeOmens.length !== sideOmens.length + poolOmens.length) ||
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
      ...(putrefaction
        ? { observedSides, side: null, removedSlotId: null }
        : {}),
    }),
  })
  verifyRulesetResponse(response, initial.rulesetIdentity)
  if (!response.ok) throw new Error(errorMessage)
  const value: unknown = await response.json()
  if (
    !validAbyssResponse(
      'desecrate',
      value,
      putrefaction ? { ...state, conditions: ['CORRUPTED'] } : state,
      initial,
    )
  )
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
  const expected = putrefaction
    ? []
    : state.explicits.filter((_, i) => `explicit-${i}` !== removedSlotId)
  if (
    !state.implicits.every((m) =>
      result.item.implicits.some((n) => sameModifier(m, n)),
    ) ||
    (fresh[0]?.revealPool ?? null) !==
      (currency === 'Altered_Collarbone'
        ? 'ALTERED'
        : poolOmens[0]
          ? poolNames[poolOmens[0]]
          : null) ||
    fresh.length !== (putrefaction ? observedSides!.length : 1) ||
    fresh.some(
      (slot, i) =>
        slot.modifier !== null ||
        slot.side !== (putrefaction ? observedSides![i] : side) ||
        (slot.revealPool ?? null) !== (fresh[0]?.revealPool ?? null) ||
        (slot.minimumModifierLevel ?? null) !==
          (currency.startsWith('Ancient_') ? 40 : null),
    ) ||
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

export const putrefactionText = {
  en: [
    'Enter the observed number of unrevealed prefixes and suffixes. All explicit modifiers will be replaced and the item will become corrupted.',
    'Observed prefix count',
    'Observed suffix count',
  ],
  ko: [
    '관측한 미공개 접두·접미 개수를 입력하세요. 기존 명시 옵션을 모두 대체하고 아이템을 타락 상태로 기록합니다.',
    '관측한 접두 개수',
    '관측한 접미 개수',
  ],
  'zh-CN': [
    '输入观察到的未揭示前缀和后缀数量。所有外延词缀将被替换，物品将被记录为已腐化。',
    '观察到的前缀数量',
    '观察到的后缀数量',
  ],
  'zh-TW': [
    '輸入觀察到的未揭示前綴和後綴數量。所有外延詞綴將被替換，物品將記錄為已汙染。',
    '觀察到的前綴數量',
    '觀察到的後綴數量',
  ],
  ja: [
    '観測した未公開のプレフィックスとサフィックスの数を入力します。すべての明示モッドを置き換え、アイテムをコラプト状態として記録します。',
    '観測したプレフィックス数',
    '観測したサフィックス数',
  ],
  es: [
    'Introduce el número observado de prefijos y sufijos sin revelar. Se sustituirán todos los modificadores explícitos y el objeto quedará corrupto.',
    'Número observado de prefijos',
    'Número observado de sufijos',
  ],
} as const
