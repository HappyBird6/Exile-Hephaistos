import { afterEach, expect, it, vi } from 'vitest'
import fixture from '../../../../../contracts/crafting-methods-v2/certified-diamond.json'
import { requestFixture } from '../path-tree/fixtures.test-support'
import { acceptSnapshot } from './api'
import {
  fetchSnapshot,
  loadDistribution,
  loadGraph,
  mutateCertifiedJob,
  normalizeView,
  validateBounds,
} from './certifiedApi'
import type { MapView } from './certifiedApi'
import type { Bounds } from './types'
import { probabilityText, quantityText } from './probabilityPresentation'

afterEach(() => vi.unstubAllGlobals())
const clone = () => structuredClone(fixture) as MapView
const response = (body: unknown) =>
  new Response(JSON.stringify(body), {
    headers: { 'X-Crafting-Ruleset': 'rules' },
  })
const interval = (
  lower: number,
  upper: number,
  denominator: number,
): Bounds => ({
  lower: { numerator: String(lower), denominator: String(denominator) },
  upper: { numerator: String(upper), denominator: String(denominator) },
  arithmeticWidth: {
    numerator: String(upper - lower),
    denominator: String(denominator),
  },
  exact: lower === upper,
  confirmedPercent: null,
})

it('preserves enclosing mass and distinguishes quantity bounds from probabilities', () => {
  const value = clone(),
    point = value.graph.methods[0]!.points[0]!
  point.exits.forEach((exit) => {
    exit.probability = interval(49, 51, 100)
  })
  point.cycleUses.EXALTED = interval(199, 201, 100)
  const normalized = normalizeView(value).graph.methods[0]!.points[0]!
  expect(normalized.exits).toHaveLength(2)
  expect(
    probabilityText(
      normalized.exits[0]!.probability,
      normalized.exits[0]!.bounds,
    ),
  ).toBe('49.00% – 51.00%')
  expect(
    quantityText(
      normalized.cycleUses.EXALTED!,
      normalized.cycleUseBounds!.EXALTED,
    ),
  ).toBe('1.99 – 2.01')
  expect(() => validateBounds(interval(199, 201, 100))).toThrow(
    'INVALID_BOUNDS',
  )
  point.exits.forEach((exit) => {
    exit.probability = interval(30, 40, 100)
  })
  expect(() => normalizeView(value)).toThrow('INVALID_MASS')
})

it('keeps unresolved upper mass even when its lower bound is zero', () => {
  const value = clone(),
    point = value.graph.methods[0]!.points[0]!
  point.exits[1]!.probability = interval(40, 50, 100)
  point.unresolved = interval(0, 10, 100)
  const normalized = normalizeView(value).graph.methods[0]!.points[0]!
  expect(normalized.unresolved.numerator).toBe('0')
  expect(normalized.unresolvedBounds?.upper.numerator).toBe('10')
  expect(
    probabilityText(normalized.unresolved, normalized.unresolvedBounds),
  ).toBe('0.00% – 10.00%')
  const malformed = interval(1, 2, 10)
  malformed.arithmeticWidth = { numerator: '0', denominator: '1' }
  expect(() => validateBounds(malformed)).toThrow('INVALID_BOUNDS')
})

it('rejects duplicates and wrong jobs while retaining newer generations', () => {
  const current = normalizeView(clone()),
    stale = clone()
  current.generation = 2
  expect(acceptSnapshot(current, normalizeView(stale))).toBe(current)
  stale.graph.methods.push(stale.graph.methods[0]!)
  expect(() => normalizeView(stale)).toThrow('DUPLICATE_ID')
  const wrong = clone()
  wrong.graph.jobId = 'different'
  expect(() => normalizeView(wrong)).toThrow('INVALID_SNAPSHOT')
})

it('creates a certified job with the original item and complete goal then reads an atomic view', async () => {
  const request = requestFixture(),
    value = clone()
  request.clientRequestId = value.search.clientRequestId
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response({ search: { jobId: value.search.jobId } }))
    .mockResolvedValueOnce(response(value))
  vi.stubGlobal('fetch', fetcher)
  const result = await fetchSnapshot(
    request,
    undefined,
    'rules',
    new AbortController().signal,
  )
  expect(result.graph.methods).toHaveLength(6)
  expect(fetcher.mock.calls[0]![0]).toBe(
    '/api/v2/crafting/method-searches/certified',
  )
  const body = JSON.parse(fetcher.mock.calls[0]![1].body)
  expect(body.version).toBe(1)
  expect(body.request.version).toBe(2)
  expect(body.request.start).toEqual(request.start)
  expect(body.request.goal).toEqual(request.goal)
  expect(body.precision).toEqual({ fractionalBits: 64, percentDecimals: 2 })
  expect(fetcher.mock.calls[1]![0]).toContain('/view')
})

it('retries a revision conflict without replaying a control from a superseded generation', async () => {
  const current = normalizeView(clone()),
    newer = clone()
  newer.search.generation++
  newer.search.revision++
  newer.graph.revision++
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      new Response(JSON.stringify({ code: 'REVISION_CONFLICT' }), {
        status: 409,
      }),
    )
    .mockResolvedValueOnce(response(newer))
  vi.stubGlobal('fetch', fetcher)
  const result = await mutateCertifiedJob(
    current,
    'RESUME',
    'rules',
    new AbortController().signal,
  )
  expect(result.generation).toBe(newer.search.generation)
  expect(fetcher).toHaveBeenCalledTimes(2)
  expect(fetcher.mock.calls[0]![0]).toContain('/control')
  expect(JSON.parse(fetcher.mock.calls[0]![1].body).retainedOutcomeLimit).toBe(
    1_000_000,
  )
})

it('rejects stale map and class distribution pages', async () => {
  const current = normalizeView(clone()),
    stale = clone().graph
  stale.revision++
  vi.stubGlobal(
    'fetch',
    vi
      .fn()
      .mockResolvedValueOnce(response(stale))
      .mockResolvedValueOnce(
        response({
          version: fixture.graph.version,
          jobId: current.jobId,
          revision: current.revision + 1,
          methodId: 'direct',
          cycles: 100,
          states: [],
          nextOffset: null,
        }),
      ),
  )
  const signal = new AbortController().signal
  await expect(loadGraph(current, '12', 'rules', signal)).rejects.toThrow(
    'STALE_GRAPH',
  )
  await expect(
    loadDistribution(current, 'direct', 100, null, 'rules', signal),
  ).rejects.toThrow('STALE_DISTRIBUTION')
})
