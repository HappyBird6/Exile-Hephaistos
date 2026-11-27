import type { PriceSnapshot } from '../currency-prices/api'
import { costAccumulator } from '../currency-prices/decimalAmount'
import type { CraftingCost } from '../currency-prices/cost'
import type {
  AcquisitionResult,
  AcquisitionRoute,
  FractureStrategy,
} from './acquisitionRouteApi'
import {
  acquisitionRouteCost,
  assumesEligibleReveal,
} from './acquisitionRouteFilm'
import { validStepChance } from '../workbenchHistory'

type FractureSession = {
  key: string
  chance: FractureStrategy['successChance']
  assumesReveal: boolean
  cost: CraftingCost
  lucky: boolean
}

/** Pay for the shared preparation and fracture once. Each outcome pays only for its own continuation. */
function strategyCost(
  routes: AcquisitionRoute[],
  stepIndex: number,
  market: PriceSnapshot | null | undefined,
): CraftingCost {
  const first = routes[0]!
  const prefix = acquisitionRouteCost(
    { ...first, steps: first.steps.slice(0, stepIndex + 1) },
    market,
  )
  const exalted = costAccumulator()
  const divine = costAccumulator()
  // Sum in units of 1/N and divide once, so decimal prices do not cross a display boundary.
  exalted.add(prefix.exalted, routes.length)
  divine.add(prefix.divine, routes.length)
  const unknown = new Set(prefix.unknown)
  let incomplete = prefix.incomplete
  let unknownUses = !!prefix.unknownUses
  const retryMaterials = [...(prefix.retryMaterials ?? [])]
  for (const route of routes) {
    const continuation = acquisitionRouteCost(
      { ...route, steps: route.steps.slice(stepIndex + 1) },
      market,
    )
    // The simulator chooses one of the eligible explicit modifiers uniformly.
    exalted.add(continuation.exalted, 1)
    divine.add(continuation.divine, 1)
    incomplete ||= continuation.incomplete
    unknownUses ||= !!continuation.unknownUses
    continuation.unknown.forEach((id) => unknown.add(id))
    if (continuation.retryMaterials)
      retryMaterials.push(...continuation.retryMaterials)
  }
  return {
    ...prefix,
    exalted: exalted.total() / routes.length,
    divine: divine.total() / routes.length,
    unknown: [...unknown],
    incomplete,
    ...(unknownUses ? { unknownUses } : {}),
    ...(retryMaterials.length ? { retryMaterials } : {}),
  }
}

export function fractureSessions(
  result: AcquisitionResult,
  market: PriceSnapshot | null | undefined,
) {
  const groups = new Map<string, number[]>()
  result.routes.forEach((route, i) => {
    const meta = route.fractureStrategy
    if (!meta) return
    const key = JSON.stringify(['fracture', route.cardIndex, meta.decisionId])
    const group = groups.get(key)
    if (group) group.push(i)
    else groups.set(key, [i])
  })
  const byIndex = new Map<number, FractureSession>()
  for (const [key, indices] of groups) {
    const routes = indices.map((i) => result.routes[i]!)
    const { stepIndex, successChance, baselineChance } =
      routes[0]!.fractureStrategy!
    const ids = new Set<string>()
    if (
      !validStepChance(successChance) ||
      !validStepChance(baselineChance) ||
      !routes.every((route) => {
        const meta = route.fractureStrategy!
        const step = route.steps[stepIndex]
        const id = step?.fracturedModifierId
        if (
          !id ||
          ids.has(id) ||
          meta.stepIndex !== stepIndex ||
          !validStepChance(meta.conditionalChance) ||
          step.action !== 'FRACTURING' ||
          step.chance?.probability == null ||
          Math.abs(step.chance.probability - 1 / routes.length) > 1e-12
        )
          return false
        ids.add(id)
        return true
      })
    )
      continue
    const session = {
      key,
      chance: successChance,
      cost: strategyCost(routes, stepIndex, market),
      assumesReveal: routes.some((route) =>
        route.steps.some(assumesEligibleReveal),
      ),
      lucky: routes.some((route) => route.lucky),
    }
    for (const index of indices) byIndex.set(index, session)
  }
  return byIndex
}
