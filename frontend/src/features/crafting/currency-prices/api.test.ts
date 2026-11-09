import { afterEach, expect, it, vi } from 'vitest'
import { loadCurrencyPrices } from './api'
import type { PriceView } from './api'
const view: PriceView = {
  snapshot: {
    league: 'Current League',
    updatedAt: '2026-11-09T00:00:00Z',
    exaltedPerDivine: 500,
    prices: {
      'Exalted Orb': { name: 'Exalted Orb', divine: 0.002, exalted: 1 },
    },
  },
  nextRefreshAllowedAt: '2026-11-09T00:05:00Z',
  stale: false,
  unavailable: false,
}
afterEach(() => vi.unstubAllGlobals())
it('uses cancellation for reads and a server-mediated POST for manual refresh', async () => {
  const fetch = vi
    .fn()
    .mockImplementation(() =>
      Promise.resolve(new Response(JSON.stringify(view))),
    )
  vi.stubGlobal('fetch', fetch)
  const signal = new AbortController().signal
  expect(await loadCurrencyPrices(signal)).toEqual(view)
  expect(fetch.mock.calls[0]).toEqual([
    '/api/v1/economy/prices',
    { signal, method: 'GET', cache: 'no-store' },
  ])
  await loadCurrencyPrices(undefined, true)
  expect(fetch.mock.calls[1]![0]).toBe('/api/v1/economy/prices/refresh')
  expect(fetch.mock.calls[1]![1].method).toBe('POST')
})
it.each([
  { ...view, nextRefreshAllowedAt: 'invalid' },
  { ...view, snapshot: { ...view.snapshot!, exaltedPerDivine: 0 } },
  { ...view, snapshot: { ...view.snapshot!, prices: 5 } },
  {
    ...view,
    snapshot: {
      ...view.snapshot!,
      prices: { 'Exalted Orb': { name: 'different', divine: 1, exalted: 1 } },
    },
  },
  { ...view, unavailable: true },
])('rejects malformed or inconsistent cached prices %#', async (data) => {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue(new Response(JSON.stringify(data))),
  )
  await expect(loadCurrencyPrices()).rejects.toThrow('Invalid currency prices')
})
it('accepts the unavailable state without inventing a zero price', async () => {
  const unavailable = { ...view, snapshot: null, unavailable: true }
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue(new Response(JSON.stringify(unavailable))),
  )
  expect(await loadCurrencyPrices()).toEqual(unavailable)
})
