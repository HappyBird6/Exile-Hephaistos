import type { Catalog, GoalFilter, Issue, Stat } from './types'

const storageKey = 'hephaistos.goal-catalog-contract.v1'
type Contract = { version: string; stats: Record<string, string> }
const signature = (s: Stat) =>
  JSON.stringify([
    s.kind,
    s.unit,
    [...s.sourceStatIds].sort(),
    [...s.contributions].sort((a, b) => a.statId.localeCompare(b.statId)),
  ])
export function readContract(): Contract | null {
  try {
    const raw = localStorage.getItem(storageKey)
    if (!raw || raw.length > 128 * 1024) return null
    const value = JSON.parse(raw) as Contract
    return typeof value.version === 'string' &&
      value.stats &&
      Object.values(value.stats).every((v) => typeof v === 'string')
      ? value
      : null
  } catch {
    return null
  }
}
export function rememberContract(goal: GoalFilter, catalog: Catalog) {
  if (goal.catalogVersion !== catalog.catalogVersion) return
  const previous = readContract()
  const stats =
    previous?.version === catalog.catalogVersion ? { ...previous.stats } : {}
  for (const group of goal.groups)
    for (const entry of group.entries) {
      const stat = catalog.stats.find((s) => s.statId === entry.statId)
      if (stat) stats[stat.statId] = signature(stat)
    }
  try {
    localStorage.setItem(
      storageKey,
      JSON.stringify({ version: catalog.catalogVersion, stats }),
    )
  } catch {
    /* The goal itself remains in the editor; recovery never deletes storage. */
  }
}
export function compatibilityIssues(
  goal: GoalFilter,
  catalog: Catalog,
  previous = readContract(),
): Issue[] {
  const issues: Issue[] = []
  const add = (path: string, code: string) =>
    issues.push({ path, code, message: code, severity: 'ERROR' })
  if (
    goal.version !== 1 ||
    goal.general.baseItemId !== catalog.context.baseItemId
  )
    add('/goal/general/baseItemId', 'RECOVERY_CONTEXT')
  goal.groups.forEach((group, i) => {
    const path = `/goal/groups/${i}`
    if (
      !catalog.groupTypes.some(
        (t) => t.type === group.type && t.evaluation === 'SUPPORTED',
      )
    )
      add(path, 'RECOVERY_OPERATION')
    group.entries.forEach((entry, j) => {
      const row = `${path}/entries/${j}`
      const stat = catalog.stats.find((s) => s.statId === entry.statId)
      if (!stat) {
        add(row, 'RECOVERY_MISSING')
        return
      }
      if (stat.unit !== entry.unit || stat.support.evaluation !== 'SUPPORTED') {
        add(row, 'RECOVERY_MEANING')
        return
      }
      const prior =
        previous?.version === goal.catalogVersion
          ? previous.stats[entry.statId]
          : undefined
      if (prior) {
        if (prior !== signature(stat)) add(row, 'RECOVERY_MEANING')
      } else {
        // v1 direct IDs encode their source and layer. A legacy pseudo has no such
        // proof: never infer its summation semantics from its name or label.
        if (
          stat.kind === 'PSEUDO' ||
          stat.sourceStatIds.length !== 1 ||
          stat.contributions.length !== 0 ||
          stat.statId !==
            `hephaistos:v1:${stat.kind.toLowerCase()}:${stat.sourceStatIds[0]}`
        )
          add(row, 'RECOVERY_UNVERIFIED')
      }
    })
  })
  return issues
}
