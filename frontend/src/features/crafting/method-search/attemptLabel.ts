import type { Locale } from '../../../shared/i18n/i18n'
export function attemptLabel(locale: Locale, attempts: number) {
  const labels = {
    en: attempts === 1 ? 'Single attempt' : 'Within ' + attempts + ' attempts',
    ko: attempts === 1 ? '1회 시도' : attempts + '회 이내',
    'zh-CN': attempts === 1 ? '单次尝试' : attempts + '次尝试以内',
    'zh-TW': attempts === 1 ? '單次嘗試' : attempts + '次嘗試以內',
    ja: attempts === 1 ? '1回の試行' : attempts + '回以内の試行',
    es: attempts === 1 ? 'Un intento' : 'Dentro de ' + attempts + ' intentos',
  }
  return labels[locale]
}
