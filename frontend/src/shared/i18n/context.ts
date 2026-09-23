import { createContext, useContext } from 'react'
import type { Locale, Translate } from './messages'

export const LocaleContext = createContext<{
  locale: Locale
  setLocale: (locale: Locale) => void
  t: Translate
} | null>(null)

export function useI18n() {
  const value = useContext(LocaleContext)
  if (!value) throw new Error('LocaleProvider is required')
  return value
}
