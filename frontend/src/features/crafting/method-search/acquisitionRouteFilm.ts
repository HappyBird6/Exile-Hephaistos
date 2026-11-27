import { getLocale, type Locale } from '../../../shared/i18n/i18n'
import { revealAction } from '../abyss-workbench/revealAction'
import type { PriceSnapshot } from '../currency-prices/api'
import { craftingStepCost } from '../currency-prices/cost'
import { isBone } from '../desecrationCurrency'
import { sameItemState } from '../crafting-state/itemIdentity'
import { pathTreeMessages } from '../path-tree/messages'
import type { CreateRequest } from '../path-tree/types'
import {
  completedReveal,
  currencyId,
} from '../session-notation/extractSessionNotation'
import type { ConcreteItem } from '../workbenchApi'
import { catalystActionType } from '../workbenchApi'
import type { Film, Frame } from '../workbenchHistory'
import {
  acquisitionPreview,
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

/** Chosen catalysts are a fixed quantity, independent of the route's success probability. */
export function qualityMaterialQuantity(step: AcquisitionStep) {
  const count = step.expectedUses
  return catalystActionType(step.action) &&
    typeof count === 'number' &&
    Number.isSafeInteger(count) &&
    count > 0
    ? count
    : undefined
}

export function assumesEligibleReveal(step: AcquisitionStep) {
  return (
    (step.chance?.probability != null || !!step.chance?.probabilityBounds) &&
    (step.action === 'REVEAL' ||
      (isBone(step.action) && step.acquired.length > 0))
  )
}

function materialUses(step: AcquisitionStep) {
  const quantity = qualityMaterialQuantity(step)
  if (quantity !== undefined) return { quantity }
  const count = step.expectedUses
  const limit = step.maxUses
  if (step.repeatUntilHit) {
    return {
      repeatUntilHit: true as const,
      expectedUses: validExpectedUses(count, materialId(step.action))
        ? count
        : null,
    }
  }
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
      ...(step.action === 'QUALITY_40'
        ? { work: 'UNSPECIFIED_CATALYST_40' as const }
        : {}),
      ...(step.action === 'REVEAL' ? { kind: 'reveal' as const } : {}),
      currencyId: materialId(step.action),
      omensPerUse: !!step.maxUses && step.omens.length > 0,
      consumedOmenIds: step.omens,
      uses: qualityMaterialQuantity(step) ?? 1,
      ...materialUses(step),
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
  const rangedOptions =
    result.cards[route.cardIndex]?.options.filter(
      (option) => option.hasLowerBound && option.hasUpperBound,
    ) ?? []
  const heldOptions = new Set<number>()
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
    const previous = frames[index]!
    if (step.action === 'FRACTURING') {
      const locked = state.explicits.filter((mod) => mod.fractured)
      if (
        !step.fracturedModifierId ||
        locked.length !== 1 ||
        locked[0]?.modifierId !== step.fracturedModifierId ||
        !sameItemState(previous.state, {
          ...state,
          explicits: state.explicits.map((mod) => ({
            ...mod,
            fractured: false,
          })),
        })
      )
        return null
    }
    if (step.action === 'DIVINE') heldOptions.clear()
    else {
      for (const lost of step.lost) heldOptions.delete(lost)
      for (const acquired of step.acquired) heldOptions.add(acquired)
    }
    const tierRanges = rangedOptions
      .filter((option) => heldOptions.has(option.number))
      .map((option) => option.modifierIds)
    const isDummy =
      step.dummy ??
      (!step.acquired.length && !!route.steps[index + 1]?.repeatUntilHit)
    const dummy = isDummy
      ? state.explicits.find(
          (mod) =>
            !mod.fractured &&
            !previous.state.explicits.some(
              (old) => old.modifierId === mod.modifierId,
            ),
        )
      : undefined
    const preview = {
      [acquisitionPreview]: {
        tierRanges,
        ...(dummy ? { dummyId: dummy.modifierId } : {}),
      },
    }
    const chance = step.chance
    const odds = validStepChance(chance)
      ? {
          projectedChance: {
            probability: chance.probability,
            ...(chance.probabilityBounds
              ? { probabilityBounds: chance.probabilityBounds }
              : {}),
            ...(assumesEligibleReveal(step)
              ? { assumesEligibleReveal: true as const }
              : {}),
          },
        }
      : {}
    if (step.action === 'REVEAL') {
      const revealed: Frame = {
        state,
        action: revealAction,
        rulesetIdentity,
        ...odds,
        ...preview,
        ...(step.maxUses && step.maxUses > 1
          ? { projectedRevealLimit: step.maxUses }
          : {}),
        manual: {
          action: revealAction,
          rulesetIdentity,
          consumedOmens: step.omens,
        },
      }
      if (!completedReveal(revealed, previous)) return null
      frames.push(revealed)
    } else if (step.action === 'QUALITY_40') {
      frames.push({
        state,
        action: 'SESSION_PLAN',
        rulesetIdentity,
        ...odds,
        ...preview,
        projectedCurrencies: [],
        projectedWork: 'UNSPECIFIED_CATALYST_40',
      })
    } else {
      frames.push({
        state,
        action: 'SESSION_PLAN',
        rulesetIdentity,
        ...odds,
        ...preview,
        ...(step.omenOutcomes
          ? { projectedOmenOutcomes: step.omenOutcomes }
          : {}),
        projectedCurrencies: [
          {
            currencyId: materialId(step.action),
            omenIds: step.omens,
            ...(step.maxUses && step.omens.length ? { omensPerUse: true } : {}),
            ...materialUses(step),
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
    ...(validStepChance(route)
      ? {
          projectedRouteChance: {
            probability: route.probability,
            ...(route.probabilityBounds
              ? { probabilityBounds: route.probabilityBounds }
              : {}),
            ...(route.steps.some(assumesEligibleReveal)
              ? { assumesEligibleReveal: true as const }
              : {}),
          },
        }
      : {}),
    frames,
  }
}
