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
  const identity = poolStateIdentity(state)
  // Static references ignore level and existing affixes; retain augment/condition/future context.
  if (!dynamic) {
    delete identity.itemLevel
    delete identity.explicits
    delete identity.rarity
  }
  return hashKey([rules, dynamic, identity])
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
  const reference = cachedPool(state, true, rules)
  return reference?.staticRevision
    ? {
        'X-Modifier-Pool-Revision': reference.revision!,
        'X-Modifier-Pool-Baseline': reference.staticRevision,
      }
    : reference?.revision
      ? { 'X-Modifier-Pool-Revision': reference.revision }
      : {}
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
    groups?: unknown
    removed?: unknown
  }
  if (
    !validRevision(update.revision) ||
    (update.staticRevision !== undefined &&
      !validRevision(update.staticRevision)) ||
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
      staticRevision: update.staticRevision,
    })
    return
  }
  const previous = cachedPool(before, true, rules)
  if (!previous) return
  const staticBasis =
    previous.staticRevision !== undefined &&
    previous.staticRevision === update.fromRevision
  if (!staticBasis && previous.revision !== update.fromRevision) return
  const entries = new Map(
    previous.entries.map((entry) => [
      entryKey(entry),
      staticBasis ? { ...entry, eligible: true, reasons: [] } : entry,
    ]),
  )
  if (update.groups !== undefined) {
    if (!Array.isArray(update.groups) || (!staticBasis && update.groups.length))
      return
    const ordered = [...entries.entries()].sort(([a], [b]) =>
      a < b ? -1 : a > b ? 1 : 0,
    )
    const seen = new Set<number>()
    for (const raw of update.groups) {
      if (!raw || typeof raw !== 'object') return
      const group = raw as {
        indices?: unknown
        eligible?: unknown
        reasons?: unknown
      }
      if (
        !Array.isArray(group.indices) ||
        !Array.isArray(group.reasons) ||
        typeof group.eligible !== 'boolean'
      )
        return
      for (const index of group.indices) {
        if (
          !Number.isSafeInteger(index) ||
          index < 0 ||
          index >= ordered.length ||
          seen.has(index)
        )
          return
        seen.add(index)
        const [id, entry] = ordered[index]!
        const changed = {
          ...entry,
          eligible: group.eligible,
          reasons: group.reasons,
        } as PoolEntry
        if (!validPoolEntries({ entries: [changed] }, definitions)) return
        entries.set(id, changed)
      }
    }
  }
  for (const id of update.removed) {
    if (!entries.delete(id as string)) return
  }
  for (const entry of update.entries) {
    if (update.removed.includes(entryKey(entry))) return
    entries.set(entryKey(entry), entry)
  }
  rememberPool(after, true, rules, {
    revision: update.revision,
    staticRevision: update.staticRevision,
    entries: [...entries.values()],
  })
}
