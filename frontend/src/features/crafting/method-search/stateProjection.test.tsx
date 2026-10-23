import { expect, it, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, within } from '@testing-library/react'
import { gunzipSync } from 'node:zlib'
import code from './large-three-goals.test-fixture.exile?raw'
import { expandScenario, type Scenario } from './scenarioCode'
import { stateProjection } from './stateProjection'
import { commonStateCard } from './commonStateCard'
import { GroupedMethodGraph } from './GroupedMethodGraph'
import { setLocale } from '../../../shared/i18n/i18n'
const original = () =>
  expandScenario(
    JSON.parse(
      gunzipSync(Buffer.from(code.split('.')[2]!, 'base64url')).toString(),
    ),
  ) as Scenario
beforeEach(() => setLocale('en'))
it('projects the unchanged third code into nine item bundles with all 63 states and all 48 terminal states', () => {
  const scenario = original(),
    before = JSON.stringify(scenario.result),
    p = stateProjection(scenario.result, 100)
  expect(p.bundles).toHaveLength(9)
  expect(
    p.bundles
      .flatMap((b) => b.nodes)
      .map((n) => n.id)
      .sort((a, b) => a - b),
  ).toEqual(Array.from({ length: 63 }, (_, i) => i))
  expect(
    p.bundles
      .filter((b) => !p.edges.some((e) => e.from === b.id))
      .flatMap((b) => b.nodes),
  ).toHaveLength(48)
  expect(
    p.bundles
      .filter((b) => b.depth === 1)
      .map((b) => b.nodes.length)
      .sort((a, b) => a - b),
  ).toEqual([1, 1, 5, 7])
  expect(JSON.stringify(scenario.result)).toBe(before)
})
it('preserves exact Essence target values and subsequent routes while grouping Regal tier variants', () => {
  const s = original(),
    p = stateProjection(s.result, 100)
  for (const id of [1, 11])
    expect(
      p.bundles.find((b) => b.nodes.some((n) => n.id === id))!.nodes,
    ).toHaveLength(1)
  for (const m of s.result.methods) {
    for (const o of m.points[0]!.outcomes.filter(
      (o) => o.target !== null && o.kind !== 'DEAD',
    ))
      expect(
        p.edges
          .flatMap((e) => e.variants)
          .some((v) => v.method.id === m.id && v.outcomes.includes(o)),
      ).toBe(true)
  }
  expect(
    p.edges
      .flatMap((e) => e.variants)
      .flatMap((v) => v.outcomes)
      .every((o) => o.target !== null),
  ).toBe(true)
})
it.each(['REGAL', 'EXALTED', 'CHAOS', 'GREATER_EXALTED', 'PERFECT_CHAOS'])(
  'groups random tiers for %s without a currency allowlist',
  (currency) => {
    const s = original()
    s.result.methods = s.result.methods.filter(
      (m) =>
        !m.initial.concat(m.cycle).some((a) => a.currency.includes('ESSENCE')),
    )
    s.result.methods.forEach(
      (m) => (m.cycle = [{ currency, omens: [], purpose: 'GOAL_ACQUISITION' }]),
    )
    const p = stateProjection(s.result, 100)
    expect(
      p.bundles
        .filter((b) => b.depth === 1)
        .map((b) => b.nodes.length)
        .sort((a, b) => a - b),
    ).toEqual([6, 8])
    expect(p.bundles.filter((b) => b.depth === 2)).toHaveLength(1)
  },
)
it('never invents a concrete tier in the common card and retains exact variations', () => {
  const s = original(),
    p = stateProjection(s.result, 100),
    b = p.bundles.find((b) => b.nodes.length === 7 && b.depth === 1)!
  const card = commonStateCard(b.nodes.map((n) => s.cards[String(n.id)]!.en))
  expect(card.modifiers.some((m) => m.text.includes('120'))).toBe(true)
  expect(
    card.modifiers.filter((m) => m.id.startsWith('candidates:')),
  ).toHaveLength(1)
  const candidates = card.modifiers.find((m) => m.id.startsWith('candidates:'))!
  expect(candidates.affixLabel).toBeUndefined()
  for (const n of b.nodes) {
    const line = s.cards[String(n.id)]!.en.modifiers.find(
      (m) => m.id === n.item.explicits[1]!.modifierId,
    )!
    expect(candidates.tierRows!.some((r) => r.low === line.text)).toBe(true)
  }
})
it('keeps terminal cards and bundle count stable through details, candidate selection and zoom', () => {
  const s = original(),
    select = vi.fn(),
    choose = vi.fn()
  const mounted = render(
    <GroupedMethodGraph
      view={s.result}
      cycles={100}
      selected={0}
      route={[]}
      select={select}
      choose={choose}
      action={(a) => [a.currency, ...a.omens].join(' + ')}
      presentItem={() => s.cards['0']!.en}
      frozenCards={s.cards}
    />,
  )
  const bundles = () => document.querySelectorAll('[data-bundle-id]')
  expect(bundles()).toHaveLength(9)
  const terminals = document.querySelectorAll('[data-terminal="true"]')
  expect(terminals).toHaveLength(4)
  terminals.forEach((t) => expect(t.querySelector('.item-card')).not.toBeNull())
  const details = document.querySelector(
    '.graph-candidates',
  ) as HTMLDetailsElement
  expect(details.open).toBe(false)
  fireEvent.click(details.querySelector('summary')!)
  expect(details.querySelectorAll('details')).toHaveLength(0)
  expect(
    details.querySelectorAll('[data-candidate-id] .graph-candidate-options'),
  ).toHaveLength(7)
  expect(
    document.querySelectorAll('.graph-method-probability').length,
  ).toBeGreaterThan(0)
  expect(
    document.querySelector('.graph-method-probability')?.textContent,
  ).toContain('Outcome probability')
  fireEvent.click(
    within(details).getAllByRole('button', {
      name: 'Select this item state',
    })[0]!,
  )
  expect(select).toHaveBeenCalled()
  expect(bundles()).toHaveLength(9)
  fireEvent.click(screen.getByRole('button', { name: 'Zoom in' }))
  expect(screen.getByText('110%')).toBeInTheDocument()
  fireEvent.click(screen.getByRole('button', { name: 'Reset zoom' }))
  expect(screen.getByText('100%')).toBeInTheDocument()
  fireEvent.click(screen.getByRole('button', { name: 'Fit graph' }))
  expect(bundles()).toHaveLength(9)
  expect(document.querySelector('pattern circle')).not.toBeNull()
  mounted.unmount()
})

