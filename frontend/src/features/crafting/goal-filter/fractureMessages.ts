import type { Locale } from '../../../shared/i18n/i18n'

export const fractureMessages: Record<Locale, { label: string; help: string }> =
  {
    en: {
      label: 'Fractured',
      help: 'Only one explicit modifier on Rare equipment can be fractured. Turn it off before changing rarity.',
    },
    ko: {
      label: '분열',
      help: '희귀 장비의 명시 속성 1개만 분열할 수 있습니다. 희귀도를 바꾸려면 먼저 분열을 끄세요.',
    },
    'zh-CN': {
      label: '破裂',
      help: '稀有装备只能设置一个破裂词缀。更改稀有度前请先关闭破裂。',
    },
    'zh-TW': {
      label: '破裂',
      help: '稀有裝備只能設定一個破裂詞綴。變更稀有度前請先關閉破裂。',
    },
    ja: {
      label: 'フラクチャー',
      help: 'レア装備の明示モッド1個のみ設定できます。レアリティ変更前に解除してください。',
    },
    es: {
      label: 'Fracturado',
      help: 'Solo se permite un modificador explícito fracturado en equipo raro. Desactívalo antes de cambiar la rareza.',
    },

    ru: {
      label: 'Сломанный',
      help: 'Только один явный модификатор редкого снаряжения может быть сломан. Выключите его, прежде чем менять редкость.',
    },
    th: {
      label: 'แตกหัก',
      help: 'ม็อดที่ชัดเจนสำหรับอุปกรณ์ระดับแรร์เพียงตัวเดียวเท่านั้นที่สามารถแตกร้าวได้ ปิดก่อนที่จะเปลี่ยนความหายาก',
    },
  }
