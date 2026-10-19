import { saveSupport } from './supportStorage'
import { beforeEach, expect, it, vi } from 'vitest'
import {
  render,
  screen,
  fireEvent,
  within,
  waitFor,
} from '@testing-library/react'
import { exaltedGroups, groupMass, graphProjection } from './outcomeGrouping'
import { methodPoint, isRepeatable } from './methodObservation'
import { AutomaticMethodSearch } from './AutomaticMethodSearch'
import type { AutomaticView, AutomaticArrow } from './automaticApi'
import type { Bounds } from './types'
import { requestFixture } from '../path-tree/fixtures.test-support'
import { createItemPresentation } from '../path-tree/presentation'
import { setLocale } from '../../../shared/i18n/i18n'
const bound = (n: string, d = '4'): Bounds => ({
  lower: { numerator: n, denominator: d },
  upper: { numerator: n, denominator: d },
  arithmeticWidth: { numerator: '0', denominator: '1' },
  exact: true,
  confirmedPercent: null,
})
const present = createItemPresentation(
  { name: 'Solar Amulet', itemClass: 'Amulet' },
  {},
)
function fixture(): AutomaticView {
  const item = requestFixture().start.item
  const point = {
    cycles: 1,
    complete: true,
    outcomes: [
      { target: 1, kind: 'GOAL', probability: bound('1') },
      { target: 2, kind: 'CHECKPOINT', probability: bound('2') },
      { target: null, kind: 'LOSS', probability: bound('1') },
    ],
    initialUses: {},
    cycleUses: { EXALTED: bound('1', '1') },
  }
  return {
    version: 'automatic-method-job-v1',
    id: '11111111-1111-4111-8111-111111111111',
    revision: 1,
    generation: 1,
    status: 'COMPLETE',
    reason: '',
    complete: true,
    operations: 1,
    retainedStates: 3,
    nodes: [0, 1, 2].map((id) => ({
      id,
      depth: id === 0 ? 0 : 1,
      quality40: false,
      item: structuredClone(item),
      progress: { goal: id === 1 ? 'MATCH' : 'NO_MATCH', entries: [] },
      discoveryFinished: true,
      directFinished: true,
    })),
    methods: [
      {
        id: 'exalt',
        source: 0,
        executionMode: 'SINGLE_PASS',
        initial: [],
        cycle: [
          { currency: 'EXALTED', omens: [], purpose: 'GOAL_ACQUISITION' },
        ],
        points: [
          point,
          ...[100, 300, 500].map((cycles) => ({ ...point, cycles })),
        ],
        reasons: [],
        finished: true,
      },
    ],
    recommendations: [],
  }
}
beforeEach(() => {
  localStorage.clear()
  setLocale('en')
})
it('groups multiple attributes/tiers only inside one Exalted method and preserves exact mass by result kind', () => {
  const view = fixture(),
    g = exaltedGroups(view, 100)[0]!
  expect(exaltedGroups(view, 100)).toHaveLength(1)
  expect(groupMass(g).map((x) => [x.kind, x.probability.lower])).toEqual([
    ['GOAL', { numerator: '1', denominator: '4' }],
    ['CHECKPOINT', { numerator: '1', denominator: '2' }],
    ['LOSS', { numerator: '1', denominator: '4' }],
  ])
  const p = graphProjection(view, 100, new Set())
  expect(p.nodes.map((n) => n.id)).toEqual([0])
  expect(p.groups).toHaveLength(1)
  expect(view.nodes).toHaveLength(3)
  expect(
    graphProjection(view, 100, new Set([g.id])).nodes.map((n) => n.id),
  ).toEqual([0, 1, 2])
})
it('does not combine different methods, omens, sources or Essence results', () => {
  const view = fixture(),
    m = view.methods[0]!
  view.methods.push({
    ...m,
    id: 'omen',
    cycle: [{ ...m.cycle[0]!, omens: ['DEXTRAL_EXALTATION'] }],
  })
  view.methods.push({ ...m, id: 'other-source', source: 2 })
  view.methods.push({
    ...m,
    id: 'essence',
    cycle: [{ ...m.cycle[0]!, currency: 'PERFECT_ESSENCE_BODY' }],
  })
  expect(exaltedGroups(view, 100).map((g) => g.method.id)).toEqual([
    'exalt',
    'omen',
    'other-source',
  ])
  expect(
    graphProjection(view, 100, new Set()).edges.some(
      (e) => e.method.id === 'essence' && e.target === 1,
    ),
  ).toBe(true)
})
it('rejects duplicate outcome rows instead of double-counting their probability', () => {
  const view = fixture()
  view.methods[0]!.points[0]!.outcomes.push({
    ...view.methods[0]!.points[0]!.outcomes[0]!,
  })
  expect(() => groupMass(exaltedGroups(view, 100)[0]!)).toThrow(
    'Duplicate method outcome',
  )
})
it('uses the first pass for Exalted and finite selected horizons for repeatable Chaos without multiplying preparation', () => {
  const m = fixture().methods[0]!
  expect(isRepeatable(m)).toBe(false)
  expect(methodPoint(m, 500)?.cycles).toBe(1)
  const chaos: AutomaticArrow = {
    ...m,
    executionMode: 'REPEATABLE',
    cycle: [{ currency: 'CHAOS', omens: [], purpose: 'GOAL_ACQUISITION' }],
    initial: [
      {
        currency: 'EXALTED',
        omens: [],
        purpose: 'TEMPORARY_AFFIX_FOR_REMOVAL',
      },
    ],
    points: m.points.map((p) => ({
      ...p,
      initialUses: { EXALTED: bound('1', '1') },
      cycleUses: { CHAOS: bound(String(p.cycles), '1') },
    })),
  }
  expect(methodPoint(chaos, 300)?.initialUses.EXALTED?.lower.numerator).toBe(
    '1',
  )
  expect(methodPoint(chaos, 300)?.cycleUses.CHAOS?.lower.numerator).toBe('300')
})
function serve(view: AutomaticView) {
  const request = requestFixture()
  vi.stubGlobal(
    'fetch',
    vi.fn(
      async () =>
        new Response(JSON.stringify(view), {
          headers: {
            'X-Crafting-Ruleset': request.start.provenance.rulesetIdentity,
          },
        }),
    ),
  )
  return request
}
it('hides repetition N for a single Exalted, exposes individual outcomes and connects the selected state and route', async () => {
  const view = fixture(),
    request = serve(view)
  render(
    <AutomaticMethodSearch
      request={request}
      inputGeneration={1}
      presentItem={present}
      onRecalculate={vi.fn()}
    />,
  )
  await screen.findByRole('heading', { name: 'Selected item' })
  const group = document.querySelector('.graph-arrival-labels button')!
  expect(
    screen.queryByRole('combobox', { name: 'Method repetitions' }),
  ).not.toBeInTheDocument()
  fireEvent.click(group)
  const details = screen.getByRole('region', {
    name: 'Candidates and probabilities',
  })
  expect(within(details).getAllByText(/25.00%/).length).toBeGreaterThan(0)
  fireEvent.click(within(details).getByRole('button', { name: 'Result 1' }))
  expect(await screen.findByLabelText('Other results')).toHaveValue('1')
  expect(
    screen.getByRole('region', { name: 'Crafting steps' }),
  ).toHaveTextContent('Exalted Orb')
  expect(
    screen.getByRole('region', { name: 'Crafting steps' }),
  ).toHaveTextContent('25.00%')
  expect(
    screen.getByRole('region', { name: 'Crafting steps' }),
  ).not.toHaveTextContent('GOAL')
  expect(screen.getByLabelText('Other results')).toHaveValue('1')
  expect(
    within(screen.getByRole('region', { name: 'Crafting steps' })).getByText(
      /Chance of this result:/,
    ),
  ).toHaveTextContent('1 attempt · Chance of this result: 25.00%')
})
it('lets the user change N only on a repeatable Chaos method', async () => {
  const view = fixture(),
    m = view.methods[0]!
  m.executionMode = 'REPEATABLE'
  m.cycle = [{ currency: 'CHAOS', omens: [], purpose: 'GOAL_ACQUISITION' }]
  const request = serve(view)
  render(
    <AutomaticMethodSearch
      request={request}
      inputGeneration={2}
      presentItem={present}
      onRecalculate={vi.fn()}
    />,
  )
  const selector = await screen.findByRole('combobox', {
    name: 'Method repetitions',
  })
  fireEvent.change(selector, { target: { value: '300' } })
  await waitFor(() => expect(selector).toHaveValue('300'))
})

