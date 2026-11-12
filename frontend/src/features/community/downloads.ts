import { accountScope } from '../account/auth'
import { boardRequest, type Stats } from './api'
import type { ReuseAction } from './reuse'
type Pending = { id: string; action: ReuseAction }
const key = () => `hephaistos.community.downloads:${accountScope() ?? 'guest'}`
function read(storageKey = key()): Pending[] {
  try {
    const parsed: unknown = JSON.parse(localStorage.getItem(storageKey) ?? '[]')
    return Array.isArray(parsed)
      ? parsed
          .filter(
            (p) =>
              p &&
              typeof p.id === 'string' &&
              ['import', 'craft', 'favorite'].includes(p.action),
          )
          .slice(-100)
      : []
  } catch {
    return []
  }
}
let running: Promise<void> | null = null
export async function recordDownload(id: string, action: ReuseAction) {
  const storageKey = key()
  try {
    localStorage.setItem(
      storageKey,
      JSON.stringify([
        ...read(storageKey).filter((p) => p.id !== id),
        { id, action },
      ]),
    )
  } catch {
    /* A counter must never undo a successful item save. */
  }
  try {
    const stats = await boardRequest<Stats>(
      'posts/' + encodeURIComponent(id) + '/download',
      {
        method: 'POST',
        body: JSON.stringify({ action }),
      },
    )
    localStorage.setItem(
      storageKey,
      JSON.stringify(read(storageKey).filter((p) => p.id !== id)),
    )
    return stats
  } catch {
    /* Retain the pending count for the next board visit. */
  }
}
export function flushDownloads(onCounted?: (id: string, stats: Stats) => void) {
  if (running) return running
  running = (async () => {
    const scope = accountScope()
    for (const p of read()) {
      if (accountScope() !== scope) return
      const stats = await recordDownload(p.id, p.action)
      if (stats && accountScope() === scope) onCounted?.(p.id, stats)
    }
  })().finally(() => {
    running = null
  })
  return running
}
