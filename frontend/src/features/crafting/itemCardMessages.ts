import type { Locale } from '../../shared/i18n/i18n'

export const itemCardMessages: Record<
  Locale,
  { sanctified: string; lastCurrencyAffected: string }
> = {
  en: {
    sanctified: 'Sanctified',
    lastCurrencyAffected: 'Affected by the last currency application',
  },
  ko: {
    sanctified: '축성됨',
    lastCurrencyAffected: '직전 화폐 적용에 영향을 받은 옵션',
  },
  'zh-CN': {
    sanctified: '已圣化',
    lastCurrencyAffected: '受上次通货使用影响的词缀',
  },
  'zh-TW': {
    sanctified: '已聖化',
    lastCurrencyAffected: '受上次通貨使用影響的詞綴',
  },
  ja: {
    sanctified: '聖別済み',
    lastCurrencyAffected: '直前の通貨使用で影響を受けたモッド',
  },
  es: {
    sanctified: 'Santificado',
    lastCurrencyAffected: 'Modificador afectado por el último uso de moneda',
  },

  ru: {
    sanctified: 'Освященный',
    lastCurrencyAffected: 'Изменено последним применением валюты',
  },
  th: {
    sanctified: 'ศักดิ์สิทธิ์',
    lastCurrencyAffected: 'เปลี่ยนแปลงจากเคอร์เรนซีที่ใช้ล่าสุด',
  },
}
