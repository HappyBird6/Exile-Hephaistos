import { catalystActionType, workbenchCurrencyActions } from '../workbenchApi'
import type { CatalystAction } from '../workbenchApi'

/** Resolve inventory selection through the existing currency mapping, never base setup. */
export function catalystCurrencyAction(
  currencyId: string,
): CatalystAction | null {
  const action = workbenchCurrencyActions[currencyId]
  return action &&
    /^(REFINED_)?CATALYST_/.test(action) &&
    catalystActionType(action)
    ? (action as CatalystAction)
    : null
}