it('reveals only the chosen exact branch while the other grouped outcomes remain collapsed', () => {
  const view = fixture()
  const p = graphProjection(view, 100, new Set(), [
    { method: 'exalt', source: 0, target: 1, kind: 'GOAL' },
  ])
  expect(p.nodes.map((n) => n.id)).toEqual([0, 1])
  expect(p.groups).toHaveLength(1)
  expect(view.nodes.map((n) => n.id)).toEqual([0, 1, 2])
})
it('shows the catalog tier beside the unchanged exact modifier value', () => {
  const display = createItemPresentation(
    { name: 'Solar Amulet', itemClass: 'Amulet' },
    {
      test: {
        id: 'test',
        name: 'Life',
        text: '+# to maximum Life',
        tier: 3,
        affixType: 'PREFIX',
        familyIds: ['life'],
        stats: [{ id: 'life', min: 1, max: 9 }],
      },
    },
  )
  const item = {
    ...requestFixture().start.item,
    explicits: [{ modifierId: 'test', values: { life: 7 }, fractured: false }],
  }
  const row = display(item, 'en').modifiers.find((m) => m.id === 'test')!
  expect(row.affixLabel).toBe('T3')
  expect(row.text).toContain('7')
  expect(item.explicits[0]!.values.life).toBe(7)
})

