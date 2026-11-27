import type { ConcreteItem } from '../workbenchApi'
import { supportsConcreteStateShape } from '../workbenchStateShape'
import { baseIdForKey, basePolicy } from '../baseRegistry'
import { assertShape } from '../path-tree/validation'
import { isFlatRouteGoal, isOrdinaryCatalyst } from './testRouteGoal'
import type { Catalog, GoalFilter } from './types'
import type { Locale } from '../../../shared/i18n/i18n'
import { compatibilityIssues } from './catalogCompatibility'
import { presentGoalStat } from './statPresentation'
import { presentNumericStats } from './numericApi'

export type PresetStart = {
  base: string
  item: ConcreteItem
  rulesetIdentity: string
  text: string
  levelEdited: boolean
  testQualityChoice: string | null
}
export type SupportPreset = { start: PresetStart; goal: GoalFilter }
export type PresetEditor = {
  start: PresetStart | null
  restore: (
    preset: SupportPreset,
    applyGoal: (goal: GoalFilter) => void,
  ) => Promise<void>
}

const prefix = 'hephaistos.support-presets.v1:'
const namespace = (owner: string | null) =>
  prefix +
  (owner === null ? 'guest:' : 'account:' + encodeURIComponent(owner) + ':')

export function listPresets(owner: string | null) {
  const scopes = owner === null ? [null] : [owner, null]
  const entries: { key: string; name: string; guest: boolean }[] = []
  for (let i = 0; i < localStorage.length; i++) {
    const key = localStorage.key(i)
    if (!key) continue
    for (const scope of scopes) {
      const start = namespace(scope)
      if (key.startsWith(start)) {
        entries.push({
          key,
          name: key.slice(start.length),
          guest: scope === null,
        })
        break
      }
    }
  }
  return entries.sort((a, b) => a.name.localeCompare(b.name))
}

export function savePreset(
  owner: string | null,
  name: string,
  preset: SupportPreset,
) {
  const key = namespace(owner) + name.trim()
  if (!name.trim()) throw new Error('Unnamed preset')
  if (localStorage.getItem(key) !== null) return null
  validatePreset(preset)
  localStorage.setItem(key, JSON.stringify(preset))
  return key
}

export function loadPreset(owner: string | null, key: string): SupportPreset {
  if (!key.startsWith(namespace(owner)) && !key.startsWith(namespace(null)))
    throw new Error('Unavailable preset')
  const raw = localStorage.getItem(key)
  if (!raw) throw new Error('Missing preset')
  const preset = JSON.parse(raw) as SupportPreset
  validatePreset(preset)
  return preset
}

function validatePreset({ start, goal }: SupportPreset) {
  assertShape('goal', goal)
  if (
    !isFlatRouteGoal(goal) ||
    !supportsConcreteStateShape(start.item) ||
    !Array.isArray(start.item.conditions) ||
    !['NORMAL', 'MAGIC', 'RARE'].includes(start.item.rarity) ||
    typeof start.text !== 'string' ||
    typeof start.levelEdited !== 'boolean' ||
    typeof start.rulesetIdentity !== 'string' ||
    !start.rulesetIdentity ||
    typeof start.base !== 'string' ||
    typeof start.item.baseItemId !== 'string' ||
    baseIdForKey(start.base) !== start.item.baseItemId ||
    start.item.baseItemId !== goal.general.baseItemId ||
    (start.testQualityChoice !== null &&
      (!basePolicy(start.item.baseItemId)?.maximumQualityBreach ||
        (start.testQualityChoice !== '' &&
          !isOrdinaryCatalyst(start.testQualityChoice))))
  )
    throw new Error('Invalid preset')
}

export class UnavailablePresetRows extends Error {
  constructor(readonly rows: string[]) {
    super('Unavailable goal rows')
  }
}

export function currentPresetGoal(
  { goal, start }: SupportPreset,
  catalog: Catalog,
  locale: Locale,
): GoalFilter {
  const item = start.item
  if (
    catalog.context.snapshotId !== item.snapshotId ||
    catalog.context.baseItemId !== item.baseItemId ||
    catalog.context.itemLevel !== item.itemLevel
  )
    throw new Error('Incompatible goal catalog')
  const issues =
    goal.catalogVersion === catalog.catalogVersion
      ? []
      : compatibilityIssues(goal, catalog)
  const incompatible = new Set(issues.map((issue) => issue.path))
  const present = presentNumericStats(item, catalog)
  const rows: string[] = []
  goal.groups.forEach((group, index) => {
    group.entries.forEach((entry, i) => {
      const stat = catalog.stats.find((s) => s.statId === entry.statId)
      if (
        !stat ||
        stat.kind !== 'EXPLICIT' ||
        stat.unit !== entry.unit ||
        stat.support.evaluation !== 'SUPPORTED' ||
        (!stat.eligible && !present.has(entry.statId)) ||
        incompatible.has(`/goal/groups/${index}/entries/${i}`)
      )
        rows.push(
          `${i + 1} · ${stat ? presentGoalStat(stat, catalog, locale).label : entry.statId}`,
        )
    })
  })
  if (rows.length) throw new UnavailablePresetRows(rows)
  if (issues.length) throw new Error('Incompatible goal')
  return { ...goal, catalogVersion: catalog.catalogVersion }
}
