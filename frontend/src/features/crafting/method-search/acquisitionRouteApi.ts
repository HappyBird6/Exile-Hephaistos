import { traceLabel } from '../../../shared/apiTrace'
import { queryOptions, skipToken } from '@tanstack/react-query'
import { craftingFetch } from '../../../shared/gameIdTransport'
import { GoalFilterApiError, readGoalFilterResponse } from '../goal-filter/api'
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
  dummy?: boolean
  repeatUntilHit?: true
  repeatTarget?: number
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
  lucky?: boolean
  cardIndex: number
  order: number[]
  steps: AcquisitionStep[]
  /** Post-action snapshots, including the hidden slot after a bone and the chosen modifier after REVEAL. */
  stateIndices: number[]
  probability: number | null
  probabilityBounds?: ProbabilityBounds
}

export interface AcquisitionResult {
  cards: { options: RouteOption[] }[]
  states: ConcreteItem[]
  routes: AcquisitionRoute[]
  complete: boolean
  diagnostics?: {
    stage: string
    outcomeBudget: number
    search: {
      actions: Record<
        string,
        { trials: number; outcomes: number; accepted: number }
      >
    } | null
  }
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
  return readAcquisitionResponse(response, request)
}

export async function readAcquisitionResponse(
  response: Response,
  request: CreateRequest,
) {
  const label = traceLabel(response)
  const { item, provenance } = request.start
  console.info(label, '탐색 입력', {
    request: request.clientRequestId,
    base: item.baseItemId,
    level: item.itemLevel,
    rarity: item.rarity,
    explicits: item.explicits.map(({ modifierId, fractured }) => ({
      modifierId,
      fractured,
    })),
    goals: request.goal.groups
      .filter((group) => !group.disabled)
      .map((group) => ({
        type: group.type,
        entries: group.entries
          .filter((entry) => !entry.disabled)
          .map(({ statId, range }) => ({ statId, range })),
      })),
  })
  try {
    if (response.ok) verifyRulesetResponse(response, provenance.rulesetIdentity)
    const result = await readGoalFilterResponse<AcquisitionResult>(response)
    const regular = result.routes.filter((route) => !route.lucky).length
    console.info(label, result.diagnostics?.stage ?? '탐색 응답', {
      cards: result.cards.length,
      firstCardOptions: result.cards[0]?.options,
      routes: result.routes.length,
      regular,
      lucky: result.routes.length - regular,
      complete: result.complete,
      ...result.diagnostics,
    })
    if (result.diagnostics?.search?.actions) {
      console.groupCollapsed(
        label,
        '화폐별 전이 (시도 · 생성 결과 · 채택 결과)',
      )
      console.table(result.diagnostics.search.actions)
      console.groupEnd()
    }
    return result
  } catch (error) {
    console.warn(
      label,
      '탐색 응답 검증 실패',
      error instanceof GoalFilterApiError
        ? error.issues.map(({ code, path }) => ({ code, path }))
        : error instanceof Error
          ? error.name
          : 'Error',
    )
    throw error
  }
}
