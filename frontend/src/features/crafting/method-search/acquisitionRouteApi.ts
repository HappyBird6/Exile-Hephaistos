import { craftingFetch } from '../../../shared/gameIdTransport'
import { readGoalFilterResponse } from '../goal-filter/api'
import type { CreateRequest } from '../path-tree/types'
import { rulesetHeaders, verifyRulesetResponse } from '../rulesetIdentity'

interface RouteOption {
  number: number
  modifierIds: string[]
}

export interface AcquisitionStep {
  action: string
  socketIndex?: number | null
  omens: string[]
  acquired: number[]
  lost: number[]
}

export interface AcquisitionRoute {
  cardIndex: number
  order: number[]
  steps: AcquisitionStep[]
  probability: number | null
}

export interface AcquisitionResult {
  cards: { options: RouteOption[] }[]
  routes: AcquisitionRoute[]
  complete: boolean
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
