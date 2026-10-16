import type { GoalFilter } from '../goal-filter/types'
const prefix = 'hephaistos.support.v1:'
export function validGoal(value: unknown): value is GoalFilter {
  const g = value as GoalFilter
  return (
    !!g &&
    g.version === 1 &&
    typeof g.catalogVersion === 'string' &&
    !!g.general &&
    typeof g.general.baseItemId === 'string' &&
    !!g.general.itemLevel &&
    Array.isArray(g.general.rarities) &&
    Array.isArray(g.groups) &&
    g.groups.length <= 32 &&
    g.groups.every(
      (group) =>
        typeof group.id === 'string' &&
        typeof group.type === 'string' &&
        Array.isArray(group.entries) &&
        group.entries.length <= 128 &&
        group.entries.every(
          (e) =>
            typeof e.id === 'string' &&
            typeof e.statId === 'string' &&
            typeof e.unit === 'string' &&
            !!e.range &&
            [e.range.min, e.range.max].every(
              (n) =>
                n === null || (typeof n === 'number' && Number.isFinite(n)),
            ),
        ),
    )
  )
}
export function hasImplicitGoal(goal: GoalFilter) {
  return goal.groups.some((g) =>
    g.entries.some((e) => e.statId.startsWith('hephaistos:v1:implicit:')),
  )
}
const ttl = 24 * 60 * 60 * 1000
const limit = 128 * 1024
export function readSupport<T>(
  key: string,
  valid: (value: unknown) => value is T,
): T | null {
  try {
    const raw = localStorage.getItem(prefix + key)
    if (!raw) return null
    if (raw.length > limit) throw new Error('Size')
    const value = JSON.parse(raw)
    if (
      value.version !== 1 ||
      !Number.isFinite(value.savedAt) ||
      Date.now() - value.savedAt > ttl ||
      value.savedAt > Date.now() + 60000 ||
      !valid(value.data)
    )
      throw new Error('Invalid saved input')
    return value.data
  } catch {
    try {
      localStorage.removeItem(prefix + key)
    } catch {
      /* Storage can be unavailable. */
    }
    return null
  }
}
export function saveSupport(key: string, data: unknown): boolean {
  try {
    const raw = JSON.stringify({ version: 1, savedAt: Date.now(), data })
    if (raw.length > limit) return false
    localStorage.setItem(prefix + key, raw)
    return true
  } catch {
    return false
  }
}
export type Selection = {
  method: string
  source: number
  target: number
  kind: string
}
export type SavedRun = {
  request: string
  job: string
  selected: number
  cycles: number
  route: Selection[]
}
export function validRun(value: unknown): value is SavedRun {
  const v = value as SavedRun
  return (
    !!v &&
    typeof v.request === 'string' &&
    /^[0-9a-f-]{36}$/.test(v.job) &&
    Number.isSafeInteger(v.selected) &&
    [100, 300, 500].includes(v.cycles) &&
    Array.isArray(v.route) &&
    v.route.length <= 64 &&
    v.route.every(
      (s) =>
        typeof s.method === 'string' &&
        s.method.length < 512 &&
        Number.isSafeInteger(s.source) &&
        Number.isSafeInteger(s.target) &&
        typeof s.kind === 'string',
    )
  )
}
