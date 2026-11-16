import { performanceSettings } from '../../shared/performance'
import { expect, it, vi } from 'vitest'
import { loadCatalogReference } from './catalogReference'
import { rulesetHeader } from './rulesetIdentity'
it('shares a catalog load without allowing one cancelled preview to cancel another', async () => {
  let complete!: (response: Response) => void
  const fetcher = vi.fn(
    () =>
      new Promise<Response>((resolve) => {
        complete = resolve
      }),
  )
  vi.stubGlobal('fetch', fetcher)
  const first = new AbortController(),
    second = new AbortController()
  const a = loadCatalogReference('/catalogs/shared', 'one', first.signal)
  const b = loadCatalogReference('/catalogs/shared', 'one', second.signal)
  first.abort()
  await expect(a).rejects.toMatchObject({ name: 'AbortError' })
  complete(
    new Response(JSON.stringify({ modifiers: {} }), {
      headers: { [rulesetHeader]: 'one' },
    }),
  )
  await expect(b).resolves.toEqual({ modifiers: {} })
  expect(fetcher).toHaveBeenCalledTimes(1)
  await expect(
    loadCatalogReference('/catalogs/shared', 'one', second.signal),
  ).resolves.toEqual({ modifiers: {} })
  expect(fetcher).toHaveBeenCalledTimes(1)
})
it('does not retain failed or mismatched ruleset responses', async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      new Response('{}', { headers: { [rulesetHeader]: 'old' } }),
    )
    .mockResolvedValueOnce(
      new Response('{}', { headers: { [rulesetHeader]: 'new' } }),
    )
  vi.stubGlobal('fetch', fetcher)
  const signal = new AbortController().signal
  await expect(
    loadCatalogReference('/catalogs/mismatch', 'new', signal),
  ).rejects.toThrow()
  await expect(
    loadCatalogReference('/catalogs/mismatch', 'new', signal),
  ).resolves.toEqual({})
  expect(fetcher).toHaveBeenCalledTimes(2)
})

it('retains in-flight deduplication even when concurrent references exceed completed-cache capacity', async () => {
  const finishes = new Map<string, (response: Response) => void>()
  const fetcher = vi.fn(
    (input: RequestInfo | URL) =>
      new Promise<Response>((resolve) => finishes.set(String(input), resolve)),
  )
  vi.stubGlobal('fetch', fetcher)
  const signal = new AbortController().signal
  const first = loadCatalogReference(
    '/catalogs/slow-shared',
    'concurrent',
    signal,
  )
  const others = Array.from(
    { length: performanceSettings.catalogRetentionEntries + 1 },
    (_, index) =>
      loadCatalogReference('/catalogs/other-' + index, 'concurrent', signal),
  )
  const duplicate = loadCatalogReference(
    '/catalogs/slow-shared',
    'concurrent',
    signal,
  )
  expect(
    fetcher.mock.calls.filter(([url]) => url === '/catalogs/slow-shared'),
  ).toHaveLength(1)
  for (const finish of finishes.values())
    finish(new Response('{}', { headers: { [rulesetHeader]: 'concurrent' } }))
  await Promise.all([first, duplicate, ...others])
})

it.each(['seconds', 'date', 'missing'])(
  'honors 429 cooldown without a ruleset header or automatic retries: %s',
  async (mode) => {
    const now = Date.UTC(
      2000,
      0,
      mode === 'seconds' ? 1 : mode === 'date' ? 2 : 3,
    )
    const retry =
      mode === 'seconds'
        ? '2'
        : mode === 'date'
          ? new Date(now + 2000).toUTCString()
          : null
    const clock = vi.spyOn(Date, 'now').mockReturnValue(now)
    try {
      const fetcher = vi
        .fn()
        .mockResolvedValueOnce(
          new Response('', {
            status: 429,
            headers: retry ? { 'Retry-After': retry } : {},
          }),
        )
        .mockResolvedValue(
          new Response('{}', { headers: { [rulesetHeader]: 'limited' } }),
        )
      vi.stubGlobal('fetch', fetcher)
      const signal = new AbortController().signal
      const url = '/catalogs/limited-' + retry
      await expect(
        loadCatalogReference(url, 'limited', signal),
      ).rejects.toThrow('Could not load catalog')
      await expect(
        loadCatalogReference(url, 'limited', signal),
      ).rejects.toThrow('temporarily limited')
      await expect(
        loadCatalogReference(url + '-other', 'limited', signal),
      ).rejects.toThrow('temporarily limited')
      expect(fetcher).toHaveBeenCalledTimes(1)
      clock.mockReturnValue(
        now + Math.max(2000, performanceSettings.catalogRateLimitCooldownMs),
      )
      await expect(
        loadCatalogReference(url, 'limited', signal),
      ).resolves.toEqual({})
      expect(fetcher).toHaveBeenCalledTimes(2)
    } finally {
      clock.mockRestore()
    }
  },
)
