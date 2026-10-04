import { useEffect } from 'react'
import { locales, localeNames, useI18n } from './i18n'
import type { Locale } from './i18n'
import './i18n.css'

export function LocaleSelector() {
  const { locale, setLocale, t } = useI18n()
  useEffect(() => {
    document.documentElement.lang = locale
  }, [locale])
  return (
    <label className="locale-selector">
      <span>{t('language')}</span>
      <select
        value={locale}
        onChange={(event) => setLocale(event.target.value as Locale)}
      >
        {locales.map((value) => (
          <option key={value} value={value} lang={value}>
            {localeNames[value]}
          </option>
        ))}
      </select>
    </label>
  )
}
