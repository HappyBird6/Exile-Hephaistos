import { LocalFilmRepository, historyStorageLimit } from '../workbenchHistory'
import type { Film, Films } from '../workbenchHistory'

export const transferLimit = 2_000_000
export type SessionDocument = {
  format: 'exile-session'
  version: 1
  exportedAt: string
  film: Film
}
export function documentFor(film: Film): SessionDocument {
  return {
    format: 'exile-session',
    version: 1,
    exportedAt: new Date().toISOString(),
    film: structuredClone(film),
  }
}
export function decodeDocument(raw: string): SessionDocument {
  if (new TextEncoder().encode(raw).length > transferLimit) throw Error('size')
  const value: unknown = JSON.parse(raw)
  let nodes = 0
  function safe(v: unknown, depth: number) {
    if (++nodes > 100000 || depth > 32) throw Error('size')
    if (v && typeof v === 'object')
      for (const [key, child] of Object.entries(v)) {
        if (['__proto__', 'prototype', 'constructor'].includes(key))
          throw Error('unsafe')
        safe(child, depth + 1)
      }
  }
  safe(value, 0)
  const d = value as SessionDocument
  if (
    !d ||
    d.format !== 'exile-session' ||
    d.version !== 1 ||
    typeof d.exportedAt !== 'string' ||
    !d.film ||
    typeof d.film.id !== 'string' ||
    !d.film.id ||
    d.film.id.length > 256 ||
    (d.film.title !== undefined && typeof d.film.title !== 'string') ||
    (d.film.rulesetIdentity !== undefined &&
      typeof d.film.rulesetIdentity !== 'string') ||
    typeof d.film.createdAt !== 'string' ||
    !Array.isArray(d.film.frames) ||
    !d.film.frames.length ||
    d.film.frames.length > 2000 ||
    (d.film.sourceDocument !== undefined &&
      typeof d.film.sourceDocument !== 'string') ||
    !d.film.frames.every(
      (f) =>
        f &&
        (f.action === null || typeof f.action === 'string') &&
        f.state &&
        typeof f.state.baseItemId === 'string' &&
        ['NORMAL', 'MAGIC', 'RARE', 'UNIQUE'].includes(f.state.rarity) &&
        Number.isInteger(f.state.itemLevel) &&
        Array.isArray(f.state.implicits) &&
        Array.isArray(f.state.explicits) &&
        Array.isArray(f.state.conditions) &&
        f.state.conditions.every((c) => typeof c === 'string'),
    )
  )
    throw Error('invalid')
  return d
}
export function appendDocument(
  history: Films,
  d: SessionDocument,
  id: () => string = () => crypto.randomUUID(),
): { history: Films; id: string } {
  const same = history.films.find((f) => f.id === d.film.id)
  if (same && JSON.stringify(same) === JSON.stringify(d.film))
    return { history, id: same.id }
  const previousImport = history.films.find(
    (f) =>
      history.importSources?.[f.id] === d.film.id &&
      JSON.stringify({ ...f, id: d.film.id }) === JSON.stringify(d.film),
  )
  if (previousImport) return { history, id: previousImport.id }
  const film = structuredClone(d.film)
  if (same) {
    const originalId = film.id
    do {
      film.id = id()
    } while (history.films.some((f) => f.id === film.id))
    if (
      history.importSources &&
      (typeof history.importSources !== 'object' ||
        Array.isArray(history.importSources) ||
        !Object.values(history.importSources).every(
          (v) => typeof v === 'string',
        ))
    )
      throw Error('invalid')
    const next = {
      ...history,
      importSources: { ...history.importSources, [film.id]: originalId },
      films: [...history.films, film],
    }
    return validateAppend(next, film.id)
  }
  const next = { ...history, films: [...history.films, film] }
  return validateAppend(next, film.id)
}
function validateAppend(next: Films, id: string) {
  if (JSON.stringify(next).length * 2 > historyStorageLimit) throw Error('size')
  // Apply the existing repository's receipt validation before touching real storage.
  const raw = JSON.stringify(next)
  new LocalFilmRepository({ getItem: () => raw, setItem: () => {} }).load()
  return { history: next, id }
}
export function saveBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
export const localHistoryUrl = (id: string) =>
  `/history/${encodeURIComponent(id)}`
