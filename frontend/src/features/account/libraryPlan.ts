import type { Film } from '../crafting/workbenchHistory'
import { validStoredFilm } from '../crafting/workbenchHistory'
export type Kind = 'session' | 'base-favorite'
export type Document = {
  kind: Kind
  id: string
  version: number
  content: Film | null
}
export const documentKey = (d: Pick<Document, 'kind' | 'id'>) =>
  `${d.kind}/${d.id}`
export function canonical(value: unknown): string {
  if (Array.isArray(value)) return '[' + value.map(canonical).join(',') + ']'
  if (value && typeof value === 'object')
    return (
      '{' +
      Object.entries(value)
        .filter(([, v]) => v !== undefined)
        .sort(([a], [b]) => a.localeCompare(b))
        .map(([k, v]) => JSON.stringify(k) + ':' + canonical(v))
        .join(',') +
      '}'
    )
  return JSON.stringify(value) ?? 'null'
}
export function readDocuments(raw: unknown): Document[] {
  if (
    !Array.isArray(raw) ||
    !raw.every(
      (d) =>
        d &&
        ['session', 'base-favorite'].includes(d.kind) &&
        typeof d.id === 'string' &&
        Number.isSafeInteger(d.version) &&
        d.version > 0 &&
        (d.content === null ||
          (validStoredFilm(d.content) && d.content.id === d.id)),
    ) ||
    new Set(raw.map(documentKey)).size !== raw.length
  )
    throw Error('INVALID_ACCOUNT_LIBRARY')
  return raw as Document[]
}
export class LibraryConflict extends Error {
  constructor(public keys: string[]) {
    super('LIBRARY_CONFLICT')
  }
}
/** Three-way comparison: absence is a deletion only when present in the confirmed baseline. */
export function planSync(
  baseline: Document[],
  local: Document[],
  remote: Document[],
) {
  const b = new Map(baseline.map((d) => [documentKey(d), d])),
    l = new Map(local.map((d) => [documentKey(d), d])),
    r = new Map(remote.map((d) => [documentKey(d), d]))
  const writes: Document[] = [],
    next: Document[] = [],
    conflicts: string[] = []
  for (const key of new Set([...b.keys(), ...l.keys(), ...r.keys()])) {
    const before = b.get(key),
      ours = l.get(key),
      theirs = r.get(key)
    const bc = canonical(before?.content ?? null),
      lc = canonical(ours?.content ?? null),
      rc = canonical(theirs?.content ?? null)
    if (lc === rc) {
      if (theirs) next.push(theirs)
      continue
    }
    const localChanged = lc !== bc,
      remoteChanged =
        rc !== bc || (theirs?.version ?? 0) !== (before?.version ?? 0)
    if (localChanged && remoteChanged) {
      conflicts.push(key)
      continue
    }
    if (localChanged) {
      const source = ours ?? before!
      writes.push({
        ...source,
        version: theirs?.version ?? 0,
        content: ours?.content ?? null,
      })
      if (ours) next.push(ours)
    } else if (theirs) next.push(theirs)
  }
  if (conflicts.length) throw new LibraryConflict(conflicts)
  return { writes, next }
}
/** Import identity ignores browser IDs and timestamps, but retains the full saved path/state. */
export function filmFingerprint(film: Film) {
  return canonical({
    frames: film.frames,
    rulesetIdentity: film.rulesetIdentity,
    sourceDocument: film.sourceDocument,
  })
}
export function mergeFilms(
  existing: Film[],
  incoming: Film[],
  newId: () => string = () => crypto.randomUUID(),
) {
  const next = [...existing],
    ids = new Set(existing.map((f) => f.id)),
    fingerprints = new Map(
      existing.map((film, index) => [filmFingerprint(film), index]),
    )
  for (const film of incoming) {
    const signature = filmFingerprint(film)
    const duplicate = fingerprints.get(signature)
    if (duplicate !== undefined) {
      if (film.favorite && !next[duplicate]!.favorite)
        next[duplicate] = {
          ...next[duplicate]!,
          favorite: true,
          favoriteAt: film.favoriteAt ?? film.updatedAt ?? film.createdAt,
        }
      continue
    }
    const copy = structuredClone(film)
    if (ids.has(copy.id)) copy.id = newId()
    if (ids.has(copy.id)) throw Error('DUPLICATE_IMPORT_ID')
    fingerprints.set(signature, next.length)
    next.push(copy)
    ids.add(copy.id)
  }
  return next
}

/** A deterministic fork makes retries safe even if a successful response was lost. */
export async function preserveConflicts(
  baseline: Document[],
  local: Document[],
  remote: Document[],
) {
  let keys: string[] = []
  try {
    planSync(baseline, local, remote)
    return { local, forks: new Map<string, string>() }
  } catch (error) {
    if (!(error instanceof LibraryConflict)) throw error
    keys = error.keys
  }
  const forks = new Map<string, string>()
  const next = local.filter((d) => !keys.includes(documentKey(d)))
  for (const key of keys) {
    const theirs = remote.find((d) => documentKey(d) === key),
      ours = local.find((d) => documentKey(d) === key)
    if (theirs?.content) next.push(theirs)
    if (ours?.content) {
      const digest = await crypto.subtle.digest(
        'SHA-256',
        new TextEncoder().encode(canonical(ours)),
      )
      const id =
        'conflict-' +
        Array.from(new Uint8Array(digest))
          .map((n) => n.toString(16).padStart(2, '0'))
          .join('')
      forks.set(key, id)
      const copy = { ...ours, id, version: 0, content: { ...ours.content, id } }
      const existing = remote.find((d) => documentKey(d) === documentKey(copy))
      if (!existing?.content) next.push(copy)
      else if (!next.some((d) => documentKey(d) === documentKey(existing)))
        next.push(existing)
    }
  }
  return { local: next, forks }
}
