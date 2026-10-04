import { useSyncExternalStore } from 'react'
import messages from './messages.json'
import terms from './gameTerms.json'

export const locales = ['en', 'ko', 'zh-CN', 'zh-TW', 'ja', 'es'] as const
export type Locale = (typeof locales)[number]
export const defaultLocale: Locale = 'ko'
export const localeNames: Record<Locale, string> = {
  en: 'English',
  ko: '한국어',
  'zh-CN': '简体中文 (CN)',
  'zh-TW': '繁體中文 (TW)',
  ja: '日本語',
  es: 'Español',
}
export const poe2dbPaths: Record<Locale, string> = {
  en: 'us',
  ko: 'kr',
  'zh-CN': 'cn',
  'zh-TW': 'tw',
  ja: 'jp',
  es: 'sp',
}
export const localeStorageKey = 'hephaistos.locale.v1'
export function resolveLocale(value: string | null | undefined): Locale {
  return locales.includes(value as Locale) ? (value as Locale) : defaultLocale
}
let selectedLocale: Locale | undefined
const listeners = new Set<() => void>()
export function getLocale(): Locale {
  if (selectedLocale) return selectedLocale
  try {
    return resolveLocale(window.localStorage.getItem(localeStorageKey))
  } catch {
    return defaultLocale
  }
}
export function setLocale(locale: Locale) {
  selectedLocale = locale
  try {
    window.localStorage.setItem(localeStorageKey, locale)
  } catch {
    /* In-memory choice still works. */
  }
  document.documentElement.lang = locale
  listeners.forEach((listener) => listener())
}
function subscribe(listener: () => void) {
  listeners.add(listener)
  const storage = (event: StorageEvent) => {
    if (event.key === localeStorageKey || event.key === null) {
      selectedLocale = undefined
      document.documentElement.lang = getLocale()
      listener()
    }
  }
  window.addEventListener('storage', storage)
  return () => {
    listeners.delete(listener)
    window.removeEventListener('storage', storage)
  }
}
type Parameters = Record<string, string | number>
export type MessageKey = keyof typeof messages.en
const dictionaries: Record<
  Locale,
  Partial<Record<MessageKey, string>>
> = messages
const uiKeys = new Map(
  Object.entries(messages.en).map(([key, value]) => [value, key as MessageKey]),
)
export function translate(
  key: MessageKey,
  params: Parameters = {},
  locale = getLocale(),
): string {
  const pluralKey =
    typeof params.count === 'number'
      ? (`${key}.${new Intl.PluralRules(locale).select(params.count)}` as MessageKey)
      : key
  const template =
    dictionaries[locale][pluralKey] ??
    dictionaries[locale][key] ??
    dictionaries.en[pluralKey] ??
    dictionaries.en[key] ??
    key
  return template.replace(/\{(\w+)\}/g, (match, name: string) =>
    params[name] === undefined ? match : String(params[name]),
  )
}
type GameTerm = {
  name: string
  lines: string[]
  itemKey: string
  sourceUrl: string
}
const gameDictionaries: Record<Locale, Record<string, GameTerm>> = terms
export function gameTerm(id: string, locale = getLocale()) {
  const key = id.startsWith('Metadata/')
    ? (Object.keys(gameDictionaries.en).find(
        (key) => gameDictionaries.en[key]?.itemKey === id,
      ) ?? id)
    : id
  const local = gameDictionaries[locale][key]
  const english = gameDictionaries.en[key]
  return {
    data: local ?? english,
    fallback: !local && locale !== 'en',
    language: local ? locale : 'en',
  }
}
export function gameName(
  id: string,
  english: string,
  locale = getLocale(),
): string {
  return gameTerm(id, locale).data?.name ?? english
}
export function matchesGameName(
  id: string,
  english: string,
  query: string,
  locale = getLocale(),
) {
  const needle = query.trim().toLocaleLowerCase(locale)
  return [english, gameName(id, english, locale), id].some((name) =>
    name.toLocaleLowerCase(locale).includes(needle),
  )
}
export function formatNumber(
  value: number,
  options?: Intl.NumberFormatOptions,
  locale = getLocale(),
) {
  return new Intl.NumberFormat(locale, options).format(value)
}
export function formatPercent(
  value: number,
  maximumFractionDigits = 6,
  locale = getLocale(),
) {
  return formatNumber(
    value,
    { style: 'percent', maximumFractionDigits },
    locale,
  )
}
export function formatDate(
  value: string | number | Date,
  locale = getLocale(),
) {
  return new Intl.DateTimeFormat(locale, {
    dateStyle: 'medium',
    timeZone: 'UTC',
  }).format(new Date(value))
}
export function localizedSource(url: string, locale = getLocale()) {
  return url.replace(
    /^https:\/\/poe2db\.tw\/(?:us|kr|cn|tw|jp|sp)\//,
    `https://poe2db.tw/${poe2dbPaths[locale]}/`,
  )
}
// Transitional adapter for existing service strings. Game identities never use this lookup.
export function uiText(english: string, locale = getLocale()) {
  const key = uiKeys.get(english)
  return key ? translate(key, {}, locale) : english
}
export function useI18n() {
  const locale = useSyncExternalStore(subscribe, getLocale, () => defaultLocale)
  return {
    locale,
    setLocale,
    t: (key: MessageKey, params?: Parameters) => translate(key, params, locale),
    name: (id: string, english: string) => gameName(id, english, locale),
  }
}
