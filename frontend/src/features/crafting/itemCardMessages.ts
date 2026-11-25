import type { Locale } from '../../shared/i18n/i18n'

export const itemCardMessages: Record<
  Locale,
  {
    replaceable: string
    tierRange: string
    sanctified: string
    lastCurrencyAffected: string
  }
> = {
  en: {
    tierRange: 'Target tier range',
    replaceable: 'One replaceable modifier',
    sanctified: 'Sanctified',
    lastCurrencyAffected: 'Affected by the last currency application',
  },
  ko: {
    tierRange: '목표의 허용 티어 범위',
    replaceable: '교체 가능한 옵션 1개',
    sanctified: '축성됨',
    lastCurrencyAffected: '직전 화폐 적용에 영향을 받은 옵션',
  },
  'zh-CN': {
    tierRange: '目标允许的词阶范围',
    replaceable: '1 条可替换词缀',
    sanctified: '已圣化',
    lastCurrencyAffected: '受上次通货使用影响的词缀',
  },
  'zh-TW': {
    tierRange: '目標允許的詞階範圍',
    replaceable: '1 條可替換詞綴',
    sanctified: '已聖化',
    lastCurrencyAffected: '受上次通貨使用影響的詞綴',
  },
  ja: {
    tierRange: '目標で許容されるティアの範囲',
    replaceable: '置き換え可能なモッド1個',
    sanctified: '聖別済み',
    lastCurrencyAffected: '直前の通貨使用で影響を受けたモッド',
  },
  es: {
    tierRange: 'Rango de grados del objetivo',
    replaceable: 'Un modificador reemplazable',
    sanctified: 'Santificado',
    lastCurrencyAffected: 'Modificador afectado por el último uso de moneda',
  },

  ru: {
    tierRange: 'Диапазон рангов цели',
    replaceable: 'Один заменяемый модификатор',
    sanctified: 'Освященный',
    lastCurrencyAffected: 'Изменено последним применением валюты',
  },
  th: {
    tierRange: 'ช่วงเทียร์ที่ตรงตามเป้าหมาย',
    replaceable: 'ม็อดที่เปลี่ยนได้ 1 ม็อด',
    sanctified: 'ศักดิ์สิทธิ์',
    lastCurrencyAffected: 'เปลี่ยนแปลงจากเคอร์เรนซีที่ใช้ล่าสุด',
  },
}
