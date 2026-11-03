import type { AbyssState } from '../AbyssPanel'
import type { Initial } from '../craftingApi'
import type { ConcreteItem, WorkbenchAction } from '../workbenchApi'
import { validAbyssResponse } from '../abyssResponse'
import { boneConsumedOmens } from '../desecrationCurrency'
import { rulesetHeaders, verifyRulesetResponse } from '../rulesetIdentity'
import { supportsConcreteStateShape } from '../workbenchStateShape'
import { storedAbyssItem } from '../workbenchHistory'

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

/** No input mutation; the caller commits only after its revision/cancellation guard. */
export async function applyAbyssCurrency(
  state: AbyssState,
  action: WorkbenchAction,
  activeOmens: string[],
  initial: Initial,
  signal: AbortSignal,
  errorMessage: string,
): Promise<AbyssCurrencyResult> {
  const dormant = boneConsumedOmens(activeOmens)
  const triggered = activeOmens.filter((id) => !dormant.includes(id))
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
  return {
    ...value,
    state: value.applied ? value.state : state,
    remainingOmens: [...value.remainingOmens, ...dormant],
  }
}
