import type { ConcreteItem } from '../workbenchApi'
import { rulesetHeaders, verifyRulesetResponse } from '../rulesetIdentity'

export const basicActions = [
  'TRANSMUTATION',
  'GREATER_TRANSMUTATION',
  'PERFECT_TRANSMUTATION',
  'AUGMENTATION',
  'GREATER_AUGMENTATION',
  'PERFECT_AUGMENTATION',
  'REGAL',
  'GREATER_REGAL',
  'PERFECT_REGAL',
  'EXALTED',
  'GREATER_EXALTED',
  'PERFECT_EXALTED',
  'ANNULMENT',
  'CHAOS',
  'GREATER_CHAOS',
  'PERFECT_CHAOS',
] as const
export type BasicAction = (typeof basicActions)[number]
export type Fraction = { numerator: string; denominator: string }
export type Provenance = {
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
export type State = { item: ConcreteItem; provenance: Provenance }
export type PathRequest = {
  start: State
  policy: { actions: BasicAction[]; mode: 'SINGLE_PASS' | 'REPEAT_CYCLE' }
  target: { checkpoint: State | null; explicitModifierIds: string[] }
  activeOmens: []
  observations: number[]
}
export type Point = {
  attempts: string
  lower: Fraction
  upper: Fraction
  active: Fraction
  dead: Fraction
  unresolved: Fraction
  status: 'COMPLETE' | 'PARTIAL' | 'UNKNOWN'
  reachability: string
}
export type PathResult = {
  purpose: 'MAIN_FIRST_HIT' | 'CONDITIONAL_RECOVERY'
  request: PathRequest
  provenance: Provenance
  interpretation: string
  points: Point[]
  reason: string
  blockers: {
    state: State
    action: BasicAction
    status: string
    reason: string
  }[]
  blockersTruncated: boolean
  recoveryIncludedInMain: false
  computationLimits: Record<string, number>
  evaluations: number
  peakFrontier: number
  peakFractionBits: number
  fractionMetricScope: string
  renewalProof: null | {
    probability: Fraction
    targetModifierId: string
    proofVersion: string
  }
}

const provenanceFields: (keyof Provenance)[] = [
  'rulesetIdentity',
  'catalogDigest',
  'modelVersion',
  'ruleVersion',
  'ledgerVersion',
  'weightPolicy',
  'sourceUrl',
  'retrievedAt',
  'rawSha256',
  'detailsSha256',
]
export function sameProvenance(a: Provenance, b: Provenance) {
  return provenanceFields.every((key) => a[key] === b[key])
}
export async function loadProvenance(signal: AbortSignal): Promise<Provenance> {
  const response = await fetch('/api/v1/crafting/basic-paths/provenance', {
    signal,
  })
  if (!response.ok) throw new Error('Probability provenance unavailable.')
  const value = (await response.json()) as Provenance
  if (
    !value ||
    !provenanceFields.every(
      (key) => typeof value[key] === 'string' && value[key],
    )
  )
    throw new Error('Invalid probability provenance.')
  return value
}
function integers(value: Fraction): [bigint, bigint] {
  if (
    !value ||
    typeof value.numerator !== 'string' ||
    typeof value.denominator !== 'string' ||
    !/^(0|[1-9][0-9]*)$/.test(value.numerator) ||
    !/^[1-9][0-9]*$/.test(value.denominator)
  )
    throw new Error('Invalid exact probability.')
  const n = BigInt(value.numerator),
    d = BigInt(value.denominator)
  if (n > d) throw new Error('Probability outside [0, 1].')
  return [n, d]
}
// Truncate at five decimal percentage places. Never round a probability below one to 100%.
export function percent(value: Fraction): string {
  const [n, d] = integers(value)
  if (n === d) return '100%'
  if (n === 0n) return '0%'
  const scaled = (n * 10000000n) / d
  if (scaled === 0n) return '<0.00001%'
  const digits = (scaled % 100000n)
    .toString()
    .padStart(5, '0')
    .replace(/0+$/, '')
  return `≈${scaled / 100000n}${digits ? '.' + digits : ''}%`
}
function add(a: Fraction, b: Fraction): Fraction {
  const [an, ad] = integers(a),
    [bn, bd] = integers(b)
  return { numerator: String(an * bd + bn * ad), denominator: String(ad * bd) }
}
function equal(a: Fraction, b: Fraction) {
  const [an, ad] = integers(a),
    [bn, bd] = integers(b)
  return an * bd === bn * ad
}
function canonical(value: unknown): string {
  if (Array.isArray(value)) return '[' + value.map(canonical).join(',') + ']'
  if (value && typeof value === 'object')
    return (
      '{' +
      Object.entries(value)
        .sort(([a], [b]) => a.localeCompare(b))
        .map(([key, entry]) => JSON.stringify(key) + ':' + canonical(entry))
        .join(',') +
      '}'
    )
  return JSON.stringify(value)
}
function stateKey(state: State | null) {
  if (!state) return null
  const modifier = (m: ConcreteItem['explicits'][number]) => ({
    ...m,
    fractured: m.fractured ?? false,
  })
  return canonical({
    ...state,
    item: {
      ...state.item,
      augmentSockets: state.item.augmentSockets ?? null,
      catalystQuality: state.item.catalystQuality ?? null,
      conditions: [...state.item.conditions].sort(),
      implicits: state.item.implicits
        .map(modifier)
        .sort((a, b) => a.modifierId.localeCompare(b.modifierId)),
      explicits: state.item.explicits
        .map(modifier)
        .sort((a, b) => a.modifierId.localeCompare(b.modifierId)),
    },
  })
}
export async function evaluatePath(
  request: PathRequest,
  recovery: boolean,
  signal: AbortSignal,
): Promise<PathResult> {
  const response = await fetch(
    `/api/v1/crafting/basic-paths/${recovery ? 'recovery' : 'first-hit'}`,
    {
      method: 'POST',
      signal,
      headers: rulesetHeaders(request.start.provenance.rulesetIdentity),
      body: JSON.stringify(request),
    },
  )
  if (!response.ok) throw new PathRequestError(response.status)
  verifyRulesetResponse(response, request.start.provenance.rulesetIdentity)
  const result = (await response.json()) as PathResult
  const echoed = result?.request
  if (
    !result ||
    !result.provenance ||
    !sameProvenance(result.provenance, request.start.provenance) ||
    result.purpose !== (recovery ? 'CONDITIONAL_RECOVERY' : 'MAIN_FIRST_HIT') ||
    result.recoveryIncludedInMain !== false ||
    !Array.isArray(result.points) ||
    result.points.length !== request.observations.length ||
    !Array.isArray(result.blockers) ||
    !echoed ||
    canonical(echoed.policy) !== canonical(request.policy) ||
    canonical(echoed.observations) !== canonical(request.observations) ||
    canonical(echoed.activeOmens) !== canonical(request.activeOmens) ||
    stateKey(echoed.start) !== stateKey(request.start) ||
    stateKey(echoed.target?.checkpoint) !==
      stateKey(request.target.checkpoint) ||
    canonical(echoed.target?.explicitModifierIds?.slice().sort()) !==
      canonical(request.target.explicitModifierIds.slice().sort())
  )
    throw new Error('Probability response does not match the current request.')
  result.points.forEach((p, i) => {
    if (
      p.attempts !== String(request.observations[i]) ||
      !['COMPLETE', 'PARTIAL', 'UNKNOWN'].includes(p.status)
    )
      throw new Error('Invalid probability observation.')
    for (const value of [p.lower, p.upper, p.active, p.dead, p.unresolved])
      integers(value)
    if (
      !equal(add(p.lower, p.unresolved), p.upper) ||
      !equal(add(add(p.lower, p.active), add(p.dead, p.unresolved)), {
        numerator: '1',
        denominator: '1',
      }) ||
      (p.status === 'COMPLETE') !== (BigInt(p.unresolved.numerator) === 0n) ||
      (p.status === 'UNKNOWN' && BigInt(p.lower.numerator) !== 0n) ||
      (p.status === 'PARTIAL' && BigInt(p.lower.numerator) === 0n)
    )
      throw new Error('Probability mass or calculation status is inconsistent.')
  })
  return result
}
export class PathRequestError extends Error {
  constructor(readonly status: number) {
    super(`Probability request failed (${status}).`)
    this.name = 'PathRequestError'
  }
}
