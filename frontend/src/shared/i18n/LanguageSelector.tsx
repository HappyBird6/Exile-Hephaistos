import { useId } from 'react'
import { useI18n } from './context'
import { isLocale, locales } from './messages'

export function LanguageSelector() {
  const id = useId()
  const { locale, setLocale, t } = useI18n()
  if (Object.keys(locales).length < 2) return null
  return (
    <label className="language-selector" htmlFor={id}>
      <span>{t('language')}</span>
      <select
        id={id}
        value={locale}
        onChange={(event) => {
          if (isLocale(event.target.value)) setLocale(event.target.value)
        }}
      >
        {Object.entries(locales).map(([key, language]) => (
          <option key={key} value={key}>
            {language.label}
          </option>
        ))}
      </select>
    </label>
  )
}
