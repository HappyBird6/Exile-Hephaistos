import type { Translate } from '../../shared/i18n/messages'
// Mirrors docs/openapi-admin.yaml. Validate server responses before using them.
export interface AdminSession {
  configured: boolean
  authenticated: boolean
  csrfToken: string
  csrfHeaderName: string
}

export interface CrawlTarget {
  id: string
  name: string
  url: string
  enabled: boolean
}

export interface CrawlSettings {
  version: number
  targets: CrawlTarget[]
  runnerEnabled: boolean
  limits: { maxTargets: number }
}

export type RunStatus = 'QUEUED' | 'RUNNING' | 'RAW_CAPTURED' | 'FAILED'

export interface CrawlRun {
  id: string
  status: RunStatus
  createdAt: string
  startedAt: string | null
  finishedAt: string | null
  targets: CrawlTarget[]
  errorCode: string | null
  sourceCount: number | null
}

export class AdminApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
  ) {
    super(code)
  }
}

function object(value: unknown): Record<string, unknown> {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) {
    throw new Error('INVALID_API_RESPONSE')
  }
  return value as Record<string, unknown>
}

function string(value: unknown): string {
  if (typeof value !== 'string') throw new Error('INVALID_API_RESPONSE')
  return value
}

function boolean(value: unknown): boolean {
  if (typeof value !== 'boolean') throw new Error('INVALID_API_RESPONSE')
  return value
}

function integer(value: unknown): number {
  if (typeof value !== 'number' || !Number.isSafeInteger(value) || value < 0) {
    throw new Error('INVALID_API_RESPONSE')
  }
  return value
}

function nullableString(value: unknown): string | null {
  return value === null ? null : string(value)
}

function targets(value: unknown): CrawlTarget[] {
  if (!Array.isArray(value)) throw new Error('INVALID_API_RESPONSE')
  return value.map((entry: unknown) => {
    const target = object(entry)
    return {
      id: string(target.id),
      name: string(target.name),
      url: string(target.url),
      enabled: boolean(target.enabled),
    }
  })
}

function settings(value: unknown): CrawlSettings {
  const result = object(value)
  return {
    version: integer(result.version),
    targets: targets(result.targets),
    runnerEnabled: boolean(result.runnerEnabled),
    limits: { maxTargets: integer(object(result.limits).maxTargets) },
  }
}

function run(value: unknown): CrawlRun {
  const result = object(value)
  const status = result.status
  if (
    status !== 'QUEUED' &&
    status !== 'RUNNING' &&
    status !== 'RAW_CAPTURED' &&
    status !== 'FAILED'
  ) {
    throw new Error('INVALID_API_RESPONSE')
  }
  return {
    id: string(result.id),
    status,
    createdAt: string(result.createdAt),
    startedAt: nullableString(result.startedAt),
    finishedAt: nullableString(result.finishedAt),
    targets: targets(result.targets),
    errorCode: nullableString(result.errorCode),
    sourceCount:
      result.sourceCount === null ? null : integer(result.sourceCount),
  }
}

async function request(path: string, init: RequestInit = {}): Promise<unknown> {
  const response = await fetch(`/api/v1/admin/${path}`, {
    ...init,
    credentials: 'same-origin',
    cache: 'no-store',
    redirect: 'error',
  })
  if (!response.ok) {
    // Never show arbitrary provider/server error text or HTML in the admin UI.
    let code = `HTTP_${response.status}`
    try {
      const body = object(await response.json())
      if (typeof body.code === 'string') code = body.code
    } catch {
      // A proxy may return HTML. Keep the HTTP status as a safe error code.
    }
    throw new AdminApiError(response.status, code)
  }
  return response.status === 204 ? null : response.json()
}

function csrf(session: AdminSession): Record<string, string> {
  return { [session.csrfHeaderName]: session.csrfToken }
}

export const adminApi = {
  async session(signal?: AbortSignal): Promise<AdminSession> {
    const result = object(await request('session', signal ? { signal } : {}))
    return {
      configured: boolean(result.configured),
      authenticated: boolean(result.authenticated),
      csrfToken: string(result.csrfToken),
      csrfHeaderName: string(result.csrfHeaderName),
    }
  },
  async login(session: AdminSession, username: string, password: string) {
    await request('login', {
      method: 'POST',
      headers: csrf(session),
      body: new URLSearchParams({ username, password }),
    })
  },
  async logout(session: AdminSession) {
    await request('logout', { method: 'POST', headers: csrf(session) })
  },
  async settings(signal?: AbortSignal) {
    return settings(await request('crawl-settings', signal ? { signal } : {}))
  },
  async saveSettings(
    session: AdminSession,
    version: number,
    draft: CrawlTarget[],
  ) {
    return settings(
      await request('crawl-settings', {
        method: 'PUT',
        headers: { ...csrf(session), 'Content-Type': 'application/json' },
        body: JSON.stringify({ version, targets: draft }),
      }),
    )
  },
  async runs(signal?: AbortSignal) {
    const result = object(await request('crawl-runs', signal ? { signal } : {}))
    if (!Array.isArray(result.runs)) throw new Error('INVALID_API_RESPONSE')
    return result.runs.map(run)
  },
  async startRun(session: AdminSession) {
    return run(
      await request('crawl-runs', { method: 'POST', headers: csrf(session) }),
    )
  },
}

export function errorMessage(error: Error, t: Translate): string {
  if (error instanceof AdminApiError) {
    if (error.status === 401) return t('errorAuth')
    if (error.status === 403) return t('errorForbidden')
    if (error.status === 409) return t('errorConflict')
    if (error.status === 422 || error.status === 400) return t('errorTarget')
    if (error.status === 429) return t('errorRateLimit')
    if (error.status === 503) return t('errorAdminUnavailable')
  }
  return t('errorNetwork')
}
