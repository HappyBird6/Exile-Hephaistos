import { useSyncExternalStore } from 'react'
import type { Locale } from '../../shared/i18n/i18n'

// Character-side condition, shared by workbench/history views; never a consumed crafting action.
const key = 'hephaistos.workbench.bonded.v1'
const event = 'hephaistos:bonded'
export const bondedEnabled = () => {
  try {
    return (
      typeof localStorage !== 'undefined' &&
      localStorage.getItem(key) === 'true'
    )
  } catch {
    return false
  }
}
export function setBonded(enabled: boolean) {
  try {
    localStorage.setItem(key, String(enabled))
  } catch {
    /* Storage may be unavailable. */
  }
  window.dispatchEvent(new Event(event))
}
const subscribe = (notify: () => void) => {
  window.addEventListener(event, notify)
  window.addEventListener('storage', notify)
  return () => {
    window.removeEventListener(event, notify)
    window.removeEventListener('storage', notify)
  }
}
export const useBonded = () =>
  useSyncExternalStore(subscribe, bondedEnabled, () => false)
export const bondedLabel: Record<Locale, string> = {
  en: 'Bonded',
  ko: '결속',
  'zh-CN': '羁绊',
  'zh-TW': '命定',
  ja: '絆',
  es: 'Chamánico',
  ru: 'Связаны',
  th: 'สายสัมพันธ์',
}
