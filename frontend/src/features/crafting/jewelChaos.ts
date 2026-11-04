import { isWorkbenchJewel, jewelCapacity } from './basicJewel'
import type { Definition } from './craftingApi'
import { equivalent, sameRolls } from './sanctification/equivalence'
import type { ConcreteItem } from './workbenchApi'

/** Existing over-cap sides are excluded before side/Whittling Chaos restrictions. */
export function chaosLockedJewelSides(
  state: ConcreteItem,
  definitions: Record<string, Definition>,
): string[] {
  if (!isWorkbenchJewel(state.baseItemId) || state.rarity !== 'RARE') return []
  return ['PREFIX', 'SUFFIX'].filter((side) => {
    const used =
      state.explicits.filter(
        (m) => definitions[m.modifierId]?.affixType === side,
      ).length +
      (state.desecrated ?? []).filter(
        (slot) => slot.side === side && slot.modifierId === null,
      ).length
    return used > jewelCapacity(state, side)
  })
}

/** Check preserved rolls and opaque slots, independent of server canonical ordering. */
export function preservesChaosJewelOverflow(
  before: ConcreteItem,
  after: ConcreteItem,
  definitions: Record<string, Definition>,
): boolean {
  const locked = chaosLockedJewelSides(before, definitions)
  const rolls = (item: ConcreteItem) =>
    item.explicits
      .filter((m) =>
        locked.includes(definitions[m.modifierId]?.affixType ?? ''),
      )
      .sort((a, b) => a.modifierId.localeCompare(b.modifierId))
  const slots = (item: ConcreteItem) =>
    (item.desecrated ?? [])
      .filter((slot) => locked.includes(slot.side))
      .map((slot) => ({
        ...slot,
        minimumModifierLevel: slot.minimumModifierLevel ?? null,
        revealPool: slot.revealPool ?? null,
      }))
      .sort((a, b) => a.id.localeCompare(b.id))
  return (
    sameRolls(rolls(before), rolls(after)) &&
    equivalent(slots(before), slots(after))
  )
}
