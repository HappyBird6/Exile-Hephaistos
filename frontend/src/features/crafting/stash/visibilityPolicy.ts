import { additionalStashCurrencies, isSelectableMaterial } from '../materials'
import { inServiceScope } from '../serviceScope'
import { legacyOmenIds } from '../workbenchApi'
import type { MaterialTab } from '../materials'

// Display/selection policy only. Retain source catalogs, films and favorite IDs.
export const legacyStashOmenIds: readonly string[] = [
  ...legacyOmenIds,
  'Omen_of_Corruption',
]
export const excludedStashOmenIds: readonly string[] = [
  'Omen_of_the_Sovereign',
  'Omen_of_the_Liege',
  'Omen_of_the_Blackblooded',
  'Omen_of_Answered_Prayers',
  'Omen_of_the_Hunt',
  'Omen_of_Secret_Compartments',
  'Omen_of_Reinforcements',
  'Omen_of_Putrefaction',
]

export function visibilityPolicy(resource: {
  id: string
  category?: MaterialTab
}): boolean {
  return (
    (inServiceScope(resource.id) ||
      additionalStashCurrencies.some(
        (currency) => currency.id === resource.id,
      )) &&
    !legacyStashOmenIds.includes(resource.id) &&
    !excludedStashOmenIds.includes(resource.id) &&
    isSelectableMaterial({
      id: resource.id,
      category: resource.category ?? 'Currency',
    })
  )
}
