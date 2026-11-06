import type { Definition } from '../craftingApi'
import { groupStartModifiers } from '../goal-filter/modifierGroups'

export type ModifierPoolGroup = {
  id: string
  tiers: Definition[]
  weight: number
}
export type ModifierPoolSide = {
  affix: 'PREFIX' | 'SUFFIX'
  groups: ModifierPoolGroup[]
  totalWeight: number
}

/** Natural explicit modifiers only; special currency outcomes are not part of this pool. */
export function modifierPool(
  definitions: Definition[],
  itemLevel: number,
): ModifierPoolSide[] {
  const eligible = definitions.filter(
    (definition) =>
      (definition.layer === undefined || definition.layer === 'EXPLICIT') &&
      Number.isSafeInteger(definition.requiredItemLevel) &&
      definition.requiredItemLevel! >= 1 &&
      definition.requiredItemLevel! <= itemLevel &&
      Number.isSafeInteger(definition.weight) &&
      definition.weight! > 0,
  )
  return (['PREFIX', 'SUFFIX'] as const).map((affix) => {
    const groups = groupStartModifiers(
      eligible.filter((definition) => definition.affixType === affix),
    ).map((group) => ({
      ...group,
      weight: group.tiers.reduce((sum, tier) => sum + tier.weight!, 0),
    }))
    return {
      affix,
      groups,
      totalWeight: groups.reduce((sum, group) => sum + group.weight, 0),
    }
  })
}

/** The denominator remains the complete level-filtered affix pool, including during search. */
export function poolProbability(weight: number, totalWeight: number): number {
  return totalWeight > 0 ? weight / totalWeight : 0
}
