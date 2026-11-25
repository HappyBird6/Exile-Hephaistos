import { queryOptions, skipToken } from '@tanstack/react-query'
import { craftingFetch } from '../../../shared/gameIdTransport'
import { currencyRank } from '../custom-exalt/messages'
import { readAcquisitionResponse } from './acquisitionRouteApi'
import type { CreateRequest } from '../path-tree/types'
import { rulesetHeaders } from '../rulesetIdentity'
import type {
  AcquisitionResult,
  AcquisitionRoute,
  AcquisitionStep,
} from './acquisitionRouteApi'

export function gradeActions({ action, dummy }: AcquisitionStep) {
  if (dummy || !currencyRank(action)) return null
  const basic = action.replace(/^(GREATER|PERFECT)_/, '')
  return [basic, 'GREATER_' + basic, 'PERFECT_' + basic]
}

const rankNumbers = { I: 1, II: 2, III: 3 } as const
function initialGrade(step: AcquisitionStep) {
  return rankNumbers[currencyRank(step.action) ?? 'I']
}

export function routeGrades(route: AcquisitionRoute) {
  return route.steps.map(initialGrade)
}

export function hasGradeOverride(
  route: AcquisitionRoute | undefined,
  grades: readonly number[] | null | undefined,
) {
  return (
    !!route &&
    !!grades &&
    route.steps.length === grades.length &&
    route.steps.every((step, i) => validGrade(step, grades[i]!)) &&
    route.steps.some((step, i) => initialGrade(step) !== grades[i])
  )
}

export function gradeSearch(grades?: readonly number[]) {
  return grades?.join('.') ?? ''
}

function validGrade(step: AcquisitionStep, grade: number) {
  return initialGrade(step) === grade || !!gradeActions(step)?.[grade - 1]
}

export function readRouteGrades(search: string, route?: AcquisitionRoute) {
  if (!route) return null
  const raw = new URLSearchParams(search).get('grades')
  if (raw === null) return routeGrades(route)
  const parts = raw.split('.')
  if (
    parts.length !== route.steps.length ||
    parts.some(
      (part, i) =>
        !/^[1-3]$/.test(part) || !validGrade(route.steps[i]!, Number(part)),
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
  maxUses,
  repeatUntilHit,
  repeatTarget,
}: AcquisitionStep) {
  return {
    action,
    omens,
    acquired,
    lost,
    ...(socketIndex == null ? {} : { socketIndex }),
    ...(maxUses == null ? {} : { maxUses }),
    ...(repeatUntilHit ? { repeatUntilHit } : {}),
    ...(repeatTarget == null ? {} : { repeatTarget }),
  }
}

/** Preserve the entire successful method, changing only explicitly selected currency grades. */
export function fixedSteps(route: AcquisitionRoute, grades: readonly number[]) {
  return route.steps.map((step, index) => {
    const action = gradeActions(step)?.[grades[index]! - 1] ?? step.action
    return {
      ...methodStep(step),
      action,
      ...(action === step.action ? {} : { originalAction: step.action }),
    }
  })
}

export function fixedRouteQuery(
  request: CreateRequest | null,
  base: AcquisitionResult | undefined,
  index: number,
  grades: readonly number[] | null,
) {
  const route = base?.routes[index]
  const steps =
    route && grades && hasGradeOverride(route, grades)
      ? fixedSteps(route, grades)
      : null
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
            const evaluation = await readAcquisitionResponse(response, request)
            const replay = evaluation.routes[0]
            if (
              evaluation.routes.length > 1 ||
              (replay &&
                (replay.cardIndex !== 0 ||
                  JSON.stringify(replay.steps.map(methodStep)) !==
                    JSON.stringify(steps.map(methodStep))))
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
