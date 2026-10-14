import type { Fraction, Item } from '../path-tree/types'
export type Bounds = {
  lower: Fraction
  upper: Fraction
  arithmeticWidth: Fraction
  exact: boolean
  confirmedPercent: string | null
}
export type RollBands = Record<
  string,
  { stat: string; min: number; max: number }
>
export type TierVariants = Record<
  string,
  { modifierId: string; stat: string; min: number; max: number }[]
>
export type OrbitViews = Record<
  string,
  { representativeIds: string[]; assignments: TierVariants[string][][] }
>
export type Exit = {
  bucketId: string
  kind: string
  probability: Fraction
  bounds?: Bounds
  states?: number
}
export type Point = {
  cycles: number
  exits: Exit[]
  unresolved: Fraction
  unresolvedBounds?: Bounds
  initialUses: Record<string, Fraction>
  cycleUses: Record<string, Fraction>
  initialUseBounds?: Record<string, Bounds>
  cycleUseBounds?: Record<string, Bounds>
}
export type Arrow = {
  id: string
  sourceStateId: string
  sourceBucketId: string
  sourceItem: Item
  sourceRolls?: RollBands
  sourceTiers?: TierVariants
  sourceOrbits?: OrbitViews
  initialActions: string[]
  cycleActions: string[]
  status: string
  reason: string | null
  points: Point[]
}
export type Bucket = {
  id: string
  depth: number
  goal: string
  entries: string[]
  states: number
  representative: Item
  representativeRolls?: RollBands
  representativeTiers?: TierVariants
  representativeOrbits?: OrbitViews
  directComplete: boolean
}
export type Graph = {
  version: 2
  jobId: string
  revision: number
  buckets: Bucket[]
  methods: Arrow[]
  methodCount: number
  nextCursor: string | null
  localRecommendations?: {
    stateId: string
    cycles: number
    methodIds: string[]
  }[]
}
export type Snapshot = {
  version: 2
  jobId: string
  clientRequestId: string
  revision: number
  generation: number
  status: string
  reason: string | null
  resumable: boolean
  enumerationComplete: boolean
  rootStateId: string
  localRecommendations: {
    stateId: string
    cycles: number
    methodIds: string[]
  }[]
  strategies: {
    stateId: string
    cycles: number
    status: string
    lower: Fraction
    upper: Fraction
    methodIds: string[]
  }[]
  graph: Graph
}
export type Distribution = {
  version: 2
  jobId: string
  revision: number
  methodId: string
  cycles: number
  states: {
    stateId: string
    item: Item
    kind: string
    probability: Fraction
    bounds?: Bounds
    rolls?: RollBands
    tiers?: TierVariants
    orbits?: OrbitViews
  }[]
  unresolved?: Fraction
  nextCursor: string | null
}
