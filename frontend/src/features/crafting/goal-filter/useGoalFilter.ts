import { useQuery } from '@tanstack/react-query'
import type { Context, GoalFilter, GoalFilterAdapter, Group } from './types'
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

export function hasActiveStatFilters(group: Group) {
  return !group.disabled && group.entries.some((entry) => !entry.disabled)
}

export function hasCompleteGoal(goal: GoalFilter) {
  return (
    (goal.general.quality ?? 0) > 0 || goal.groups.some(hasActiveStatFilters)
  )
}
