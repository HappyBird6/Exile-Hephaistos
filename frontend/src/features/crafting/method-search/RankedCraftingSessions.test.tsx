import { useState } from 'react'
import { fireEvent, render, screen, within } from '@testing-library/react'
import { expect, it, vi } from 'vitest'
import { AppProviders } from '../../../app/AppProviders'
import { setLocale } from '../../../shared/i18n/i18n'
import { requestFixture } from '../path-tree/fixtures.test-support'
import { createItemPresentation } from '../path-tree/presentation'
import { RankedCraftingSessions } from './RankedCraftingSessions'
import { OpenCraftingSession } from './openCraftingSession'
import type {
  AutomaticView,
  AutomaticArrow,
  AutomaticNode,
} from './automaticApi'
import type { PolicyCandidate } from './policyCandidates'
import type { Selection } from './supportStorage'
import type { Bounds } from './types'

it('ranks equal-looking percentages by exact mass and opens the completed film with the selected currency history', () => {
  setLocale('en')
  const start = requestFixture().start.item
  const bound = (numerator: string): Bounds => {
    const value = { numerator, denominator: '100000000000000000000' }
    return {
      lower: value,
      upper: value,
      exact: true,
      arithmeticWidth: { numerator: '0', denominator: '1' },
      confirmedPercent: null,
    }
  }
  const arrow = (
    id: string,
    source: number,
    currency: string,
    kind: string,
    target: number,
  ): AutomaticArrow => ({
    id,
    source,
    executionMode: currency === 'CHAOS' ? 'REPEATABLE' : 'SINGLE_PASS',
    initial: [],
    cycle: [{ currency, omens: [], purpose: 'GOAL_ACQUISITION' }],
    finished: true,
    reasons: [],
    points: [
      {
        cycles: currency === 'CHAOS' ? 100 : 1,
        complete: true,
        initialUses: {},
        cycleUses: {},
        outcomes: [
          { kind, target, probability: bound('100000000000000000000') },
        ],
      },
    ],
  })
  const methods = [
    arrow('m1', 0, 'REGAL', 'CHECKPOINT', 1),
    arrow('m2', 0, 'EXALTED', 'GOAL', 2),
    arrow('m3', 1, 'CHAOS', 'GOAL', 2),
  ]
  const policy = (
    id: string,
    success: string,
    method: string,
  ): PolicyCandidate => ({
    id,
    success: bound(success),
    cycles: 100,
    decisions: [
      { node: 0, method },
      { node: 1, method: 'm3' },
    ],
    distinguishingDecisions: [],
    rebase: bound('0'),
    failure: bound('0'),
    unresolved: bound('0'),
    expectedUses: {},
    rankStatus: 'SEPARATED_IN_CURRENT_SET',
    usageStatus: 'BOUNDED',
  })
  const node = (id: number): AutomaticNode => ({
    id,
    depth: id,
    item: { ...start, itemLevel: start.itemLevel + id },
    quality40: false,
    progress: { goal: 'NO_MATCH', entries: [] },
    discoveryFinished: true,
    directFinished: true,
  })
  const view: AutomaticView = {
    version: 'automatic-method-job-v1',
    id: 'job',
    revision: 1,
    generation: 1,
    operations: 1,
    retainedStates: 3,
    status: 'COMPLETE',
    reason: '',
    complete: true,
    nodes: [node(0), node(1), node(2)],
    methods,
    recommendations: [],
    candidates: [
      policy('a', '50000000000000000000', 'm2'),
      policy('z', '50000000000000000001', 'm1'),
      policy('duplicate-route', '40000000000000000000', 'm1'),
    ],
  }
  const open = vi.fn().mockResolvedValue(true),
    present = createItemPresentation(
      { name: 'Solar Amulet', itemClass: 'Amulet' },
      {},
    )
  function Screen() {
    const [route, setRoute] = useState<Selection[]>([])
    return (
      <OpenCraftingSession.Provider value={open}>
        <RankedCraftingSessions
          start={start}
          view={view}
          allowStart
          identity="rules"
          route={route}
          onRoute={setRoute}
          cycles={100}
          card={(n) => present(n.item, 'en')}
          action={(a) => a.currency!}
        />
      </OpenCraftingSession.Provider>
    )
  }
  render(<Screen />, { wrapper: AppProviders })
  const buttons = within(screen.getByRole('list', { name: 'Crafting methods' }))
    .getAllByRole('button')
    .filter((b) => b.hasAttribute('aria-pressed'))
  expect(buttons).toHaveLength(2)
  expect(buttons[0]).toHaveAccessibleName(/1\. .*ex · 50.00%/)
  expect(buttons[1]).toHaveAccessibleName(/2\. .*ex · 50.00%/)
  expect(screen.getAllByRole('combobox')).toHaveLength(2)
  fireEvent.click(buttons[0]!)
  expect(screen.getAllByRole('combobox')).toHaveLength(2)
  fireEvent.click(screen.getByRole('button', { name: 'Start in workbench' }))
  expect(open).toHaveBeenCalledWith(
    expect.objectContaining({
      rulesetIdentity: 'rules',
      frames: [
        expect.objectContaining({ state: start }),
        expect.objectContaining({
          state: node(1).item,
          projectedCurrencies: [{ currencyId: 'Regal_Orb', omenIds: [] }],
        }),
        expect.objectContaining({
          state: node(2).item,
          projectedCurrencies: [
            { currencyId: 'Chaos_Orb', omenIds: [], maxUses: 100 },
          ],
        }),
      ],
    }),
  )
  fireEvent.click(buttons[1]!)
  expect(screen.getAllByRole('combobox')).toHaveLength(1)
  expect(buttons[1]).toHaveAttribute('aria-pressed', 'true')
})
