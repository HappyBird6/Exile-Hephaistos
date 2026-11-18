import { rareGoal } from '../goal-filter/editor'
import type { ConcreteItem } from '../workbenchApi'
import type { GoalFilter } from '../goal-filter/types'
import type { Provenance } from '../basic-paths/api'
import type { CreateRequest, Item } from './types'
import { validateRequest } from './validation'

/** Serialize existing optional ItemState defaults, without changing rolls or the goal AST. */
export function createPathSearchRequest(
  item: ConcreteItem | Item,
  goal: GoalFilter,
  provenance: Provenance,
  activeOmens: string[] = [],
): CreateRequest {
  const modifier = (m: ConcreteItem['explicits'][number]) => ({
    ...m,
    fractured: m.fractured ?? false,
  })
  // The planner represents an untouched item with no equipment quality field.
  const startItem = { ...item }
  if ('equipmentQuality' in startItem && startItem.equipmentQuality === 0)
    delete startItem.equipmentQuality
  const request = structuredClone({
    version: 1,
    clientRequestId: crypto.randomUUID(),
    start: {
      item: {
        ...startItem,
        implicits: item.implicits.map(modifier),
        explicits: item.explicits.map(modifier),
        augmentSockets: item.augmentSockets ?? null,
        catalystQuality: item.catalystQuality ?? null,
      },
      provenance,
    },
    goal: rareGoal(goal),
    activeOmens,
    observations: ['100', '300', '500'],
  }) as CreateRequest
  validateRequest(request)
  return request
}
