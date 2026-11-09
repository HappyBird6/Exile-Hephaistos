import type { Definition, Initial } from '../craftingApi'
import type { ConcreteItem } from '../workbenchApi'
import { workbenchEssenceTargets } from '../workbenchApi'
import { baseAffixCapacity } from '../baseRegistry'
import { isWorkbenchJewel, jewelCapacity } from '../basicJewel'
import { groupStartModifiers } from '../goal-filter/modifierGroups'

export type ModifierPoolMode = 'dynamic' | 'static'
export type ModifierPoolKind = 'ordinary' | 'desecrated' | 'essence'
export type ModifierPoolGroup = {
  id: string
  tiers: Definition[]
  weight: number
  actions: string[]
}
export type ModifierPoolSide = {
  affix: 'PREFIX' | 'SUFFIX'
  groups: ModifierPoolGroup[]
  totalWeight: number
  kind: ModifierPoolKind
}
function groupPool(
  definitions: Definition[],
  kind: ModifierPoolKind,
  targets: Record<string, string[]> = {},
): ModifierPoolSide[] {
  return (['PREFIX', 'SUFFIX'] as const).map((affix) => {
    const groups = groupStartModifiers(
      definitions.filter((d) => d.affixType === affix),
    ).map((group) => ({
      ...group,
      weight: group.tiers.reduce((sum, tier) => sum + (tier.weight ?? 0), 0),
      actions: [
        ...new Set(group.tiers.flatMap((tier) => targets[tier.id] ?? [])),
      ],
    }))
    return {
      affix,
      kind,
      groups,
      totalWeight: groups.reduce((sum, group) => sum + group.weight, 0),
    }
  })
}
/** Natural explicit modifiers only; special currency outcomes are not part of this pool. */
export function modifierPool(
  definitions: Definition[],
  itemLevel: number,
): ModifierPoolSide[] {
  return groupPool(
    definitions.filter(
      (d) =>
        (d.layer === undefined || d.layer === 'EXPLICIT') &&
        Number.isSafeInteger(d.requiredItemLevel) &&
        d.requiredItemLevel! >= 1 &&
        d.requiredItemLevel! <= itemLevel &&
        Number.isSafeInteger(d.weight) &&
        d.weight! > 0 &&
        !d.tags?.includes('reveal_only'),
    ),
    'ordinary',
  )
}
/** Reference shares are conditional on the affix side, not on a selected currency. */
export function modifierPoolSections(
  initial: Initial,
  itemLevel: number,
  mode: ModifierPoolMode,
  item?: ConcreteItem,
): ModifierPoolSide[] {
  const definitions = Object.values(initial.modifiers)
  const targets = workbenchEssenceTargets(initial)
  const families = new Set(
    item?.explicits.flatMap(
      (m) => initial.modifiers[m.modifierId]?.familyIds ?? [],
    ) ?? [],
  )
  const count = (side: string) =>
    (item?.explicits.filter(
      (m) => initial.modifiers[m.modifierId]?.affixType === side,
    ).length ?? 0) +
    (item?.desecrated?.filter((s) => s.modifierId === null && s.side === side)
      .length ?? 0)
  const capacity = (side: 'PREFIX' | 'SUFFIX') => {
    if (!item || item.rarity === 'NORMAL') return Infinity // Both sides are open before the first modifier.
    if (isWorkbenchJewel(item.baseItemId)) return jewelCapacity(item, side)
    const fallback = baseAffixCapacity(item.baseItemId, item.rarity)
    const rules = initial.baseRules
    return side === 'PREFIX'
      ? ((item.rarity === 'MAGIC'
          ? rules?.magicPrefixes
          : rules?.rarePrefixes) ?? fallback.prefixes)
      : ((item.rarity === 'MAGIC'
          ? rules?.magicSuffixes
          : rules?.rareSuffixes) ?? fallback.suffixes)
  }
  const eligible = (d: Definition, kind: ModifierPoolKind) => {
    if (mode === 'static') return true
    if (
      (d.requiredItemLevel ?? 1) > itemLevel ||
      d.familyIds.some((f) => families.has(f))
    )
      return false
    const side = d.affixType
    if (side === 'NONE') return false
    const hidden = item?.desecrated?.filter((s) => s.modifierId === null) ?? []
    if (kind === 'desecrated' && hidden.length)
      return hidden.some(
        (s) =>
          s.side === side &&
          (d.requiredItemLevel ?? 1) >= (s.minimumModifierLevel ?? 0) &&
          (!d.tags?.includes('otherworldly') || s.revealPool === 'ALTERED') &&
          (!s.revealPool ||
            s.revealPool === 'ALTERED' ||
            d.tags?.includes(`${s.revealPool.toLowerCase()}_mod`)),
      )
    return count(side) < capacity(side)
  }
  const ordinary = modifierPool(
    definitions,
    mode === 'static' ? 100 : itemLevel,
  )
  const normal = groupPool(
    ordinary
      .flatMap((side) => side.groups.flatMap((g) => g.tiers))
      .filter((d) => eligible(d, 'ordinary')),
    'ordinary',
  )
  const reveal = definitions.filter(
    (d) =>
      d.affixType !== 'NONE' &&
      (d.tags?.includes('reveal_only') || d.tags?.includes('unveiled_mod')) &&
      eligible(d, 'desecrated'),
  )
  const essence = definitions.filter(
    (d) => targets[d.id]?.length && eligible(d, 'essence'),
  )
  return [
    ...normal,
    ...groupPool(reveal, 'desecrated'),
    ...groupPool(essence, 'essence', targets),
  ]
}
/** Search never changes the denominator. */
export function poolProbability(weight: number, totalWeight: number): number {
  return totalWeight > 0 ? weight / totalWeight : 0
}
