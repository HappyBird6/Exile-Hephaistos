import type { ConcreteItem } from '../workbenchApi'
import { readGoalFilterResponse } from './api'
import type { Catalog, GoalFilter, Issue, Recommendation } from './types'
type Evaluation = {
  status: string
  generalStatus: string
  groups: {
    id: string
    status: string
    count: number | null
    score: number | null
    entries: {
      id: string
      status: string
      presence: string
      value: number | null
    }[]
  }[]
  issues: Issue[]
}
export async function evaluateNumericItem(
  item: ConcreteItem,
  goal: GoalFilter,
  activeOmens: string[],
  maxMillis: number,
  signal: AbortSignal,
) {
  async function post<T>(path: string, body: unknown): Promise<T> {
    const response = await fetch(
      `/api/v1/crafting/support/goal-filters/${path}`,
      {
        method: 'POST',
        signal,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      },
    )
    return readGoalFilterResponse<T>(response)
  }
  const evaluation = await post<Evaluation>('evaluate', { item, goal })
  const recommendation = await post<Recommendation>('recommend', {
    item,
    goal,
    activeOmens,
    limits: { maxStates: 10000, maxEdges: 100000, maxMillis },
  })
  return { evaluation, recommendation }
}

export function presentNumericStats(
  item: ConcreteItem | null,
  catalog: Catalog,
): Set<string> {
  const present = new Set<string>()
  if (
    !item ||
    item.baseItemId !== catalog.context.baseItemId ||
    item.snapshotId !== catalog.context.snapshotId
  )
    return present
  for (const stat of catalog.stats) {
    if (stat.kind === 'PSEUDO') continue
    const sources = stat.kind === 'EXPLICIT' ? item.explicits : item.implicits
    if (
      sources.some((modifier) =>
        stat.sourceStatIds.some((id) => Object.hasOwn(modifier.values, id)),
      )
    )
      present.add(stat.statId)
  }
  for (const stat of catalog.stats)
    if (
      stat.kind === 'PSEUDO' &&
      stat.contributions.some((source) => present.has(source.statId))
    )
      present.add(stat.statId)
  return present
}
