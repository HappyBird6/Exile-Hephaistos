import { afterEach, beforeEach, expect, it, vi } from 'vitest'
const auth = vi.hoisted(() => ({
  user: null as null | { id: string },
  csrf: 'initial',
}))
vi.mock('../account/auth', () => ({ currentAuth: () => auth }))
beforeEach(() => {
  vi.resetModules()
  auth.user = null
  auth.csrf = 'initial'
})
afterEach(() => vi.unstubAllGlobals())
const mutation = { method: 'POST', body: '{}' }
it('shares the first context for simultaneous writes, and reuses it for subsequent writes', async () => {
  const request = vi.fn(async (path: RequestInfo | URL) =>
    Response.json(
      String(path).endsWith('/context') ? { csrf: 'context' } : { saved: true },
    ),
  )
  vi.stubGlobal('fetch', request)
  const { boardRequest } = await import('./api')
  await Promise.all([
    boardRequest('posts/a/download', mutation),
    boardRequest('posts/b/download', mutation),
  ])
  await boardRequest('posts/a/comments', mutation)
  expect(
    request.mock.calls.filter(([p]) => String(p).endsWith('/context')),
  ).toHaveLength(1)
  expect(request).toHaveBeenCalledTimes(4)
})
it('does not request a context for public reads', async () => {
  const request = vi.fn().mockResolvedValue(Response.json([]))
  vi.stubGlobal('fetch', request)
  const { boardRequest } = await import('./api')
  await boardRequest('posts')
  expect(request).toHaveBeenCalledTimes(1)
})
it('refreshes an expired token once and uses the new token on the retry', async () => {
  const request = vi
    .fn()
    .mockResolvedValueOnce(Response.json({ csrf: 'expired' }))
    .mockResolvedValueOnce(new Response(null, { status: 403 }))
    .mockResolvedValueOnce(Response.json({ csrf: 'renewed' }))
    .mockResolvedValueOnce(Response.json({ saved: true }))
  vi.stubGlobal('fetch', request)
  const { boardRequest } = await import('./api')
  await expect(boardRequest('posts', mutation)).resolves.toEqual({
    saved: true,
  })
  expect(request.mock.calls[3]?.[1].headers['X-CSRF-Token']).toBe('renewed')
  expect(request).toHaveBeenCalledTimes(4)
})
it('does not replay a permission rejection when the CSRF token is unchanged', async () => {
  const request = vi.fn(async (path: RequestInfo | URL) =>
    String(path).endsWith('/context')
      ? Response.json({ csrf: 'same' })
      : new Response(null, { status: 403 }),
  )
  vi.stubGlobal('fetch', request)
  const { boardRequest } = await import('./api')
  await expect(boardRequest('posts/private/text', mutation)).rejects.toThrow(
    'COMMUNITY_403',
  )
  expect(
    request.mock.calls.filter(([p]) => !String(p).endsWith('/context')),
  ).toHaveLength(1)
})
it('uses a new context after login and blocks an account change during context loading', async () => {
  const request = vi.fn(async () => Response.json({ csrf: 'token' }))
  vi.stubGlobal('fetch', request)
  const { boardRequest } = await import('./api')
  await boardRequest('posts', mutation)
  auth.user = { id: 'member' }
  auth.csrf = 'signed'
  await boardRequest('posts', mutation)
  expect(request).toHaveBeenCalledTimes(4)
  expect(
    (request.mock.calls[3] as unknown as [unknown, RequestInit])[1].headers,
  ).toMatchObject({ 'X-Account-Id': 'member' })
  auth.csrf = 'new-session'
  request.mockImplementationOnce(async () => {
    auth.user = { id: 'other' }
    return Response.json({ csrf: 'other-token' })
  })
  await expect(boardRequest('posts', mutation)).rejects.toThrow(
    'ACCOUNT_CHANGED',
  )
  expect(request).toHaveBeenCalledTimes(5)
})
it('recovers after context fetch failure without caching the rejection', async () => {
  const request = vi
    .fn()
    .mockRejectedValueOnce(Error('offline'))
    .mockResolvedValueOnce(Response.json({ csrf: 'ok' }))
    .mockResolvedValueOnce(Response.json({ saved: true }))
  vi.stubGlobal('fetch', request)
  const { boardRequest } = await import('./api')
  await expect(boardRequest('posts', mutation)).rejects.toThrow('offline')
  await expect(boardRequest('posts', mutation)).resolves.toEqual({
    saved: true,
  })
})
it('does not send a cancelled write after shared context loading', async () => {
  const controller = new AbortController()
  const request = vi.fn(async () => {
    controller.abort()
    return Response.json({ csrf: 'ok' })
  })
  vi.stubGlobal('fetch', request)
  const { boardRequest } = await import('./api')
  await expect(
    boardRequest('posts', { ...mutation, signal: controller.signal }),
  ).rejects.toThrow()
  expect(request).toHaveBeenCalledTimes(1)
})
