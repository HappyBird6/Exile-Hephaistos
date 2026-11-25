import { catalystItemIds } from '../localizedCrafting'
import type { GoalFilter } from './types'

// null disables quality work; an empty choice leaves the catalyst unspecified.
export type TestQualityEditor = {
  choice: string | null
  change: (choice: string | null) => void
}

export function isOrdinaryCatalyst(value: unknown): value is string {
  return (
    typeof value === 'string' && Object.values(catalystItemIds).includes(value)
  )
}

export function isFlatRouteGoal(goal: GoalFilter) {
  if (goal.groups.some((group) => group.type !== 'AND' || group.range !== null))
    return false
  const entries = goal.groups.flatMap((group) => group.entries)
  return (
    new Set(entries.map((entry) => entry.statId)).size === entries.length &&
    new Set(entries.map((entry) => entry.id)).size === entries.length
  )
}

export function flatEditorGoal(
  saved: GoalFilter | null | undefined,
  empty: GoalFilter,
): GoalFilter {
  if (!saved || !isFlatRouteGoal(saved)) return empty
  const group = saved.groups[0]
  if (!group) return { ...saved, groups: empty.groups }
  if (saved.groups.length === 1 && !group.disabled) return saved
  return {
    ...saved,
    groups: [
      {
        ...group,
        disabled: false,
        entries: saved.groups.flatMap((g) =>
          g.entries.map((entry) =>
            g.disabled ? { ...entry, disabled: true } : entry,
          ),
        ),
      },
    ],
  }
}

export function testRouteGoal(goal: GoalFilter, quality: boolean): GoalFilter {
  return {
    ...goal,
    general: { ...goal.general, quality: quality ? 40 : null },
    // An inactive AND keeps the saved-request group shape when quality is the only goal.
    groups: goal.groups.map((group) =>
      quality && !group.entries.some((entry) => !entry.disabled)
        ? { ...group, disabled: true }
        : group,
    ),
  }
}
