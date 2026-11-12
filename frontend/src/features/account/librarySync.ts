import { syncStorage } from './syncStorage'
import { readRemoteLibrary } from './libraryRead'
import { accountRequest, accountScope } from './auth'
import {
  canonical,
  documentKey,
  mergeFilms,
  planSync,
  readDocuments,
  preserveConflicts,
} from './libraryPlan'
import type { Document } from './libraryPlan'
import { IndexedFilmRepository } from '../crafting/session-library/IndexedFilmRepository'
import {
  baseFavoritesKey,
  readBaseFavorites,
} from '../crafting/base-favorites/repository'
import { scopedStorage } from './storageScope'
import { emptyFilms, historyStorageKey } from '../crafting/workbenchHistory'
import type { Films } from '../crafting/workbenchHistory'

type Metadata = {
  baseline: Document[]
  pending: Document | null
  generation?: number
  decision?: string
}
async function metadata(owner: string): Promise<Metadata> {
  const parsed = await syncStorage<Metadata>(owner)
  if (!parsed) return { baseline: [], pending: null }
  readDocuments(parsed.baseline)
  return parsed
}
async function saveMetadata(owner: string, value: Metadata) {
  await syncStorage(owner, value)
}
function guard(owner: string) {
  if (accountScope() !== owner) throw Error('ACCOUNT_CHANGED')
}
function favorites(owner: string | null) {
  return readBaseFavorites(
    scopedStorage(localStorage, owner).getItem(baseFavoritesKey),
  )
}
function localDocuments(history: Films, owner: string): Document[] {
  // Keep a pending deletion on the server during its undo window.
  const films = [...history.films]
  if (
    history.pendingDeletion &&
    history.pendingDeletion.expiresAt > Date.now() &&
    !films.some((f) => f.id === history.pendingDeletion!.deletion.film.id)
  )
    films.push(history.pendingDeletion.deletion.film)
  return [
    ...films.map((content) => ({
      kind: 'session' as const,
      id: content.id,
      version: 0,
      content,
    })),
    ...favorites(owner).map((content) => ({
      kind: 'base-favorite' as const,
      id: content.id,
      version: 0,
      content,
    })),
  ]
}
const running = new Map<string, Promise<boolean>>()
function withSyncLock<T>(owner: string, run: () => Promise<T>): Promise<T> {
  return navigator.locks
    ? navigator.locks.request(`hephaistos-account-sync-${owner}`, run)
    : run()
}
export function syncLibrary(owner: string): Promise<boolean> {
  const current = running.get(owner)
  if (current) return current
  const pending = withSyncLock(owner, () => performSync(owner)).finally(() => {
    running.delete(owner)
  })
  running.set(owner, pending)
  return pending
}
async function performSync(owner: string): Promise<boolean> {
  guard(owner)
  const repo = new IndexedFilmRepository(
    localStorage,
    globalThis.indexedDB,
    owner,
  )
  try {
    let history = await repo.load()
    let before = localDocuments(history, owner)
    const meta = await metadata(owner)
    const response = await accountRequest('library', {}, owner)
    const generation = Number(
      response.headers.get('X-Session-Generation') ?? '0',
    )
    if (!Number.isSafeInteger(generation) || generation < 0)
      throw Error('INVALID_GENERATION')
    if (generation !== (meta.generation ?? 0)) {
      // A bulk clear wins over stale offline edits and unacknowledged uploads.
      history = emptyFilms()
      await repo.save(history)
      before = localDocuments(history, owner)
      meta.baseline = meta.baseline.filter((d) => d.kind !== 'session')
      if (meta.pending?.kind === 'session') meta.pending = null
    }
    meta.generation = generation
    await saveMetadata(owner, meta)
    const references: unknown = await response.json()
    if (
      !Array.isArray(references) ||
      !references.every(
        (r) =>
          r &&
          ['session', 'base-favorite'].includes(r.kind) &&
          typeof r.id === 'string' &&
          Number.isSafeInteger(r.version) &&
          r.version > 0 &&
          typeof r.deleted === 'boolean',
      ) ||
      new Set(references.map(documentKey)).size !== references.length
    )
      throw Error('INVALID_LIBRARY_INDEX')
    const remote = await readRemoteLibrary(
      owner,
      generation,
      references,
      meta.baseline,
      response.headers.get('X-Library-Read-Batch-Size'),
    )
    guard(owner)
    if (meta.pending) {
      const saved = remote.find(
        (d) => documentKey(d) === documentKey(meta.pending!),
      )
      if (
        saved &&
        canonical(saved.content) === canonical(meta.pending.content)
      ) {
        meta.baseline = meta.baseline
          .filter((d) => documentKey(d) !== documentKey(saved))
          .concat(saved)
        meta.pending = null
        await saveMetadata(owner, meta)
      }
    }
    const resolved = await preserveConflicts(meta.baseline, before, remote)
    const plan = planSync(meta.baseline, resolved.local, remote)
    for (const write of plan.writes) {
      guard(owner)
      meta.pending = write
      await saveMetadata(owner, meta)
      const result = await accountRequest(
        `library/${encodeURIComponent(write.kind)}/${encodeURIComponent(write.id)}`,
        {
          method: 'PUT',
          headers: { 'X-Session-Generation': String(generation) },
          body: JSON.stringify({
            version: write.version,
            content: write.content,
          }),
        },
        owner,
      )
      const saved = readDocuments([await result.json()])[0]!
      guard(owner)
      meta.baseline = meta.baseline
        .filter((d) => documentKey(d) !== documentKey(saved))
        .concat(saved)
      meta.pending = null
      await saveMetadata(owner, meta)
      const i = remote.findIndex((d) => documentKey(d) === documentKey(saved))
      if (i < 0) remote.push(saved)
      else remote[i] = saved
    }
    // Read again: crafting remains usable while requests are in flight. Never overwrite a newer local checkpoint.
    const fresh = new IndexedFilmRepository(
      localStorage,
      globalThis.indexedDB,
      owner,
    )
    try {
      const latest = await fresh.load()
      if (canonical(localDocuments(latest, owner)) !== canonical(before))
        return false
      const merged = planSync(meta.baseline, resolved.local, remote).next
      const visible = merged.filter((d) => d.content !== null)
      const films = visible
        .filter((d) => d.kind === 'session')
        .map((d) => d.content!)
      // A deleted session stays hidden while its undo token is active.
      const hidden = latest.pendingDeletion?.deletion.film.id
      const order = new Map(latest.films.map((f, i) => [f.id, i]))
      const filtered = films
        .filter((f) => f.id !== hidden)
        .sort(
          (a, b) =>
            (order.get(a.id) ?? Number.MAX_SAFE_INTEGER) -
            (order.get(b.id) ?? Number.MAX_SAFE_INTEGER),
        )
      const requestedActive =
        resolved.forks.get(`session/${latest.active}`) ?? latest.active
      const active = filtered.some((f) => f.id === requestedActive)
        ? requestedActive
        : null
      const next = {
        ...latest,
        films: filtered,
        active,
        cursor: active
          ? Math.min(
              latest.cursor,
              filtered.find((f) => f.id === active)!.frames.length - 1,
            )
          : 0,
      }
      const nextFavorites = visible
        .filter((d) => d.kind === 'base-favorite')
        .map((d) => d.content!)
      const changed =
        canonical(next) !== canonical(latest) ||
        canonical(nextFavorites) !== canonical(favorites(owner))
      guard(owner)
      const favoriteBefore = canonical(favorites(owner))
      if (canonical(next) !== canonical(latest)) await fresh.save(next)
      if (canonical(favorites(owner)) !== favoriteBefore) return false
      scopedStorage(localStorage, owner).setItem(
        baseFavoritesKey,
        JSON.stringify(nextFavorites),
      )
      meta.baseline = remote
      await saveMetadata(owner, meta)
      return changed
    } finally {
      await fresh.close()
    }
  } finally {
    await repo.close()
  }
}
export async function browserMergeStatus(owner: string) {
  const repo = new IndexedFilmRepository(
    localStorage,
    globalThis.indexedDB,
    null,
  )
  try {
    const history = await repo.load()
    const bases = favorites(null)
    const bytes = await crypto.subtle.digest(
      'SHA-256',
      new TextEncoder().encode(canonical({ films: history.films, bases })),
    )
    const fingerprint = Array.from(new Uint8Array(bytes))
      .map((n) => n.toString(16).padStart(2, '0'))
      .join('')
    return {
      needed:
        history.films.length + bases.length > 0 &&
        (await metadata(owner)).decision !== fingerprint,
      fingerprint,
      history,
      bases,
    }
  } finally {
    await repo.close()
  }
}
export function decideBrowserMerge(owner: string, add: boolean) {
  return withSyncLock(owner, () => applyBrowserMerge(owner, add))
}
async function applyBrowserMerge(owner: string, add: boolean) {
  guard(owner)
  const source = await browserMergeStatus(owner)
  if (add) {
    const repo = new IndexedFilmRepository(
      localStorage,
      globalThis.indexedDB,
      owner,
    )
    try {
      const target = await repo.load()
      guard(owner)
      await repo.save({
        ...target,
        films: mergeFilms(target.films, source.history.films),
      })
      const bases = mergeFilms(favorites(owner), source.bases)
      scopedStorage(localStorage, owner).setItem(
        baseFavoritesKey,
        JSON.stringify(bases),
      )
    } finally {
      await repo.close()
    }
  }
  const meta = await metadata(owner)
  meta.decision = source.fingerprint
  await saveMetadata(owner, meta)
}

export async function clearSessions(scope: string | null) {
  if (scope) {
    guard(scope)
    await accountRequest('library/sessions/clear', { method: 'POST' }, scope)
    await running.get(scope)?.catch(() => {})
    await syncLibrary(scope)
  } else {
    const repo = new IndexedFilmRepository(
      localStorage,
      globalThis.indexedDB,
      null,
    )
    try {
      await repo.load()
      await repo.save(emptyFilms())
      localStorage.removeItem(historyStorageKey)
      localStorage.removeItem(historyStorageKey + '.preserved')
    } finally {
      await repo.close()
    }
  }
}
