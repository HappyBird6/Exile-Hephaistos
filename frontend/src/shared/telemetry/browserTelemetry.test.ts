import { afterEach, expect, it, vi } from 'vitest'
import { reportBrowserEvent } from './browserTelemetry'
afterEach(() => vi.useRealTimers())
it('batches only bounded categories and counts, never the item or error message', async () => {
  vi.useFakeTimers()
  const request = vi.fn().mockResolvedValue(new Response(null, { status: 204 }))
  vi.stubGlobal('fetch', request)
  reportBrowserEvent('history_save')
  reportBrowserEvent('history_save')
  reportBrowserEvent('catalog_load')
  expect(request).not.toHaveBeenCalled()
  await vi.advanceTimersByTimeAsync(15000)
  expect(request).toHaveBeenCalledTimes(1)
  const body = JSON.parse(request.mock.calls[0]?.[1]?.body)
  expect(body).toEqual([
    { kind: 'history_save', count: 2 },
    { kind: 'catalog_load', count: 1 },
  ])
  expect(request.mock.calls[0]?.[0]).toBe('/api/telemetry')
})
