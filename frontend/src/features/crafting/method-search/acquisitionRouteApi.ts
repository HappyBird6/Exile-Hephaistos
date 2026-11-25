import { queryOptions, skipToken } from '@tanstack/react-query'
import { craftingFetch } from '../../../shared/gameIdTransport'
import { readGoalFilterResponse } from '../goal-filter/api'
import type { CreateRequest } from '../path-tree/types'
import { rulesetHeaders, verifyRulesetResponse } from '../rulesetIdentity'
import type { ConcreteItem } from '../workbenchApi'
import type {
  ProbabilityBounds,
  ProjectedOmenOutcomes,
} from '../workbenchHistory'

export interface RouteOption {
  hasLowerBound?: boolean
  hasUpperBound?: boolean
  number: number
  modifierIds: string[]
}

export interface AcquisitionStep {
  omenOutcomes?: ProjectedOmenOutcomes
  action: string
  chance?: {
    probability: number | null
    probabilityBounds?: ProbabilityBounds
  } | null
  expectedUses?: number | null
  maxUses?: number
  socketIndex?: number | null
  omens: string[]
  acquired: number[]
  lost: number[]
}

export interface AcquisitionRoute {
  cardIndex: number
  order: number[]
  steps: AcquisitionStep[]
  stateIndices: number[]
  probability: number | null
  probabilityBounds?: ProbabilityBounds
}

export interface AcquisitionResult {
  cards: { options: RouteOption[] }[]
  states: ConcreteItem[]
  routes: AcquisitionRoute[]
  complete: boolean
}

export function acquisitionRouteQuery(request: CreateRequest | null) {
  return queryOptions({
    queryKey: [
      'support-test-routes',
      request?.start.provenance.rulesetIdentity,
      request?.start.item,
      request?.goal,
    ],
    queryFn: request
      ? ({ signal }) => loadAcquisitionRoutes(request, signal)
      : skipToken,
    staleTime: Infinity,
    retry: false,
  })
}

export async function loadAcquisitionRoutes(
  request: CreateRequest,
  signal: AbortSignal,
): Promise<AcquisitionResult> {
  const { item, provenance } = request.start
  const response = await craftingFetch(
    '/api/v1/crafting/support/goal-filters/test-routes',
    {
      method: 'POST',
      signal,
      headers: rulesetHeaders(provenance.rulesetIdentity),
      body: JSON.stringify({ item, goal: request.goal }),
    },
  )
  if (response.ok) verifyRulesetResponse(response, provenance.rulesetIdentity)
  return readGoalFilterResponse<AcquisitionResult>(response)
}
