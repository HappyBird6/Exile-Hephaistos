import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, expect, it } from 'vitest'
import { initialFixture } from '../../../shared/test/craftingFixtures'
import { setLocale } from '../../../shared/i18n/i18n'
import { concreteInitial } from '../workbenchApi'
import { HistoryCard } from './HistoryCard'
afterEach(() => cleanup())
it('keeps known option sentences readable across ruleset changes without editing the saved item', async () => {
  setLocale('en')
  const state = {
    ...concreteInitial(initialFixture),
    baseItemId: 'Metadata/Items/Amulets/FourAmulet9',
    rarity: 'RARE' as const,
    explicits: [{ modifierId: 'p', values: { life: 17 } }],
  }
  const original = JSON.stringify(state)
  const client = new QueryClient({
    defaultOptions: { queries: { staleTime: Infinity, retry: false } },
  })
  client.setQueryData(
    ['crafting', 'initial', 'solar', state.itemLevel, 'manual'],
    initialFixture,
  )
  const frame = { state, action: null }
  render(
    <QueryClientProvider client={client}>
      <HistoryCard
        film={{
          id: 'old',
          createdAt: new Date().toISOString(),
          rulesetIdentity: 'old-ruleset',
          frames: [frame],
        }}
        frame={frame}
      />
    </QueryClientProvider>,
  )
  expect(await screen.findByText('+17 to maximum Life')).toBeInTheDocument()
  expect(screen.queryByText('p: life 17')).not.toBeInTheDocument()
  expect(JSON.stringify(state)).toBe(original)
})

it('uses the translated catalyst quality in a preserved session instead of the raw enum', async () => {
  await setLocale('ko')
  const state = {
    ...concreteInitial(initialFixture),
    baseItemId: 'Metadata/Items/Amulets/FourAmulet9',
    catalystQuality: { type: 'NECROTIC' as const, amount: 40 },
  }
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  client.setQueryData(
    ['crafting', 'initial', 'solar', state.itemLevel, 'manual'],
    initialFixture,
  )
  const frame = { state, action: null }
  render(
    <QueryClientProvider client={client}>
      <HistoryCard
        film={{
          id: 'quality',
          createdAt: new Date().toISOString(),
          rulesetIdentity: 'old-ruleset',
          frames: [frame],
        }}
        frame={frame}
      />
    </QueryClientProvider>,
  )
  expect(await screen.findByText('퀄리티 (괴사의 기폭제): +40%')).toBeVisible()
  expect(screen.queryByText(/NECROTIC/)).toBeNull()
  await setLocale('en')
})

it('restores the workbench affected-option mark at each saved step and clears it at step zero', async () => {
  const { twoSteps } = await import('../session-notation/fixtures.test-support')
  const { history } = await twoSteps(),
    film = history.films[0]!
  const initial = structuredClone(initialFixture)
  initial.state.baseItemId = 'Metadata/Items/Amulets/FourAmulet9'
  for (const frame of film.frames)
    frame.state.baseItemId = initial.state.baseItemId
  const saved = JSON.stringify(film)
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false, staleTime: Infinity } },
  })
  client.setQueryData(['crafting', 'initial', 'solar', 82, 'manual'], initial)
  const view = (index: number) => (
    <QueryClientProvider client={client}>
      <HistoryCard film={film} frame={film.frames[index]!} />
    </QueryClientProvider>
  )
  const rendered = render(view(1))
  expect(await screen.findByText('+17 to maximum Life')).toHaveAttribute(
    'data-last-currency-affected',
  )
  rendered.rerender(view(2))
  expect(await screen.findByText('+6 to Strength')).toHaveAttribute(
    'data-last-currency-affected',
  )
  expect(screen.getByText('+17 to maximum Life')).not.toHaveAttribute(
    'data-last-currency-affected',
  )
  rendered.rerender(view(0))
  expect(
    rendered.container.querySelector('[data-last-currency-affected]'),
  ).toBeNull()
  expect(JSON.stringify(film)).toBe(saved)
})
