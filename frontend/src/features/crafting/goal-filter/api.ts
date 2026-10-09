import {
  rulesetHeaders,
  rulesetHeader,
  verifyRulesetResponse,
} from '../rulesetIdentity'
import type {
  Catalog,
  Context,
  GoalFilter,
  GoalFilterAdapter,
  Validation,
  Issue,
} from './types'
export class GoalFilterApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly issues: Issue[] = [],
  ) {
    super(`Goal filter request failed (${status})`)
  }
}
export function createHttpGoalFilterAdapter(
  baseUrl = '/api/v1/crafting/support/goal-filters',
): GoalFilterAdapter {
  const identities = new Map<string, string>()
  const key = (context: Context) =>
    JSON.stringify([context.snapshotId, context.baseItemId, context.itemLevel])
  async function request<T>(
    path: string,
    signal: AbortSignal,
    body?: unknown,
    rulesetIdentity?: string,
  ): Promise<T> {
    const response = await fetch(`${baseUrl}${path}`, {
      signal,
      ...(body === undefined
        ? {}
        : {
            method: 'POST',
            headers: rulesetHeaders(rulesetIdentity),
            body: JSON.stringify(body),
          }),
    })
    if (body !== undefined && response.ok)
      verifyRulesetResponse(response, rulesetIdentity!)
    const value = await readGoalFilterResponse<T>(response)
    if (body === undefined && path.startsWith('/catalog?') && response.ok) {
      const identity = response.headers.get(rulesetHeader)
      if (!identity)
        throw new Error('Goal catalog ruleset identity is missing.')
      const catalog = value as Catalog
      identities.set(key(catalog.context), identity)
    }
    return value
  }
  return {
    id: baseUrl,
    catalog: (context, signal) =>
      request<Catalog>(
        `/catalog?${new URLSearchParams({ ...context, itemLevel: String(context.itemLevel) })}`,
        signal,
      ),
    validate: (context: Context, goal: GoalFilter, signal) =>
      request<Validation>(
        '/validate',
        signal,
        { context, goal },
        identities.get(key(context)),
      ),
  }
}

export async function readGoalFilterResponse<T>(
  response: Response,
): Promise<T> {
  if (response.ok) return response.json() as Promise<T>
  let issues: Issue[] = []
  try {
    const body = (await response.json()) as { issues?: unknown }
    if (Array.isArray(body.issues))
      issues = body.issues.filter(
        (issue): issue is Issue =>
          issue !== null &&
          typeof issue === 'object' &&
          typeof issue.code === 'string' &&
          typeof issue.path === 'string' &&
          typeof issue.message === 'string' &&
          ['ERROR', 'WARNING'].includes(issue.severity),
      )
  } catch {
    /* A proxy failure need not contain JSON. */
  }
  throw new GoalFilterApiError(response.status, issues)
}
