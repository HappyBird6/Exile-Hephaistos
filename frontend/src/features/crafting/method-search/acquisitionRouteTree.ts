import type { PriceSnapshot } from '../currency-prices/api'
import type { CraftingCost } from '../currency-prices/cost'
import { canonical } from '../path-tree/validation'
import type { ConcreteItem } from '../workbenchApi'
import { type ProjectedChance, validStepChance } from '../workbenchHistory'
import type {
  AcquisitionResult,
  AcquisitionRoute,
  AcquisitionStep,
} from './acquisitionRouteApi'
import {
  acquisitionRouteCost,
  assumesEligibleReveal,
} from './acquisitionRouteFilm'

/** A grade replay has its own state dictionary. The index always refers to the original session. */
export interface AcquisitionTreeSession {
  index: number
  result: AcquisitionResult
  route: AcquisitionRoute
}

export interface RouteTreeNode {
  id: number
  parentId: number | null
  depth: number
  step: AcquisitionStep | null
  state: ConcreteItem
  reachChance: ProjectedChance | null
  cost: CraftingCost
  children: number[]
  terminals: number[]
  routeIndices: number[]
  routeIndex: number
}

export interface AcquisitionRouteTree {
  nodes: RouteTreeNode[]
  terminalNodes: Map<number, number>
}

/** Merge equal transitions only among siblings; equal item snapshots later in two routes remain separate. */
export function buildAcquisitionRouteTree(
  start: ConcreteItem,
  sessions: readonly AcquisitionTreeSession[],
  market?: PriceSnapshot | null,
): AcquisitionRouteTree {
  const nodes: RouteTreeNode[] = [
    {
      id: 0,
      parentId: null,
      depth: 0,
      step: null,
      state: start,
      reachChance: { probability: 1 },
      cost: acquisitionRouteCost({ steps: [] }, market),
      children: [],
      terminals: [],
      routeIndices: [],
      routeIndex: -1,
    },
  ]
  const terminalNodes = new Map<number, number>()
  const childrenByKey: Map<string, number>[] = [new Map()]
  // Grade replays can use different indices for the same concrete item.
  const stateIds = new WeakMap<ConcreteItem, number>()
  const stateKeys = new Map<string, number>()
  function stateId(state: ConcreteItem) {
    let id = stateIds.get(state)
    if (id === undefined) {
      const key = canonical(state)
      id = stateKeys.get(key) ?? stateKeys.size
      stateKeys.set(key, id)
      stateIds.set(state, id)
    }
    return id
  }

  type Card = AcquisitionResult['cards'][number]
  const targets = new WeakMap<Card, Map<number, string>>()
  function targetKeys(card: Card) {
    let keys = targets.get(card)
    if (!keys) {
      keys = new Map(
        card.options.map((option) => [
          option.number,
          JSON.stringify([
            option.number,
            [...option.modifierIds].sort(),
            !!option.hasLowerBound,
            !!option.hasUpperBound,
          ]),
        ]),
      )
      targets.set(card, keys)
    }
    return keys
  }

  for (const { index, result, route } of sessions) {
    const card = result.cards[route.cardIndex]
    if (
      !card ||
      !Array.isArray(route.stateIndices) ||
      route.steps.length !== route.stateIndices.length ||
      terminalNodes.has(index)
    )
      continue
    const frames = route.stateIndices.map((i) =>
      Number.isInteger(i) ? result.states?.[i] : undefined,
    )
    if (
      frames.some(
        (state) =>
          !state ||
          state.baseItemId !== start.baseItemId ||
          state.snapshotId !== start.snapshotId,
      )
    )
      continue

    const targetIds = targetKeys(card)
    const target = (n: number) =>
      targetIds.get(n) ?? JSON.stringify([route.cardIndex, n])
    let node = nodes[0]!
    if (!node.routeIndices.length) node.routeIndex = index
    node.routeIndices.push(index)
    let assumesReveal = false
    for (const [i, step] of route.steps.entries()) {
      const state = frames[i]!
      assumesReveal ||= assumesEligibleReveal(step)
      // Older responses only establish the cumulative probability at completion.
      const reach =
        step.reachChance ?? (i + 1 === route.steps.length ? route : null)
      const reachChance = validStepChance(reach)
        ? {
            probability: reach.probability,
            ...(reach.probabilityBounds
              ? { probabilityBounds: reach.probabilityBounds }
              : {}),
            ...(assumesReveal ? { assumesEligibleReveal: true as const } : {}),
          }
        : null
      const key = canonical([
        stateId(state),
        step.action,
        [...step.omens].sort(),
        step.socketIndex ?? null,
        step.acquired.map(target),
        step.lost.map(target),
        step.repeatTarget == null ? null : target(step.repeatTarget),
        !!step.dummy,
        !!step.lucky,
        !!step.repeatUntilHit,
        step.maxUses ?? null,
        step.expectedUses === undefined ? 'absent' : step.expectedUses,
        step.chance ?? null,
        step.omenOutcomes ?? null,
        reachChance,
      ])
      let child = childrenByKey[node.id]!.get(key)
      if (child === undefined) {
        child = nodes.length
        nodes.push({
          id: child,
          parentId: node.id,
          depth: i + 1,
          step,
          state,
          reachChance,
          cost: acquisitionRouteCost(
            { steps: route.steps.slice(0, i + 1) },
            market,
          ),
          children: [],
          terminals: [],
          routeIndices: [],
          routeIndex: index,
        })
        childrenByKey[node.id]!.set(key, child)
        childrenByKey.push(new Map())
        node.children.push(child)
      }
      node = nodes[child]!
      node.routeIndices.push(index)
    }
    node.terminals.push(index)
    terminalNodes.set(index, node.id)
  }
  return { nodes, terminalNodes }
}

/** Keep stable node IDs so the visible graph can refer back to all sessions sharing a prefix. */
export function projectAcquisitionRouteTree(
  tree: AcquisitionRouteTree,
  visibleIndices: ReadonlySet<number>,
): AcquisitionRouteTree {
  const terminalNodes = new Map(
    [...tree.terminalNodes].filter(([index]) => visibleIndices.has(index)),
  )
  if (terminalNodes.size === tree.terminalNodes.size) return tree
  const routeIndices = tree.nodes.map((node) =>
    node.routeIndices.filter((index) => visibleIndices.has(index)),
  )
  const nodes = tree.nodes.map((node) => ({
    ...node,
    routeIndices: routeIndices[node.id]!,
    routeIndex: routeIndices[node.id]![0] ?? -1,
    children: node.children.filter((id) => routeIndices[id]!.length > 0),
    terminals: node.terminals.filter((index) => visibleIndices.has(index)),
  }))
  return { nodes, terminalNodes }
}
