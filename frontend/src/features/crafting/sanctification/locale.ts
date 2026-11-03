import type { Locale } from '../../../shared/i18n/i18n'
const labels: Record<Locale, string> = {
  en: 'Sanctified',
  ko: '축성됨',
  'zh-CN': '已圣化',
  'zh-TW': '已聖化',
  ja: '聖別化',
  es: 'Santificado',
}
export function sanctifiedLabel(locale: Locale): string {
  return labels[locale]
}
