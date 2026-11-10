import type { Locale } from '../../../shared/i18n/i18n'
export const customExaltMessages: Record<
  Locale,
  {
    title: string
    choose: string
    chance: string
    apply: string
    cancel: string
    empty: string
    error: string
  }
> = {
  en: {
    title: 'Custom Exalted Orb',
    choose: 'Choose modifier',
    chance: 'Natural chance',
    apply: 'Apply',
    cancel: 'Cancel',
    empty: 'No eligible modifiers.',
    error: 'Could not apply the selected modifier.',
  },
  ko: {
    title: '확정 옵션 엑잘',
    choose: '속성 선택',
    chance: '등장 확률',
    apply: '적용',
    cancel: '취소',
    empty: '추가할 수 있는 속성이 없습니다.',
    error: '선택한 속성을 적용하지 못했습니다.',
  },
  'zh-CN': {
    title: '自选崇高石',
    choose: '选择词缀',
    chance: '自然出现概率',
    apply: '应用',
    cancel: '取消',
    empty: '没有可添加的词缀。',
    error: '无法应用所选词缀。',
  },
  'zh-TW': {
    title: '自選崇高石',
    choose: '選擇詞綴',
    chance: '自然出現機率',
    apply: '套用',
    cancel: '取消',
    empty: '沒有可新增的詞綴。',
    error: '無法套用所選詞綴。',
  },
  ja: {
    title: 'カスタム高貴なオーブ',
    choose: 'モッドを選択',
    chance: '出現確率',
    apply: '適用',
    cancel: 'キャンセル',
    empty: '追加できるモッドがありません。',
    error: '選択したモッドを適用できませんでした。',
  },
  es: {
    title: 'Orbe exaltado personalizado',
    choose: 'Elegir modificador',
    chance: 'Probabilidad natural',
    apply: 'Aplicar',
    cancel: 'Cancelar',
    empty: 'No hay modificadores disponibles.',
    error: 'No se pudo aplicar el modificador.',
  },
  ru: {
    title: 'Сфера возвышения с выбором',
    choose: 'Выберите свойство',
    chance: 'Шанс появления',
    apply: 'Применить',
    cancel: 'Отмена',
    empty: 'Нет доступных свойств.',
    error: 'Не удалось применить свойство.',
  },
  th: {
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
