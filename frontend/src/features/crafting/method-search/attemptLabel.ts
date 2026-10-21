import type { Locale } from '../../../shared/i18n/i18n'
export function attemptLabel(locale: Locale, attempts: number) {
  const labels = {
    en: attempts === 1 ? '' : 'Within ' + attempts + ' attempts',
    ko: attempts === 1 ? '' : attempts + '회 이내',
    'zh-CN': attempts === 1 ? '' : attempts + '次尝试以内',
    'zh-TW': attempts === 1 ? '' : attempts + '次嘗試以內',
    ja: attempts === 1 ? '' : attempts + '回以内の試行',
    es: attempts === 1 ? '' : 'Dentro de ' + attempts + ' intentos',
  }
  return labels[locale]
}
