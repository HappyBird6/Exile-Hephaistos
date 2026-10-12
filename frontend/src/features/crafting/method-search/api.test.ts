import { afterEach, describe, expect, it, vi } from 'vitest'
import { acceptSnapshot, createBody, mutateJob, validateGraph } from './api'
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
  afterEach(() => vi.unstubAllGlobals())
  it('bounds revision retries and does not retry other conflicts', async () => {
    const conflict = (code: string) =>
      new Response(JSON.stringify({ code }), { status: 409 })
    const fetcher = vi.fn().mockImplementation((_url, init) =>
      Promise.resolve(
        init.method === 'POST'
          ? conflict('REVISION_CONFLICT')
          : new Response(JSON.stringify(snapshot()), {
              headers: { 'X-Crafting-Ruleset': 'rules' },
            }),
      ),
    )
    vi.stubGlobal('fetch', fetcher)
    await expect(
      mutateJob(snapshot(), 'CANCEL', 'rules', new AbortController().signal),
    ).rejects.toThrow('REVISION_CONFLICT')
    expect(fetcher).toHaveBeenCalledTimes(7)
    fetcher.mockReset().mockResolvedValue(conflict('COMMAND_ID_REUSED'))
    await expect(
      mutateJob(snapshot(), 'CANCEL', 'rules', new AbortController().signal),
    ).rejects.toThrow('COMMAND_ID_REUSED')
    expect(fetcher).toHaveBeenCalledTimes(1)
  })
  it('refreshes a revision conflict and retains command identity on a transport retry', async () => {
    const fresh = {
      ...snapshot(),
      revision: 2,
      graph: { ...graph(), revision: 2 },
    }
    const cancelled = { ...fresh, generation: 2, status: 'CANCELLED' as const }
    const response = (body: unknown, status = 200) =>
      new Response(JSON.stringify(body), {
        status,
        headers: { 'X-Crafting-Ruleset': 'rules' },
      })
    const fetcher = vi
      .fn()
      .mockResolvedValueOnce(response({ code: 'REVISION_CONFLICT' }, 409))
      .mockResolvedValueOnce(response(fresh))
      .mockRejectedValueOnce(new TypeError('network'))
      .mockResolvedValueOnce(response(cancelled))
    vi.stubGlobal('fetch', fetcher)
    expect(
      await mutateJob(
        snapshot(),
        'CANCEL',
        'rules',
        new AbortController().signal,
      ),
    ).toEqual(cancelled)
    const calls = fetcher.mock.calls
    expect(JSON.parse(calls[2]![1].body).expectedRevision).toBe(2)
    expect(calls[2]![1].body).toBe(calls[3]![1].body)
    expect(JSON.parse(calls[0]![1].body).commandId).not.toBe(
      JSON.parse(calls[2]![1].body).commandId,
    )
  })
  it('does not override a newer control generation', async () => {
    const newer = {
      ...snapshot(),
      generation: 2,
      revision: 2,
      graph: { ...graph(), revision: 2 },
    }
    const fetcher = vi
      .fn()
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ code: 'REVISION_CONFLICT' }), {
          status: 409,
        }),
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify(newer), {
          headers: { 'X-Crafting-Ruleset': 'rules' },
        }),
      )
    vi.stubGlobal('fetch', fetcher)
    expect(
      await mutateJob(
        snapshot(),
        'CANCEL',
        'rules',
        new AbortController().signal,
      ),
    ).toEqual(newer)
    expect(fetcher).toHaveBeenCalledTimes(2)
  })
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
