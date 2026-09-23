import { useCallback, useEffect, useMemo, useState } from 'react'
import type { PropsWithChildren } from 'react'
import { LocaleContext } from './context'
import { initialLocale, localeStorageKey, translate } from './messages'
import type { Locale, Translate } from './messages'

export function LocaleProvider({
  children,
  initialLanguage,
}: PropsWithChildren<{ initialLanguage?: Locale }>) {
  const [locale, updateLocale] = useState<Locale>(
    () => initialLanguage ?? initialLocale(),
  )
  const setLocale = useCallback((next: Locale) => {
    updateLocale(next)
    try {
      localStorage.setItem(localeStorageKey, next)
    } catch {
      /* Storage is optional. */
    }
  }, [])
  useEffect(() => {
    document.documentElement.lang = locale
  }, [locale])
  const t = useCallback<Translate>(
    (key, params) => translate(locale, key, params),
    [locale],
  )
  const value = useMemo(
    () => ({ locale, setLocale, t }),
    [locale, setLocale, t],
  )
  return (
    <LocaleContext.Provider value={value}>{children}</LocaleContext.Provider>
  )
}
