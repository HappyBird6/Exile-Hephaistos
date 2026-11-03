import type { ConcreteItem, WorkbenchAction, AppliedItem } from './workbenchApi'
import { equivalent, sameRolls } from './sanctification/equivalence'
import { sameObservations } from './itemObservations'
import { maximumQuality } from './qualityLimit'

export const equipmentQualityPolicy =
  'user-selected-equipment-quality-maximum-v2'

// Only the reviewed ordinary equipment cap applies; Amulet catalyst caps do not.
function equipmentQualityMaximum(state: ConcreteItem): number | null {
  const maximum = maximumQuality(state, {})
  return maximum === 20 ? maximum : null
}

export function isEquipmentQualityAction(action: WorkbenchAction) {
  return action === 'ARMOURERS_SCRAP' || action === 'BLACKSMITHS_WHETSTONE'
}

export function equipmentQualityRefusal(
  state: ConcreteItem,
  action: WorkbenchAction,
): string | null {
  if (!isEquipmentQualityAction(action)) return 'action'
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
  const maximum = equipmentQualityMaximum(state)
  if (maximum === null) return 'class'
  return state.equipmentQuality >= maximum ? 'maximum' : null
}

export function qualityTransitionMatches(
  state: ConcreteItem,
  result: AppliedItem,
  action: WorkbenchAction,
): boolean {
  const next = result.state
  if (!isEquipmentQualityAction(action) || !result.applied)
    return (next.equipmentQuality ?? null) === (state.equipmentQuality ?? null)
  return (
    equipmentQualityRefusal(state, action) === null &&
    next.equipmentQuality === equipmentQualityMaximum(state) &&
    next.snapshotId === state.snapshotId &&
    next.baseItemId === state.baseItemId &&
    next.itemLevel === state.itemLevel &&
    next.rarity === state.rarity &&
    sameRolls(next.implicits, state.implicits) &&
    sameRolls(next.explicits, state.explicits) &&
    equivalent(next.conditions, state.conditions) &&
    equivalent(next.desecrated ?? [], state.desecrated ?? []) &&
    equivalent(next.catalystQuality ?? null, state.catalystQuality ?? null) &&
    equivalent(next.sanctification ?? null, state.sanctification ?? null) &&
    sameObservations(next.observations, state.observations) &&
    (next.augmentSockets ?? null) === (state.augmentSockets ?? null) &&
    result.events.length === 0 &&
    result.consumedOmens.length === 0 &&
    result.assumptions.length === 1 &&
    result.assumptions[0]?.id === equipmentQualityPolicy &&
    result.assumptions[0]?.n === 1 &&
    result.assumptions[0]?.candidateUnit ===
      'equipment quality target percentage' &&
    result.assumptions[0]?.candidates.length === 0 &&
    result.assumptions[0]?.min === next.equipmentQuality &&
    result.assumptions[0]?.max === next.equipmentQuality
  )
}
