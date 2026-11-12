import { accountScope } from '../account/auth'
import { boardRequest } from './api'
import type { ReuseAction } from './reuse'
type Pending = { id: string; action: ReuseAction }
const key = () => `hephaistos.community.downloads:${accountScope() ?? 'guest'}`
function read(): Pending[] {
  try {
    const parsed: unknown = JSON.parse(localStorage.getItem(key()) ?? '[]')
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
  try {
    localStorage.setItem(
      key(),
      JSON.stringify([...read().filter((p) => p.id !== id), { id, action }]),
    )
  } catch {
    /* A counter must never undo a successful item save. */
  }
  try {
    await boardRequest('posts/' + encodeURIComponent(id) + '/download', {
      method: 'POST',
      body: JSON.stringify({ action }),
    })
    localStorage.setItem(
      key(),
      JSON.stringify(read().filter((p) => p.id !== id)),
    )
  } catch {
    /* Retain the pending count for the next board visit. */
  }
}
export function flushDownloads() {
  if (running) return running
  running = (async () => {
    const scope = accountScope()
    for (const p of read()) {
      if (accountScope() !== scope) return
      await recordDownload(p.id, p.action)
    }
  })().finally(() => {
    running = null
  })
  return running
}
