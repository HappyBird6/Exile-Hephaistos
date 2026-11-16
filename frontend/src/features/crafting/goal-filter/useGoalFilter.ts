import { useQuery } from '@tanstack/react-query'
import type { Context, GoalFilter, GoalFilterAdapter } from './types'
export function useGoalFilterCatalog(
  adapter: GoalFilterAdapter,
  context: Context,
) {
  return useQuery({
    queryKey: ['goalFilter', adapter.id, 'catalog', context],
    queryFn: ({ signal }) => adapter.catalog(context, signal),
    retry: false,
  })
}
export function useGoalFilterValidation(
  adapter: GoalFilterAdapter,
  context: Context,
  goal: GoalFilter,
  enabled = true,
) {
  return useQuery({
    queryKey: ['goalFilter', adapter.id, 'validate', context, goal],
    enabled: enabled && hasCompleteGoal(goal),
    queryFn: ({ signal }) => adapter.validate(context, goal, signal),
    retry: false,
  })
}

/** An empty editor is an unfinished draft, not a goal for the server to reject. */
export function hasCompleteGoal(goal: GoalFilter) {
  const active = goal.groups.filter((group) => !group.disabled)
  return (
    (active.length > 0 || (goal.general.quality ?? 0) > 0) &&
    active.every((group) => group.entries.some((entry) => !entry.disabled))
  )
}
