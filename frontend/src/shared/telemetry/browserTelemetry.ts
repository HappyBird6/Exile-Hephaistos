/** Only bounded event categories and counts leave the browser; never item text, messages or URLs. */
export type BrowserEvent =
  | 'view_workbench'
  | 'view_history'
  | 'runtime_error'
  | 'promise_error'
  | 'history_save'
  | 'catalog_load'
const pending = new Map<BrowserEvent, number>()
let timer: ReturnType<typeof setTimeout> | undefined
export function reportBrowserEvent(kind: BrowserEvent) {
  if (typeof window === 'undefined' || location.pathname.startsWith('/admin'))
    return
  pending.set(kind, Math.min(100, (pending.get(kind) ?? 0) + 1))
  timer ??= setTimeout(flush, 15000)
}
function flush() {
  if (timer) clearTimeout(timer)
  timer = undefined
  if (!pending.size) return
  const events = [...pending].map(([kind, count]) => ({ kind, count }))
  pending.clear()
  void fetch('/api/telemetry', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(events),
    keepalive: true,
  }).catch(() => {})
}
export function startBrowserTelemetry() {
  if (location.pathname.startsWith('/admin')) return
  reportBrowserEvent(
    location.pathname.startsWith('/history/')
      ? 'view_history'
      : 'view_workbench',
  )
  window.addEventListener('error', () => reportBrowserEvent('runtime_error'))
  window.addEventListener('unhandledrejection', () =>
    reportBrowserEvent('promise_error'),
  )
  window.addEventListener('pagehide', flush)
}
