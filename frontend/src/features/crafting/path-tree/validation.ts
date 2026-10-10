import schema from './schema.generated.json'
import type {
  Fraction,
  GraphPage,
  JobSnapshot,
  CreateRequest,
  Provenance,
} from './types'

type Schema = {
  $ref?: string
  const?: unknown
  enum?: unknown[]
  anyOf?: Schema[]
  type?: string
  required?: string[]
  properties?: Record<string, Schema>
  additionalProperties?: boolean | Schema
  items?: Schema
  minItems?: number
  maxItems?: number
  uniqueItems?: boolean
  minLength?: number
  pattern?: string
  minimum?: number
  maximum?: number
}
const definitions = schema.definitions as Record<string, Schema>
export class PathSearchError extends Error {
  constructor(
    readonly code: string,
    readonly status = 0,
  ) {
    super(code)
    this.name = 'PathSearchError'
  }
}
function invalid(): never {
  throw new PathSearchError('INVALID_RESPONSE')
}
export function canonical(value: unknown): string {
  if (Array.isArray(value)) return '[' + value.map(canonical).join(',') + ']'
  if (value && typeof value === 'object')
    return (
      '{' +
      Object.entries(value)
        .sort(([a], [b]) => a.localeCompare(b))
        .map(([k, v]) => JSON.stringify(k) + ':' + canonical(v))
        .join(',') +
      '}'
    )
  return JSON.stringify(value)
}
function matches(s: Schema, value: unknown): boolean {
  if (s.$ref) return matches(definitions[s.$ref.split('/').at(-1)!]!, value)
  if ('const' in s) return value === s.const
  if (s.enum) return s.enum.includes(value)
  if (s.anyOf) return s.anyOf.some((x) => matches(x, value))
  if (s.type === 'null') return value === null
  if (s.type === 'string')
    return (
      typeof value === 'string' &&
      value.length >= (s.minLength ?? 0) &&
      (!s.pattern || new RegExp(s.pattern).test(value))
    )
  if (s.type === 'boolean') return typeof value === 'boolean'
  if (s.type === 'number' || s.type === 'integer')
    return (
      typeof value === 'number' &&
      Number.isFinite(value) &&
      (s.type !== 'integer' || Number.isSafeInteger(value)) &&
      value >= (s.minimum ?? -Infinity) &&
      value <= (s.maximum ?? Infinity)
    )
  if (s.type === 'array')
    return (
      Array.isArray(value) &&
      value.length >= (s.minItems ?? 0) &&
      value.length <= (s.maxItems ?? Infinity) &&
      (!s.items || value.every((v) => matches(s.items!, v))) &&
      (!s.uniqueItems || new Set(value.map(canonical)).size === value.length)
    )
  if (s.type === 'object') {
    if (!value || typeof value !== 'object' || Array.isArray(value))
      return false
    const object = value as Record<string, unknown>
    return (
      (s.required ?? []).every((k) => k in object) &&
      Object.entries(object).every(([k, v]) =>
        s.properties?.[k]
          ? matches(s.properties[k], v)
          : s.additionalProperties === false
            ? false
            : typeof s.additionalProperties === 'object'
              ? matches(s.additionalProperties, v)
              : true,
      )
    )
  }
  return false
}
export function assertShape(
  name: keyof typeof schema.definitions,
  value: unknown,
): void {
  if (!matches(definitions[name]!, value)) invalid()
}
export function fraction(value: Fraction): [bigint, bigint] {
  assertShape('fraction', value)
  const n = BigInt(value.numerator),
    d = BigInt(value.denominator)
  if (n > d) invalid()
  return [n, d]
}
export function compare(a: Fraction, b: Fraction): number {
  const [an, ad] = fraction(a),
    [bn, bd] = fraction(b)
  return an * bd < bn * ad ? -1 : an * bd > bn * ad ? 1 : 0
}
function sumEquals(values: Fraction[], expected: Fraction) {
  let n = 0n,
    d = 1n
  for (const v of values) {
    const [vn, vd] = fraction(v)
    n = n * vd + vn * d
    d *= vd
  }
  const [en, ed] = fraction(expected)
  return n * ed === en * d
}
const one = { numerator: '1', denominator: '1' }
export function percent(value: Fraction): string {
  const [n, d] = fraction(value)
  if (n === 0n) return '0%'
  if (n === d) return '100%'
  const scaled = (n * 10000000n) / d
  if (!scaled) return '<0.00001%'
  const tail = (scaled % 100000n).toString().padStart(5, '0').replace(/0+$/, '')
  return `≈${scaled / 100000n}${tail ? '.' + tail : ''}%`
}
export function validateRequest(request: CreateRequest) {
  assertShape('createRequest', request)
  if (request.observations.some((n) => BigInt(n) > 9223372036854775807n))
    invalid()
}
export function validateGraph(page: GraphPage) {
  assertShape('graphPage', page)
  page.edges.forEach((e) => fraction(e.probability))
  page.expansions.forEach((e) => fraction(e.unresolved))
}
export function validateJob(job: JobSnapshot) {
  assertShape('jobSnapshot', job)
  validateGraph(job.graph)
  if (
    job.graph.jobId !== job.jobId ||
    job.graph.revision !== job.revision ||
    !Number.isFinite(Date.parse(job.expiresAt))
  )
    invalid()
  if (job.resumable && !['PAUSED', 'CANCELLED'].includes(job.status)) invalid()
  if (
    job.status === 'UNSUPPORTED' &&
    (job.recommendations.length || job.rankings.length)
  )
    invalid()
  const ids = new Set<string>()
  for (const r of job.recommendations) {
    if (ids.has(r.policy.id)) invalid()
    ids.add(r.policy.id)
    let previous = -1n
    for (const p of r.points) {
      if (BigInt(p.attempts) <= previous) invalid()
      previous = BigInt(p.attempts)
      if (
        !sumEquals([p.lower, p.active, p.dead, p.unresolved], one) ||
        !sumEquals([p.lower, p.unresolved], p.upper)
      )
        invalid()
      const expected =
        BigInt(p.unresolved.numerator) === 0n
          ? 'COMPLETE'
          : BigInt(p.lower.numerator) === 0n
            ? 'UNKNOWN'
            : 'PARTIAL'
      if (p.status !== expected) invalid()
    }
    if (r.eventual.status === 'PROVEN') {
      if (!r.eventual.probability || !r.eventual.proofVersion) invalid()
      fraction(r.eventual.probability)
    } else if (
      r.eventual.probability !== null ||
      r.eventual.proofVersion !== null
    )
      invalid()
  }
  for (const ranking of job.rankings) {
    if (ranking.status === 'UNAVAILABLE') {
      if (ranking.entries.length) invalid()
      continue
    }
    const points = ranking.entries.map((e) =>
      job.recommendations
        .find((r) => r.policy.id === e.policyId)
        ?.points.find((p) => p.attempts === ranking.attempts),
    )
    if (
      new Set(ranking.entries.map((e) => e.policyId)).size !==
        ranking.entries.length ||
      points.some((p) => !p) ||
      ranking.entries.length !== job.recommendations.length
    )
      invalid()
    if (
      ranking.status === 'CERTIFIED_WITHIN_CANDIDATES' &&
      (!job.candidateScope.enumerationComplete ||
        job.candidateScope.total !== job.recommendations.length ||
        job.candidateScope.generated !== job.candidateScope.total ||
        points.some((p) => p?.status !== 'COMPLETE'))
    )
      invalid()
    ranking.entries.forEach((entry, i) => {
      const relation = i ? compare(points[i - 1]!.lower, points[i]!.lower) : 1
      const expected =
        i && relation === 0 ? ranking.entries[i - 1]!.rank : i + 1
      if (
        relation < 0 ||
        entry.rank !== expected ||
        (i &&
          relation === 0 &&
          ranking.entries[i - 1]!.policyId > entry.policyId)
      )
        invalid()
    })
  }
}
export function assertProvenance(actual: Provenance, expected: Provenance) {
  if (canonical(actual) !== canonical(expected))
    throw new PathSearchError('VERSION_CHANGED')
}
export function validateCreated(job: JobSnapshot, request: CreateRequest) {
  validateJob(job)
  if (job.clientRequestId !== request.clientRequestId || job.recovery !== null)
    invalid()
  if (
    canonical(job.provenance.transition) !==
      canonical(request.start.provenance) ||
    job.provenance.goalCatalogVersion !== request.goal.catalogVersion
  )
    throw new PathSearchError('VERSION_CHANGED')
}

