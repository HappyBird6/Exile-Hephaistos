import { currentAuth } from '../account/auth'
import type { Film } from '../crafting/workbenchHistory'
export type Summary = {
  id: string
  authorId: string | null
  author: string | null
  gameVersion: string
  baseId: string
  title: string
  steps: number
  createdAt: string
  updatedAt: string
  revision: number
}
export type Post = Omit<Summary, 'steps'> & {
  sourceId: string
  body: string
  film: Film
  sharedStep: number
}
export type Page = {
  posts: Summary[]
  total: number
  page: number
  pageSize: number
}
export type Options = {
  currentVersion: string
  versions: string[]
  bases: string[]
}
export async function boardRequest<T>(
  path: string,
  init: RequestInit = {},
): Promise<T> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  const owner = currentAuth()?.user?.id
  if (owner) headers['X-Account-Id'] = owner
  if (init.method && init.method !== 'GET') {
    const context = await fetch('/api/community/context', {
      credentials: 'same-origin',
      cache: 'no-store',
      signal: AbortSignal.timeout(15000),
    })
    if (!context.ok) throw Error('COMMUNITY_UNAVAILABLE')
    const data = (await context.json()) as { csrf: string }
    headers['X-CSRF-Token'] = data.csrf
  }
  const result = await fetch('/api/community/' + path, {
    ...init,
    credentials: 'same-origin',
    cache: 'no-store',
    signal: init.signal ?? AbortSignal.timeout(15000),
    headers: { ...headers, ...init.headers },
  })
  if (!result.ok) throw Error(`COMMUNITY_${result.status}`)
  const raw = await result.text()
  return (raw ? JSON.parse(raw) : undefined) as T
}
