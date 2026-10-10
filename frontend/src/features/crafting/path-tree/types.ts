// Generated from contracts/crafting-paths-v1/schema.json. Do not edit wire types by hand.
export type Fraction = { numerator: string; denominator: string }
export type Range = { min: number | null; max: number | null }
export type Modifier = {
  modifierId: string
  values: Record<string, number>
  fractured: boolean
}
export type Item = {
  snapshotId: string
  baseItemId: string
  itemLevel: number
  rarity: 'NORMAL' | 'MAGIC' | 'RARE' | 'UNIQUE'
  implicits: Array<Modifier>
  explicits: Array<Modifier>
  conditions: Array<'CORRUPTED' | 'MIRRORED' | 'UNIDENTIFIED' | 'SANCTIFIED'>
  augmentSockets: number | null
  catalystQuality: { type: string; amount: number } | null
}
export type TransitionProvenance = {
  rulesetIdentity: string
  catalogDigest: string
  modelVersion: string
  ruleVersion: string
  ledgerVersion: string
  weightPolicy: string
  sourceUrl: string
  retrievedAt: string
  rawSha256: string
  detailsSha256: string
}
export type Start = { item: Item; provenance: TransitionProvenance }
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
  type: 'AND' | 'NOT' | 'IF' | 'COUNT' | 'WEIGHTED_V1' | 'WEIGHTED_V2'
  disabled: boolean
  range: Range | null
  entries: Array<Entry>
}
export type Goal = {
  version: 1
  catalogVersion: string
  general: {
    baseItemId: string
    itemLevel: Range
    rarities: Array<'NORMAL' | 'MAGIC' | 'RARE' | 'UNIQUE'>
  }
  groups: Array<Group>
}
export type CreateRequest = {
  version: 1
  clientRequestId: string
  start: Start
  goal: Goal
  activeOmens: Array<never>
  observations: Array<string>
}
export type Capabilities = {
  evaluation: 'SUPPORTED' | 'UNKNOWN' | 'UNSUPPORTED'
  search: 'SUPPORTED' | 'UNKNOWN' | 'UNSUPPORTED'
  reasonCode: string | null
  actions: Array<string>
  goalGroupTypes: Array<'AND' | 'COUNT'>
  conditionalRecovery: boolean
  resume: boolean
}
export type Provenance = {
  transition: TransitionProvenance
  goalCatalogVersion: string
  predicateVersion: string
  searchVersion: string
  candidateSetVersion: string
  interpretation: 'DECLARED_MODEL_NOT_VERIFIED_GAME_PROBABILITY'
}
export type Point = {
  attempts: string
  lower: Fraction
  upper: Fraction
  active: Fraction
  dead: Fraction
  unresolved: Fraction
  status: 'COMPLETE' | 'PARTIAL' | 'UNKNOWN'
}
export type Policy = {
  id: string
  actions: Array<string>
  mode: 'SINGLE_PASS' | 'REPEAT_CYCLE'
}
export type Recommendation = {
  policy: Policy
  points: Array<Point>
  eventual: {
    status: 'PROVEN' | 'UNKNOWN'
    probability: Fraction | null
    proofVersion: string | null
  }
}
export type Rank = { policyId: string; rank: number }
export type Ranking = {
  attempts: string
  status: 'CERTIFIED_WITHIN_CANDIDATES' | 'PROVISIONAL' | 'UNAVAILABLE'
  entries: Array<Rank>
}
export type Node = {
  id: string
  item: Item
  goalStatus: 'MATCH' | 'NO_MATCH' | 'UNKNOWN' | 'UNSUPPORTED'
}
export type Execution = {
  id: string
  stateId: string
  policyId: string
  phase: number
}
export type Edge = {
  id: string
  from: string
  to: string
  action: string
  probability: Fraction
  kind: 'FORWARD' | 'REPEAT'
}
export type Expansion = {
  executionId: string
  status: 'COMPLETE' | 'PARTIAL' | 'UNSUPPORTED' | 'UNAVAILABLE'
  unresolved: Fraction
}
export type GraphPage = {
  version: 1
  jobId: string
  revision: number
  nodes: Array<Node>
  executions: Array<Execution>
  edges: Array<Edge>
  expansions: Array<Expansion>
  nextCursor: string | null
}
export type Recovery = {
  parentJobId: string
  parentRevision: number
  failureExecutionId: string
  checkpointStateId: string
  conditional: true
  includedInMain: false
}
export type JobSnapshot = {
  version: 1
  jobId: string
  clientRequestId: string
  requestFingerprint: string
  revision: number
  status:
    | 'QUEUED'
    | 'RUNNING'
    | 'PAUSED'
    | 'CANCELLED'
    | 'COMPLETED'
    | 'UNSUPPORTED'
    | 'FAILED'
    | 'EXPIRED'
  expiresAt: string
  capabilities: Capabilities
  provenance: Provenance
  candidateScope: {
    kind: 'FINITE_POLICY_CANDIDATES'
    generated: number
    total: number | null
    enumerationComplete: boolean
  }
  recommendations: Array<Recommendation>
  rankings: Array<Ranking>
  graph: GraphPage
  recovery: Recovery | null
  resumable: boolean
  reasonCode: string | null
}
export type MutationRequest = {
  version: 1
  operation: 'CANCEL' | 'RESUME'
  commandId: string
  expectedRevision: number
}
export type RecoveryRequest = {
  version: 1
  clientRequestId: string
  parentRevision: number
  failureExecutionId: string
  checkpointStateId: string
  observations: Array<string>
}
export type Problem = {
  type: string
  title: string
  status: number
  code: string
}
