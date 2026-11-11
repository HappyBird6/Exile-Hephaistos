import { hashKey } from '@tanstack/react-query'
import { performanceSettings } from '../../../shared/performance'
import { itemIdentity } from '../crafting-state/itemIdentity'
import type { ConcreteItem } from '../workbenchApi'
import type { Definition } from '../craftingApi'
import { validPoolEntries } from './poolReference'
import type { PoolReference, PoolEntry } from './poolReference'

const snapshots = new Map<string, PoolReference>()
const validRevision = (value: unknown): value is string =>
  typeof value === 'string' && /^[a-f0-9]{64}$/.test(value)
const entryKey = (entry: PoolEntry) => `${entry.kind}:${entry.modifierId}`
// Same eligibility invariant as the backend: only numeric explicit rolls are omitted.
export function poolStateIdentity(state: ConcreteItem) {
  const identity = itemIdentity(state) as Record<string, unknown>
  identity.explicits = (identity.explicits as Record<string, unknown>[]).map(
    (modifier) => {
      const result = { ...modifier }
      delete result.values
      return result
    },
  )
  return identity
}
function key(state: ConcreteItem, dynamic: boolean, rules: string) {
  return hashKey([rules, dynamic, poolStateIdentity(state)])
}
export function cachedPool(
  state: ConcreteItem,
  dynamic: boolean,
  rules: string,
) {
  const id = key(state, dynamic, rules)
  const reference = snapshots.get(id)
  if (reference) {
    snapshots.delete(id)
    snapshots.set(id, reference)
  }
  return reference
}
export function rememberPool(
  state: ConcreteItem,
  dynamic: boolean,
  rules: string,
  reference: PoolReference,
) {
  if (!validRevision(reference.revision)) return
  const id = key(state, dynamic, rules)
  snapshots.delete(id)
  snapshots.set(id, reference)
  while (snapshots.size > performanceSettings.poolSnapshotEntries)
    snapshots.delete(snapshots.keys().next().value!)
}
export function poolHeaders(
  state: ConcreteItem,
  rules: string,
): Record<string, string> {
  const revision = cachedPool(state, true, rules)?.revision
  return revision ? { 'X-Modifier-Pool-Revision': revision } : {}
}
/** Invalid/missing presentation updates fall back to the ordinary reference request.
 * Never reject a completed craft or persist this transport field in session history. */
export function acceptPoolUpdate(
  raw: unknown,
  before: ConcreteItem,
  after: ConcreteItem,
  rules: string,
  definitions: Record<string, Definition>,
): void {
  if (!raw || typeof raw !== 'object') return
  const update = raw as PoolReference & {
    reset?: unknown
    fromRevision?: unknown
    removed?: unknown
  }
  if (
    !validRevision(update.revision) ||
    typeof update.reset !== 'boolean' ||
    !Array.isArray(update.removed) ||
    update.removed.some((id) => typeof id !== 'string') ||
    new Set(update.removed).size !== update.removed.length ||
    !validPoolEntries(update, definitions)
  )
    return
  if (update.reset) {
    if (update.fromRevision !== null || update.removed.length) return
    rememberPool(after, true, rules, {
      entries: update.entries,
      revision: update.revision,
    })
    return
  }
  const previous = cachedPool(before, true, rules)
  if (!previous || previous.revision !== update.fromRevision) return
  const entries = new Map(
    previous.entries.map((entry) => [entryKey(entry), entry]),
  )
  for (const id of update.removed) {
    if (!entries.delete(id as string)) return
  }
  for (const entry of update.entries) {
    if (update.removed.includes(entryKey(entry))) return
    entries.set(entryKey(entry), entry)
  }
  rememberPool(after, true, rules, {
    revision: update.revision,
    entries: [...entries.values()],
  })
}
