export const groupTypes = [
  'AND',
  'NOT',
  'IF',
  'COUNT',
  'WEIGHTED_V1',
  'WEIGHTED_V2',
] as const
export type GroupType = (typeof groupTypes)[number]
export type Support = 'SUPPORTED' | 'UNKNOWN' | 'UNSUPPORTED'
export type Range = { min: number | null; max: number | null }
export type Context = {
  snapshotId: string
  baseItemId: string
  itemLevel: number
}
export type Entry = {
  id: string
  statId: string
  unit: string
  range: Range
  weight: number | null
  disabled: boolean
}
export type Group = {
  id: string
  type: GroupType
  disabled: boolean
  range: Range | null
  entries: Entry[]
}
export type GoalFilter = {
  version: 1
  catalogVersion: string
  general: { baseItemId: string; itemLevel: Range; rarities: string[] }
  groups: Group[]
}
export type Issue = {
  code: string
  path: string
  message: string
  severity: 'ERROR' | 'WARNING'
}
export type Stat = {
  statId: string
  label: string
  unit: string
  kind: 'EXPLICIT' | 'IMPLICIT' | 'PSEUDO'
  support: {
    evaluation: Support
    probability: Support
    reasonCode: string | null
  }
  eligible: boolean
  eligibilityReason: string | null
  sourceStatIds: string[]
  contributions: { statId: string; coefficient: number }[]
  sourceUrls: string[]
}
export type Catalog = {
  version: 1
  catalogVersion: string
  context: Context
  groupTypes: {
    type: GroupType
    evaluation: Support
    probability: Support
    reasonCode: string | null
  }[]
  stats: Stat[]
  issues: Issue[]
}
export type Validation = {
  version: 1
  valid: boolean
  issues: Issue[]
  capabilities: { evaluation: Support; probability: Support }
}
export type Recommendation = {
  version: 1
  catalogVersion: string
  evaluation: string
  probability: {
    status: 'COMPLETE' | 'PARTIAL' | 'UNKNOWN' | 'UNSUPPORTED'
    reasonCode: string | null
    modelVersion: string | null
    ledgerVersion: string | null
  }
  comparisons: {
    sequence: string[]
    successLower: number
    successUpper: number
    failureProbability: number
    unresolvedProbability: number
    complete: boolean
  }[]
  rankingCertified: boolean
  comparedSequences: number
  totalSequences: number | null
}
export interface GoalFilterAdapter {
  readonly id: string
  readonly mock?: boolean
  catalog(context: Context, signal: AbortSignal): Promise<Catalog>
  validate(
    context: Context,
    goal: GoalFilter,
    signal: AbortSignal,
  ): Promise<Validation>
}
export const weighted = (type: GroupType) =>
  type === 'WEIGHTED_V1' || type === 'WEIGHTED_V2'
