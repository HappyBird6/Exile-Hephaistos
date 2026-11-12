import { useSyncExternalStore } from 'react'
export type Account = { id: string; displayName: string }
export type Auth = { enabled: boolean; user: Account | null; csrf: string }
let state: Auth | null = null
const listeners = new Set<() => void>()
function publish(next: Auth) {
  state = next
  listeners.forEach((fn) => fn())
}
export const currentAuth = () => state
export const accountScope = () => state?.user?.id ?? null
export function useAccount() {
  return useSyncExternalStore(
    (fn) => {
      listeners.add(fn)
      return () => listeners.delete(fn)
    },
    () => state,
    () => null,
  )
}
export async function loadAccount() {
  const response = await fetch('/api/account/me', {
    cache: 'no-store',
    credentials: 'same-origin',
  })
  if (!response.ok) throw new Error('ACCOUNT_UNAVAILABLE')
  const next = (await response.json()) as Auth
  if (
    typeof next.enabled !== 'boolean' ||
    typeof next.csrf !== 'string' ||
    (next.user !== null &&
      (typeof next.user?.id !== 'string' ||
        typeof next.user.displayName !== 'string'))
  )
    throw new Error('INVALID_ACCOUNT_RESPONSE')
  publish(next)
  return next
}
export async function accountRequest(
  path: string,
  init: RequestInit = {},
  owner = accountScope(),
) {
  if (!state) await loadAccount()
  const response = await fetch(`/api/account/${path}`, {
    ...init,
    credentials: 'same-origin',
    cache: 'no-store',
    headers: {
      'Content-Type': 'application/json',
      'X-CSRF-Token': state?.csrf ?? '',
      ...(owner ? { 'X-Account-Id': owner } : {}),
      ...init.headers,
    },
  })
  if (!response.ok) throw new AccountError(response.status)
  return response
}
export class AccountError extends Error {
  constructor(public status: number) {
    super(`ACCOUNT_${status}`)
  }
}
export async function authenticate(
  username: string,
  password: string,
  register: boolean,
) {
  const response = await accountRequest(register ? 'register' : 'login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })
  publish((await response.json()) as Auth)
}
export async function logout() {
  await accountRequest('logout', { method: 'POST' })
  publish({ enabled: true, user: null, csrf: '' })
}
