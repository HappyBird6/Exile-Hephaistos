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
  downloads: number
  recommendations: number
  createdAt: string
  updatedAt: string
  revision: number
}
export type Post = Omit<Summary, 'steps' | 'downloads' | 'recommendations'> & {
  sourceId: string
  body: string
  film: Film
  sharedStep: number
}
export type Page = {
  author?: { id: string; name: string } | null
  posts: Summary[]
  total: number
  page: number
  pageSize: number
}
export type Options = {
  limits: {
    bodyLength: number
    commentLength: number
    sessionBytes: number
    frames: number
  }
  currentVersion: string
  versions: string[]
  bases: string[]
  classes: string[]
  baseClasses: Record<string, string>
}
export type Stats = {
  downloads: number
  recommendations: number
  recommended: boolean
}
export type Comment = {
  id: string
  authorId: string
  author: string
  body: string
  revision: number
  createdAt: string
  updatedAt: string
}
export type CommentPage = {
  comments: Comment[]
  total: number
  page: number
  pageSize: number
}
// Reuse one session-bound CSRF context across mutations, including concurrent callers.
// Public reads never need this extra request. The first context also establishes the
// browser cookie used to deduplicate anonymous downloads.
let writeContext: { identity: string; promise: Promise<string> } | null = null
function identity() {
  const auth = currentAuth()
  return `${auth?.user?.id ?? 'guest'}:${auth?.csrf ?? ''}`
}
function csrfContext(key: string, refresh = false) {
  if (!refresh && writeContext?.identity === key) return writeContext.promise
  const pending = {
    identity: key,
    promise: (async () => {
      const response = await fetch('/api/community/context', {
        credentials: 'same-origin',
        cache: 'no-store',
        signal: AbortSignal.timeout(15000),
      })
      if (!response.ok) throw Error('COMMUNITY_UNAVAILABLE')
      const data: unknown = await response.json()
      if (
        !data ||
        typeof data !== 'object' ||
        !('csrf' in data) ||
        typeof data.csrf !== 'string' ||
        !data.csrf
      )
        throw Error('INVALID_COMMUNITY_CONTEXT')
      return data.csrf
    })(),
  }
  writeContext = pending
  void pending.promise.catch(() => {
    if (writeContext === pending) writeContext = null
  })
  return pending.promise
}
export async function boardRequest<T>(
  path: string,
  init: RequestInit = {},
): Promise<T> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  const owner = currentAuth()?.user?.id
  const key = identity()
  const mutation = !!init.method && !['GET', 'HEAD'].includes(init.method)
  if (owner) headers['X-Account-Id'] = owner
  let token = mutation ? await csrfContext(key) : null
  const send = () => {
    init.signal?.throwIfAborted()
    if (identity() !== key) throw Error('ACCOUNT_CHANGED')
    return fetch('/api/community/' + path, {
      ...init,
      credentials: 'same-origin',
      cache: 'no-store',
      signal: init.signal ?? AbortSignal.timeout(15000),
      headers: {
        ...headers,
        ...(token ? { 'X-CSRF-Token': token } : {}),
        ...init.headers,
      },
    })
  }
  let result = await send()
  // Expired guest sessions can rotate their token. Never replay an authorization
  // rejection unless the context actually changed, and never retry a successful write.
  if (mutation && result.status === 403) {
    const refreshed = await csrfContext(key, true)
    if (refreshed !== token) {
      token = refreshed
      result = await send()
    }
  }
  if (!result.ok) throw Error(`COMMUNITY_${result.status}`)
  const raw = await result.text()
  return (raw ? JSON.parse(raw) : undefined) as T
}
