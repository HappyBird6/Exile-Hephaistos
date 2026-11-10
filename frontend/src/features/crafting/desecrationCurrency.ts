import type { AbyssState } from './crafting-state/types'
import type { Initial } from './craftingApi'
import type { ConcreteItem } from './workbenchApi'
import { validAbyssResponse } from './abyssResponse'
import { rulesetHeaders, verifyRulesetResponse } from './rulesetIdentity'
import { baseAffixCapacity } from './baseRegistry'
import { isWorkbenchJewel, jewelCapacity } from './basicJewel'

import { boneConsumedOmens } from './crafting-state/activeOmens'
export {
  desecrationOmenIds,
  boneConsumedOmens,
  boneRemainingOmens,
} from './crafting-state/activeOmens'

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
    'Without a Necromancy omen, the simulator randomly chooses an available prefix or suffix. On a full item it randomly removes an eligible modifier. These are simulator policies; game probabilities are unverified. Choose the reveal modifier separately.',
    'Observed affix side',
    'Choose…',
    'Prefix',
    'Suffix',
    'Removed modifier (full item only)',
    'Check the currency, item class, level, slots and observed outcome. Only one Necromancy omen can be active.',
  ],
  ko: [
    'Necromancy 징조가 없으면 가능한 접두·접미 중 무작위로 결정합니다. 슬롯이 가득 차면 제거 가능한 옵션을 무작위로 제거합니다. 시뮬레이터 정책이며 실제 게임 확률은 미확인입니다. 드러낼 옵션은 직접 선택하세요.',
    '관측한 옵션 종류',
    '선택…',
    '접두',
    '접미',
    '제거된 옵션 (슬롯이 가득 찬 경우)',
    '화폐·장비 종류·레벨·슬롯과 관측 결과를 확인하고 징조 조합을 확인하세요. Necromancy는 접두 또는 접미 하나만 활성화할 수 있습니다.',
  ],
  'zh-CN': [
    '没有死灵预兆时，模拟器随机选择可用的前缀或后缀。槽位已满时随机移除可移除词缀。这是模拟器策略，游戏概率未经验证。揭示词缀仍需手动选择。',
    '观察到的词缀类型',
    '选择…',
    '前缀',
    '后缀',
    '被移除的词缀（仅满词缀时）',
    '请检查通货、装备类型、等级、槽位和观察结果，仅可启用一个死灵预兆。',
  ],
  'zh-TW': [
    '沒有死靈預兆時，模擬器隨機選擇可用的前綴或後綴。欄位已滿時隨機移除可移除詞綴。這是模擬器策略，遊戲機率未經驗證。揭示詞綴仍需手動選擇。',
    '觀察到的詞綴類型',
    '選擇…',
    '前綴',
    '後綴',
    '被移除的詞綴（僅滿詞綴時）',
    '請檢查通貨、裝備類型、等級、欄位及觀察結果，僅可啟用一個死靈預兆。',
  ],
  ja: [
    'ネクロマンシーのオーメンがなければ、使用可能なプレフィックスかサフィックスをランダムに選びます。枠が埋まっている場合は除去可能なモッドをランダムに除去します。シミュレーターの方針であり、実際のゲーム確率は未確認です。開示するモッドは手動で選択してください。',
    '観測したモッドの種類',
    '選択…',
    'プレフィックス',
    'サフィックス',
    '除去されたモッド（モッド枠が埋まっている時のみ）',
    '通貨、装備の種類、レベル、枠、観測結果を確認し、ネクロマンシーのオーメンは1つだけ有効にしてください。',
  ],
  es: [
    'Sin un presagio de nigromancia, el simulador elige al azar un prefijo o sufijo disponible. Con los espacios llenos elimina al azar un modificador elegible. Son reglas del simulador; las probabilidades del juego no están verificadas. Elige manualmente el modificador que se revela.',
    'Tipo de modificador observado',
    'Elegir…',
    'Prefijo',
    'Sufijo',
    'Modificador eliminado (solo objeto lleno)',
    'Comprueba moneda, clase, nivel, espacios y resultado observado. Solo puede estar activo un presagio de nigromancia.',
  ],

  ru: [
    'Без предзнаменования некромантии симулятор случайным образом выбирает доступный префикс или суффикс. У полного предмета случайным образом удаляется подходящий модификатор. Это политика симулятора; Вероятности игры не проверены. Выберите модификатор раскрытия отдельно.',
    'Наблюдаемая сторона аффикса',
    'Выберите…',
    'Префикс',
    'Суффикс',
    'Удален модификатор (только полный предмет)',
    'Проверьте валюту, класс предмета, уровень, слоты и наблюдаемый результат. Активным может быть только одно предзнаменование Некромантии.',
  ],
  th: [
    'หากไม่มีลางแห่งเวทมนตร์ เครื่องจำลองจะสุ่มเลือกพรีฟิกซ์หรือซัฟฟิกซ์ที่มีอยู่ เมื่อไอเท็มเต็ม มันจะสุ่มลบม็อดที่มีสิทธิ์ออก นี่คือนโยบายการจำลอง ความน่าจะเป็นของเกมยังไม่ได้รับการยืนยัน เลือกตัวปรับแต่งการเปิดเผยแยกกัน',
    'สังเกตด้านติด',
    'เลือก...',
    'พรีฟิกซ์',
    'ซัฟฟิกซ์',
    'ม็อดที่ถูกลบออก (เฉพาะรายการเต็มเท่านั้น)',
    'ตรวจสอบสกุลเงิน คลาสไอเท็ม ระดับ ช่อง และผลลัพธ์ที่สังเกตได้ มีลาง Necromancy เพียงอันเดียวเท่านั้นที่สามารถใช้งานได้',
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
  // Only the currency's triggered omens are sent/consumed. Other active omens,
  // including Omen_of_Sanctification, remain active for their own currency.
  activeOmens = boneConsumedOmens(activeOmens)
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
  if (signal.aborted) throw new DOMException('Aborted', 'AbortError')
  if (!response.ok) throw new Error(errorMessage)
  const value: unknown = await response.json()
  if (signal.aborted) throw new DOMException('Aborted', 'AbortError')
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
  const capacity = baseAffixCapacity(
    state.baseItemId,
    state.rarity,
    state.installedAugments,
  )
  const full =
    state.explicits.length ===
    (isWorkbenchJewel(state.baseItemId)
      ? jewelCapacity(state, 'PREFIX') + jewelCapacity(state, 'SUFFIX')
      : capacity.prefixes + capacity.suffixes)
  const expected = putrefaction
    ? []
    : state.explicits.filter((m) =>
        retained.some(
          (s) => s.modifier !== null && sameModifier(m, s.modifier),
        ),
      )
  const removedModifiers = state.explicits.filter((m) => !expected.includes(m))
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
        (putrefaction
          ? slot.side !== observedSides![i]
          : !!side && slot.side !== side) ||
        (slot.revealPool ?? null) !== (fresh[0]?.revealPool ?? null) ||
        (slot.minimumModifierLevel ?? null) !==
          (currency.startsWith('Ancient_') ? 40 : null),
    ) ||
    result.offer !== null ||
    (fresh[0]!.minimumModifierLevel ?? null) !==
      (currency.startsWith('Ancient_') ? 40 : null) ||
    retained.length !== expected.length ||
    (!putrefaction &&
      (removedModifiers.length !== (full ? 1 : 0) ||
        removedModifiers.some((m) => m.fractured) ||
        (!!removedSlotId &&
          (!removed ||
            !removedModifiers.some((m) => sameModifier(m, removed)))))) ||
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

  ru: [
    'Введите наблюдаемое количество невыявленных префиксов и суффиксов. Все явные модификаторы будут заменены, и предмет станет поврежденным.',
    'Наблюдаемое количество префиксов',
    'Наблюдаемое количество суффиксов',
  ],
  th: [
    'ป้อนจำนวนพรีฟิกซ์และซัฟฟิกซ์ที่ยังไม่ได้เปิดเผยที่สังเกตได้ ม็อดที่ชัดเจนทั้งหมดจะถูกแทนที่และรายการจะเสียหาย',
    'จำนวนพรีฟิกซ์ที่สังเกตได้',
    'จำนวนซัฟฟิกซ์ที่สังเกตได้',
  ],
} as const
