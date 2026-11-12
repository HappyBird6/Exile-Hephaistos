import { useSyncExternalStore } from 'react'
const changed = 'hephaistos:navigate'
const snapshot = () => location.pathname + location.search + location.hash
export function subscribeAppLocation(fn: () => void) {
  window.addEventListener('popstate', fn)
  window.addEventListener(changed, fn)
  return () => {
    window.removeEventListener('popstate', fn)
    window.removeEventListener(changed, fn)
  }
}
export const useAppLocation = () =>
  useSyncExternalStore(subscribeAppLocation, snapshot, snapshot)
export function navigate(
  href: string,
  { replace = false, scroll = true } = {},
) {
  const url = new URL(href, location.href)
  if (url.origin !== location.origin) {
    location.assign(url.href)
    return
  }
  window.history[replace ? 'replaceState' : 'pushState'](
    null,
    '',
    url.pathname + url.search + url.hash,
  )
  window.dispatchEvent(new Event(changed))
  if (scroll) window.scrollTo({ top: 0, behavior: 'instant' })
}
