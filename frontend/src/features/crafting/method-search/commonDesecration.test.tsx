import { expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { GroupedMethodGraph } from './GroupedMethodGraph'
import { requestFixture } from '../path-tree/fixtures.test-support'
import { createItemPresentation } from '../path-tree/presentation'
import { setLocale } from '../../../shared/i18n/i18n'
import type { AutomaticView, AutomaticArrow } from './automaticApi'
import { tierCandidates } from './graphDisplay'

it('connects desecration and currency through the same graph states without an Abyss panel', () => {
  setLocale('en')
  const item = requestFixture().start.item
  const node = {
    id: 0,
    depth: 0,
    quality40: false,
    item,
    progress: { goal: 'NO_MATCH', entries: ['NO_MATCH'] },
    discoveryFinished: true,
    directFinished: true,
  }
  const one = {
    lower: { numerator: '1', denominator: '1' },
    upper: { numerator: '1', denominator: '1' },
    arithmeticWidth: { numerator: '0', denominator: '1' },
    exact: true,
    confirmedPercent: '100',
  }
  const arrow = (
    id: string,
    source: number,
    target: number,
    desecrate = false,
  ): AutomaticArrow => ({
    id,
    source,
    initial: [],
    cycle: [
      desecrate
        ? {
            currency: null,
            operation: 'DESECRATE',
            side: 'PREFIX',
            omens: [],
            purpose: 'ORDINARY',
          }
        : { currency: 'EXALTED', omens: [], purpose: 'GOAL_ACQUISITION' },
    ],
    reasons: [],
    finished: true,
    executionMode: 'SINGLE_PASS',
    points: [
      {
        cycles: 1,
        complete: true,
        outcomes: [{ target, kind: 'CHECKPOINT', probability: one }],
        initialUses: {},
        cycleUses: {},
      },
    ],
  })
  const view: AutomaticView = {
    version: 'automatic-method-job-v1',
    id: '11111111-1111-4111-8111-111111111111',
    revision: 1,
    generation: 1,
    status: 'COMPLETE',
    reason: '',
    complete: true,
    operations: 3,
    retainedStates: 4,
    recommendations: [],
    nodes: [
      node,
      {
        ...node,
        id: 1,
        depth: 1,
        item: {
          ...item,
          desecrated: [{ id: 'd', side: 'PREFIX', modifierId: null }],
        },
      },
      { ...node, id: 2, depth: 1 },
      {
        ...node,
        id: 3,
        depth: 2,
        item: {
          ...item,
          desecrated: [{ id: 'd', side: 'PREFIX', modifierId: null }],
        },
      },
    ],
    methods: [
      arrow('desecrate', 0, 1, true),
      arrow('exalt', 0, 2),
      arrow('after', 1, 3),
    ],
  }
  render(
    <GroupedMethodGraph
      view={view}
      cycles={100}
      selected={0}
      route={[]}
      choose={() => {}}
      select={() => {}}
      action={(a) =>
        a.operation === 'DESECRATE' ? 'Desecrate' : 'Exalted Orb'
      }
      presentItem={createItemPresentation(
        { name: 'Solar Amulet', itemClass: 'Amulet' },
        {},
      )}
    />,
  )
  expect(screen.getAllByRole('button', { name: /Desecrate/ })).not.toHaveLength(
    0,
  )
  expect(
    screen.getAllByRole('button', { name: /Exalted Orb/ }),
  ).not.toHaveLength(0)
  expect(document.querySelector('[data-state-ids="1"]')).not.toBeNull()
  expect(document.querySelector('[data-state-ids="3"]')).not.toBeNull()
  expect(screen.getAllByText('Unrevealed desecrated modifier')).toHaveLength(2)
  expect(document.querySelector('[data-automatic-abyss-node]')).toBeNull()
  expect(screen.queryByText('Abyss routes')).not.toBeInTheDocument()
  expect(tierCandidates([view.nodes[0]!, view.nodes[1]!])).toHaveLength(2)
})
