import type { GoalFilter } from './types'
/** Legacy numeric goals use the above-cap boundary; inactive equipment retains its stored preference. */
export function qualityWorkGoal(
  goal: GoalFilter,
  allowed: boolean,
): GoalFilter {
  const quality = allowed && (goal.general.quality ?? 0) > 20 ? 40 : null
  return (quality === null && goal.general.quality == null) ||
    goal.general.quality === quality
    ? goal
    : { ...goal, general: { ...goal.general, quality } }
}
