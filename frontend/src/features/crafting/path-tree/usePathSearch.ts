import { useQuery } from '@tanstack/react-query'
import { useStore } from 'zustand'
import type { PathSearchSession } from './session'
import type { GraphPage, JobSnapshot } from './types'

export function usePathSearch(session: PathSearchSession) {
  const view = useStore(session.view)
  const job = useQuery<JobSnapshot>({
    queryKey: session.key(view.generation),
    queryFn: () => session.refresh(),
    enabled: !!session.job && !view.error && !view.operation,
    refetchInterval: (query) =>
      ['RUNNING', 'QUEUED'].includes(query.state.data?.status ?? '')
        ? 1000
        : false,
    retry: false,
    refetchOnWindowFocus: false,
    staleTime: Infinity,
  })
  const graph = useQuery<GraphPage>({
    queryKey: session.graphKey(),
    queryFn: async () => session.graph!,
    enabled: false,
  })
  return {
    ...view,
    job: view.error ? undefined : job.data,
    graph: view.error ? undefined : graph.data,
  }
}
