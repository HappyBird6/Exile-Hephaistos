import type { CreateRequest, Fraction } from '../path-tree/types'
import {
  acceptSnapshot,
  call,
  createBody,
  MethodSearchError,
  validateGraph,
} from './api'
import type {
  Arrow,
  Bounds,
  Bucket,
  Distribution,
  Graph,
  Point,
  Snapshot,
} from './types'

export const MAP_VERSION = 'certified-bucket-map-v1'
type Local = {
  stateId: string
  cycles: number
  target: { goal: string; entries: string[] } | null
  status: string
  methodIds: string[]
}
type WirePoint = {
  cycles: number
  exits: { bucketId: string; kind: string; probability: Bounds }[]
  unresolved: Bounds
  initialUses: Record<string, Bounds>
  cycleUses: Record<string, Bounds>
}
type WireArrow = Omit<Arrow, 'points' | 'reason'> & { points: WirePoint[] }
export type MapPage = {
  version: string
  jobId: string
  revision: number
  buckets: Bucket[]
  methods: WireArrow[]
  localRecommendations: Local[]
  methodCount: number
  nextOffset: number | null
}
export type MapView = {
  search: Omit<Snapshot, 'version' | 'graph' | 'localRecommendations'> & {
    version: string
    strategyStatus: string
    localRecommendations: Local[]
  }
  graph: MapPage
}

