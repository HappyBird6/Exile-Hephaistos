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
    enabled,
    queryFn: ({ signal }) => adapter.validate(context, goal, signal),
    retry: false,
  })
}
