import type {
  Catalog,
  Context,
  GoalFilter,
  GoalFilterAdapter,
  Validation,
} from './types'
export class GoalFilterApiError extends Error {
  constructor(public readonly status: number) {
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
    if (!response.ok) throw new GoalFilterApiError(response.status)
    return response.json() as Promise<T>
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
