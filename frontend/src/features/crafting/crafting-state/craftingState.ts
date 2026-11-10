import type { ConcreteItem } from '../workbenchApi'
import type { Initial } from '../craftingApi'
import type { AbyssState } from './types'
import { validAbyssResponse } from '../abyssResponse'
import { itemFromSlots, sameItemState } from './itemIdentity'

/** Saved offers belong to the frame; this fallback is only for common states without an offer. */
export function abyssFromConcrete(
  state: ConcreteItem,
  initial: Initial,
): AbyssState {
  const result: AbyssState = {
    item: { ...state, explicits: [], desecrated: [] },
    slots: [
      ...state.explicits.map((modifier, index) => {
        const marked = state.desecrated?.find(
          (slot) => slot.modifierId === modifier.modifierId,
        )
        const side = initial.modifiers[modifier.modifierId]?.affixType
        if (side !== 'PREFIX' && side !== 'SUFFIX')
          throw new Error('Invalid modifier side')
        return {
          id: marked?.id ?? `explicit-${index}`,
          side,
          modifier,
          desecrated: !!marked,
          ...(marked?.minimumModifierLevel == null
            ? {}
            : { minimumModifierLevel: marked.minimumModifierLevel }),
          ...(marked?.revealPool == null
            ? {}
            : { revealPool: marked.revealPool }),
        }
      }),
      ...(state.desecrated ?? [])
        .filter((slot) => slot.modifierId === null)
        .map((slot) => ({
          id: slot.id,
          side: slot.side,
          modifier: null,
          desecrated: true,
          ...(slot.minimumModifierLevel == null
            ? {}
            : { minimumModifierLevel: slot.minimumModifierLevel }),
          ...(slot.revealPool == null ? {} : { revealPool: slot.revealPool }),
        })),
    ],
    offer: null,
  }
  if (!validAbyssResponse('import', result, state, initial))
    throw new Error('Invalid Abyss state')
  return result
}

/** A checkpoint is accepted only when its flat item and slot views describe the same state. */
export function resolveCraftingState(
  state: ConcreteItem,
  initial: Initial,
  source?: AbyssState,
) {
  const abyss =
    source ??
    (state.desecrated?.length ? abyssFromConcrete(state, initial) : undefined)
  if (
    abyss &&
    (!validAbyssResponse('validate', abyss, state, initial) ||
      !sameItemState(itemFromSlots(abyss), state))
  )
    throw Error('Inconsistent crafting state')
  return { state, ...(abyss ? { abyss } : {}) }
}

/** Metadata-only operations preserve slot IDs, hidden constraints and a pending reveal offer. */
export function withSlotItemMetadata(
  source: AbyssState | undefined,
  item: ConcreteItem,
): AbyssState | undefined {
  if (!source) return undefined
  const next = { ...source, item: { ...item, explicits: [], desecrated: [] } }
  if (!sameItemState(itemFromSlots(next), item))
    throw Error('Slot-changing operation requires its updated checkpoint')
  return next
}