it('also groups fixed-valued random tiers without roll bands', () => {
  const s = original()
  s.result.methods = s.result.methods.filter(
    (m) =>
      !m.initial.concat(m.cycle).some((a) => a.currency.includes('ESSENCE')),
  )
  s.result.nodes.forEach((n) => (n.rolls = {}))
  const p = stateProjection(s.result, 100)
  expect(
    p.bundles
      .filter((b) => b.depth === 1)
      .map((b) => b.nodes.length)
      .sort((a, b) => a - b),
  ).toEqual([6, 8])
  expect(p.bundles.filter((b) => b.depth === 2)).toHaveLength(1)
})

it('keeps actual terminal items even when a method marks them DEAD, without making a null LOSS item', () => {
  const s = original()
  const method = s.result.methods.find((m) => m.source === 2)!
  method.points[0]!.outcomes[0]!.kind = 'DEAD'
  const p = stateProjection(s.result, 100)
  expect(
    p.edges
      .flatMap((e) => e.variants)
      .some(
        (v) =>
          v.method.id === method.id &&
          v.outcomes.some((o) => o.kind === 'DEAD' && o.target !== null),
      ),
  ).toBe(true)
  expect(p.bundles.flatMap((b) => b.nodes)).toHaveLength(63)
})

it('preserves a guaranteed Essence modifier without freezing the random tiers in a combined method', () => {
  const s = original()
  s.result.methods = s.result.methods.filter(
    (m) =>
      !m.initial.concat(m.cycle).some((a) => a.currency.includes('ESSENCE')),
  )
  for (const m of s.result.methods.filter((m) => m.source === 0))
    m.initial = [
      { currency: 'GREATER_ESSENCE_BODY', omens: [], purpose: 'ORDINARY' },
    ]
  const p = stateProjection(s.result, 100)
  expect(
    p.bundles
      .filter((b) => b.depth === 1)
      .map((b) => b.nodes.length)
      .sort((a, b) => a - b),
  ).toEqual([6, 8])
  expect(p.bundles.filter((b) => b.depth === 2)).toHaveLength(1)
})

it.each(['REGAL', 'EXALTED'])(
  'defaults %s graph edges to the better whole method while keeping all alternatives',
  (currency) => {
    const s = original()
    const base = s.result.methods.find((m) => m.source === 0)!
    const target = base.points[0]!.outcomes.find(
      (o) => o.target !== null,
    )!.target
    const point = (numerator: string) => ({
      cycles: 1,
      complete: true,
      initialUses: {},
      cycleUses: {},
      outcomes: [
        {
          target,
          kind: 'GOAL',
          probability: {
            lower: { numerator, denominator: '100' },
            upper: { numerator, denominator: '100' },
            arithmeticWidth: { numerator: '0', denominator: '1' },
            exact: true,
            confirmedPercent: null,
          },
        },
      ],
    })
    const plain = {
      ...base,
      id: 'plain',
      initial: [],
      cycle: [{ currency, omens: [], purpose: 'GOAL_ACQUISITION' }],
      points: [point('10')],
    }
    const omen = {
      ...plain,
      id: 'omen',
      cycle: [
        {
          currency,
          omens: ['SINISTRAL_CORONATION'],
          purpose: 'GOAL_ACQUISITION',
        },
      ],
      points: [point('20')],
    }
    s.result.methods = [plain, omen]
    s.result.recommendations = []
    const p = stateProjection(s.result, 100)
    expect(p.edges).toHaveLength(1)
    expect(p.edges[0]!.variants.map((v) => v.method.id)).toEqual([
      'omen',
      'plain',
    ])
    expect(p.edges[0]!.preferred).toEqual(['omen'])
  },
)
