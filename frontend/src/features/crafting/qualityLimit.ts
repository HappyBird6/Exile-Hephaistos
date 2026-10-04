import type { Definition } from './craftingApi'
import type { ConcreteItem } from './workbenchApi'

export type QualityLimit = {
  ruleVersion: 'quality-limit-v1'
  maximumQuality: number
}
const solar = 'Metadata/Items/Amulets/FourAmulet9'
const stocky = 'Metadata/Items/Armours/Gloves/FourGlovesStr1'
const breach = 'amulet:prefix:essence-maximum-quality'
const stat = 'local_maximum_quality_+'

// Source-reviewed cap only. This does not imply an applied quality amount or scaled stats.
export function maximumQuality(
  state: ConcreteItem,
  definitions: Record<string, Definition>,
): number | null {
  if (
    ![
      solar,
      'Metadata/Items/Jewels/JewelInt',
      'Metadata/Items/Rings/FourRing1',
      stocky,
      'Metadata/Items/Armours/Helmets/FourHelmetStr1',
      'Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre1',
      'Metadata/Items/Armours/BodyArmours/FourBodyStr1',
      'Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand3',
      'Metadata/Items/Weapons/TwoHandWeapons/Bows/FourBow1',
    ].includes(state.baseItemId)
  )
    return null
  let maximum = 20
  for (const instance of state.explicits) {
    if (!Object.hasOwn(instance.values, stat)) continue
    const d = definitions[instance.modifierId]
    if (
      state.baseItemId !== solar ||
      instance.modifierId !== breach ||
      d?.affixType !== 'PREFIX' ||
      d.familyIds.length !== 1 ||
      d.familyIds[0] !== 'LocalMaximumQuality' ||
      d.stats?.length !== 1 ||
      d.stats[0]?.id !== stat ||
      d.stats[0].min !== 20 ||
      d.stats[0].max !== 20 ||
      Object.keys(instance.values).length !== 1 ||
      instance.values[stat] !== 20 ||
      maximum !== 20
    )
      return null
    maximum = 40
  }
  return maximum
}

export function qualityLimitMatches(
  value: unknown,
  state: ConcreteItem,
  definitions: Record<string, Definition>,
): boolean {
  try {
    const expected = maximumQuality(state, definitions)
    return (
      expected !== null &&
      value !== null &&
      typeof value === 'object' &&
      'ruleVersion' in value &&
      value.ruleVersion === 'quality-limit-v1' &&
      'maximumQuality' in value &&
      value.maximumQuality === expected
    )
  } catch {
    return false
  }
}
