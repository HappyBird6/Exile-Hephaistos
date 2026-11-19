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
    goal: rareGoal(initial),
    collapsedByGroupId: {},
    revision: 0,
    edit: (fn) =>
      set((state) => {
        const goal = structuredClone(state.goal)
        fn(goal)
        goal.general.rarities = ['RARE']
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
  if (!weighted(type)) group.entries.forEach((entry) => (entry.weight = null))
}
export function emptyGroup(type: GroupType): Group {
  return {
    id: crypto.randomUUID(),
    type,
    range: type === 'COUNT' || weighted(type) ? { min: null, max: null } : null,
    entries: [],
    disabled: false,
  }
}
export function addStat(group: Group, stat: Stat, present = false) {
  if (!stat.eligible && !present) return
  group.entries.push({
    id: crypto.randomUUID(),
    statId: stat.statId,
    unit: stat.unit,
    range: { min: null, max: null },
    weight: null,
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
      rarities: ['RARE'],
    },
    groups: [emptyGroup('AND')],
  }
}

export function rareGoal<T extends GoalFilter>(goal: T): T {
  const copy = structuredClone(goal)
  copy.general.rarities = ['RARE']
  return copy
}
