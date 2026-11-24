import { getLocale, type Locale } from '../../../shared/i18n/i18n'
import { revealAction } from '../abyss-workbench/revealAction'
import type { PriceSnapshot } from '../currency-prices/api'
import { craftingStepCost } from '../currency-prices/cost'
import { pathTreeMessages } from '../path-tree/messages'
import type { CreateRequest } from '../path-tree/types'
import { currencyId } from '../session-notation/extractSessionNotation'
import type { ConcreteItem } from '../workbenchApi'
import type { Film, Frame } from '../workbenchHistory'
import type { AcquisitionResult, AcquisitionRoute } from './acquisitionRouteApi'
import { acquisitionRouteMessages } from './acquisitionRouteMessages'

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

export function acquisitionOverviewHref(requestId: string, routeIndex: number) {
  return (
    '/support-test/overview?' +
    new URLSearchParams({
      request: requestId,
      route: String(routeIndex),
    })
  )
}

function materialId(action: string) {
  return currencyId(action) ?? action
}

export function acquisitionRouteCost(
  route: AcquisitionRoute,
  snapshot: PriceSnapshot | null | undefined,
) {
  return craftingStepCost(
    route.steps
      .filter((s) => s.action !== 'REVEAL')
      .map((step) => ({
        currencyId: materialId(step.action),
        consumedOmenIds: step.omens,
        uses: 1,
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
    if (step.action === 'REVEAL') {
      frames.push({
        state,
        action: revealAction,
        rulesetIdentity,
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
        projectedCurrencies: [
          {
            currencyId: materialId(step.action),
            omenIds: step.omens,
          },
        ],
      })
    }
  }
  return {
    id: `${request.clientRequestId}-${routeIndex}`,
    title: acquisitionRouteTitle(route, locale),
    createdAt: '',
    rulesetIdentity,
    frames,
  }
}
