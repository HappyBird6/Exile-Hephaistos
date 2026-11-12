import { afterEach, expect, it, vi } from 'vitest'
import {
  accountRequest,
  authenticate,
  currentAuth,
  loadAccount,
  logout,
} from './auth'
afterEach(() => vi.unstubAllGlobals())
it('uses CSRF and the authenticated account scope, and clears the account after logout', async () => {
  const request = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json({ enabled: true, user: null, csrf: 'guest-token' }),
    )
    .mockResolvedValueOnce(
      Response.json({
        enabled: true,
        user: { id: 'owner', displayName: 'player' },
        csrf: 'signed-token',
      }),
    )
    .mockResolvedValueOnce(Response.json([]))
    .mockResolvedValueOnce(new Response(null, { status: 204 }))
  vi.stubGlobal('fetch', request)
  await loadAccount()
  await authenticate('player', 'private-password', false)
  expect(request.mock.calls[1]?.[1].headers['X-CSRF-Token']).toBe('guest-token')
  await accountRequest('library')
  expect(request.mock.calls[2]?.[1].headers['X-Account-Id']).toBe('owner')
  expect(request.mock.calls[2]?.[1].headers['X-CSRF-Token']).toBe(
    'signed-token',
  )
  expect(localStorage.getItem('password')).toBeNull()
  await logout()
  expect(currentAuth()?.user).toBeNull()
})
it('does not publish an authenticated user after a rejected login', async () => {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue(new Response(null, { status: 401 })),
  )
  await expect(
    authenticate('player', 'wrong-password', false),
  ).rejects.toMatchObject({ status: 401 })
  expect(currentAuth()?.user).toBeNull()
})
