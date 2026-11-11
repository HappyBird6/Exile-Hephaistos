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
