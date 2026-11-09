import type { Definition, Initial } from '../craftingApi'
import type { PoolReference, PoolEntry } from '../workbenchReferenceApi'
import { groupStartModifiers } from '../goal-filter/modifierGroups'

export type ModifierPoolMode = 'dynamic' | 'static'
export type ModifierPoolKind = 'ordinary' | 'desecrated' | 'essence'
export type ModifierPoolGroup = {
  id: string
  tiers: Definition[]
  weight: number
  actions: string[]
  eligibility: Record<string, PoolEntry>
}
export type ModifierPoolSide = {
  affix: 'PREFIX' | 'SUFFIX'
  groups: ModifierPoolGroup[]
  totalWeight: number
  kind: ModifierPoolKind
}
/** Presentation only: the server owns membership, eligibility and reasons. */
export function modifierPoolSections(
  initial: Initial,
  reference: PoolReference,
): ModifierPoolSide[] {
  return (['ordinary', 'desecrated', 'essence'] as const).flatMap((kind) => {
    const entries = reference.entries.filter((e) => e.kind === kind)
    const eligibility = Object.fromEntries(
      entries.map((e) => [e.modifierId, e]),
    )
    return (['PREFIX', 'SUFFIX'] as const).map((affix) => {
      const definitions = entries
        .map((e) => initial.modifiers[e.modifierId]!)
        .filter((d) => d.affixType === affix)
      const groups = groupStartModifiers(definitions).map((group) => ({
        ...group,
        eligibility,
        weight: group.tiers.reduce(
          (sum, tier) =>
            sum + (eligibility[tier.id]!.eligible ? (tier.weight ?? 0) : 0),
          0,
        ),
        actions: [
          ...new Set(
            group.tiers.flatMap((tier) => eligibility[tier.id]!.actions),
          ),
        ],
      }))
      return {
        affix,
        kind,
        groups,
        totalWeight: groups.reduce((sum, group) => sum + group.weight, 0),
      }
    })
  })
}
/** Search never changes the denominator. */
export function poolProbability(weight: number, totalWeight: number): number {
  return totalWeight > 0 ? weight / totalWeight : 0
}
