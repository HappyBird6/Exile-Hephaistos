import type { AbyssState } from '../crafting-state/types'
import type { Initial } from '../craftingApi'
import type { WorkbenchAction } from '../workbenchApi'
import { validAbyssResponse } from '../abyssResponse'
import { currencyOmens } from '../crafting-state/activeOmens'
import { rulesetHeaders, verifyRulesetResponse } from '../rulesetIdentity'
import { supportsConcreteStateShape } from '../workbenchStateShape'
import { storedAbyssItem } from '../workbenchHistory'
import { preservesChaosJewelOverflow } from '../jewelChaos'

export type AbyssCurrencyResult = {
  state: AbyssState
  action: WorkbenchAction
  applied: boolean
  reason: string
  consumedOmens: string[]
  remainingOmens: string[]
}

const canonical = (value: unknown): string =>
  JSON.stringify(value, (_key, v: unknown) =>
    v && typeof v === 'object' && !Array.isArray(v)
      ? Object.fromEntries(
          Object.entries(v)
            .sort(([a], [b]) => a.localeCompare(b))
            .filter(
              ([key, entry]) =>
                entry != null &&
                entry !== false &&
                !(
                  key === 'desecrated' &&
                  Array.isArray(entry) &&
                  entry.length === 0
                ),
            ),
        )
      : v,
  )

export { abyssFromConcrete } from '../crafting-state/craftingState'

/** No input mutation; the caller commits only after its revision/cancellation guard. */
export async function applyAbyssCurrency(
  state: AbyssState,
  action: WorkbenchAction,
  activeOmens: string[],
  initial: Initial,
  signal: AbortSignal,
  errorMessage: string,
): Promise<AbyssCurrencyResult> {
  const omenContext = currencyOmens(activeOmens)
  const triggered = omenContext.requested
  const response = await fetch('/api/v1/crafting/workbench/abyss/currency', {
    method: 'POST',
    signal,
    headers: rulesetHeaders(initial.rulesetIdentity),
    body: JSON.stringify({ state, action, activeOmens: triggered }),
  })
  verifyRulesetResponse(response, initial.rulesetIdentity)
  if (signal.aborted) throw new DOMException('Aborted', 'AbortError')
  if (!response.ok) throw new Error(errorMessage)
  const value = (await response.json()) as AbyssCurrencyResult
  if (signal.aborted) throw new DOMException('Aborted', 'AbortError')
  const next = value?.state?.item
  if (
    !next ||
    value.action !== action ||
    typeof value.applied !== 'boolean' ||
    typeof value.reason !== 'string' ||
    !Array.isArray(value.consumedOmens) ||
    !Array.isArray(value.remainingOmens) ||
    [...value.consumedOmens, ...value.remainingOmens].some(
      (id) => typeof id !== 'string',
    ) ||
    new Set([...value.consumedOmens, ...value.remainingOmens]).size !==
      triggered.length ||
    !triggered.every((id) =>
      [...value.consumedOmens, ...value.remainingOmens].includes(id),
    ) ||
    next.snapshotId !== state.item.snapshotId ||
    next.baseItemId !== state.item.baseItemId ||
    next.itemLevel !== state.item.itemLevel ||
    next.rarity !== state.item.rarity ||
    !validAbyssResponse('currency', value.state, state.item, initial) ||
    !supportsConcreteStateShape(storedAbyssItem(value.state)) ||
    (value.applied &&
      ['CHAOS', 'GREATER_CHAOS', 'PERFECT_CHAOS'].includes(action) &&
      !preservesChaosJewelOverflow(
        storedAbyssItem(state),
        storedAbyssItem(value.state),
        initial.modifiers,
      )) ||
    (value.applied && state.item.conditions.length > 0) ||
    (value.applied &&
      next.conditions.some(
        (condition) =>
          condition !== 'SANCTIFIED' ||
          action !== 'DIVINE' ||
          !triggered.includes('Omen_of_Sanctification'),
      )) ||
    (!value.applied &&
      (value.consumedOmens.length > 0 ||
        canonical(value.state) !== canonical(state)))
  )
    throw new Error(errorMessage)
  if (
    value.applied &&
    action === 'ANNULMENT' &&
    triggered.includes('Omen_of_Light')
  ) {
    const removed = state.slots.filter(
      (old) => !value.state.slots.some((slot) => slot.id === old.id),
    )
    const preserved = state.slots.filter((old) =>
      removed.every((slot) => slot.id !== old.id),
    )
    if (
      removed.length !== 1 ||
      !removed[0]!.desecrated ||
      removed[0]!.modifier?.fractured ||
      canonical(preserved) !== canonical(value.state.slots) ||
      canonical(state.item) !== canonical(value.state.item) ||
      canonical(value.consumedOmens) !== canonical(['Omen_of_Light']) ||
      (state.offer &&
        removed[0]!.id !== state.offer.slotId &&
        canonical(state.offer) !== canonical(value.state.offer))
    )
      throw new Error(errorMessage)
  }
  return {
    ...value,
    state: value.applied ? value.state : state,
    remainingOmens: omenContext.restore(value.remainingOmens),
  }
}
