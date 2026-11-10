import { gameName, translate } from '../../../shared/i18n/i18n'
import type { Locale } from '../../../shared/i18n/i18n'
import type { Film } from '../workbenchHistory'

const dateLocales: Record<Locale, string> = {
  en: 'en-US',
  ko: 'ko-KR',
  'zh-CN': 'zh-CN',
  'zh-TW': 'zh-TW',
  ja: 'ja-JP',
  es: 'es-ES',

  ru: 'ru-RU',
  th: 'th-TH',
}

/** Defaults stay derived so changing language never overwrites the saved record. */
export function sessionTitle(film: Film, locale: Locale): string {
  if (typeof film.title === 'string' && film.title.trim()) return film.title
  const recordedBase = film.frames[0]?.state.baseItemId
  const baseId = typeof recordedBase === 'string' ? recordedBase : ''
  const item = gameName(
    baseId,
    translate('ui.equipment_base', {}, locale),
    locale,
  )
  const date = new Date(film.createdAt)
  if (!Number.isFinite(date.getTime())) return item
  const formatter = new Intl.DateTimeFormat(dateLocales[locale], {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  })
  const parts = formatter.formatToParts(date)
  const formatted =
    locale === 'ko'
      ? ['year', 'month', 'day']
          .map((type) => parts.find((part) => part.type === type)?.value)
          .join('.')
      : formatter.format(date)
  const time = new Intl.DateTimeFormat(dateLocales[locale], {
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
  return `${item}(${formatted} ${time})`
}
