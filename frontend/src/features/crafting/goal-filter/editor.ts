import { createStore } from 'zustand/vanilla'
import type { GoalFilter, Group, GroupType, Stat } from './types'
import { weighted } from './types'
import type { GroupPresentation } from './groupPresentation'
export type Editor = {
  goal: GoalFilter
  collapsedByGroupId: Record<string, boolean>
  revision: number
  presentedTypes: Record<string, GroupPresentation>
  chooseGroupType: (id: string, type: GroupPresentation) => void
  edit: (fn: (goal: GoalFilter) => void) => void
  collapse: (id: string) => void
}
export function createGoalFilterEditor(initial: GoalFilter) {
  return createStore<Editor>((set) => ({
    goal: structuredClone(initial),
    collapsedByGroupId: {},
    revision: 0,
    presentedTypes: {},
    chooseGroupType: (id, type) =>
      set((state) => {
        const goal = structuredClone(state.goal)
        const group = goal.groups.find((group) => group.id === id)
        if (!group) return state
        changeGroupType(group, type === 'OR' ? 'COUNT' : type)
        if (type === 'OR') group.range = { min: 1, max: null }
        return {
          goal,
          revision: state.revision + 1,
          presentedTypes: { ...state.presentedTypes, [id]: type },
        }
      }),
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
export function addStat(group: Group, stat: Stat, present = false) {
  if (
    (!stat.eligible && !present) ||
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
