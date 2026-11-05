import { basePolicy } from '../baseRegistry'
import { augmentEffect } from '../socketAugments'
import { workbenchCurrencyActions } from '../workbenchApi'
import type { WorkbenchResource } from './stashTypes'

export function supportsBaseMaterial(
  resource: Pick<WorkbenchResource, 'id' | 'category'>,
  baseItemId?: string,
  actions?: readonly string[],
): boolean {
  if (!baseItemId) return true
  if (resource.category === 'Augments') {
    return (
      (basePolicy(baseItemId)?.socketExecutionMaximum ?? 0) > 0 &&
      Boolean(augmentEffect(resource.id, baseItemId))
    )
  }
  if (
    !['Essence', 'Alloy', 'Catalysts', 'Liquid_Emotions'].includes(
      resource.category,
    )
  )
    return true
  // Keep the catalog visible until its matching server metadata has arrived.
  if (!actions) return true
  const action = workbenchCurrencyActions[resource.id]
  return Boolean(action && actions.includes(action))
}
