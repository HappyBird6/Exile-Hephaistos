import { infuserSupportsBase } from '../qualityInfuser'
import { basePolicy } from '../baseRegistry'
import { augmentEffect } from '../socketAugments'
import { workbenchCurrencyActions } from '../workbenchApi'
import { isBone } from '../desecrationCurrency'
import { isWorkbenchJewel } from '../basicJewel'
import descriptions from '../materialTooltips.json'
import type { WorkbenchResource } from './stashTypes'

const itemTargets: Record<string, string> = {
  'Body Armours': 'Body Armour',
  Helmets: 'Helmet',
  Gloves: 'Gloves',
  Boots: 'Boots',
  Shields: 'Shield',
  Bucklers: 'Shield',
  Foci: 'Focus',
  Bows: 'Bow',
  Crossbows: 'Crossbow',
  'One Hand Maces': 'Mace',
  'Two Hand Maces': 'Mace',
  Quarterstaves: 'Quarterstaff',
  Spears: 'Spear',
  Staves: 'Staff',
  Wands: 'Wand',
  Sceptres: 'Sceptre',
  Talismans: 'Talisman',
  Amulet: 'Amulet',
  Amulets: 'Amulet',
  Rings: 'Ring',
  Belts: 'Belt',
  Quivers: 'Quiver',
}
const groupTargets = [
  'Armour',
  'Jewellery',
  'Equipment',
  'Weapons',
  'Martial Weapon',
  'Caster Weapon',
  'Melee Weapon',
  'One Handed Melee Weapon',
  'Two Handed Melee Weapon',
]
const knownTargets = new Set([...Object.values(itemTargets), ...groupTargets])

/** Source target labels and registered class containment; never the current rarity or roll. */
export function baseMaterialTargets(base: string): ReadonlySet<string> {
  const targets = new Set<string>()
  const itemClass = basePolicy(base)?.itemClass
  if (!itemClass || !itemTargets[itemClass]) return targets
  targets.add(itemTargets[itemClass])
  targets.add('Equipment')
  if (base.startsWith('Metadata/Items/Armours/')) targets.add('Armour')
  if (['Amulet', 'Amulets', 'Rings', 'Belts'].includes(itemClass))
    targets.add('Jewellery')
  if (base.startsWith('Metadata/Items/Weapons/')) {
    targets.add('Weapons')
    if (['Wands', 'Staves', 'Sceptres'].includes(itemClass))
      targets.add('Caster Weapon')
    else {
      targets.add('Martial Weapon')
      if (!['Bows', 'Crossbows'].includes(itemClass)) {
        targets.add('Melee Weapon')
        if (base.includes('/OneHandWeapons/'))
          targets.add('One Handed Melee Weapon')
        if (base.includes('/TwoHandWeapons/'))
          targets.add('Two Handed Melee Weapon')
      }
    }
  }
  return targets
}

export function supportsBaseMaterial(
  resource: Pick<WorkbenchResource, 'id' | 'category'>,
  baseItemId?: string,
  actions?: readonly string[],
): boolean {
  if (!baseItemId) return true
  const targets = baseMaterialTargets(baseItemId)
  if (
    !targets.size &&
    !basePolicy(baseItemId)?.itemClass &&
    resource.category !== 'Augments'
  )
    return true
  if (resource.category === 'Currency') {
    if (resource.id.startsWith('Vaal_') && resource.id.endsWith('_Infuser'))
      return infuserSupportsBase(resource.id, baseItemId)
    if (resource.id === 'Armourers_Scrap') return targets.has('Armour')
    if (resource.id === 'Blacksmiths_Whetstone')
      return targets.has('Martial Weapon')
    if (isBone(resource.id)) {
      if (resource.id === 'Preserved_Cranium')
        return isWorkbenchJewel(baseItemId)
      if (resource.id.endsWith('_Rib')) return targets.has('Armour')
      if (resource.id.endsWith('_Jawbone'))
        return targets.has('Weapons') || targets.has('Quiver')
      return targets.has('Jewellery')
    }
    return true
  }
  if (resource.category === 'Augments') {
    return (
      (basePolicy(baseItemId)?.socketExecutionMaximum ?? 0) > 0 &&
      Boolean(augmentEffect(resource.id, baseItemId))
    )
  }
  if (
    !['Essence', 'Alloy', 'Catalysts', 'Liquid_Emotions'].includes(
      resource.category,
    )
  )
    return true
  if (resource.category === 'Essence' || resource.category === 'Alloy') {
    const source = (descriptions as Record<string, { lines: string[] }>)[
      resource.id
    ]
    const labels =
      source?.lines.flatMap((line) => {
        const colon = line.indexOf(':')
        return colon < 0
          ? []
          : line
              .slice(0, colon)
              .split(/, | or /)
              .filter((label) => knownTargets.has(label))
      }) ?? []
    if (labels.length) return labels.some((label) => targets.has(label))
  }
  // Keep the catalog visible until its matching server metadata has arrived.
  if (!actions) return true
  const action = workbenchCurrencyActions[resource.id]
  return Boolean(action && actions.includes(action))
}
