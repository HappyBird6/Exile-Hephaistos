import { getLocale, type Locale } from '../../../shared/i18n/i18n'
import { revealAction } from '../abyss-workbench/revealAction'
import type { PriceSnapshot } from '../currency-prices/api'
import { craftingStepCost } from '../currency-prices/cost'
import { isBone } from '../desecrationCurrency'
import { pathTreeMessages } from '../path-tree/messages'
import type { CreateRequest } from '../path-tree/types'
import { currencyId } from '../session-notation/extractSessionNotation'
import type { ConcreteItem } from '../workbenchApi'
import type { Film, Frame } from '../workbenchHistory'
import {
  boundedChaosCurrency,
  validExpectedUses,
  validStepChance,
} from '../workbenchHistory'
import type {
  AcquisitionResult,
  AcquisitionRoute,
  AcquisitionStep,
} from './acquisitionRouteApi'
import { acquisitionRouteMessages } from './acquisitionRouteMessages'
import { gradeSearch } from './fixedRouteGrades'

export function acquisitionRouteTitle(route: AcquisitionRoute, locale: Locale) {
  const target = acquisitionRouteMessages[locale].card.replace(
    '{n}',
    String(route.cardIndex + 1),
  )
  const order =
    route.steps.length === 0
      ? pathTreeMessages[locale].achieved
      : route.order.join(' → ')
  return [target, order].filter(Boolean).join(' · ')
}

export function acquisitionOverviewHref(
  requestId: string,
  routeIndex: number,
  grades?: readonly number[],
) {
  const params = new URLSearchParams({
    request: requestId,
    route: String(routeIndex),
  })
  const selection = gradeSearch(grades)
  if (selection) params.set('grades', selection)
  return '/support-test/overview?' + params
}

function materialId(action: string) {
  return currencyId(action) ?? action
}

export function assumesEligibleReveal(step: AcquisitionStep) {
  return (
    step.chance?.probability != null &&
    (step.action === 'REVEAL' ||
      (isBone(step.action) && step.acquired.length > 0))
  )
}

function repeatUses(step: AcquisitionStep) {
  const count = step.expectedUses
  const limit = step.maxUses
  const bounds =
    typeof limit === 'number' &&
    Number.isInteger(limit) &&
    limit >= 1 &&
    limit <= 1000
      ? { maxUses: limit }
      : {}
  if (validExpectedUses(count, materialId(step.action), bounds.maxUses))
    return { ...bounds, expectedUses: count }
  return step.action === 'DIVINE' ||
    count !== undefined ||
    boundedChaosCurrency(materialId(step.action), bounds.maxUses)
    ? { ...bounds, expectedUses: null }
    : bounds
}

export function acquisitionRouteCost(
  route: AcquisitionRoute,
  snapshot: PriceSnapshot | null | undefined,
) {
  return craftingStepCost(
    route.steps.map((step) => ({
      ...(step.action === 'REVEAL' ? { kind: 'reveal' as const } : {}),
      currencyId: materialId(step.action),
      omensPerUse: !!step.maxUses && step.omens.length > 0,
      consumedOmenIds: step.omens,
      uses: 1,
      ...repeatUses(step),
    })),
    snapshot,
  )
}

/** Each transition uses the corresponding state in the server's verified example. */
export function acquisitionRouteFilm(
  request: CreateRequest,
  result: AcquisitionResult,
  routeIndex: number,
  locale: Locale = getLocale(),
  sourceIndex = routeIndex,
): Film | null {
  const route = result.routes[routeIndex]
  if (
    !route ||
    !Array.isArray(route.stateIndices) ||
    route.steps.length !== route.stateIndices.length
  )
    return null
  const { rulesetIdentity } = request.start.provenance
  const start = request.start.item as ConcreteItem
  const frames: Frame[] = [{ state: start, action: null, rulesetIdentity }]
  for (const [index, step] of route.steps.entries()) {
    const stateIndex = route.stateIndices[index]!
    const state = result.states?.[stateIndex]
    if (
      !Number.isInteger(stateIndex) ||
      !state ||
      state.baseItemId !== start.baseItemId ||
      state.snapshotId !== start.snapshotId
    )
      return null
    const chance = step.chance
    const odds = validStepChance(chance)
      ? {
          projectedChance: {
            probability: chance.probability,
            ...(assumesEligibleReveal(step)
              ? { assumesEligibleReveal: true as const }
              : {}),
          },
        }
      : {}
    if (step.action === 'REVEAL') {
      frames.push({
        state,
        action: revealAction,
        rulesetIdentity,
        ...odds,
        ...(step.maxUses && step.maxUses > 1
          ? { projectedRevealLimit: step.maxUses }
          : {}),
        manual: {
          action: revealAction,
          rulesetIdentity,
          consumedOmens: step.omens,
        },
      })
    } else {
      frames.push({
        state,
        action: 'SESSION_PLAN',
        rulesetIdentity,
        ...odds,
        projectedCurrencies: [
          {
            currencyId: materialId(step.action),
            omenIds: step.omens,
            ...(step.maxUses && step.omens.length ? { omensPerUse: true } : {}),
            ...repeatUses(step),
          },
        ],
      })
    }
  }
  return {
    id: `${request.clientRequestId}-${sourceIndex}`,
    title: acquisitionRouteTitle(route, locale),
    createdAt: '',
    rulesetIdentity,
    frames,
  }
}
