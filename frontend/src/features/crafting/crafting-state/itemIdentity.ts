import type { ConcreteItem } from '../workbenchApi'
import type { AbyssState, Modifier, Slot } from './types'
import { equivalent } from '../sanctification/equivalence'

// Normalize wire defaults only. Nested ordered arrays (observations, rolls) retain their order.
function wireDefaults(value: unknown): unknown {
  if (Array.isArray(value)) return value.map(wireDefaults)
  if (!value || typeof value !== 'object') return value
  return Object.fromEntries(
    Object.entries(value)
      .filter(
        ([key, v]) =>
          v != null &&
          !(
            ['fractured', 'crafted', 'qualityInfused'].includes(key) &&
            v === false
          ) &&
          !(
            ['desecrated', 'installedAugments'].includes(key) &&
            Array.isArray(v) &&
            !v.length
          ),
      )
      .map(([key, v]) => [key, wireDefaults(v)]),
  )
}
const byId = (a: Modifier, b: Modifier) =>
  a.modifierId < b.modifierId ? -1 : a.modifierId > b.modifierId ? 1 : 0
export const sameModifier = (a: Modifier, b: Modifier) =>
  equivalent(wireDefaults(a), wireDefaults(b))

export function itemIdentity(item: ConcreteItem) {
  return wireDefaults({
    ...item,
    explicits: [...item.explicits].sort(byId),
    implicits: [...item.implicits].sort(byId),
    conditions: [...item.conditions].sort(),
    installedAugments: [...(item.installedAugments ?? [])].sort(
      (a, b) => a.slotIndex - b.slotIndex,
    ),
    desecrated: [...(item.desecrated ?? [])].sort((a, b) =>
      a.id < b.id ? -1 : a.id > b.id ? 1 : 0,
    ),
  })
}
/** Item collections are unordered; slot identity, original rolls and all metadata must still match. */
export function sameItemState(
  a: ConcreteItem | null | undefined,
  b: ConcreteItem | null | undefined,
): boolean {
  return a == null || b == null
    ? a === b
    : equivalent(itemIdentity(a), itemIdentity(b))
}

/** Slot order belongs to the reveal workflow; concrete modifiers have canonical catalog order. */
export function itemFromSlots(state: AbyssState): ConcreteItem {
  return {
    ...state.item,
    explicits: state.slots
      .flatMap((slot) => (slot.modifier ? [slot.modifier] : []))
      .sort(byId),
    desecrated: state.slots
      .filter((slot) => slot.desecrated)
      .map((slot) => ({
        id: slot.id,
        side: slot.side,
        modifierId: slot.modifier?.modifierId ?? null,
        ...(slot.revealPool ? { revealPool: slot.revealPool } : {}),
        ...(slot.minimumModifierLevel != null
          ? { minimumModifierLevel: slot.minimumModifierLevel }
          : {}),
      })),
  }
}

export const sameSlot = (a: Slot, b: Slot) =>
  equivalent(wireDefaults(a), wireDefaults(b))

/** Candidate order is meaningful, unlike the item's explicit-modifier collection. */
export const sameRevealOffer = (
  a: AbyssState['offer'],
  b: AbyssState['offer'],
) => equivalent(wireDefaults(a), wireDefaults(b))
