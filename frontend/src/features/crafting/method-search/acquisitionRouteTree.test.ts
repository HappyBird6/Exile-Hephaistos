import { expect, it } from 'vitest'
import type { PriceSnapshot } from '../currency-prices/api'
import { requestFixture } from '../path-tree/fixtures.test-support'
import type { ConcreteItem } from '../workbenchApi'
import type { AcquisitionRoute, AcquisitionStep } from './acquisitionRouteApi'
import {
  buildAcquisitionRouteTree,
  projectAcquisitionRouteTree,
  type AcquisitionTreeSession,
} from './acquisitionRouteTree'
import { fixedSteps } from './fixedRouteGrades'

const start = requestFixture().start.item as ConcreteItem
const prices: PriceSnapshot = {
  updatedAt: '2026-11-01T00:00:00Z',
  league: 'test',
  exaltedPerDivine: 100,
  prices: {
    'Exalted Orb': { name: 'Exalted Orb', divine: 0.01, exalted: 1 },
    'Divine Orb': { name: 'Divine Orb', divine: 1, exalted: 100 },
    'Omen of Whittling': {
      name: 'Omen of Whittling',
      divine: 0.1,
      exalted: 10,
    },
    'Chaos Orb': { name: 'Chaos Orb', divine: 0.02, exalted: 2 },
  },
}

const first: AcquisitionStep = {
  action: 'EXALTED',
  acquired: [1],
  lost: [],
  omens: [],
  chance: { probability: 0.4 },
  reachChance: { probability: 0.4 },
}
const second: AcquisitionStep = {
  action: 'DIVINE',
  acquired: [],
  lost: [],
  omens: [],
  maxUses: 1000,
  expectedUses: 2.5,
  chance: { probability: 0.4 },
  reachChance: {
    probability: null,
    probabilityBounds: { lower: 0.3, upper: 0.4 },
  },
}
const card = {
  options: [
    { number: 1, modifierIds: ['life'] },
    { number: 2, modifierIds: ['mana'] },
  ],
}
function session(
  index: number,
  steps: AcquisitionStep[],
  frames?: ConcreteItem[],
  extra?: Partial<AcquisitionRoute>,
): AcquisitionTreeSession {
  const states = frames ?? steps.map(() => start)
  const route = {
    cardIndex: 0,
    steps,
    order: steps.flatMap((s) => s.acquired),
    stateIndices: steps.map((_, i) => i),
    probability: 1,
    ...extra,
  }
  return {
    index,
    route,
    result: { complete: true, cards: [card], states, routes: [route] },
  }
}

it('keeps zero-step and intermediate terminals and links every original sharing that prefix', () => {
  const tree = buildAcquisitionRouteTree(
    start,
    [
      session(3, []),
      session(5, [first], [structuredClone(start)], { probability: 0.4 }),
      session(9, [first, second]),
      session(24, [first, second], undefined, { lucky: true }),
    ],
    prices,
  )
  expect(tree.nodes).toHaveLength(3)
  expect(tree.nodes[0]).toMatchObject({
    depth: 0,
    terminals: [3],
    routeIndices: [3, 5, 9, 24],
    reachChance: { probability: 1 },
    cost: { exalted: 0 },
  })
  expect(tree.nodes[1]).toMatchObject({
    parentId: 0,
    terminals: [5],
    children: [2],
    routeIndices: [5, 9, 24],
    reachChance: first.reachChance,
    cost: { exalted: 1 },
  })
  expect(tree.nodes[2]).toMatchObject({
    parentId: 1,
    terminals: [9, 24],
    reachChance: second.reachChance,
    cost: { exalted: 251 },
  })
  expect([...tree.terminalNodes]).toEqual([
    [3, 0],
    [5, 1],
    [9, 2],
    [24, 2],
  ])
  const view = projectAcquisitionRouteTree(tree, new Set([24]))
  expect(view.nodes[1]).toMatchObject({
    children: [2],
    terminals: [],
    routeIndex: 24,
  })
  expect(view.nodes[2]!.terminals).toEqual([24])
  expect(tree.nodes[1]!.routeIndices).toEqual([5, 9, 24])
  const alreadyDone = projectAcquisitionRouteTree(tree, new Set([3]))
  expect(alreadyDone.nodes[0]).toMatchObject({ children: [], terminals: [3] })
  expect([...alreadyDone.terminalNodes]).toEqual([[3, 0]])
})

