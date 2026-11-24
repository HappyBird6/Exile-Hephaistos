import { useQuery } from '@tanstack/react-query'
import { performanceSettings } from '../../../shared/performance'
import { baseKeyForItem } from '../baseRegistry'
import { loadInitial } from '../craftingApi'
import type { ConcreteItem } from '../workbenchApi'

export function useHistoryInitial(state: ConcreteItem) {
  const base = baseKeyForItem(state.baseItemId)
  const initial = useQuery({
    queryKey: ['crafting', 'initial', base, state.itemLevel, 'manual'],
    staleTime: performanceSettings.initialRetentionMs,
    enabled: !!base,
    queryFn: ({ signal }) => loadInitial(state.itemLevel, signal, base!, true),
    retry: false,
  })
  return { base, initial }
}
