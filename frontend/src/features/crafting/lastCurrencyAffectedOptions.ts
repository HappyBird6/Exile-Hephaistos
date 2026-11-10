import type { Initial, Definition } from './craftingApi'
import type { ConcreteItem } from './workbenchApi'
import { workbenchActionNames } from './workbenchApi'
import type { Film } from './workbenchHistory'
import {
  storedAbyssItem,
  verifiedFrameEvidence,
  verifiedHistoryState,
} from './workbenchHistory'
import { isBone } from './desecrationCurrency'
import { validAbyssResponse } from './abyssResponse'
import { sameItemState } from './crafting-state/itemIdentity'
import { catalystProjection } from './catalystQuality'
import { preciseModifierProjection } from './sanctification/preciseModifierProjection'

function sameValues(a: Record<string, number>, b: Record<string, number>) {
  return (
    Object.keys(a).length === Object.keys(b).length &&
    Object.entries(a).every(([key, value]) => value === b[key])
  )
}

function displayedValues(
  state: ConcreteItem,
  definition: Definition,
  modifier: ConcreteItem['explicits'][number],
  layer: 'implicits' | 'explicits',
) {
  return state.conditions.includes('SANCTIFIED')
    ? preciseModifierProjection(state, definition, modifier, layer).values
    : catalystProjection(
        definition,
        modifier.values,
        state.catalystQuality,
        state,
      ).values
}

/** Derive display marks from the selected history; never persist or edit item data. */
export function lastCurrencyAffectedOptions(
  film: Film | undefined,
  cursor: number,
  initial: Initial,
): ReadonlySet<string> {
  const none = new Set<string>()
  if (!film || cursor < 1 || !film.frames[cursor]) return none
  let index = cursor
  while (index > 0) {
    const action = film.frames[index]!.action
    if (
      action &&
      (Object.hasOwn(workbenchActionNames, action) || isBone(action))
    )
      break
    index--
  }
  if (index === 0) return none
  const step = film.frames[index]!
  const before = film.frames[index - 1]!.state
  const after = step.state
  const current = film.frames[cursor]!.state
  if (
    ![before, after, current].every((state) =>
      verifiedHistoryState(state, initial),
    )
  )
    return none
  const evidence = verifiedFrameEvidence(step, initial)
  const manualCurrency =
    step.manual?.action === step.action &&
    step.manual.rulesetIdentity === initial.rulesetIdentity &&
    step.rulesetIdentity === initial.rulesetIdentity &&
    step.abyss &&
    validAbyssResponse('validate', step.abyss, step.abyss.item, initial) &&
    sameItemState(storedAbyssItem(step.abyss), after)
  if (!evidence && !manualCurrency) return none
  const affected = new Set(
    evidence?.events
      .filter((event) => event.kind !== 'REMOVE')
      .map((event) => event.modifierId),
  )
  for (const layer of ['implicits', 'explicits'] as const) {
    for (const modifier of after[layer]) {
      const previous = before[layer].find(
        (entry) => entry.modifierId === modifier.modifierId,
      )
      const definition = initial.modifiers[modifier.modifierId]!
      if (
        !previous ||
        Boolean(previous.fractured) !== Boolean(modifier.fractured) ||
        !sameValues(previous.values, modifier.values) ||
        !sameValues(
          displayedValues(before, definition, previous, layer),
          displayedValues(after, definition, modifier, layer),
        )
      ) {
        affected.add(modifier.modifierId)
      }
    }
  }
  for (const slot of after.desecrated ?? []) {
    const previous = before.desecrated?.find((entry) => entry.id === slot.id)
    if (
      !previous ||
      previous.modifierId !== slot.modifierId ||
      previous.side !== slot.side ||
      (previous.revealPool ?? null) !== (slot.revealPool ?? null) ||
      (previous.minimumModifierLevel ?? null) !==
        (slot.minimumModifierLevel ?? null)
    ) {
      affected.add(slot.id)
      if (slot.modifierId) affected.add(slot.modifierId)
    }
    if (slot.modifierId && affected.has(slot.modifierId)) affected.add(slot.id)
  }
  const visible = new Set(
    [...current.implicits, ...current.explicits]
      .filter((modifier) => affected.has(modifier.modifierId))
      .map((modifier) => modifier.modifierId),
  )
  for (const slot of current.desecrated ?? []) {
    if (affected.has(slot.id)) visible.add(slot.modifierId ?? slot.id)
  }
  return visible
}
