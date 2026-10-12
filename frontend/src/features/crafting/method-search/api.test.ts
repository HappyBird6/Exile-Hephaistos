import { describe, expect, it } from 'vitest'
import { acceptSnapshot, createBody, validateGraph } from './api'
import type { Graph, Snapshot } from './types'
import { requestFixture } from '../path-tree/fixtures.test-support'

const item = requestFixture().start.item
const one = { numerator: '1', denominator: '1' },
  zero = { numerator: '0', denominator: '1' }
function graph(): Graph {
  return {
    version: 2,
    jobId: 'job',
    revision: 1,
    buckets: [
      {
        id: 'root',
        depth: 0,
        goal: 'NO_MATCH',
        entries: ['NO_MATCH'],
        states: 1,
        representative: item,
        directComplete: false,
      },
      {
        id: 'goal',
        depth: 1,
        goal: 'MATCH',
        entries: ['MATCH'],
        states: 1,
        representative: item,
        directComplete: true,
      },
    ],
    methods: ['one', 'two'].map((id) => ({
      id,
      sourceStateId: 'full-state',
      sourceBucketId: 'root',
      sourceItem: item,
      initialActions: [],
      cycleActions: ['EXALTED'],
      status: 'COMPLETE',
      reason: null,
      points: [100, 300, 500].map((cycles) => ({
        cycles,
        exits: [
          { bucketId: 'goal', kind: 'GOAL', probability: one, states: 1 },
        ],
        unresolved: zero,
        initialUses: {},
        cycleUses: { EXALTED: one },
      })),
    })),
    methodCount: 2,
    nextCursor: null,
  }
}
function snapshot(): Snapshot {
  return {
    version: 2,
    jobId: 'job',
    clientRequestId: 'request',
    revision: 1,
    generation: 1,
    status: 'PAUSED',
    reason: 'COMPUTE_BUDGET',
    resumable: true,
    enumerationComplete: false,
    rootStateId: 'full-state',
    localRecommendations: [],
    strategies: [],
    graph: graph(),
  }
}
describe('method search v2 contract', () => {
  it('changes repetition contract explicitly and retains parallel alternatives', () => {
    const body = createBody(requestFixture())
    expect(body.version).toBe(2)
    expect(body).not.toHaveProperty('observations')
    expect(validateGraph(graph()).methods).toHaveLength(2)
  })
  it('rejects missing outcome mass and preserves old data on stale revisions or generations', () => {
    const bad = graph()
    bad.methods[0]!.points[0]!.exits[0]!.probability = {
      numerator: '1',
      denominator: '2',
    }
    expect(() => validateGraph(bad)).toThrow('INVALID_MASS')
    const current = snapshot(),
      stale = { ...snapshot(), generation: 0 }
    expect(acceptSnapshot(current, stale)).toBe(current)
    expect(
      acceptSnapshot(current, {
        ...snapshot(),
        revision: 0,
        graph: { ...graph(), revision: 0 },
      }),
    ).toBe(current)
    expect(() =>
      acceptSnapshot(current, { ...snapshot(), jobId: 'other' }),
    ).toThrow()
  })
  it('does not turn unknown mass into failure probability', () => {
    const value = graph()
    value.methods[0]!.points[0]!.exits = []
    value.methods[0]!.points[0]!.unresolved = one
    expect(validateGraph(value).methods[0]!.points[0]!.unresolved).toEqual(one)
  })
})
