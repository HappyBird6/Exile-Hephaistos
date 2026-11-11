import type { Definition } from '../craftingApi'
export const exclusionReasons = [
  'ITEM_LEVEL',
  'FAMILY_OCCUPIED',
  'AFFIX_FULL',
  'REVEAL_POOL',
  'STATE_RESTRICTION',
] as const
export type ExclusionReason = (typeof exclusionReasons)[number]
export type PoolEntry = {
  modifierId: string
  kind: 'ordinary' | 'desecrated' | 'essence'
  eligible: boolean
  reasons: ExclusionReason[]
  actions: string[]
}
export type PoolReference = {
  entries: PoolEntry[]
  revision?: string
  staticRevision?: string | undefined
}

export function validPoolEntries(
  data: PoolReference,
  definitions: Record<string, Definition>,
): boolean {
  const seen = new Set<string>()
  if (
    !Array.isArray(data?.entries) ||
    data.entries.some((entry) => {
      const key = `${entry?.kind}:${entry?.modifierId}`
      const invalid =
        !entry ||
        !Object.hasOwn(definitions, entry.modifierId) ||
        !['ordinary', 'desecrated', 'essence'].includes(entry.kind) ||
        typeof entry.eligible !== 'boolean' ||
        !Array.isArray(entry.actions) ||
        entry.actions.some((a) => typeof a !== 'string') ||
        !Array.isArray(entry.reasons) ||
        entry.reasons.some((r) => !exclusionReasons.includes(r)) ||
        entry.eligible !== (entry.reasons.length === 0) ||
        seen.has(key)
      seen.add(key)
      return invalid
    })
  )
    return false
  return true
}
