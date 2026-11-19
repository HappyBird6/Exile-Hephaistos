import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, within } from '@testing-library/react'
import { expect, it, vi } from 'vitest'
import {
  initialFixture,
  jsonResponse,
  rootBucket,
} from '../../../shared/test/craftingFixtures'
import type { Catalog, GoalFilter } from '../goal-filter/types'
import { GoalPreviewResults } from './GoalPreviewResults'

const { snapshotId, baseItemId, itemLevel } = rootBucket
const context = { snapshotId, baseItemId, itemLevel }
const catalog: Catalog = {
  version: 1,
  catalogVersion: 'fixture',
  context,
  groupTypes: [],
  stats: [],
  issues: [],
}
const goal: GoalFilter = {
  version: 1,
  catalogVersion: 'fixture',
  general: {
    baseItemId,
    itemLevel: { min: null, max: null },
    rarities: ['RARE'],
  },
  groups: [
    {
      id: 'goal',
      type: 'AND',
      disabled: false,
      range: null,
      entries: [
        {
          id: 'life',
          statId: 'Life',
          unit: 'flat',
          range: { min: 15, max: null },
          disabled: false,
          weight: null,
        },
      ],
    },
  ],
}
const item = {
  ...rootBucket,
  rarity: 'RARE',
  explicits: [{ modifierId: 'p', values: { life: 15 } }],
}
const sources = {
  implicit: [{ kind: 'IMPLICIT' }],
  p: [{ kind: 'NORMAL' }, { kind: 'ESSENCE', id: 'GREATER_ESSENCE_BODY' }],
  p2: [{ kind: 'NORMAL' }],
}
const otherTier = {
  ...item,
  explicits: [{ modifierId: 'p2', values: { life: 25 } }],
}
const preview = {
  modifiers: {
    ...initialFixture.modifiers,
    p2: {
      ...initialFixture.modifiers.p!,
      tier: 2,
      id: 'p2',
      name: 'Stalwart',
      text: '+(21—30) to maximum Life',
      stats: [{ id: 'life', min: 21, max: 30 }],
    },
  },
  cards: [{ item, tierVariants: [item, otherTier], sources }],
}

function renderPreview(response: unknown, filter = goal) {
  const fetch = vi.fn(() => Promise.resolve(jsonResponse(response)))
  vi.stubGlobal('fetch', fetch)
  render(
    <QueryClientProvider client={new QueryClient()}>
      <GoalPreviewResults
        base={['solar', 'Amulets', 'Solar Amulet']}
        initial={initialFixture}
        context={context}
        catalog={catalog}
        goal={filter}
      />
    </QueryClientProvider>,
  )
  return fetch
}

it('shows matching tiers on one card with the roll and acquisition sources for the selected tier', async () => {
  const fetch = renderPreview(preview)
  const cards = await screen.findAllByRole('article')
  expect(cards).toHaveLength(1)
  expect(screen.getByText(/Life · ≥ 15/)).toBeVisible()
  const card = within(cards[0]!)
  expect(card.getByText('+15 to maximum Life')).toBeVisible()
  expect(
    card.getByText('Sources · base, Essence (Greater Essence of the Body)'),
  ).toBeVisible()
  const tiers = card.getByRole('combobox', {
    name: /Matching tier combinations/,
  })
  expect(within(tiers).getAllByRole('option')).toHaveLength(2)
  expect(
    within(tiers).getByRole('option', {
      name: 'T2 · Stalwart · +25 to maximum Life',
    }),
  ).toBeInTheDocument()
  fireEvent.change(tiers, { target: { value: '1' } })
  expect(card.getByText('+25 to maximum Life')).toBeVisible()
  expect(card.getByText('Sources · base')).toBeVisible()
  expect(card.queryByText(/Greater Essence/)).toBeNull()
  expect(card.getByText('Sources · Implicit')).toBeVisible()
  expect(fetch.mock.calls).toHaveLength(1)
})

it('renders every returned option configuration, including the fifth one', async () => {
  const modifiers = Array.from({ length: 5 }, (_, index) => ({
    ...initialFixture.modifiers.p!,
    id: 'prefix-' + index,
    familyIds: ['family-' + index],
  }))
  renderPreview({
    modifiers: {
      ...preview.modifiers,
      ...Object.fromEntries(modifiers.map((m) => [m.id, m])),
    },
    cards: modifiers.map((m) => {
      const state = {
        ...item,
        explicits: [{ ...item.explicits[0]!, modifierId: m.id }],
      }
      return {
        item: state,
        tierVariants: [state],
        sources: { ...sources, [m.id]: [{ kind: 'NORMAL' }] },
      }
    }),
  })
  expect(await screen.findAllByRole('article')).toHaveLength(5)
  expect(screen.queryByRole('combobox')).toBeNull()
})

it('distinguishes an empty search from a failed response', async () => {
  renderPreview({ cards: [], modifiers: {} })
  expect(
    await screen.findByText(
      'No matching example was found for these conditions.',
    ),
  ).toBeVisible()
  expect(screen.queryByRole('alert')).toBeNull()
})

it('summarizes Count and weighted filters with the same names and defaults as the editor', async () => {
  const row = goal.groups[0]!.entries[0]!
  renderPreview(
    { ...preview, resolvedCountRanges: { count: { min: 1, max: null } } },
    {
      ...goal,
      groups: [
        { ...goal.groups[0]!, entries: [] },
        {
          id: 'count',
          type: 'COUNT',
          disabled: false,
          range: { min: -1, max: null },
          entries: [row],
        },
        {
          id: 'weighted',
          type: 'WEIGHTED_V2',
          disabled: false,
          range: { min: null, max: null },
          entries: [{ ...row, id: 'weighted-life' }],
        },
      ],
    },
  )
  expect(await screen.findByText('Count')).toBeVisible()
  expect(await screen.findByText(/Row count ≥ 1/)).toBeVisible()
  expect(screen.getByText('Weighted Sum v2')).toBeVisible()
  expect(screen.getByText(/Weight 1/)).toBeVisible()
  expect(screen.queryByText('Or')).toBeNull()
  expect(screen.queryByText('And')).toBeNull()
})

it('rejects a variant for a different base before showing the card', async () => {
  renderPreview({
    ...preview,
    cards: [
      {
        item,
        tierVariants: [item, { ...otherTier, baseItemId: 'other' }],
        sources,
      },
    ],
  })
  expect(await screen.findByRole('alert')).toBeVisible()
  expect(screen.queryByRole('article')).toBeNull()
})

it('explains when catalyst quality prevents a numeric goal check', async () => {
  renderPreview({
    cards: [],
    modifiers: {},
    reasonCode: 'QUALITY_PROJECTION_NOT_IMPLEMENTED',
  })
  expect(
    await screen.findByText(/values affected by catalyst quality/),
  ).toBeVisible()
  expect(screen.queryByRole('article')).toBeNull()
})
