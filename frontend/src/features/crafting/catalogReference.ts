import { reportBrowserEvent } from '../../shared/telemetry/browserTelemetry'
import { verifyRulesetResponse } from './rulesetIdentity'
import { performanceSettings } from '../../shared/performance'

const catalogs = new Map<string, Promise<unknown>>()
/** Catalog URLs include the exact ruleset. Share concurrent loads and parsing across item levels;
 * cancelling one preview must not cancel another consumer of the same immutable reference. */
export function loadCatalogReference(
  url: string,
  identity: string,
  signal: AbortSignal,
): Promise<unknown> {
  if (signal.aborted)
    return Promise.reject(new DOMException('Aborted', 'AbortError'))
  const key = `${identity}:${url}`
  let pending = catalogs.get(key)
  if (!pending) {
    pending = fetch(url, {
      cache: 'default',
      signal: AbortSignal.timeout(performanceSettings.catalogRequestTimeoutMs),
    }).then(async (response) => {
      verifyRulesetResponse(response, identity)
      if (!response.ok) throw Error('Could not load catalog')
      return response.json() as Promise<unknown>
    })
    catalogs.set(key, pending)
    const entry = pending
    void pending.catch(() => {
      reportBrowserEvent('catalog_load')
      if (catalogs.get(key) === entry) catalogs.delete(key)
    })
    if (catalogs.size > performanceSettings.catalogRetentionEntries)
      catalogs.delete(catalogs.keys().next().value!)
  } else {
    catalogs.delete(key)
    catalogs.set(key, pending)
  }
  return new Promise((resolve, reject) => {
    const abort = () => reject(new DOMException('Aborted', 'AbortError'))
    signal.addEventListener('abort', abort, { once: true })
    pending
      .then(resolve, reject)
      .finally(() => signal.removeEventListener('abort', abort))
  })
}
