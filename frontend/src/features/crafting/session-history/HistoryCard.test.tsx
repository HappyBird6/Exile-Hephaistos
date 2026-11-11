import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { expect, it } from 'vitest'
import { initialFixture } from '../../../shared/test/craftingFixtures'
import { setLocale } from '../../../shared/i18n/i18n'
import { concreteInitial } from '../workbenchApi'
import { HistoryCard } from './HistoryCard'
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
