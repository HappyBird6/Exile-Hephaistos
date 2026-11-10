import type { Locale } from '../../../shared/i18n/i18n'
export const customExaltMessages: Record<
  Locale,
  {
    title: string
    description: string
    choose: string
    chance: string
    apply: string
    cancel: string
    empty: string
    error: string
  }
> = {
  en: {
    description:
      'Enable this mode, then use an Exalted Orb on the item to choose its modifier and tier. Values are rolled randomly, and the natural selection chance is shown.',
    title: 'Custom Exalted Orb',
    choose: 'Choose modifier',
    chance: 'Natural chance',
    apply: 'Apply',
    cancel: 'Cancel',
    empty: 'No eligible modifiers.',
    error: 'Could not apply the selected modifier.',
  },
  ko: {
    description:
      '이 모드를 켠 뒤 엑잘티드 오브를 아이템에 사용하면 속성과 티어를 선택할 수 있습니다. 수치는 무작위로 정해지며, 해당 속성의 등장 확률도 표시됩니다.',
    title: '확정 옵션 엑잘',
    choose: '속성 선택',
    chance: '등장 확률',
    apply: '적용',
    cancel: '취소',
    empty: '추가할 수 있는 속성이 없습니다.',
    error: '선택한 속성을 적용하지 못했습니다.',
  },
  'zh-CN': {
    description:
      '开启后，对物品使用崇高石即可选择词缀和阶级。数值随机生成，并显示该词缀的自然出现概率。',
    title: '自选崇高石',
    choose: '选择词缀',
    chance: '自然出现概率',
    apply: '应用',
    cancel: '取消',
    empty: '没有可添加的词缀。',
    error: '无法应用所选词缀。',
  },
  'zh-TW': {
    description:
      '開啟後，對物品使用崇高石即可選擇詞綴與階級。數值隨機產生，並顯示該詞綴的自然出現機率。',
    title: '自選崇高石',
    choose: '選擇詞綴',
    chance: '自然出現機率',
    apply: '套用',
    cancel: '取消',
    empty: '沒有可新增的詞綴。',
    error: '無法套用所選詞綴。',
  },
  ja: {
    description:
      '有効にしてからアイテムに高貴なオーブを使うと、モッドとティアを選べます。数値はランダムに決まり、出現確率も表示されます。',
    title: 'カスタム高貴なオーブ',
    choose: 'モッドを選択',
    chance: '出現確率',
    apply: '適用',
    cancel: 'キャンセル',
    empty: '追加できるモッドがありません。',
    error: '選択したモッドを適用できませんでした。',
  },
  es: {
    description:
      'Activa este modo y usa un orbe exaltado en el objeto para elegir el modificador y su nivel. Los valores son aleatorios y se muestra su probabilidad natural.',
    title: 'Orbe exaltado personalizado',
    choose: 'Elegir modificador',
    chance: 'Probabilidad natural',
    apply: 'Aplicar',
    cancel: 'Cancelar',
    empty: 'No hay modificadores disponibles.',
    error: 'No se pudo aplicar el modificador.',
  },
  ru: {
    description:
      'Включите режим и примените сферу возвышения к предмету, чтобы выбрать свойство и его тир. Значения случайны; шанс появления свойства указан рядом.',
    title: 'Сфера возвышения с выбором',
    choose: 'Выберите свойство',
    chance: 'Шанс появления',
    apply: 'Применить',
    cancel: 'Отмена',
    empty: 'Нет доступных свойств.',
    error: 'Не удалось применить свойство.',
  },
  th: {
    description:
      'เปิดโหมดนี้แล้วใช้ Exalted Orb กับไอเทมเพื่อเลือกม็อดและเทียร์ ค่าม็อดจะสุ่ม และแสดงโอกาสปรากฏของม็อดนั้นด้วย',
    title: 'Exalted Orb แบบเลือกม็อด',
    choose: 'เลือกม็อด',
    chance: 'โอกาสปรากฏ',
    apply: 'ใช้',
    cancel: 'ยกเลิก',
    empty: 'ไม่มีม็อดที่เพิ่มได้',
    error: 'ไม่สามารถใช้ม็อดที่เลือกได้',
  },
}
export const exaltedActions = [
  'EXALTED',
  'GREATER_EXALTED',
  'PERFECT_EXALTED',
] as const
export type ExaltedAction = (typeof exaltedActions)[number]
export const isExaltedAction = (
  action: string | undefined,
): action is ExaltedAction => exaltedActions.some((a) => a === action)
export function currencyRank(action: string | undefined): string | undefined {
  if (
    !action ||
    !/^(?:(?:GREATER|PERFECT)_)?(?:TRANSMUTATION|AUGMENTATION|REGAL|EXALTED|CHAOS)$/.test(
      action,
    )
  )
    return undefined
  return action.startsWith('PERFECT_')
    ? 'III'
    : action.startsWith('GREATER_')
      ? 'II'
      : 'I'
}
export const chanceText = (chance: number, locale: Locale) =>
  new Intl.NumberFormat(locale, {
    style: 'percent',
    maximumSignificantDigits: 4,
  }).format(chance)