it('separates different actions, targets, omens, witnesses, and measured costs or chances', () => {
  const variants: Partial<AcquisitionStep>[] = [
    { action: 'GREATER_EXALTED' },
    { omens: ['Omen_of_Sinistral_Exaltation'] },
    { acquired: [2] },
    { lost: [2] },
    { dummy: true },
    { lucky: true },
    { repeatUntilHit: true, repeatTarget: 1 },
    { repeatUntilHit: true, repeatTarget: 2 },
    { socketIndex: 0 },
    { expectedUses: 2 },
    { reachChance: { probability: 0.3 } },
    { chance: { probability: 0.5 } },
  ]
  const sessions = [
    session(0, [first]),
    ...variants.map((v, i) => session(i + 1, [{ ...first, ...v }])),
  ]
  const differentCard = session(50, [first])
  differentCard.result.cards = [
    { options: [{ number: 1, modifierIds: ['resistance'] }] },
  ]
  sessions.push(
    differentCard,
    session(60, [first], [{ ...start, qualityInfused: true }]),
  )
  const tree = buildAcquisitionRouteTree(start, sessions)
  expect(tree.nodes[0]!.children).toHaveLength(sessions.length)
  expect(new Set(tree.terminalNodes.values()).size).toBe(sessions.length)
  expect(fixedSteps(sessions[6]!.route, [2])[0]).not.toHaveProperty('lucky')
  expect(fixedSteps(sessions[0]!.route, [2])[0]).not.toHaveProperty(
    'reachChance',
  )
})

it('retains distinct histories that later arrive at equal concrete items', () => {
  const a = session(1, [first, second])
  const b = session(2, [{ ...first, action: 'REGAL' }, second])
  const tree = buildAcquisitionRouteTree(start, [a, b])
  const left = tree.nodes[tree.terminalNodes.get(1)!]!
  const right = tree.nodes[tree.terminalNodes.get(2)!]!
  expect(left.state).toEqual(right.state)
  expect(left.parentId).not.toEqual(right.parentId)
  expect(left.id).not.toEqual(right.id)
  expect(
    projectAcquisitionRouteTree(tree, new Set([1])).nodes[0]!.children,
  ).toEqual([left.parentId])
})

it('uses cumulative server evidence, prices repeated currency and omens, and leaves unmeasured legacy prefixes unknown', () => {
  const chaos: AcquisitionStep = {
    action: 'CHAOS',
    omens: ['Omen_of_Whittling'],
    acquired: [],
    lost: [],
    chance: { probability: 0.05 },
  }
  const route = session(
    1,
    [chaos, { ...second, reachChance: null }],
    undefined,
    { probability: 0.03 },
  )
  const tree = buildAcquisitionRouteTree(start, [route], prices)
  expect(tree.nodes[1]).toMatchObject({
    reachChance: null,
    cost: { exalted: 12, incomplete: false },
  })
  expect(tree.nodes[2]).toMatchObject({
    reachChance: { probability: 0.03 },
    cost: { exalted: 262, incomplete: false },
  })
  const missingPrices = buildAcquisitionRouteTree(start, [route], null)
  expect(missingPrices.nodes[1]!.cost).toMatchObject({
    incomplete: true,
    unknown: ['Chaos_Orb', 'Omen_of_Whittling'],
  })
})

it('keeps every node tied to a replayable frame from the same starting item', () => {
  const invalid = session(10, [first, second])
  invalid.route.stateIndices[1] = 100
  const wrongBase = session(
    11,
    [first],
    [{ ...start, baseItemId: 'another-base' }],
  )
  const valid = session(12, [first])
  const tree = buildAcquisitionRouteTree(start, [invalid, wrongBase, valid])
  expect(tree.nodes).toHaveLength(2)
  expect(tree.nodes[0]!.routeIndices).toEqual([12])
  expect([...tree.terminalNodes]).toEqual([[12, 1]])
})
