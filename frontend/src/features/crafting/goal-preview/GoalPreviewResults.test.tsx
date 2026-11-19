import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, within } from '@testing-library/react'
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
}
const preview = {
  modifiers: initialFixture.modifiers,
  cards: [
    { item, sources },
    {
      item: { ...item, explicits: [{ modifierId: 'p', values: { life: 20 } }] },
      sources: { ...sources, p: [{ kind: 'NORMAL' }] },
    },
  ],
}

function renderPreview(response: unknown) {
  const fetch = vi.fn(() => Promise.resolve(jsonResponse(response)))
  vi.stubGlobal('fetch', fetch)
  render(
    <QueryClientProvider client={new QueryClient()}>
      <GoalPreviewResults
        base={['solar', 'Amulets', 'Solar Amulet']}
        initial={initialFixture}
        context={context}
        catalog={catalog}
        goal={goal}
      />
    </QueryClientProvider>,
  )
  return fetch
}

it('shows the numeric goal and binds each rolled modifier to its own acquisition sources', async () => {
  const fetch = renderPreview(preview)
  const cards = await screen.findAllByRole('article')
  expect(cards).toHaveLength(2)
  expect(screen.getByText(/Life · ≥ 15/)).toBeVisible()
  expect(within(cards[0]!).getByText(/15 to maximum Life/)).toBeVisible()
  expect(
    within(cards[0]!).getByText(
      /Normal modifier pool \/ Greater Essence of the Body/,
    ),
  ).toBeVisible()
  expect(within(cards[1]!).getByText(/20 to maximum Life/)).toBeVisible()
  expect(within(cards[1]!).queryByText(/Greater Essence/)).toBeNull()
  expect(within(cards[0]!).getByText(/Base implicit/)).toBeVisible()
  expect(fetch.mock.calls).toHaveLength(1)
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

it('rejects a card for a different base before showing its modifiers', async () => {
  renderPreview({
    ...preview,
    cards: [{ item: { ...item, baseItemId: 'other' }, sources }],
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
