import { craftingFetch as fetch } from '../../shared/gameIdTransport'
import { reportBrowserEvent } from '../../shared/telemetry/browserTelemetry'
import { verifyRulesetResponse } from './rulesetIdentity'
import { performanceSettings } from '../../shared/performance'

const catalogs = new Map<string, unknown>()
const loading = new Map<string, Promise<unknown>>()
let retryAfter = 0
/** Completed catalogs use bounded LRU retention. In-flight references remain shared until settled;
 * cancelling one preview must not cancel another consumer of the same immutable reference. */
export function loadCatalogReference(
  url: string,
  identity: string,
  signal: AbortSignal,
): Promise<unknown> {
  if (signal.aborted)
    return Promise.reject(new DOMException('Aborted', 'AbortError'))
  const key = `${identity}:${url}`
  if (catalogs.has(key)) {
    const value = catalogs.get(key)
    catalogs.delete(key)
    catalogs.set(key, value)
    return Promise.resolve(value)
  }
  let pending = loading.get(key)
  if (!pending) {
    // Honor an edge/backend rate-limit even when its response lacks a ruleset header.
    // Callers may explicitly retry later; this loader never schedules automatic retry requests.
    if (Date.now() < retryAfter)
      return Promise.reject(
        new Error('Catalog requests are temporarily limited.'),
      )
    pending = fetch(url, {
      cache: 'default',
      signal: AbortSignal.timeout(performanceSettings.catalogRequestTimeoutMs),
    })
      .then(async (response) => {
        if (response.status === 429) {
          const header = response.headers.get('Retry-After')
          const delay =
            header && /^\d+(\.\d+)?$/.test(header.trim())
              ? Number(header) * 1000
              : header
                ? Date.parse(header) - Date.now()
                : NaN
          retryAfter = Math.max(
            retryAfter,
            Date.now() +
              (Number.isFinite(delay) && delay > 0
                ? delay
                : performanceSettings.catalogRateLimitCooldownMs),
          )
        }
        if (!response.ok) throw Error('Could not load catalog')
        verifyRulesetResponse(response, identity)
        return response.json() as Promise<unknown>
      })
      .then((value) => {
        catalogs.set(key, value)
        if (catalogs.size > performanceSettings.catalogRetentionEntries)
          catalogs.delete(catalogs.keys().next().value!)
        return value
      })
    loading.set(key, pending)
    const entry = pending
    void pending.then(
      () => {
        if (loading.get(key) === entry) loading.delete(key)
      },
      () => {
        reportBrowserEvent('catalog_load')
        if (loading.get(key) === entry) loading.delete(key)
      },
    )
  }
  return new Promise((resolve, reject) => {
    const abort = () => reject(new DOMException('Aborted', 'AbortError'))
    signal.addEventListener('abort', abort, { once: true })
    pending
      .then(resolve, reject)
      .finally(() => signal.removeEventListener('abort', abort))
  })
}
