import { validateSearchResources } from './searchResourcesValidation'
import { assertShape } from '../path-tree/validation'
import type { Item, CreateRequest } from '../path-tree/types'
import type { Bounds, RollBands } from './types'
import { rulesetHeaders, verifyRulesetResponse } from '../rulesetIdentity'
export type AutomaticAction = {
  currency: string
  omens: string[]
  purpose: string
}
export type AutomaticNode = {
  rolls?: RollBands
  quality40: boolean
  id: number
  depth: number
  item: Item
  progress: { goal: string; entries: string[] }
  discoveryFinished: boolean
  directFinished: boolean
}
export type AutomaticArrow = {
  executionMode?: 'SINGLE_PASS' | 'REPEATABLE' | 'PENDING'
  id: string
  source: number
  initial: AutomaticAction[]
  cycle: AutomaticAction[]
  reasons: string[]
  finished: boolean
  points: {
    cycles: number
    complete: boolean
    outcomes: { target: number | null; kind: string; probability: Bounds }[]
    initialUses: Record<string, Bounds>
    cycleUses: Record<string, Bounds>
  }[]
}
export type AutomaticView = {
  version: string
  id: string
  revision: number
  generation: number
  status: 'READY' | 'PAUSED' | 'CANCELLED' | 'PARTIAL' | 'COMPLETE'
  reason: string
  complete: boolean
  canResume?: boolean
  operations: number
  retainedStates: number
  resources?: {
    computationStates: number
    stateLimit: number
    methodExecutions: number
    methodLimit: number
    concreteStates: number
    concreteMethods: number
    retainedRecords: number
    retainedLimit: number
    primitiveOutcomeBudget: number
    blockers: string[]
  }
  nodes: AutomaticNode[]
  methods: AutomaticArrow[]
  recommendations: {
    node: number
    cycles: number
    local: string[]
    strategy: Bounds
    strategyStatus: string
    strategyMethods: string[]
  }[]
}
const root = '/api/v2/crafting/method-searches/automatic'
export class AutomaticError extends Error {
  constructor(
    readonly status: number,
    readonly code = '',
    readonly retryAt: number | null = null,
  ) {
    super('Automatic search request failed')
  }
}
function validate(raw: unknown): AutomaticView {
  const v = raw as AutomaticView
  if (
    !v ||
    v.version !== 'automatic-method-job-v1' ||
    !/^[0-9a-f-]{36}$/.test(v.id) ||
    !Number.isSafeInteger(v.revision) ||
    v.revision < 1 ||
    !Number.isSafeInteger(v.generation) ||
    v.generation < 1 ||
    !['READY', 'PAUSED', 'CANCELLED', 'PARTIAL', 'COMPLETE'].includes(
      v.status,
    ) ||
    !Array.isArray(v.nodes) ||
    v.nodes.length > 50000 ||
    !Array.isArray(v.methods) ||
    v.methods.length > (v.resources ? 50000 : 256) ||
    !Array.isArray(v.recommendations)
  )
    throw new Error('Invalid automatic graph')
  if (v.resources !== undefined)
    validateSearchResources(v.resources, v.nodes.length, v.methods.length)
  if (
    !v.resources &&
    v.nodes.filter((node) => node.progress?.goal !== 'MATCH').length > 128
  )
    throw new Error('Invalid automatic graph')
  const ids = new Set<number>()
  for (const n of v.nodes) {
    assertShape('item', n.item)
    if (
      ids.has(n.id) ||
      !Number.isSafeInteger(n.id) ||
      !Number.isSafeInteger(n.depth) ||
      n.depth < 0
    )
      throw new Error('Invalid node')
    ids.add(n.id)
  }
  const bound = (b: Bounds) => {
    for (const f of [b.lower, b.upper]) {
      if (
        !/^\d+$/.test(f.numerator) ||
        !/^\d+$/.test(f.denominator) ||
        BigInt(f.denominator) === 0n
      )
        throw new Error('Invalid probability')
    }
  }
  for (const m of v.methods) {
    if (!ids.has(m.source)) throw new Error('Invalid source')
    if (
      m.executionMode !== undefined &&
      !['SINGLE_PASS', 'REPEATABLE', 'PENDING'].includes(m.executionMode)
    )
      throw new Error('Invalid execution mode')
    for (const p of m.points) {
      if (![1, 100, 300, 500].includes(p.cycles))
        throw new Error('Invalid cycle count')
      const outcomeKeys = new Set<string>()
      for (const o of p.outcomes) {
        const key = `${o.target}:${o.kind}`
        if (outcomeKeys.has(key)) throw new Error('Duplicate method outcome')
        outcomeKeys.add(key)
        if (o.target !== null && !ids.has(o.target))
          throw new Error('Invalid target')
        bound(o.probability)
      }
    }
  }
  for (const r of v.recommendations) bound(r.strategy)
  return v
}
async function request(
  path: string,
  identity: string,
  body?: unknown,
  signal?: AbortSignal,
) {
  const response = await fetch(`${root}${path}`, {
    method: body === undefined ? 'GET' : 'POST',
    credentials: 'same-origin',
    ...(signal ? { signal } : {}),
    ...(body === undefined
      ? {}
      : {
          headers: rulesetHeaders(identity),
          body: JSON.stringify(body),
        }),
  })
  if (!response.ok) {
    let code = ''
    try {
      const body = (await response.json()) as { code?: unknown }
      if (
        body.code === 'JOB_CAPACITY' ||
        body.code === 'JOB_STORAGE_CAPACITY' ||
        body.code === 'GOAL_EXCEEDS_FOUR_EXPLICIT_SCOPE'
      )
        code = body.code
    } catch {
      /* Error bodies may be unavailable. */
    }
    const raw = response.headers.get('Retry-After')
    const seconds = raw && /^\d+$/.test(raw) ? Number(raw) : NaN
    const retryAt =
      Number.isSafeInteger(seconds) && seconds > 0 && seconds <= 86401
        ? Date.now() + seconds * 1000
        : null
    throw new AutomaticError(response.status, code, retryAt)
  }
  verifyRulesetResponse(response, identity)
  return validate(await response.json())
}
const pendingCreates = new Map<string, Promise<AutomaticView>>()
let createQueue: Promise<void> = Promise.resolve()
export const automaticApi = {
  create: (
    input: CreateRequest,
    signal?: AbortSignal,
    abstractQuality40 = false,
  ) => {
    if (signal?.aborted)
      return Promise.reject(new DOMException('Aborted', 'AbortError'))
    const key = JSON.stringify([input, abstractQuality40])
    const pending = pendingCreates.get(key)
    if (pending) return pending
    // A dispatched POST can succeed after a local abort. Keep its result and let
    // the owning component cancel the newly created job when it has unmounted.
    const created = createQueue
      .then(() =>
        request(
          '',
          input.start.provenance.rulesetIdentity,
          {
            abstractQuality40,
            request: {
              version: 2,
              clientRequestId: input.clientRequestId,
              start: input.start,
              goal: input.goal,
              activeOmens: [],
            },
          },
          undefined,
        ),
      )
      .finally(() => pendingCreates.delete(key))
    createQueue = created.then(
      () => undefined,
      () => undefined,
    )
    pendingCreates.set(key, created)
    return created
  },
  get: (id: string, identity: string, signal?: AbortSignal) =>
    request(`/${id}`, identity, undefined, signal),
  control: (
    view: AutomaticView,
    operation: 'ADVANCE' | 'RESUME' | 'CANCEL',
    identity: string,
    signal?: AbortSignal,
  ) =>
    request(
      `/${view.id}/control`,
      identity,
      {
        commandId: crypto.randomUUID(),
        revision: view.revision,
        generation: view.generation,
        operation,
      },
      signal,
    ),
}
export function acceptsAutomatic(
  current: AutomaticView | null,
  next: AutomaticView,
) {
  return (
    !current ||
    (current.id === next.id &&
      next.revision >= current.revision &&
      next.generation >= current.generation)
  )
}
export async function cancelAutomatic(id: string, identity: string) {
  for (let i = 0; i < 2; i++)
    try {
      const current = await automaticApi.get(id, identity)
      if (current.status === 'CANCELLED') return current
      return await automaticApi.control(current, 'CANCEL', identity)
    } catch (error) {
      if (!(error instanceof AutomaticError) || error.status !== 409) return
    }
}
