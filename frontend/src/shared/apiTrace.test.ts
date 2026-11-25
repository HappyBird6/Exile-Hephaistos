import { afterEach, expect, it, vi } from 'vitest'
import { inheritApiTrace, installApiTrace, traceLabel } from './apiTrace'

afterEach(() => {
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

it('correlates overlapping requests and decoded responses without logging payloads', async () => {
  const info = vi.spyOn(console, 'info').mockImplementation(() => {})
  const warn = vi.spyOn(console, 'warn').mockImplementation(() => {})
  const response = new Response(null, { status: 403 })
  let finish!: (value: Response) => void
  vi.stubGlobal(
    'fetch',
    vi
      .fn()
      .mockImplementationOnce(
        () =>
          new Promise<Response>((resolve) => {
            finish = resolve
          }),
      )
      .mockRejectedValueOnce(new DOMException('private reason', 'AbortError')),
  )
  installApiTrace()
  const first = fetch('/api/account/login?private', {
    method: 'POST',
    body: 'private body',
  })
  await expect(
    fetch('/api/v1/crafting/catalogs/example'),
  ).rejects.toHaveProperty('name', 'AbortError')
  finish(response)
  expect(await first).toBe(response)
  const label = traceLabel(inheritApiTrace(response, new Response()))
  expect(info.mock.calls[0]?.[0]).toBe(label)
  expect(warn.mock.calls[0]?.[0]).not.toBe(label)
  expect(warn.mock.calls[1]?.[0]).toBe(label)
  expect(
    JSON.stringify([...info.mock.calls, ...warn.mock.calls]),
  ).not.toContain('private')
})
