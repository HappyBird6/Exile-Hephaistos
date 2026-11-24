import { queryOptions, skipToken } from '@tanstack/react-query'
import { craftingFetch } from '../../../shared/gameIdTransport'
import { currencyRank } from '../custom-exalt/messages'
import { readGoalFilterResponse } from '../goal-filter/api'
import type { CreateRequest } from '../path-tree/types'
import { rulesetHeaders, verifyRulesetResponse } from '../rulesetIdentity'
import type {
  AcquisitionResult,
  AcquisitionRoute,
  AcquisitionStep,
} from './acquisitionRouteApi'

export function gradeActions(action: string) {
  if (!currencyRank(action)) return null
  const basic = action.replace(/^(GREATER|PERFECT)_/, '')
  return [basic, 'GREATER_' + basic, 'PERFECT_' + basic]
}

export function gradeSearch(grades?: readonly number[]) {
  return grades?.some((grade) => grade !== 1) ? grades.join('.') : ''
}

export function readRouteGrades(search: string, route?: AcquisitionRoute) {
  if (!route) return null
  const raw = new URLSearchParams(search).get('grades')
  if (raw === null) return route.steps.map(() => 1)
  const parts = raw.split('.')
  if (
    parts.length !== route.steps.length ||
    parts.some(
      (part, i) =>
        !/^[1-3]$/.test(part) ||
        (part !== '1' && !currencyRank(route.steps[i]!.action)),
    )
  )
    return null
  return parts.map(Number)
}

function methodStep({
  action,
  omens,
  acquired,
  lost,
  socketIndex,
}: AcquisitionStep) {
  return {
    action,
    omens,
    acquired,
    lost,
    ...(socketIndex == null ? {} : { socketIndex }),
  }
}

/** Preserve the entire successful method, changing only explicitly selected currency grades. */
export function fixedSteps(route: AcquisitionRoute, grades: readonly number[]) {
  return route.steps.map((step, index) => ({
    ...methodStep(step),
    action: gradeActions(step.action)?.[grades[index]! - 1] ?? step.action,
  }))
}

export function fixedRouteQuery(
  request: CreateRequest | null,
  base: AcquisitionResult | undefined,
  index: number,
  grades: readonly number[] | null,
) {
  const route = base?.routes[index]
  const steps =
    route && grades && gradeSearch(grades) ? fixedSteps(route, grades) : null
  return queryOptions({
    queryKey: [
      'support-test-fixed-grade',
      request?.start.provenance.rulesetIdentity,
      request?.start.item,
      request?.goal,
      route?.cardIndex,
      steps,
    ],
    queryFn:
      request && route && steps
        ? async ({ signal }) => {
            const { item, provenance } = request.start
            const response = await craftingFetch(
              '/api/v1/crafting/support/goal-filters/test-routes/grade',
              {
                method: 'POST',
                signal,
                headers: rulesetHeaders(provenance.rulesetIdentity),
                body: JSON.stringify({
                  item,
                  goal: request.goal,
                  cardIndex: route.cardIndex,
                  steps,
                }),
              },
            )
            if (response.ok)
              verifyRulesetResponse(response, provenance.rulesetIdentity)
            const evaluation =
              await readGoalFilterResponse<AcquisitionResult>(response)
            const replay = evaluation.routes[0]
            if (
              evaluation.routes.length > 1 ||
              (replay &&
                (replay.cardIndex !== 0 ||
                  JSON.stringify(replay.steps.map(methodStep)) !==
                    JSON.stringify(steps)))
            ) {
              throw new Error('Grade evaluation returned another method')
            }
            return evaluation
          }
        : skipToken,
    staleTime: Infinity,
    retry: false,
  })
}

/** Fixed replay uses its own state dictionary and locally numbers the submitted card zero. */
export function evaluatedRouteResult(
  base: AcquisitionResult,
  index: number,
  replay: AcquisitionResult,
): AcquisitionResult {
  const cardIndex = base.routes[index]!.cardIndex
  return {
    ...replay,
    cards: base.cards,
    routes: replay.routes.map((route) => ({ ...route, cardIndex })),
  }
}
