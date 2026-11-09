import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { setLocale, locales } from '../../../shared/i18n/i18n'
import { CurrencyPriceContext } from './context'
import {
  CurrencyTicker,
  CurrencyPriceFooter,
  CraftingCostLine,
} from './CurrencyPrices'
import { useCurrencyPrices } from './useCurrencyPrices'
import type { PriceView } from './api'
import { priceMessages } from './messages'
import { MaterialDescription } from '../MaterialTooltip'

const view: PriceView = {
  snapshot: {
    league: 'Current League',
    updatedAt: '2026-11-09T06:00:00Z',
    exaltedPerDivine: 500,
    prices: {
      'Exalted Orb': { name: 'Exalted Orb', divine: 0.002, exalted: 1 },
    },
  },
  nextRefreshAllowedAt: '2099-11-09T06:05:00Z',
  unavailable: false,
  stale: false,
}
afterEach(() => {
  setLocale('en')
  vi.unstubAllGlobals()
})
it.each(locales)(
  'shows currency images and local collection time, keeping league in the tooltip with accessible translated controls in %s',
  (locale) => {
    setLocale(locale)
    const refresh = vi.fn()
    const page = render(
      <CurrencyPriceContext.Provider
        value={{
          data: view,
          pending: false,
          refreshing: false,
          error: false,
          refresh,
        }}
      >
        <CurrencyTicker />
        <CurrencyPriceFooter materialId="Exalted_Orb" />
      </CurrencyPriceContext.Provider>,
    )
    const ticker = screen.getByRole('region', {
      name: priceMessages[locale].market,
    })
    expect(ticker.querySelectorAll('img')).toHaveLength(2)
    expect(ticker.querySelector('time')).toHaveAttribute(
      'datetime',
      view.snapshot!.updatedAt,
    )
    expect(within(ticker).getByText('500')).toBeVisible()
    expect(within(ticker).queryByText('Current League')).toBeNull()
    expect(within(ticker).queryByText('1', { exact: true })).toBeNull()
    expect(ticker).toHaveAttribute(
      'title',
      expect.stringContaining('Current League'),
    )
    const button = screen.getByRole('button', {
      name: priceMessages[locale].refresh,
    })
    expect(button).toBeEnabled()
    fireEvent.click(button)
    expect(refresh).toHaveBeenCalledTimes(1)
    expect(
      page.container.querySelectorAll('.currency-price-footer img'),
    ).toHaveLength(2)
  },
)
it('keeps the price at the bottom after description and priority hints', () => {
  const page = render(
    <CurrencyPriceContext.Provider
      value={{
        data: view,
        pending: false,
        refreshing: false,
        error: false,
        refresh: () => {},
      }}
    >
      <MaterialDescription materialId="Exalted_Orb">
        <p>Priority hint</p>
      </MaterialDescription>
    </CurrencyPriceContext.Provider>,
  )
  const description = page.container.querySelector('.material-description')!
  expect(description.lastElementChild).toHaveClass('currency-price-footer')
  expect(
    description.lastElementChild?.previousElementSibling,
  ).toHaveTextContent('Priority hint')
})
it('manual refresh replaces the shared query result, including a cache-hit response inside the cooldown', async () => {
  const latest = {
    ...view,
    snapshot: { ...view.snapshot!, exaltedPerDivine: 600 },
  }
  const fetch = vi.fn(
    async (_url: string, init: RequestInit) =>
      new Response(JSON.stringify(init.method === 'POST' ? latest : view)),
  )
  vi.stubGlobal('fetch', fetch)
  function Harness() {
    const market = useCurrencyPrices()
    return (
      <CurrencyPriceContext.Provider value={market}>
        <CurrencyTicker />
        <CraftingCostLine film={undefined} cursor={0} />
      </CurrencyPriceContext.Provider>
    )
  }
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  const page = render(
    <QueryClientProvider client={client}>
      <Harness />
    </QueryClientProvider>,
  )
  await screen.findByText('500')
  fireEvent.click(screen.getByRole('button', { name: 'Refresh prices' }))
  await screen.findByText('600')
  expect(
    fetch.mock.calls.filter(([, init]) => init.method === 'POST'),
  ).toHaveLength(1)
  expect(client.getQueryData(['economy', 'prices'])).toEqual(latest)
  page.unmount()
  client.clear()
})
it('keeps unavailable prices distinct from zero and disables refresh while pending', async () => {
  const page = render(
    <CurrencyPriceContext.Provider
      value={{
        data: { ...view, snapshot: null, unavailable: true },
        pending: false,
        refreshing: true,
        error: true,
        refresh: () => {},
      }}
    >
      <CurrencyTicker />
      <CurrencyPriceFooter materialId="Exalted_Orb" />
    </CurrencyPriceContext.Provider>,
  )
  expect(screen.getByText('Prices unavailable')).toBeVisible()
  expect(screen.getByText('No price')).toBeVisible()
  expect(screen.getByRole('button', { name: 'Refresh prices' })).toBeDisabled()
  expect(page.container.textContent).not.toContain('500')
  await waitFor(() => expect(screen.getByText('—')).toBeVisible())
})