function fraction(value: Fraction) {
  if (
    !value ||
    typeof value.numerator !== 'string' ||
    typeof value.denominator !== 'string' ||
    !/^\d+$/.test(value.numerator) ||
    !/^[1-9]\d*$/.test(value.denominator)
  )
    throw new Error('INVALID_BOUNDS')
}
export function validateBounds(value: Bounds, probability = true): Bounds {
  if (!value) throw new Error('INVALID_BOUNDS')
  for (const v of [value.lower, value.upper, value.arithmeticWidth]) fraction(v)
  const ln = BigInt(value.lower.numerator),
    ld = BigInt(value.lower.denominator),
    un = BigInt(value.upper.numerator),
    ud = BigInt(value.upper.denominator)
  const difference = un * ld - ln * ud
  if (
    difference < 0n ||
    (probability && un > ud) ||
    value.exact !== (difference === 0n) ||
    BigInt(value.arithmeticWidth.numerator) * ld * ud !==
      difference * BigInt(value.arithmeticWidth.denominator)
  )
    throw new Error('INVALID_BOUNDS')
  return value
}
function quantities(values: Record<string, Bounds>) {
  return Object.fromEntries(
    Object.entries(values).map(([key, value]) => [
      key,
      validateBounds(value, false).lower,
    ]),
  )
}
export function normalizeGraph(page: MapPage): Graph {
  if (
    page.version !== MAP_VERSION ||
    !Number.isSafeInteger(page.methodCount) ||
    (page.nextOffset !== null &&
      (!Number.isSafeInteger(page.nextOffset) || page.nextOffset < 0))
  )
    throw new Error('INVALID_GRAPH')
  const graph = validateGraph({
    version: 2,
    jobId: page.jobId,
    revision: page.revision,
    buckets: page.buckets,
    methodCount: page.methodCount,
    nextCursor: page.nextOffset?.toString() ?? null,
    methods: page.methods.map((method) => ({
      ...method,
      reason: null,
      points: method.points.map((point): Point => ({
        cycles: point.cycles,
        exits: point.exits.map((exit) => ({
          ...exit,
          probability: validateBounds(exit.probability).lower,
          bounds: exit.probability,
        })),
        unresolved: validateBounds(point.unresolved).lower,
        unresolvedBounds: point.unresolved,
        initialUses: quantities(point.initialUses),
        cycleUses: quantities(point.cycleUses),
        initialUseBounds: point.initialUses,
        cycleUseBounds: point.cycleUses,
      })),
    })),
  })
  return {
    ...graph,
    localRecommendations: recommendations(page.localRecommendations, graph),
  }
}
function recommendations(locals: Local[], graph: Graph) {
  const selected = locals.filter(
    (local) =>
      local.status === 'CERTIFIED' &&
      (local.target !== null ||
        graph.methods.some(
          (method) =>
            local.methodIds.includes(method.id) &&
            method.points.some(
              (point) =>
                point.cycles === local.cycles &&
                point.exits.some(
                  (exit) =>
                    exit.kind === 'GOAL' &&
                    BigInt((exit.bounds?.upper ?? exit.probability).numerator) >
                      0n,
                ),
            ),
        )),
  )
  const grouped = new Map<
    string,
    { stateId: string; cycles: number; methodIds: Set<string> }
  >()
  for (const local of selected) {
    const key = JSON.stringify([local.stateId, local.cycles])
    const group = grouped.get(key) ?? {
      stateId: local.stateId,
      cycles: local.cycles,
      methodIds: new Set<string>(),
    }
    local.methodIds.forEach((id) => group.methodIds.add(id))
    grouped.set(key, group)
  }
  return [...grouped.values()].map((group) => ({
    ...group,
    methodIds: [...group.methodIds],
  }))
}
export function normalizeView(view: MapView): Snapshot {
  if (view.search.version !== MAP_VERSION) throw new Error('INVALID_SNAPSHOT')
  const graph = normalizeGraph(view.graph)
  return acceptSnapshot(undefined, {
    ...view.search,
    version: 2,
    graph,
    localRecommendations: recommendations(
      [...view.search.localRecommendations, ...view.graph.localRecommendations],
      graph,
    ),
  })
}
async function view(jobId: string, ruleset: string, signal: AbortSignal) {
  const next = normalizeView(
    await call<MapView>(`/certified/${jobId}/view`, ruleset, signal),
  )
  if (next.jobId !== jobId) throw new Error('WRONG_JOB')
  return next
}
export async function fetchSnapshot(
  request: CreateRequest,
  current: Snapshot | undefined,
  ruleset: string,
  signal: AbortSignal,
) {
  let jobId = current?.jobId
  if (!jobId) {
    const created = await call<{ search: { jobId: string } }>(
      '/certified',
      ruleset,
      signal,
      {
        version: 1,
        request: createBody(request),
        precision: { fractionalBits: 64, percentDecimals: 2 },
      },
    )
    jobId = created.search?.jobId
    if (typeof jobId !== 'string' || jobId.length === 0)
      throw new Error('INVALID_SNAPSHOT')
  }
  const next = await view(jobId, ruleset, signal)
  if (next.clientRequestId !== request.clientRequestId)
    throw new Error('WRONG_REQUEST')
  return acceptSnapshot(current, next)
}
export async function loadGraph(
  current: Snapshot,
  cursor: string,
  ruleset: string,
  signal: AbortSignal,
) {
  if (!/^\d+$/.test(cursor)) throw new Error('INVALID_CURSOR')
  const page = await call<MapPage>(
    `/certified/${current.jobId}/map?revision=${current.revision}&offset=${cursor}`,
    ruleset,
    signal,
  )
  if (page.jobId !== current.jobId || page.revision !== current.revision)
    throw new Error('STALE_GRAPH')
  return normalizeGraph(page)
}
export async function loadDistribution(
  current: Snapshot,
  methodId: string,
  cycles: number,
  cursor: string | null,
  ruleset: string,
  signal: AbortSignal,
): Promise<Distribution> {
  if (cursor !== null && !/^\d+$/.test(cursor))
    throw new Error('INVALID_CURSOR')
  const params = new URLSearchParams({
    revision: String(current.revision),
    methodId,
    cycles: String(cycles),
    offset: cursor ?? '0',
  })
  const value = await call<{
    version: string
    jobId: string
    revision: number
    methodId: string
    cycles: number
    states: (Omit<Distribution['states'][number], 'probability'> & {
      probability: Bounds
    })[]
    nextOffset: number | null
  }>(`/certified/${current.jobId}/map-outcomes?${params}`, ruleset, signal)
  if (
    value.version !== MAP_VERSION ||
    value.jobId !== current.jobId ||
    value.revision !== current.revision ||
    value.methodId !== methodId ||
    value.cycles !== cycles
  )
    throw new Error('STALE_DISTRIBUTION')
  return {
    ...value,
    version: 2,
    states: value.states.map((state) => ({
      ...state,
      probability: validateBounds(state.probability).lower,
      bounds: state.probability,
    })),
    nextCursor: value.nextOffset?.toString() ?? null,
  }
}
export async function mutateCertifiedJob(
  current: Snapshot,
  operation: 'CANCEL' | 'RESUME',
  ruleset: string,
  signal: AbortSignal,
): Promise<Snapshot> {
  const generation = current.generation
  for (let attempt = 0; ; attempt++) {
    try {
      await call(`/certified/${current.jobId}/control`, ruleset, signal, {
        version: 2,
        commandId: crypto.randomUUID(),
        expectedRevision: current.revision,
        operation,
        retainedOutcomeLimit: operation === 'RESUME' ? 1_000_000 : null,
      })
      return acceptSnapshot(current, await view(current.jobId, ruleset, signal))
    } catch (error) {
      if (
        !(error instanceof MethodSearchError) ||
        error.status !== 409 ||
        error.code !== 'REVISION_CONFLICT' ||
        attempt >= 3
      )
        throw error
      current = acceptSnapshot(
        current,
        await view(current.jobId, ruleset, signal),
      )
      if (
        current.generation !== generation ||
        (operation === 'CANCEL' &&
          !['QUEUED', 'RUNNING', 'PAUSED'].includes(current.status))
      )
        return current
    }
  }
}