it('restores a single-pass grouped selection without applying saved repeat N and preserves it across tabs', async () => {
  const view = fixture(),
    request = serve(view)
  saveSupport('run', {
    request: request.clientRequestId,
    job: view.id,
    selected: 1,
    cycles: 300,
    route: [{ method: 'exalt', source: 0, target: 1, kind: 'GOAL' }],
  })
  const props = {
    request,
    inputGeneration: 3,
    presentItem: present,
    onRecalculate: vi.fn(),
  }
  const mounted = render(<AutomaticMethodSearch {...props} />)
  expect(await screen.findByLabelText('Other results')).toHaveValue('1')
  expect(
    within(screen.getByRole('region', { name: 'Crafting steps' })).getByText(
      /Chance of this result:/,
    ),
  ).toHaveTextContent('1 attempt · Chance of this result: 25.00%')
  expect(
    screen.queryByRole('combobox', { name: 'Method repetitions' }),
  ).not.toBeInTheDocument()
  mounted.rerender(<AutomaticMethodSearch {...props} active={false} />)
  mounted.rerender(<AutomaticMethodSearch {...props} active />)
  expect(
    screen.getByRole('region', { name: 'Crafting steps' }),
  ).toHaveTextContent('Exalted Orb')
  expect(
    screen.getByRole('region', { name: 'Crafting steps' }),
  ).toHaveTextContent('25.00%')
  expect(
    screen.getByRole('region', { name: 'Crafting steps' }),
  ).not.toHaveTextContent('GOAL')
  expect(screen.getByLabelText('Other results')).toHaveValue('1')
  mounted.unmount()
  render(<AutomaticMethodSearch {...props} />)
  expect(await screen.findByLabelText('Other results')).toHaveValue('1')
  expect(
    within(screen.getByRole('region', { name: 'Crafting steps' })).getByText(
      /Chance of this result:/,
    ),
  ).toHaveTextContent('1 attempt · Chance of this result: 25.00%')
})

it('keeps an Essence preparation value separate even when followed by Exalted', () => {
  const view = fixture()
  view.methods[0]!.initial = [
    { currency: 'PERFECT_ESSENCE_BODY', omens: [], purpose: 'ORDINARY' },
  ]
  expect(exaltedGroups(view, 100)).toEqual([])
  expect(graphProjection(view, 100, new Set()).nodes.map((n) => n.id)).toEqual([
    0, 1, 2,
  ])
})

