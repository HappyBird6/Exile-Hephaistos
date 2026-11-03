import type { ConcreteItem, WorkbenchAction, AppliedItem } from './workbenchApi'

export function isEquipmentQualityAction(action: WorkbenchAction) {
  return action === 'ARMOURERS_SCRAP' || action === 'BLACKSMITHS_WHETSTONE'
}

export function equipmentQualityRefusal(
  state: ConcreteItem,
  action: WorkbenchAction,
): string | null {
  if (state.conditions.length) return 'condition'
  const armour = state.baseItemId.startsWith('Metadata/Items/Armours/')
  const martial =
    state.baseItemId.startsWith('Metadata/Items/Weapons/') &&
    !/\/(Wands|Staves|Sceptres)\//.test(state.baseItemId)
  if (
    (action === 'ARMOURERS_SCRAP' && !armour) ||
    (action === 'BLACKSMITHS_WHETSTONE' && !martial)
  )
    return 'class'
  if (state.equipmentQuality == null) return 'unknown'
  return state.equipmentQuality >= 20 ? 'maximum' : 'unverified-increment'
}

export function qualityTransitionMatches(
  state: ConcreteItem,
  result: AppliedItem,
  action: WorkbenchAction,
): boolean {
  const next = result.state
  if (!isEquipmentQualityAction(action) || !result.applied)
    return (next.equipmentQuality ?? null) === (state.equipmentQuality ?? null)
  // No verified per-use increment or user-selected policy is available yet.
  return false
}
