import { poolHeaders, acceptPoolUpdate } from '../modifier-pool/poolSnapshots'
import { currencyOmens } from '../crafting-state/activeOmens'
import type { Initial } from '../craftingApi'
import type { AppliedItem, ConcreteItem } from '../workbenchApi'
import type { AbyssState } from '../crafting-state/types'
import type { ExaltedAction } from './messages'
import { rulesetHeaders, verifyRulesetResponse } from '../rulesetIdentity'
import {
  sameItemState,
  sameModifier,
  sameSlot,
  sameRevealOffer,
} from '../crafting-state/itemIdentity'
import { resolveCraftingState } from '../crafting-state/craftingState'
import { storedAbyssItem, verifiedHistoryState } from '../workbenchHistory'
import { validAbyssResponse } from '../abyssResponse'
export type ExaltChoice = { modifierId: string; probability: number }
export type CustomExaltContext = {
  state: ConcreteItem
  abyss?: AbyssState | undefined
  action: ExaltedAction
  activeOmens: string[]
}
const validChoice = (c: ExaltChoice, initial: Initial) =>
  c &&
  !!initial.modifiers[c.modifierId] &&
  Number.isFinite(c.probability) &&
  c.probability > 0 &&
  c.probability <= 1

async function post(
  path: string,
  context: CustomExaltContext,
  initial: Initial,
  signal: AbortSignal,
  modifierId?: string,
) {
  const current = resolveCraftingState(context.state, initial, context.abyss)
  const response = await fetch(
    `/api/v1/crafting/workbench/custom-exalt/${path}`,
    {
      method: 'POST',
      headers: {
        ...rulesetHeaders(initial.rulesetIdentity),
        ...poolHeaders(context.state, initial.rulesetIdentity),
      },
      signal,
      body: JSON.stringify({
        ...context,
        ...current,
        activeOmens: currencyOmens(context.activeOmens).requested,
        modifierId,
      }),
    },
  )
  verifyRulesetResponse(response, initial.rulesetIdentity)
  if (!response.ok) throw Error('Custom Exalted request failed')
  return response.json()
}
export async function customExaltChoices(
  context: CustomExaltContext,
  initial: Initial,
  signal: AbortSignal,
): Promise<ExaltChoice[]> {
  const result = (await post(
    'choices',
    context,
    initial,
    signal,
  )) as ExaltChoice[]
  if (
    !Array.isArray(result) ||
    result.some((c) => !validChoice(c, initial)) ||
    new Set(result.map((c) => c.modifierId)).size !== result.length
  )
    throw Error('Invalid Exalted choices')
  return result
}
export async function applyCustomExalt(
  context: CustomExaltContext,
  initial: Initial,
  signal: AbortSignal,
  modifierId: string,
): Promise<{ result: AppliedItem; abyss?: AbyssState; choice: ExaltChoice }> {
  const data = await post('apply', context, initial, signal, modifierId)
  const result = {
    ...data.result,
    rulesetIdentity: initial.rulesetIdentity,
  } as AppliedItem
  const added = result.state?.explicits?.filter(
    (m) =>
      !context.state.explicits.some((old) => old.modifierId === m.modifierId),
  )
  const restored = result.state && {
    ...result.state,
    explicits: context.state.explicits,
  }
  if (
    !validChoice(data.choice, initial) ||
    data.choice.modifierId !== modifierId ||
    !result.applied ||
    result.action !== context.action ||
    !verifiedHistoryState(result.state, initial) ||
    !added?.some((m) => m.modifierId === modifierId) ||
    !sameItemState(restored, context.state) ||
    context.state.explicits.some(
      (old) => !result.state.explicits.some((m) => sameModifier(m, old)),
    ) ||
    !Array.isArray(result.events) ||
    !Array.isArray(result.assumptions) ||
    !Array.isArray(result.consumedOmens) ||
    !currencyOmens(context.activeOmens).valid(
      result.consumedOmens,
      result.remainingOmens,
    )
  )
    throw Error('Invalid custom Exalted result')
  if (
    data.abyss &&
    (!validAbyssResponse('validate', data.abyss, result.state, initial) ||
      !sameItemState(storedAbyssItem(data.abyss), result.state) ||
      (context.abyss &&
        (!sameRevealOffer(context.abyss.offer, data.abyss.offer) ||
          context.abyss.slots.some(
            (old) =>
              !data.abyss.slots.some((slot: AbyssState['slots'][number]) =>
                sameSlot(slot, old),
              ),
          ))))
  )
    throw Error('Invalid custom Abyss result')
  if ((context.abyss || context.state.desecrated?.length) && !data.abyss)
    throw Error('Missing Abyss state')
  signal.throwIfAborted()
  acceptPoolUpdate(
    data.modifierPool,
    context.state,
    result.state,
    initial.rulesetIdentity,
    initial.modifiers,
  )
  return {
    ...(data.abyss ? { abyss: data.abyss } : {}),
    result: {
      ...result,
      remainingOmens: currencyOmens(context.activeOmens).restore(
        result.remainingOmens,
      ),
    },
    choice: data.choice,
  }
}