// Fixed-revision pages may repeat identical entities, never mutate them.
export function mergeGraph(previous: GraphPage, page: GraphPage): GraphPage {
  validateGraph(page)
  if (page.jobId !== previous.jobId || page.revision !== previous.revision)
    throw new PathSearchError('STALE_PAGE')
  function merge<T>(a: T[], b: T[], key: (v: T) => string) {
    const map = new Map(a.map((v) => [key(v), v]))
    for (const v of b) {
      const old = map.get(key(v))
      if (old && canonical(old) !== canonical(v)) invalid()
      map.set(key(v), v)
    }
    return [...map.values()]
  }
  const graph = {
    ...page,
    nodes: merge(previous.nodes, page.nodes, (v) => v.id),
    executions: merge(previous.executions, page.executions, (v) => v.id),
    edges: merge(previous.edges, page.edges, (v) => v.id),
    expansions: merge(
      previous.expansions,
      page.expansions,
      (v) => v.executionId,
    ),
  }
  const states = new Set(graph.nodes.map((n) => n.id)),
    executions = new Map(graph.executions.map((e) => [e.id, e]))
  if (
    graph.executions.some((e) => !states.has(e.stateId)) ||
    graph.edges.some(
      (e) =>
        !executions.has(e.from) ||
        !executions.has(e.to) ||
        executions.get(e.from)!.policyId !== executions.get(e.to)!.policyId,
    )
  )
    invalid()
  for (const expansion of graph.expansions) {
    if (!executions.has(expansion.executionId)) invalid()
    const outgoing = graph.edges
      .filter((e) => e.from === expansion.executionId)
      .map((e) => e.probability)
    if (
      expansion.status === 'COMPLETE' &&
      (!sumEquals(outgoing, one) ||
        BigInt(expansion.unresolved.numerator) !== 0n)
    )
      invalid()
    if (
      expansion.status === 'PARTIAL' &&
      !sumEquals([...outgoing, expansion.unresolved], one)
    )
      invalid()
    if (
      expansion.status === 'UNSUPPORTED' &&
      (outgoing.length || compare(expansion.unresolved, one) !== 0)
    )
      invalid()
    if (
      expansion.status === 'UNAVAILABLE' &&
      (outgoing.length || BigInt(expansion.unresolved.numerator) !== 0n)
    )
      invalid()
  }
  return graph
}
