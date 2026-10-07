import { createStore } from 'zustand/vanilla'
import type { GoalFilter, Group, GroupType, Stat } from './types'
import { weighted } from './types'
export type Editor = {
  goal: GoalFilter
  collapsedByGroupId: Record<string, boolean>
  revision: number
  edit: (fn: (goal: GoalFilter) => void) => void
  collapse: (id: string) => void
}
export function createGoalFilterEditor(initial: GoalFilter) {
  return createStore<Editor>((set) => ({
    goal: structuredClone(initial),
    collapsedByGroupId: {},
    revision: 0,
    edit: (fn) =>
      set((state) => {
        const goal = structuredClone(state.goal)
        fn(goal)
        return { goal, revision: state.revision + 1 }
      }),
    collapse: (id) =>
      set((state) => ({
        collapsedByGroupId: {
          ...state.collapsedByGroupId,
          [id]: !state.collapsedByGroupId[id],
        },
      })),
  }))
}
export function changeGroupType(group: Group, type: GroupType) {
  const previous = group.type
  group.type = type
  group.range =
    type === 'COUNT' || weighted(type)
      ? previous === type
        ? group.range
        : { min: null, max: null }
      : null
  group.entries.forEach((entry) => {
    entry.weight = weighted(type) ? (entry.weight ?? 1) : null
  })
}
export function addStat(group: Group, stat: Stat) {
  if (
    !stat.eligible ||
    group.entries.some((entry) => entry.statId === stat.statId)
  )
    return
  group.entries.push({
    id: crypto.randomUUID(),
    statId: stat.statId,
    unit: stat.unit,
    range: { min: null, max: null },
    weight: weighted(group.type) ? 1 : null,
    disabled: false,
  })
}
export function emptyGoal(
  context: { baseItemId: string },
  catalogVersion: string,
): GoalFilter {
  return {
    version: 1,
    catalogVersion,
    general: {
      baseItemId: context.baseItemId,
      itemLevel: { min: null, max: null },
      rarities: ['NORMAL', 'MAGIC', 'RARE'],
    },
    groups: [
      {
        id: crypto.randomUUID(),
        type: 'AND',
        disabled: false,
        range: null,
        entries: [],
      },
    ],
  }
}
