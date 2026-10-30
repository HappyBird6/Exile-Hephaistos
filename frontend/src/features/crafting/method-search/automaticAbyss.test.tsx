import { expect, it } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { requestFixture } from '../path-tree/fixtures.test-support'
import { createItemPresentation } from '../path-tree/presentation'
import { setLocale } from '../../../shared/i18n/i18n'
import {
  validateAutomaticAbyss,
  type AutomaticAbyssRoute,
} from './automaticAbyss'
import { GroupedMethodGraph } from './GroupedMethodGraph'
import type { AutomaticView } from './automaticApi'

const route = (): AutomaticAbyssRoute => {
  const prepared = {
    item: {
      item: {
        ...requestFixture().start.item,
        rarity: 'RARE' as const,
        catalystQuality: null,
        explicits: [],
      },
      slots: [
        {
          id: 'lock',
          side: 'PREFIX' as const,
          desecrated: false,
          modifier: {
            modifierId: 'life',
            values: { life: 19 },
            fractured: true,
          },
        },
        {
          id: 'abyss',
          side: 'PREFIX' as const,
          desecrated: true,
          modifier: null,
        },
      ],
      offer: null,
    },
    ranges: {},
    condition: 'UNREVEALED_SLOT',
  }
  const result = {
    ...prepared,
    item: {
      ...prepared.item,
      slots: [
        ...prepared.item.slots,
        {
          id: 'add',
          side: 'SUFFIX' as const,
          desecrated: false,
          modifier: { modifierId: 'str', values: { str: 5 }, fractured: false },
        },
      ],
    },
    ranges: { str: [{ stat: 'str', min: 5, max: 8 }] },
  }
  return {
    strategy: { kind: 'SHARE_REMOVAL_RISK', modifierSelection: null },
    prepared,
    assumed: null,
    status: 'CONDITIONAL_FRONTIER',
    transitions: [
      {
        source: prepared,
        currency: 'EXALTED',
        omens: [],
        removals: [
          {
            slotId: null,
            probability: { numerator: 1, denominator: 1 },
            remaining: prepared,
            candidates: [
              {
                modifierId: 'str',
                selection: { numerator: 1, denominator: 1 },
                result,
                next: [],
              },
            ],
          },
        ],
      },
    ],
  }
}
it('renders server-generated desecration in the graph without a user preparation action', () => {
  setLocale('en')
  const r = route()
  const view: AutomaticView = {
    version: 'automatic-method-job-v1',
    id: 'test',
    revision: 1,
    generation: 1,
    status: 'COMPLETE',
    reason: '',
    complete: true,
    operations: 1,
    retainedStates: 1,
    methods: [],
    recommendations: [],
    nodes: [
      {
        id: 0,
        depth: 0,
        item: {
          ...requestFixture().start.item,
          rarity: 'RARE',
          explicits: [
            { ...r.prepared.item.slots[0]!.modifier!, fractured: true },
          ],
        },
        quality40: false,
        progress: { goal: 'NO_MATCH', entries: ['NO_MATCH'] },
        discoveryFinished: true,
        directFinished: true,
        abyss: [r],
      },
    ],
  }
  const present = createItemPresentation(
    { name: 'Test', itemClass: 'Amulet' },
    {
      life: {
        id: 'life',
        name: 'Life',
        text: '+# Life',
        tier: 1,
        affixType: 'PREFIX',
        familyIds: ['life'],
        stats: [{ id: 'life', min: 1, max: 19 }],
      },
      str: {
        id: 'str',
        name: 'Strength',
        text: '+# Strength',
        tier: 1,
        affixType: 'SUFFIX',
        familyIds: ['str'],
        stats: [{ id: 'str', min: 5, max: 8 }],
      },
    },
  )
  const { container } = render(
    <GroupedMethodGraph
      view={view}
      cycles={1}
      selected={0}
      route={[]}
      choose={() => {}}
      select={() => {}}
      action={(a) => a.currency ?? a.operation ?? ''}
      presentItem={present}
    />,
  )
  expect(container.querySelector('[data-automatic-abyss-node]')).not.toBeNull()
  expect(
    screen.getByText(/Automatically planned desecration/),
  ).toBeInTheDocument()
  expect(container.querySelector('[data-flow-step]')).toBeNull()
  fireEvent.click(screen.getByRole('button', { name: 'Result states' }))
  expect(screen.getByText(/No removal: 100.00%/)).toBeInTheDocument()
  expect(
    screen.getAllByText(/Strength: str = 5.*Strength: str = 8/),
  ).toHaveLength(2)
  expect(screen.getByText(/total success are unknown/)).toBeInTheDocument()
})
it('restores conditional states and rejects loss of a fracture or probability mass', () => {
  const r = route()
  expect(() =>
    validateAutomaticAbyss(JSON.parse(JSON.stringify([r]))),
  ).not.toThrow()
  const c = r.transitions[0]!.removals[0]!.candidates[0]!
  c.selection.numerator = 0
  expect(() => validateAutomaticAbyss([r])).toThrow()
  c.selection.numerator = 1
  c.result.item.slots = c.result.item.slots.filter((s) => s.id !== 'lock')
  expect(() => validateAutomaticAbyss([r])).toThrow()
})
