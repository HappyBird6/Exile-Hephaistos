import { isLocaleLoaded, loadLocaleData } from './localeData'
import { useSyncExternalStore } from 'react'
import { messages, terms } from './localeData'
import itemClasses from './itemClassNames.json'

import { locales, localeContract } from '../contracts/public'
export { locales }
export type Locale = (typeof locales)[number]
export const defaultLocale: Locale = localeContract.defaultLocale
export const localeNames = Object.fromEntries(
  locales.map((code) => [code, localeContract.locales[code].name]),
) as Record<Locale, string>
export const poe2dbPaths = Object.fromEntries(
  locales.map((code) => [code, localeContract.locales[code].sourcePath]),
) as Record<Locale, string>
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
/** Load the saved preference before render; keep it for the next attempt if the network fails. */
export async function prepareLocale() {
  try {
    await loadLocaleData(getLocale())
  } catch {
    selectedLocale = 'en'
    document.documentElement.lang = 'en'
  }
}
let localeRequest = 0
export function setLocale(locale: Locale) {
  const request = ++localeRequest
  if (!isLocaleLoaded(locale)) {
    return loadLocaleData(locale).then(() => {
      if (request === localeRequest) setLocale(locale)
    })
  }
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
      void Promise.resolve(setLocale(resolveLocale(event.newValue))).catch(
        () => {},
      )
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
  nameProvenance?: string
  linesLanguage?: string
}
const gameDictionaries: Record<Locale, Record<string, GameTerm>> = terms
const metadataKeys = new Map(
  Object.entries(gameDictionaries.en).map(([key, term]) => [term.itemKey, key]),
)
export function gameTerm(id: string, locale = getLocale()) {
  const key = id.startsWith('Metadata/') ? (metadataKeys.get(id) ?? id) : id
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
    /^https:\/\/poe2db\.tw\/(?:us|kr|cn|tw|jp|sp|ru|th)\//,
    `https://poe2db.tw/${poe2dbPaths[locale]}/`,
  )
}
// Transitional adapter for existing service strings. Game identities never use this lookup.
export function uiText(english: string, locale = getLocale()) {
  const itemClass = itemClasses[english as keyof typeof itemClasses]
  if (itemClass) return itemClass[locale]
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
