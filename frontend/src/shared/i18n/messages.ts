import { en, enCurrencyNames } from './locales/en'
import { ko, koCurrencyNames } from './locales/ko'

export const locales = {
  en: { label: 'English', messages: en, currencies: enCurrencyNames },
  ko: { label: '한국어', messages: ko, currencies: koCurrencyNames },
}
export type Locale = keyof typeof locales
export type MessageKey = keyof typeof ko
export type MessageParams = Record<string, string | number>
export type Translate = (key: MessageKey, params?: MessageParams) => string
export const localeStorageKey = 'exile-hephaistos.locale'

export function isLocale(value: unknown): value is Locale {
  return typeof value === 'string' && Object.hasOwn(locales, value)
}

export function initialLocale(): Locale {
  try {
    const saved = localStorage.getItem(localeStorageKey)
    if (isLocale(saved)) return saved
  } catch {
    /* Language selection remains available when storage is blocked. */
  }
  for (const language of navigator.languages ?? [navigator.language]) {
    const locale = language.toLowerCase().split('-')[0]
    if (isLocale(locale)) return locale
  }
  return 'en'
}

export function translate(
  locale: Locale,
  key: MessageKey,
  params: MessageParams = {},
) {
  return locales[locale].messages[key].replace(
    /\{(\w+)\}/g,
    (match: string, name: string) =>
      params[name] === undefined ? match : String(params[name]),
  )
}
