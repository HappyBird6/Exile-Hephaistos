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
  async function request<T>(
    path: string,
    signal: AbortSignal,
    body?: unknown,
  ): Promise<T> {
    const response = await fetch(`${baseUrl}${path}`, {
      signal,
      ...(body === undefined
        ? {}
        : {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body),
          }),
    })
    return readGoalFilterResponse<T>(response)
  }
  return {
    id: baseUrl,
    catalog: (context, signal) =>
      request<Catalog>(
        `/catalog?${new URLSearchParams({ ...context, itemLevel: String(context.itemLevel) })}`,
        signal,
      ),
    validate: (context: Context, goal: GoalFilter, signal) =>
      request<Validation>('/validate', signal, { context, goal }),
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
