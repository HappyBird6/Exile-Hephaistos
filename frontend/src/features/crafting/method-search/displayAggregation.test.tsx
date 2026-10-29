import { expect, it, vi } from 'vitest'
import { render } from '@testing-library/react'
import { requestFixture } from '../path-tree/fixtures.test-support'
import { createGoalFilterEditor } from '../goal-filter/editor'
import { createItemPresentation } from '../path-tree/presentation'
import type { Definition } from '../craftingApi'
import type { AutomaticNode, AutomaticView } from './automaticApi'
import {
  tierCandidates,
  junkClassifier,
  junkCard,
  goalSources,
} from './graphDisplay'
import { stateProjection } from './stateProjection'
import { arrivalProbabilities } from './arrivalProbabilities'
import { GraphCandidates } from './GraphCandidates'
import { commonStateCard } from './commonStateCard'
import { presentAutomaticItem } from './automaticTierLaws'
import { setLocale } from '../../../shared/i18n/i18n'
import type { Catalog } from '../goal-filter/types'
const definition = (
  id: string,
  stat: string,
  affixType: 'PREFIX' | 'SUFFIX',
  tier = 1,
): Definition => ({
  id,
  name: id,
  text: `+# ${stat}`,
  tier,
  affixType,
  familyIds: [stat],
  stats: [{ id: stat, min: 1, max: 50 }],
})
const definitions = {
  spirit: definition('spirit', 'spirit', 'PREFIX'),
  mana: definition('mana', 'mana', 'PREFIX'),
  life: definition('life', 'life', 'PREFIX'),
  dex: definition('dex', 'dex', 'SUFFIX'),
  spirit2: definition('spirit2', 'spirit', 'PREFIX', 2),
}
const present = createItemPresentation(
  { name: 'Test', itemClass: 'Amulet' },
  definitions,
)
const node = (
  id: number,
  modifierId: keyof typeof definitions,
  value: number,
): AutomaticNode => ({
  id,
  depth: 1,
  quality40: false,
  item: {
    ...requestFixture().start.item,
    rarity: 'RARE',
    explicits: [
      {
        modifierId,
        values: { [definitions[modifierId].stats![0]!.id]: value },
        fractured: false,
      },
    ],
  },
  progress: {
    goal: value >= 49 ? 'MATCH' : 'NO_MATCH',
    entries: [value >= 49 ? 'MATCH' : 'NO_MATCH'],
  },
  discoveryFinished: true,
  directFinished: true,
})
const bound = (n: string, d = '4') => ({
  lower: { numerator: n, denominator: d },
  upper: { numerator: n, denominator: d },
  arithmeticWidth: { numerator: '0', denominator: '1' },
  exact: true,
  confirmedPercent: null,
})
function view(nodes: AutomaticNode[]): AutomaticView {
  const root = {
    ...node(0, 'spirit', 1),
    depth: 0,
    item: {
      ...requestFixture().start.item,
      rarity: 'NORMAL' as const,
      explicits: [],
    },
  }
  return {
    version: 'automatic-method-job-v1',
    id: 'test',
    revision: 1,
    generation: 1,
    status: 'COMPLETE',
    reason: '',
    complete: true,
    operations: 1,
    retainedStates: nodes.length + 1,
    nodes: [root, ...nodes],
    recommendations: [],
    methods: [
      {
        id: 'regal',
        source: 0,
        initial: [
          { currency: 'TRANSMUTATION', omens: [], purpose: 'GOAL_ACQUISITION' },
        ],
        cycle: [{ currency: 'REGAL', omens: [], purpose: 'GOAL_ACQUISITION' }],
        reasons: [],
        finished: true,
        points: [
          {
            cycles: 1,
            complete: true,
            outcomes: nodes.map((n) => ({
              target: n.id,
              kind: n.progress.goal === 'MATCH' ? 'GOAL' : 'CHECKPOINT',
              probability: bound('1', String(nodes.length)),
            })),
            initialUses: {},
            cycleUses: {},
          },
        ],
      },
    ],
  }
}
it('normalizes old target settings and subsequent edits without mutating starting rarity or saved inputs', () => {
  const request = requestFixture()
  request.goal.general.rarities = ['NORMAL', 'MAGIC']
  request.start.item.rarity = 'NORMAL'
  const before = structuredClone(request),
    editor = createGoalFilterEditor(request.goal)
  expect(editor.getState().goal.general.rarities).toEqual(['RARE'])
  editor.getState().edit((g) => {
    g.general.rarities = ['MAGIC']
    g.general.itemLevel.min = 80
  })
  expect(editor.getState().goal.general.rarities).toEqual(['RARE'])
  expect(editor.getState().goal.general.itemLevel.min).toBe(80)
  expect(request).toEqual(before)
})
it('shows 47–50 as one T1 candidate while retaining a 49 target boundary and exact outcome masses', () => {
  setLocale('en')
  const nodes = [47, 48, 49, 50].map((v, i) => node(i + 1, 'spirit', v)),
    graph = view(nodes),
    before = JSON.stringify(graph)
  expect(tierCandidates(nodes)).toHaveLength(1)
  expect(
    tierCandidates([...nodes, node(5, 'spirit2', 46), node(6, 'mana', 47)]),
  ).toHaveLength(3)
  const card = commonStateCard(nodes.map((n) => present(n.item, 'en')))
  expect(card.modifiers[0]!.tierRows).toHaveLength(1)
  expect(card.modifiers[0]!.tierRows![0]).toMatchObject({
    label: 'T1',
    low: present(nodes[0]!.item, 'en').modifiers[0]!.text,
    high: present(nodes[3]!.item, 'en').modifiers[0]!.text,
  })
  const arrivals = new Map(
    nodes.map((n) => [
      n.id,
      [
        {
          method: graph.methods[0]!,
          outcome: graph.methods[0]!.points[0]!.outcomes.find(
            (o) => o.target === n.id,
          )!,
        },
      ],
    ]),
  )
  const mounted = render(
    <GraphCandidates
      nodes={nodes}
      cardFor={(id) => present(nodes.find((n) => n.id === id)!.item, 'en')}
      displayCardFor={(id) =>
        present(nodes.find((n) => n.id === id)!.item, 'en')
      }
      arrivals={arrivals}
      select={vi.fn()}
      choose={vi.fn()}
      selected={0}
      action={(a) => a.currency}
    />,
  )
  expect(
    mounted.container.querySelectorAll('[data-tier-candidates]'),
  ).toHaveLength(1)
  const masses = [
    ...mounted.container.querySelectorAll('[data-tier-candidates] > p'),
  ].map((p) => p.textContent)
  expect(masses).toHaveLength(2)
  expect(masses.every((p) => p?.includes('50.00%'))).toBe(true)
  expect(
    mounted.container.querySelectorAll('[data-candidate-id]'),
  ).toHaveLength(4)
  expect(JSON.stringify(graph)).toBe(before)
})
it('sums a node tier law without assigning probability across distinct modifiers', () => {
  const n = node(1, 'spirit', 47)
  n.attributes = {
    spirit: [47, 48, 49, 50].map((v) => ({
      modifierId: 'spirit',
      stat: 'spirit',
      min: v,
      max: v,
      tier: 1,
      role: 'PREFIX spirit',
    })),
  }
  n.tiers = {
    spirit: {
      variants: [47, 48, 49, 50].map((v) => ({
        modifierId: 'spirit',
        stat: 'spirit',
        min: v,
        max: v,
        probability: { numerator: '1', denominator: '4' },
      })),
    },
  }
  const before = JSON.stringify(n),
    card = presentAutomaticItem(n, 'en', present)
  expect(card.modifiers[0]!.tierRows).toHaveLength(1)
  expect(card.modifiers[0]!.tierRows![0]!.chance).toBe('100%')
  expect(JSON.stringify(n)).toBe(before)
  expect(
    tierCandidates([
      n,
      {
        ...n,
        id: 2,
        attributes: {
          spirit: [{ ...n.attributes.spirit![0]!, min: 48, max: 48 }],
        },
      },
    ]),
  ).toHaveLength(2)
})
it('bundles only non-target differences with counts and sides intact, retaining every conditional route once', () => {
  const nodes = [
    node(1, 'mana', 5),
    node(2, 'life', 6),
    node(3, 'dex', 5),
    node(4, 'spirit', 47),
  ]
  nodes.push({
    ...node(5, 'mana', 5),
    item: {
      ...node(5, 'mana', 5).item,
      explicits: [
        ...node(5, 'mana', 5).item.explicits,
        ...node(6, 'life', 6).item.explicits,
      ],
    },
  })
  const graph = view(nodes),
    before = JSON.stringify(graph),
    junk = junkClassifier(['spirit'], definitions)
  const projection = stateProjection(graph, 1, junk)
  expect(projection.owner.get(1)).toBe(projection.owner.get(2))
  for (const id of [3, 4, 5])
    expect(projection.owner.get(id)).not.toBe(projection.owner.get(1))
  expect(projection.bundles.flatMap((b) => b.nodes)).toHaveLength(6)
  expect(
    projection.edges.flatMap((e) => e.variants).flatMap((v) => v.outcomes),
  ).toHaveLength(5)
  const edge = projection.edges.find((e) => e.to === projection.owner.get(1))!
  expect(arrivalProbabilities(edge, 1)[0]!.probability.lower).toEqual({
    numerator: '2',
    denominator: '5',
  })
  expect(
    junkCard(nodes[0]!, present(nodes[0]!.item, 'ko'), junk, 'ko').modifiers[0],
  ).toMatchObject({ text: '비유효옵(잡옵) ×1', affixLabel: 'P' })
  expect(
    junkCard(nodes[2]!, present(nodes[2]!.item, 'ko'), junk, 'ko').modifiers[0]!
      .affixLabel,
  ).toBe('S')
  expect(JSON.stringify(graph)).toBe(before)
})
it('resolves pseudo contributions and conservatively disables junk grouping for unknown goals', () => {
  const goal = requestFixture().goal
  goal.groups = [
    {
      id: 'g',
      type: 'AND',
      disabled: false,
      range: null,
      entries: [
        {
          id: 'e',
          statId: 'total',
          unit: 'flat',
          disabled: false,
          weight: null,
          range: { min: 49, max: null },
        },
      ],
    },
  ]
  const catalog = {
    stats: [
      {
        statId: 'total',
        kind: 'PSEUDO',
        contributions: [{ statId: 'direct', coefficient: 1 }],
      },
      { statId: 'direct', kind: 'EXPLICIT', sourceStatIds: ['spirit'] },
    ],
  } as unknown as Catalog
  expect(goalSources(goal, catalog)).toEqual(['spirit'])
  goal.groups[0]!.entries[0]!.statId = 'unknown'
  expect(goalSources(goal, catalog)).toBeUndefined()
  expect(
    junkClassifier(undefined, definitions)(node(1, 'mana', 5), 'mana'),
  ).toBeUndefined()
})

it('keeps fractured junk separate and preserves original conditional probabilities', () => {
  const unlocked = node(1, 'mana', 5)
  const locked = node(2, 'mana', 5)
  locked.item.explicits[0]!.fractured = true
  const graph = view([unlocked, locked])
  const junk = junkClassifier(['spirit'], definitions)
  const projection = stateProjection(graph, 1, junk)
  expect(projection.owner.get(1)).not.toBe(projection.owner.get(2))
  expect(
    junkCard(locked, present(locked.item, 'en'), junk, 'en').modifiers[0],
  ).toMatchObject({ fractured: true })
  expect(
    projection.edges.flatMap((e) => e.variants).flatMap((v) => v.outcomes),
  ).toHaveLength(2)
})
