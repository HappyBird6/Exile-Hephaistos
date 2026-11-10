import type { Initial } from '../craftingApi'
import type { AppliedItem, ConcreteItem } from '../workbenchApi'
import type { AbyssState } from '../AbyssPanel'
import type { ExaltedAction } from './messages'
import { rulesetHeaders, verifyRulesetResponse } from '../rulesetIdentity'
import { equivalent } from '../sanctification/equivalence'
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

// Omitted nullable properties and false modifier flags carry the same item state on the wire.
function canonical(value: unknown): unknown {
  if (Array.isArray(value)) return value.map(canonical)
  if (!value || typeof value !== 'object') return value
  return Object.fromEntries(
    Object.entries(value)
      .filter(
        ([key, v]) =>
          v != null &&
          !(['fractured', 'crafted'].includes(key) && v === false) &&
          !(
            ['desecrated', 'installedAugments'].includes(key) &&
            Array.isArray(v) &&
            !v.length
          ),
      )
      .map(([key, v]) => [key, canonical(v)]),
  )
}
const sameState = (a: unknown, b: unknown) =>
  equivalent(canonical(a), canonical(b))
async function post(
  path: string,
  context: CustomExaltContext,
  initial: Initial,
  signal: AbortSignal,
  modifierId?: string,
) {
  const response = await fetch(
    `/api/v1/crafting/workbench/custom-exalt/${path}`,
    {
      method: 'POST',
      headers: rulesetHeaders(initial.rulesetIdentity),
      signal,
      body: JSON.stringify({ ...context, modifierId }),
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
    !sameState(restored, context.state) ||
    context.state.explicits.some(
      (old) => !result.state.explicits.some((m) => sameState(m, old)),
    ) ||
    !Array.isArray(result.events) ||
    !Array.isArray(result.assumptions) ||
    !Array.isArray(result.consumedOmens) ||
    !Array.isArray(result.remainingOmens)
  )
    throw Error('Invalid custom Exalted result')
  if (
    data.abyss &&
    (!validAbyssResponse('validate', data.abyss, result.state, initial) ||
      !sameState(storedAbyssItem(data.abyss), result.state))
  )
    throw Error('Invalid custom Abyss result')
  if ((context.abyss || context.state.desecrated?.length) && !data.abyss)
    throw Error('Missing Abyss state')
  return {
    ...(data.abyss ? { abyss: data.abyss } : {}),
    result,
    choice: data.choice,
  }
}
