import { queryOptions } from '@tanstack/react-query'
import { loadInitial } from '../craftingApi'
import type { WorkbenchBaseKey } from '../baseRegistry'
import { startBases } from './startItem'

/** List metadata never needs every equipment family's modifier catalog. */
export const startInventoryQuery = queryOptions({
  queryKey: ['craftStart', 'inventory'],
  staleTime: Infinity,
  retry: false,
  retryOnMount: false,
  refetchOnWindowFocus: false,
  queryFn: async ({ signal }) => {
    const response = await fetch('/api/v1/crafting/workbench/registry', {
      signal,
      cache: 'no-store',
    })
    if (!response.ok) throw new Error('Base inventory unavailable.')
    const registry = (await response.json()) as {
      workbenchBases: Record<string, unknown>
      entries: { serviceScope?: string; supportedBases?: string[] }[]
    }
    const keys = new Set([
      ...Object.keys(registry.workbenchBases),
      ...registry.entries
        .filter((entry) => entry.serviceScope === 'ACTIVE')
        .flatMap((entry) => entry.supportedBases ?? []),
    ])
    return {
      bases: startBases.filter((base) => keys.has(base[0])),
      // Explicit registry refresh must revalidate selected details even if the base list is unchanged.
      generation: crypto.randomUUID(),
    }
  },
})

export const startInitialQuery = (base: WorkbenchBaseKey, generation: string) =>
  queryOptions({
    queryKey: ['craftStart', 'initial', generation, base],
    queryFn: ({ signal }) => loadInitial(82, signal, base),
    staleTime: Infinity,
    retry: false,
    retryOnMount: false,
    refetchOnWindowFocus: false,
  })
