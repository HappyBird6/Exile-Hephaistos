import type { Fraction, Item } from '../path-tree/types'
export type RollBands = Record<
  string,
  { stat: string; min: number; max: number }
>
export type Exit = {
  bucketId: string
  kind: string
  probability: Fraction
  states: number
}
export type Point = {
  cycles: number
  exits: Exit[]
  unresolved: Fraction
  initialUses: Record<string, Fraction>
  cycleUses: Record<string, Fraction>
}
export type Arrow = {
  id: string
  sourceStateId: string
  sourceBucketId: string
  sourceItem: Item
  sourceRolls?: RollBands
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
  states: { stateId: string; item: Item; kind: string; probability: Fraction }[]
  unresolved: Fraction
  nextCursor: string | null
}
