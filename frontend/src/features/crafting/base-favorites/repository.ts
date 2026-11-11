import { sameItemState } from '../crafting-state/itemIdentity'
import type { Film } from '../workbenchHistory'
import { validStoredFilm } from '../workbenchHistory'
import { useSyncExternalStore } from 'react'
export const baseFavoritesKey = 'hephaistos.workbench.base-favorites.v1'
const listeners = new Set<() => void>()
const changed = () => listeners.forEach((listener) => listener())
if (typeof window !== 'undefined')
  window.addEventListener('storage', (e) => {
    if (e.key === baseFavoritesKey || e.key === null) changed()
  })
function subscribe(listener: () => void) {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}
export function readBaseFavorites(raw: string | null): Film[] {
  if (!raw) return []
  const data: unknown = JSON.parse(raw)
  if (
    !Array.isArray(data) ||
    !data.every(validStoredFilm) ||
    new Set(data.map((f) => f.id)).size !== data.length
  )
    throw Error('Invalid base favorites')
  return data
}
export function favoriteCheckpoint(film: Film): Film {
  const last = film.frames.at(-1)
  if (!last) throw Error('Empty session')
  return structuredClone({
    ...film,
    id: crypto.randomUUID(),
    createdAt: new Date().toISOString(),
    frames: [
      {
        state: last.state,
        action: null,
        ...(last.rulesetIdentity
          ? { rulesetIdentity: last.rulesetIdentity }
          : {}),
        ...(last.abyss ? { abyss: last.abyss } : {}),
      },
    ],
  })
}
export function saveBaseFavorite(film: Film) {
  const current = readBaseFavorites(localStorage.getItem(baseFavoritesKey))
  const next = favoriteCheckpoint(film)
  const duplicate = current.find(
    (entry) =>
      entry.rulesetIdentity === next.rulesetIdentity &&
      sameItemState(entry.frames[0]?.state, next.frames[0]?.state) &&
      JSON.stringify(entry.frames[0]?.abyss) ===
        JSON.stringify(next.frames[0]?.abyss),
  )
  if (duplicate) return duplicate.id
  if (!validStoredFilm(next)) throw Error('Invalid favorite checkpoint')
  // Commit storage first: quota/serialization errors must not pretend the favorite was saved.
  localStorage.setItem(baseFavoritesKey, JSON.stringify([next, ...current]))
  changed()
  return next.id
}
export function removeBaseFavorite(id: string) {
  const current = readBaseFavorites(localStorage.getItem(baseFavoritesKey))
  localStorage.setItem(
    baseFavoritesKey,
    JSON.stringify(current.filter((f) => f.id !== id)),
  )
  changed()
}
const snapshot = () => {
  try {
    return localStorage.getItem(baseFavoritesKey)
  } catch {
    return 'invalid'
  }
}
export function useBaseFavorites() {
  const raw = useSyncExternalStore(subscribe, snapshot, () => null)
  try {
    return { entries: readBaseFavorites(raw), error: false }
  } catch {
    return { entries: [] as Film[], error: true }
  }
}
