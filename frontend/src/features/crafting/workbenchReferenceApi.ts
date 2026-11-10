import type { Initial } from './craftingApi'
import type { ConcreteItem } from './workbenchApi'
import type { AbyssState } from './AbyssPanel'
import { rulesetHeaders, verifyRulesetResponse } from './rulesetIdentity'
import { sameItemState, sameRevealOffer } from './crafting-state/itemIdentity'
import { storedAbyssItem, verifiedHistoryState } from './workbenchHistory'
import { validAbyssResponse } from './abyssResponse'

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
export type PoolReference = { entries: PoolEntry[] }
async function post(
  path: string,
  body: unknown,
  initial: Initial,
  signal: AbortSignal,
): Promise<unknown> {
  const response = await fetch(`/api/v1/crafting/workbench/${path}`, {
    method: 'POST',
    headers: rulesetHeaders(initial.rulesetIdentity),
    body: JSON.stringify(body),
    signal,
  })
  verifyRulesetResponse(response, initial.rulesetIdentity)
  if (!response.ok) throw new Error('Reference request failed')
  return response.json()
}
export async function loadModifierPools(
  initial: Initial,
  state: ConcreteItem,
  dynamic: boolean,
  signal: AbortSignal,
): Promise<PoolReference> {
  const data = (await post(
    'modifier-pools',
    { state, dynamic },
    initial,
    signal,
  )) as PoolReference
  const seen = new Set<string>()
  if (
    !Array.isArray(data?.entries) ||
    data.entries.some((entry) => {
      const key = `${entry?.kind}:${entry?.modifierId}`
      const invalid =
        !entry ||
        !Object.hasOwn(initial.modifiers, entry.modifierId) ||
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
    throw new Error('Invalid modifier reference')
  return data
}
export type SessionCompatibility = {
  compatible: boolean
  sameRules: boolean
  compatibilityId: string | null
  state: ConcreteItem | null
  abyss: AbyssState | null
  reasons: string[]
}
export async function checkSessionCompatibility(
  initial: Initial,
  state: ConcreteItem,
  abyss: AbyssState | undefined,
  compatibilityId: string | undefined,
  signal: AbortSignal,
): Promise<SessionCompatibility> {
  const data = (await post(
    'session-compatibility',
    { state, abyss, compatibilityId },
    initial,
    signal,
  )) as SessionCompatibility
  if (
    !data ||
    typeof data.compatible !== 'boolean' ||
    typeof data.sameRules !== 'boolean' ||
    !Array.isArray(data.reasons) ||
    data.reasons.some((r) => typeof r !== 'string')
  )
    throw new Error('Invalid compatibility response')
  if (data.compatible) {
    const rebased = { ...state, snapshotId: initial.metadata.snapshotId }
    if (
      !data.state ||
      typeof data.compatibilityId !== 'string' ||
      !data.compatibilityId ||
      data.reasons.length > 0 ||
      (initial.compatibilityId !== undefined &&
        initial.compatibilityId !== data.compatibilityId) ||
      !verifiedHistoryState(data.state, initial) ||
      !sameItemState(rebased, data.state) ||
      (data.sameRules && data.compatibilityId !== compatibilityId) ||
      (!!abyss && !data.abyss) ||
      (data.abyss &&
        (!validAbyssResponse('validate', data.abyss, data.state, initial) ||
          !sameItemState(data.state, storedAbyssItem(data.abyss)) ||
          (abyss &&
            (!sameItemState(
              {
                ...abyss.item,
                snapshotId: initial.metadata.snapshotId,
              },
              data.abyss.item,
            ) ||
              !sameRevealOffer(
                abyss.offer ?? null,
                data.abyss.offer ?? null,
              )))))
    )
      throw new Error('Invalid continued checkpoint')
  }
  return data
}
