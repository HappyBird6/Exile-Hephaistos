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
  p4: [{ kind: 'NORMAL' }],
}
const optionTiers = { implicit: ['implicit'], p: ['p2', 'p', 'p4'] }
const otherTier = {
  ...item,
  explicits: [{ modifierId: 'p2', values: { life: 25 } }],
}
const tierFour = {
  ...item,
  explicits: [{ modifierId: 'p4', values: { life: 18 } }],
}
const preview = {
  modifiers: {
    ...initialFixture.modifiers,
    p4: { ...initialFixture.modifiers.p!, tier: 4, id: 'p4' },
    p2: {
      ...initialFixture.modifiers.p!,
      tier: 2,
      id: 'p2',
      name: 'Stalwart',
      text: '+(21—30) to maximum Life',
      stats: [{ id: 'life', min: 21, max: 30 }],
    },
  },
  cards: [
    { item, tierVariants: [item, otherTier, tierFour], optionTiers, sources },
  ],
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

it('shows disjoint matching tiers beside the option and qualifies the sources that apply to specific tiers', async () => {
  const fetch = renderPreview(preview)
  const cards = await screen.findAllByRole('article')
  expect(cards).toHaveLength(1)
  expect(screen.getByText(/Life · ≥ 15/)).toBeVisible()
  const card = within(cards[0]!)
  expect(card.getByText('T4, T2~T1')).toBeVisible()
  const summary = card.getByText('+15 to maximum Life', { selector: 'summary' })
  expect(summary).toBeVisible()
  expect(
    card.getByText(
      'Sources · base, Essence (Greater Essence of the Body) [T1]',
    ),
  ).toBeVisible()
  expect(card.queryByRole('combobox')).toBeNull()
  expect(card.queryByText('T0')).toBeNull()
  fireEvent.click(summary)
  expect(card.getByRole('row', { name: /T2 · Stalwart/ })).toHaveTextContent(
    '+25 to maximum Life',
  )
  expect(card.getByText('Sources · Implicit')).toBeVisible()
  expect(fetch.mock.calls).toHaveLength(1)
})

it('names the affix when only one of two definitions at the same tier has a source', async () => {
  renderPreview({
    ...preview,
    modifiers: {
      ...preview.modifiers,
      p2: { ...preview.modifiers.p2, tier: 1 },
    },
  })
  const card = within(await screen.findByRole('article'))
  expect(card.getByText('T4, T1')).toBeVisible()
  expect(
    card.getByText(
      'Sources · base, Essence (Greater Essence of the Body) [T1 Healthy]',
    ),
  ).toBeVisible()
})

it('keeps the tier ranges attached to their options when the order of affixes changes between matching items', async () => {
  const state = {
    ...item,
    explicits: [
      { modifierId: 's', values: { strength: 8 } },
      ...item.explicits,
    ],
  }
  const variant = {
    ...otherTier,
    explicits: [
      ...otherTier.explicits,
      { modifierId: 's3', values: { strength: 5 } },
    ],
  }
  renderPreview({
    modifiers: {
      ...preview.modifiers,
      s3: { ...initialFixture.modifiers.s!, id: 's3', tier: 3 },
    },
    cards: [
      {
        item: state,
        tierVariants: [state, variant],
        optionTiers: { implicit: ['implicit'], p: ['p', 'p2'], s: ['s3', 's'] },
        sources: {
          ...sources,
          s: [{ kind: 'NORMAL' }],
          s3: [{ kind: 'NORMAL' }],
        },
      },
    ],
  })
  const card = within(await screen.findByRole('article'))
  const lifeLine = card.getByText('T2~T1').parentElement!
  const strengthLine = card.getByText('T3, T1').parentElement!
  expect(
    within(lifeLine).getByText('+15 to maximum Life', { selector: 'summary' }),
  ).toBeVisible()
  expect(
    within(strengthLine).getByText('+8 to Strength', { selector: 'summary' }),
  ).toBeVisible()
  expect(within(strengthLine).queryByText(/Essence/)).toBeNull()
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
        optionTiers: { implicit: ['implicit'], [m.id]: [m.id] },
        sources: { ...sources, [m.id]: [{ kind: 'NORMAL' }] },
      }
    }),
  })
  const cards = await screen.findAllByRole('article')
  expect(cards).toHaveLength(5)
  expect(within(cards[0]!).getByText('T1')).toBeVisible()
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
        optionTiers,
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
