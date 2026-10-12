import { rulesetHeaders, verifyRulesetResponse } from '../rulesetIdentity'
import type { CreateRequest, Fraction } from '../path-tree/types'
import type { Graph, Snapshot } from './types'
const root = '/api/v2/crafting/method-searches'
export class MethodSearchError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
  ) {
    super(`METHOD_SEARCH_${status}:${code}`)
  }
}
function probability(p: Fraction) {
  if (
    !p ||
    typeof p.numerator !== 'string' ||
    typeof p.denominator !== 'string' ||
    !/^\d+$/.test(p.numerator) ||
    !/^[1-9]\d*$/.test(p.denominator) ||
    BigInt(p.numerator) > BigInt(p.denominator)
  )
    throw new Error('INVALID_PROBABILITY')
}
export function validateGraph(graph: Graph) {
  if (
    graph.version !== 2 ||
    !Number.isSafeInteger(graph.revision) ||
    !Array.isArray(graph.methods) ||
    !Array.isArray(graph.buckets)
  )
    throw new Error('INVALID_GRAPH')
  const buckets = new Set(graph.buckets.map((b) => b.id))
  if (
    buckets.size !== graph.buckets.length ||
    new Set(graph.methods.map((m) => m.id)).size !== graph.methods.length
  )
    throw new Error('DUPLICATE_ID')
  for (const method of graph.methods) {
    if (!buckets.has(method.sourceBucketId)) throw new Error('MISSING_SOURCE')
    for (const point of method.points) {
      if (![100, 300, 500].includes(point.cycles))
        throw new Error('INVALID_CYCLES')
      let n = 0n,
        d = 1n
      for (const p of [
        point.unresolved,
        ...point.exits.map((e) => e.probability),
      ]) {
        probability(p)
        const pn = BigInt(p.numerator),
          pd = BigInt(p.denominator)
        n = n * pd + pn * d
        d *= pd
        let a = n,
          b = d
        while (b !== 0n) {
          const remainder = a % b
          a = b
          b = remainder
        }
        n /= a
        d /= a
      }
      if (n !== d || point.exits.some((e) => !buckets.has(e.bucketId)))
        throw new Error('INVALID_MASS')
    }
  }
  return graph
}
export function acceptSnapshot(previous: Snapshot | undefined, next: Snapshot) {
  if (
    next.version !== 2 ||
    !Number.isSafeInteger(next.generation) ||
    !Number.isSafeInteger(next.revision) ||
    next.graph.jobId !== next.jobId ||
    next.graph.revision !== next.revision
  )
    throw new Error('INVALID_SNAPSHOT')
  validateGraph(next.graph)
  if (
    previous &&
    (previous.jobId !== next.jobId ||
      previous.clientRequestId !== next.clientRequestId)
  )
    throw new Error('WRONG_JOB')
  return previous &&
    (next.generation < previous.generation || next.revision < previous.revision)
    ? previous
    : next
}
export async function call<T>(
  path: string,
  ruleset: string,
  signal: AbortSignal,
  body?: unknown,
): Promise<T> {
  const init: RequestInit = {
    signal,
    ...(body === undefined
      ? {}
      : {
          method: 'POST',
          headers: rulesetHeaders(ruleset),
          body: JSON.stringify(body),
        }),
  }
  let response: Response
  try {
    response = await fetch(root + path, init)
  } catch (error) {
    if (signal.aborted || !(error instanceof TypeError)) throw error
    response = await fetch(root + path, init)
  }
  if (!response.ok) {
    const body = (await response.json().catch(() => ({}))) as { code?: string }
    throw new MethodSearchError(response.status, body.code ?? 'UNKNOWN')
  }
  verifyRulesetResponse(response, ruleset)
  return (await response.json()) as T
}
export function createBody(request: CreateRequest) {
  return {
    version: 2,
    clientRequestId: request.clientRequestId,
    start: request.start,
    goal: request.goal,
    activeOmens: request.activeOmens,
  }
}

export async function mutateJob(
  current: Snapshot,
  operation: 'CANCEL' | 'RESUME',
  ruleset: string,
  signal: AbortSignal,
): Promise<Snapshot> {
  const generation = current.generation
  for (let attempt = 0; ; attempt++) {
    try {
      return acceptSnapshot(
        current,
        await call<Snapshot>(
          `/${current.jobId}/${operation.toLowerCase()}`,
          ruleset,
          signal,
          {
            version: 2,
            commandId: crypto.randomUUID(),
            expectedRevision: current.revision,
            operation,
            retainedOutcomeLimit: null,
          },
        ),
      )
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
        await call<Snapshot>(`/${current.jobId}`, ruleset, signal),
      )
      // Another control already changed this generation. Do not undo it.
      if (
        current.generation !== generation ||
        (operation === 'CANCEL' &&
          !['QUEUED', 'RUNNING', 'PAUSED'].includes(current.status))
      )
        return current
    }
  }
}