it('keeps probabilities prominent while hiding implementation prose and detailed outcomes until requested', async () => {
  const request = serve(fixture())
  render(
    <AutomaticMethodSearch
      request={request}
      inputGeneration={4}
      presentItem={present}
      onRecalculate={vi.fn()}
    />,
  )
  await screen.findByRole('heading', { name: 'Selected item' })
  const method = document.querySelector('.method-map__methods .craft-choice')!
  expect(method.querySelector('.craft-chance')).toHaveTextContent('25.00%')
  const details = method.querySelector('details.craft-outcomes')!
  expect(details).not.toHaveAttribute('open')
  expect(screen.queryByText(/The API provides/)).not.toBeInTheDocument()
  expect(
    screen.queryByText(/Choose arrows from state 0/),
  ).not.toBeInTheDocument()
  expect(screen.queryByText(/global optimality/)).not.toBeInTheDocument()
  expect(
    screen.queryByText(/GOAL_ACQUISITION|Item state 0/),
  ).not.toBeInTheDocument()
})
it('keeps removal risk and incomplete calculation visible without opening outcome details', async () => {
  const view = fixture()
  view.methods[0]!.cycle[0]!.currency = 'CHAOS'
  view.methods[0]!.executionMode = 'REPEATABLE'
  view.methods[0]!.points.forEach((p) => (p.complete = false))
  const request = serve(view)
  render(
    <AutomaticMethodSearch
      request={request}
      inputGeneration={5}
      presentItem={present}
      onRecalculate={vi.fn()}
    />,
  )
  expect(
    await screen.findByText('An existing modifier may be removed.'),
  ).toBeVisible()
  expect(
    document.querySelector('.method-map__methods [role="status"]'),
  ).toBeVisible()
  expect(document.querySelector('.craft-outcomes')).not.toHaveAttribute('open')
})

it('labels preparation once and N only for the Chaos follower', async () => {
  const view = fixture()
  const m = view.methods[0]!
  m.initial = [
    { currency: 'EXALTED', omens: [], purpose: 'TEMPORARY_AFFIX_FOR_REMOVAL' },
  ]
  m.cycle = [{ currency: 'CHAOS', omens: [], purpose: 'GOAL_ACQUISITION' }]
  m.executionMode = 'REPEATABLE'
  for (const point of m.points) point.outcomes.at(-1)!.kind = 'RETRY'
  vi.stubGlobal(
    'fetch',
    vi.fn(
      async () =>
        new Response(JSON.stringify(view), {
          headers: {
            'X-Crafting-Ruleset':
              requestFixture().start.provenance.rulesetIdentity,
          },
        }),
    ),
  )
  render(
    <AutomaticMethodSearch
      request={requestFixture()}
      inputGeneration={1}
      presentItem={present}
      onRecalculate={vi.fn()}
    />,
  )
  await screen.findByLabelText('Method repetitions')
  const summary = document.querySelector('.craft-choice > summary')!
  expect(summary).toHaveTextContent(
    'Exalted Orb: 1 attempt · Chaos Orb: Up to 100 attempts',
  )
  expect(summary).toHaveTextContent('25.00%')
  expect(
    screen.getAllByText(/Target not reached within 100 attempts/),
  ).toHaveLength(1)
})

it('paints every graph arrow before every action label so later routes cannot obscure text', async () => {
  const view = fixture()
  const alternative = structuredClone(view.methods[0]!)
  alternative.id = 'chaos-alternative'
  alternative.cycle = [
    { currency: 'CHAOS', omens: [], purpose: 'GOAL_ACQUISITION' },
  ]
  view.methods.push(alternative)
  const request = serve(view)
  render(
    <AutomaticMethodSearch
      request={request}
      inputGeneration={6}
      presentItem={present}
      onRecalculate={vi.fn()}
    />,
  )
  await screen.findByRole('heading', { name: 'Selected item' })
  const graph = document.querySelector('.method-map__overview svg')!
  const arrows = [...graph.querySelectorAll('path[role="button"]')]
  const labels = [...graph.querySelectorAll('.graph-action-label')]
  expect(arrows.length).toBeGreaterThan(1)
  expect(labels.length).toBeGreaterThan(1)
  for (const arrow of arrows) {
    for (const label of labels) {
      expect(
        arrow.compareDocumentPosition(label) & Node.DOCUMENT_POSITION_FOLLOWING,
      ).not.toBe(0)
    }
  }
})
