import type { GoalFilterLanguage } from './i18n'

export const levelMessages: Record<
  GoalFilterLanguage,
  {
    label: string
    conflict: string
    invalid: string
  }
> = {
  en: {
    label: 'Item level',
    conflict:
      'A selected tier needs a higher item level. Change its tier or restore the level.',
    invalid: 'Check the selected modifiers and rarity.',
  },
  ko: {
    label: '아이템 레벨',
    conflict:
      '선택한 티어에 더 높은 아이템 레벨이 필요합니다. 티어를 바꾸거나 레벨을 복원하세요.',
    invalid: '선택한 속성과 희귀도를 확인하세요.',
  },
  'zh-CN': {
    label: '物品等级',
    conflict: '所选阶级需要更高物品等级。请更改阶级或恢复等级。',
    invalid: '请检查所选属性和稀有度。',
  },
  'zh-TW': {
    label: '物品等級',
    conflict: '所選階級需要更高物品等級。請更改階級或恢復等級。',
    invalid: '請檢查所選屬性和稀有度。',
  },
  ja: {
    label: 'アイテムレベル',
    conflict:
      '選択したティアにはより高いアイテムレベルが必要です。ティアを変更するかレベルを戻してください。',
    invalid: '選択したモッドとレアリティを確認してください。',
  },
  es: {
    label: 'Nivel del objeto',
    conflict:
      'El tier seleccionado requiere un nivel mayor. Cambia el tier o restaura el nivel.',
    invalid: 'Revisa los modificadores y la rareza seleccionados.',
  },
}
