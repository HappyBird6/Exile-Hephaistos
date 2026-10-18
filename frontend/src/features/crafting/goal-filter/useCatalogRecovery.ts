import { useEffect, useRef } from 'react'
import { useQuery } from '@tanstack/react-query'
import type { StoreApi } from 'zustand/vanilla'
import type { Editor } from './editor'
import type { Catalog, Context, GoalFilter, GoalFilterAdapter } from './types'
import { GoalFilterApiError } from './api'
import { compatibilityIssues, rememberContract } from './catalogCompatibility'

export function useCatalogRecovery(
  adapter: GoalFilterAdapter,
  context: Context,
  goal: GoalFilter,
  catalog: Catalog | undefined,
  editor: StoreApi<Editor>,
  enabled: boolean,
) {
  const stale = !!catalog && goal.catalogVersion !== catalog.catalogVersion
  const issues = stale ? compatibilityIssues(goal, catalog) : []
  const candidate = catalog
    ? { ...goal, catalogVersion: catalog.catalogVersion }
    : goal
  const original = JSON.stringify(goal)
  const result = useQuery({
    queryKey: [
      'goalFilter',
      adapter.id,
      'recover',
      context,
      goal,
      catalog?.catalogVersion,
    ],
    enabled: enabled && stale && issues.length === 0,
    queryFn: ({ signal }) => adapter.validate(context, candidate, signal),
    retry: false,
    staleTime: Infinity,
    refetchOnWindowFocus: false,
  })
  useEffect(() => {
    if (!stale || !result.data?.valid || issues.length || !catalog || !enabled)
      return
    if (JSON.stringify(editor.getState().goal) !== original) return
    editor.getState().edit((value) => {
      value.catalogVersion = catalog.catalogVersion
    })
  }, [stale, result.data, issues.length, catalog, enabled, editor, original])
  useEffect(() => {
    if (catalog) rememberContract(goal, catalog)
  }, [goal, catalog])
  return {
    error: result.error,
    pending: stale && result.isFetching,
    issues: [
      ...issues,
      ...(result.error instanceof GoalFilterApiError
        ? result.error.issues
        : (result.data?.issues ?? [])),
    ],
    failed: stale && result.isError,
  }
}

// Only a catalog/ruleset validation conflict refreshes this catalog. Revisions
// belong to method jobs and must never be swallowed or retried here.
export function useRefreshConflictedCatalog(
  error: unknown,
  refetch: () => unknown,
) {
  const seen = useRef<unknown>(null)
  useEffect(() => {
    if (
      !(error instanceof GoalFilterApiError) ||
      seen.current === error ||
      !error.issues.some(
        (i) =>
          i.code === 'CATALOG_VERSION_MISMATCH' ||
          i.code === 'RULESET_IDENTITY_MISMATCH',
      )
    )
      return
    seen.current = error
    void refetch()
  }, [error, refetch])
}
