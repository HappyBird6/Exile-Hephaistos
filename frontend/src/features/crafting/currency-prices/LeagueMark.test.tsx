import { fireEvent, render, screen } from '@testing-library/react'
import { expect, it } from 'vitest'
import { CurrencyPriceContext } from './context'
import { LeagueMark } from './LeagueMark'
import type { PriceView } from './api'
const data: PriceView = {
  snapshot: {
    league: 'Forbidden Rites',
    updatedAt: '2026-11-09T06:00:00Z',
    exaltedPerDivine: 500,
    prices: {
      'Exalted Orb': { name: 'Exalted Orb', divine: 0.002, exalted: 1 },
    },
  },
  nextRefreshAllowedAt: '2026-11-09T06:05:00Z',
  unavailable: false,
  stale: false,
}
it('uses official artwork matching the current league and links its source announcement', () => {
  const page = render(
    <CurrencyPriceContext.Provider
      value={{
        data,
        pending: false,
        refreshing: false,
        error: false,
        refresh: () => {},
      }}
    >
      <LeagueMark />
    </CurrencyPriceContext.Provider>,
  )
  expect(screen.getByRole('img', { name: 'Forbidden Rites' })).toBeVisible()
  expect(screen.getByRole('link')).toHaveAttribute(
    'href',
    'https://store.steampowered.com/news/app/2694490/view/1842212951308718',
  )
  fireEvent.error(page.container.querySelector('image')!)
  expect(screen.queryByRole('link')).toBeNull()
})
it('does not label another league with the Forbidden Rites artwork', () => {
  render(
    <CurrencyPriceContext.Provider
      value={{
        data: {
          ...data,
          snapshot: { ...data.snapshot!, league: 'Next League' },
        },
        pending: false,
        refreshing: false,
        error: false,
        refresh: () => {},
      }}
    >
      <LeagueMark />
    </CurrencyPriceContext.Provider>,
  )
  expect(screen.queryByRole('img')).toBeNull()
})
