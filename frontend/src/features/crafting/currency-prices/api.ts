export type CurrencyQuote = { name: string; divine: number; exalted: number }
export type PriceSnapshot = {
  league: string
  updatedAt: string
  exaltedPerDivine: number
  prices: Record<string, CurrencyQuote>
}
export type PriceView = {
  refreshing?: boolean
  snapshot: PriceSnapshot | null
  nextRefreshAllowedAt: string
  stale: boolean
  unavailable: boolean
}
const date = (v: unknown): v is string =>
  typeof v === 'string' && Number.isFinite(Date.parse(v))
const positive = (v: unknown): v is number =>
  typeof v === 'number' && Number.isFinite(v) && v > 0
export async function loadCurrencyPrices(
  signal?: AbortSignal,
  refresh = false,
  revalidate = false,
  current?: PriceView,
): Promise<PriceView> {
  if (revalidate && current) {
    const response = await fetch('/api/v1/economy/prices/status', {
      ...(signal ? { signal } : {}),
      cache: 'no-store',
    })
    if (!response.ok) throw new Error('Could not retrieve currency status')
    const status = await response.json()
    if (
      !status ||
      !date(status.nextRefreshAllowedAt) ||
      typeof status.stale !== 'boolean' ||
      typeof status.unavailable !== 'boolean' ||
      typeof status.refreshing !== 'boolean' ||
      !(status.revision === null || typeof status.revision === 'string') ||
      status.unavailable !== (status.revision === null)
    )
      throw new Error('Invalid currency status')
    const revision = current.snapshot
      ? `${current.snapshot.league}:${current.snapshot.updatedAt}`
      : null
    if (status.revision === revision)
      return {
        ...current,
        nextRefreshAllowedAt: status.nextRefreshAllowedAt,
        stale: status.stale,
        unavailable: status.unavailable,
        refreshing: status.refreshing,
      }
  }
  const response = await fetch(
    `/api/v1/economy/prices${refresh ? '/refresh' : ''}`,
    {
      ...(signal ? { signal } : {}),
      method: refresh ? 'POST' : 'GET',
      cache: refresh ? 'no-store' : revalidate ? 'no-cache' : 'default',
    },
  )
  if (!response.ok) throw new Error('Could not retrieve currency prices')
  const data = (await response.json()) as PriceView
  if (
    !data ||
    !date(data.nextRefreshAllowedAt) ||
    typeof data.stale !== 'boolean' ||
    typeof data.unavailable !== 'boolean' ||
    (data.snapshot !== null &&
      (!data.snapshot ||
        typeof data.snapshot.league !== 'string' ||
        !data.snapshot.league ||
        !date(data.snapshot.updatedAt) ||
        !positive(data.snapshot.exaltedPerDivine) ||
        !data.snapshot.prices ||
        typeof data.snapshot.prices !== 'object' ||
        Array.isArray(data.snapshot.prices) ||
        Object.entries(data.snapshot.prices).some(
          ([name, quote]) =>
            !quote ||
            quote.name !== name ||
            !positive(quote.divine) ||
            !positive(quote.exalted),
        ))) ||
    data.unavailable !== (data.snapshot === null)
  )
    throw new Error('Invalid currency prices')
  return data
}
