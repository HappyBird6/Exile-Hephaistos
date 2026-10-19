import { validateSearchResources } from './searchResourcesValidation'
import { assertShape, canonical } from '../path-tree/validation'
import type { CreateRequest, Item } from '../path-tree/types'
import type { AutomaticView } from './automaticApi'
import type { Selection } from './supportStorage'
import type { ItemCardData } from '../itemCardData'
import type { Locale } from '../../../shared/i18n/i18n'
import { validRoute } from './routeSelection'
export const scenarioLocales: Locale[] = [
  'en',
  'ko',
  'zh-CN',
  'zh-TW',
  'ja',
  'es',
]
export const SCENARIO_RAW_LIMIT = 16 * 1024 * 1024
export const SCENARIO_COMPRESSED_LIMIT = 2 * 1024 * 1024
export const SCENARIO_CODE_LIMIT = 2_800_000
export type Scenario = {
  format: 'exile-scenario'
  version: 1
  createdAt: string
  baseKey: string
  request: CreateRequest
  abstractQuality40: boolean
  result: AutomaticView
  selected: number
  cycles: number
  route: Selection[]
  goalLabels?: Record<Locale, Record<string, string>> | undefined
  cards: Record<string, Record<Locale, ItemCardData>>
}
function fail(message = 'INVALID_SCENARIO'): never {
  throw new Error(message)
}
function object(v: unknown, required: string[], optional: string[] = []) {
  if (!v || typeof v !== 'object' || Array.isArray(v)) fail()
  const r = v as Record<string, unknown>
  if (
    required.some((k) => !Object.hasOwn(r, k)) ||
    Object.keys(r).some((k) => !required.includes(k) && !optional.includes(k))
  )
    fail()
  return r
}
function array(v: unknown, max: number): unknown[] {
  if (!Array.isArray(v) || v.length > max) fail()
  return v
}
function text(v: unknown, max = 4096): asserts v is string {
  if (typeof v !== 'string' || v.length > max) fail()
}
function bool(v: unknown) {
  if (typeof v !== 'boolean') fail()
}
function integer(v: unknown, max = 1_000_000_000) {
  if (!Number.isSafeInteger(v) || (v as number) < 0 || (v as number) > max)
    fail()
}
function choice(v: unknown, choices: unknown[]) {
  if (!choices.includes(v)) fail()
}
function safeTree(value: unknown) {
  let count = 0
  const visit = (v: unknown, depth: number) => {
    if (++count > 500_000 || depth > 32) fail('SCENARIO_TOO_LARGE')
    if (typeof v === 'string') text(v, 8192)
    else if (typeof v === 'number' && !Number.isFinite(v)) fail()
    else if (v && typeof v === 'object') {
      for (const [key, child] of Object.entries(v)) {
        if (
          ['__proto__', 'prototype', 'constructor'].includes(key) ||
          key.length > 1024
        )
          fail()
        visit(child, depth + 1)
      }
    }
  }
  visit(value, 0)
}
function rational(v: unknown) {
  const r = object(v, ['numerator', 'denominator'])
  for (const value of Object.values(r))
    if (typeof value !== 'string' || !/^(0|[1-9][0-9]{0,39})$/.test(value))
      fail()
  const n = BigInt(r.numerator as string),
    d = BigInt(r.denominator as string)
  // v1 automatic bounds are dyadic intervals. This also bounds arithmetic work on hostile codes.
  if (d === 0n || d > 1n << 128n || (d & (d - 1n)) !== 0n) fail()
  return [n, d] as const
}
function bounds(v: unknown, probability = true) {
  const r = object(v, [
    'lower',
    'upper',
    'arithmeticWidth',
    'exact',
    'confirmedPercent',
  ])
  const [ln, ld] = rational(r.lower),
    [un, ud] = rational(r.upper),
    [wn, wd] = rational(r.arithmeticWidth)
  if (
    ln * ud > un * ld ||
    un > ud * BigInt(probability ? 1 : 1_000_000_000) ||
    wn * ld * ud !== (un * ld - ln * ud) * wd
  )
    fail()
  bool(r.exact)
  if (r.exact && ln * ud !== un * ld) fail()
  if (r.confirmedPercent !== null) text(r.confirmedPercent, 128)
}
function item(v: unknown) {
  assertShape('item', v)
  const i = v as Item
  if (i.explicits.length > 4 || i.itemLevel > 100) fail()
}
function card(v: unknown) {
  const r = object(v, [
    'rarity',
    'name',
    'base',
    'itemClass',
    'itemLevel',
    'properties',
    'requirements',
    'modifiers',
    'flags',
  ])
  choice(r.rarity, ['NORMAL', 'MAGIC', 'RARE', 'UNIQUE'])
  text(r.name)
  text(r.itemClass)
  if (r.base !== null) text(r.base)
  if (r.itemLevel !== null) integer(r.itemLevel, 100)
  for (const key of ['properties', 'requirements', 'modifiers', 'flags'])
    for (const line of array(r[key], 64)) {
      const l = object(
        line,
        ['id', 'text'],
        ['kind', 'detail', 'affixLabel', 'removalCandidate', 'fractured'],
      )
      text(l.id)
      text(l.text)
      for (const k of ['detail', 'affixLabel'])
        if (l[k] !== undefined) text(l[k])
      if (l.kind !== undefined)
        choice(l.kind, [
          'implicit',
          'enchant',
          'rune',
          'desecrated',
          'fractured',
          'crafted',
          'mutated',
          'explicit',
          'unknown',
        ])
      for (const k of ['removalCandidate', 'fractured'])
        if (l[k] !== undefined) bool(l[k])
    }
}
export function validateScenario(raw: unknown): Scenario {
  safeTree(raw)
  const s = object(
    raw,
    [
      'format',
      'version',
      'createdAt',
      'baseKey',
      'request',
      'abstractQuality40',
      'result',
      'selected',
      'cycles',
      'route',
      'cards',
    ],
    ['goalLabels'],
  )
  if (s.format !== 'exile-scenario' || s.version !== 1)
    fail('UNSUPPORTED_SCENARIO_VERSION')
  text(s.createdAt, 40)
  if (!Number.isFinite(Date.parse(s.createdAt as string))) fail()
  text(s.baseKey, 80)
  if (!/^[a-z0-9-]+$/.test(s.baseKey as string)) fail()
  assertShape('createRequest', s.request)
  const request = s.request as CreateRequest
  item(request.start.item)
  bool(s.abstractQuality40)
  choice(s.cycles, [100, 300, 500])
  integer(s.selected)
  const v = object(
    s.result,
    [
      'version',
      'id',
      'revision',
      'generation',
      'status',
      'reason',
      'complete',
      'operations',
      'retainedStates',
      'nodes',
      'methods',
      'recommendations',
    ],
    ['canResume', 'resources'],
  )
  if (v.version !== 'automatic-method-job-v1')
    fail('UNSUPPORTED_SCENARIO_VERSION')
  text(v.id, 36)
  if (!/^[0-9a-f-]{36}$/.test(v.id as string)) fail()
  for (const k of ['revision', 'generation', 'operations', 'retainedStates'])
    integer(v[k])
  choice(v.status, ['COMPLETE', 'PARTIAL'])
  bool(v.complete)
  if ((v.status === 'COMPLETE') !== v.complete) fail()
  text(v.reason)
  if (v.canResume !== undefined) bool(v.canResume)
  if (v.resources !== undefined)
    validateSearchResources(
      v.resources,
      array(v.nodes, 5000).length,
      array(v.methods, 50000).length,
    )
  const ids = new Set<number>(),
    methods = new Set<string>()
  for (const rawNode of array(v.nodes, 5000)) {
    const n = object(
      rawNode,
      [
        'id',
        'depth',
        'item',
        'quality40',
        'progress',
        'discoveryFinished',
        'directFinished',
      ],
      ['rolls'],
    )
    integer(n.id)
    integer(n.depth, 10000)
    if (ids.has(n.id as number)) fail()
    ids.add(n.id as number)
    item(n.item)
    bool(n.quality40)
    bool(n.discoveryFinished)
    bool(n.directFinished)
    const p = object(n.progress, ['goal', 'entries'])
    choice(p.goal, ['MATCH', 'NO_MATCH', 'UNKNOWN'])
    for (const e of array(p.entries, 4096))
      choice(e, ['MATCH', 'NO_MATCH', 'UNKNOWN'])
    if (n.rolls !== undefined) {
      if (!n.rolls || typeof n.rolls !== 'object' || Array.isArray(n.rolls))
        fail()
      for (const [modifier, b] of Object.entries(n.rolls)) {
        const band = object(b, ['stat', 'min', 'max'])
        text(band.stat)
        if (
          ![band.min, band.max].every(
            (x) => typeof x === 'number' && Number.isFinite(x),
          ) ||
          (band.min as number) > (band.max as number) ||
          !(n.item as Item).explicits.some(
            (m) =>
              m.modifierId === modifier &&
              Object.hasOwn(m.values, band.stat as string),
          )
        )
          fail()
      }
    }
  }
  if (!ids.has(0) || !ids.has(s.selected as number)) fail()
  const result = s.result as AutomaticView,
    root = result.nodes.find((n) => n.id === 0)!
  if (
    canonical(root.item) !== canonical(request.start.item) ||
    root.quality40 !== s.abstractQuality40
  )
    fail()
  let outcomeCount = 0
  for (const rawMethod of array(v.methods, v.resources ? 50000 : 256)) {
    const m = object(
      rawMethod,
      ['id', 'source', 'initial', 'cycle', 'points', 'reasons', 'finished'],
      ['executionMode'],
    )
    text(m.id, 4096)
    if (methods.has(m.id as string) || !ids.has(m.source as number)) fail()
    methods.add(m.id as string)
    if (m.executionMode !== undefined)
      choice(m.executionMode, ['SINGLE_PASS', 'REPEATABLE', 'PENDING'])
    bool(m.finished)
    for (const key of ['initial', 'cycle'])
      for (const a of array(m[key], 16)) {
        const action = object(a, ['currency', 'omens', 'purpose'])
        text(action.currency, 128)
        text(action.purpose, 128)
        for (const omen of array(action.omens, 16)) text(omen, 128)
      }
    for (const reason of array(m.reasons, 64)) text(reason)
    const cycles = new Set<number>()
    for (const rawPoint of array(m.points, 4)) {
      const p = object(rawPoint, [
        'cycles',
        'complete',
        'outcomes',
        'initialUses',
        'cycleUses',
      ])
      choice(p.cycles, [1, 100, 300, 500])
      if (cycles.has(p.cycles as number)) fail()
      cycles.add(p.cycles as number)
      bool(p.complete)
      const outcomes = new Set<string>()
      let lowerMass = 0n,
        upperMass = 0n
      const scale = 1n << 128n
      for (const rawOutcome of array(p.outcomes, 10000)) {
        if (++outcomeCount > 100000) fail('SCENARIO_TOO_LARGE')
        const o = object(rawOutcome, ['target', 'kind', 'probability'])
        if (o.target !== null && !ids.has(o.target as number)) fail()
        choice(o.kind, [
          'GOAL',
          'CHECKPOINT',
          'LOSS',
          'DEAD',
          'UNRESOLVED',
          'RETRY',
          'ACTIVE',
        ])
        const key = `${o.target}:${o.kind}`
        if (outcomes.has(key)) fail()
        outcomes.add(key)
        bounds(o.probability)
        const values = o.probability as { lower: unknown; upper: unknown }
        const [ln, ld] = rational(values.lower),
          [un, ud] = rational(values.upper)
        lowerMass += ln * (scale / ld)
        upperMass += un * (scale / ud)
        if (
          o.kind === 'GOAL' &&
          (o.target === null ||
            result.nodes.find((n) => n.id === o.target)?.progress.goal !==
              'MATCH')
        )
          fail()
        if (o.kind === 'RETRY' && o.target !== null) fail()
      }
      if (lowerMass > scale || (p.complete && upperMass < scale)) fail()
      for (const key of ['initialUses', 'cycleUses']) {
        if (!p[key] || typeof p[key] !== 'object' || Array.isArray(p[key]))
          fail()
        if (Object.keys(p[key]).length > 64) fail()
        for (const b of Object.values(p[key])) bounds(b, false)
      }
    }
  }
  for (const rawRecommendation of array(v.recommendations, 15000)) {
    const r = object(rawRecommendation, [
      'node',
      'cycles',
      'local',
      'strategy',
      'strategyStatus',
      'strategyMethods',
    ])
    if (!ids.has(r.node as number)) fail()
    choice(r.cycles, [100, 300, 500])
    text(r.strategyStatus, 128)
    bounds(r.strategy)
    for (const key of ['local', 'strategyMethods'])
      for (const id of array(r[key], 256))
        if (!methods.has(id as string)) fail()
  }
  for (const e of array(s.route, 64)) {
    const edge = object(e, ['method', 'source', 'target', 'kind'])
    text(edge.method)
    text(edge.kind)
    integer(edge.source)
    integer(edge.target)
  }
  if (!validRoute(result, s.route as Selection[], s.cycles as number)) fail()
  if (
    (s.route as Selection[]).length &&
    (s.route as Selection[]).at(-1)!.target !== s.selected
  )
    fail()
  const cards = object(s.cards, [...ids].map(String))
  for (const value of Object.values(cards)) {
    const languages = object(value, scenarioLocales)
    for (const locale of scenarioLocales) card(languages[locale])
  }
  if (s.goalLabels !== undefined) {
    const labels = object(s.goalLabels, scenarioLocales)
    const stats = new Set(
      request.goal.groups.flatMap((g) => g.entries.map((e) => e.statId)),
    )
    for (const labelMap of Object.values(labels)) {
      const values = object(labelMap, [...stats])
      for (const label of Object.values(values)) text(label)
    }
  }
  return raw as Scenario
}

function record(value: unknown): Record<string, unknown> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) fail()
  return value as Record<string, unknown>
}
function mapWireReferences(
  snapshot: unknown,
  bound: (v: unknown) => unknown,
  method: (v: unknown, names: unknown[]) => unknown,
) {
  const root = record(snapshot),
    result = record(root.result)
  const methods = array(result.methods, result.resources ? 50000 : 256),
    names = methods.map((m) => record(m).id)
  for (const raw of methods)
    for (const rawPoint of array(record(raw).points, 4)) {
      const point = record(rawPoint)
      for (const rawOutcome of array(point.outcomes, 10000)) {
        const outcome = record(rawOutcome)
        outcome.probability = bound(outcome.probability)
      }
      for (const field of ['initialUses', 'cycleUses']) {
        const uses = record(point[field])
        for (const key of Object.keys(uses)) uses[key] = bound(uses[key])
      }
    }
  for (const raw of array(result.recommendations, 15000)) {
    const recommendation = record(raw)
    recommendation.strategy = bound(recommendation.strategy)
    for (const field of ['local', 'strategyMethods'])
      recommendation[field] = array(recommendation[field], 256).map((id) =>
        method(id, names),
      )
  }
  for (const raw of array(root.route, 64)) {
    const edge = record(raw)
    edge.method = method(edge.method, names)
  }
}
export function compactScenario(
  scenario: Scenario,
  compactCards = false,
): unknown {
  const snapshot = structuredClone(scenario),
    table: unknown[][] = [],
    ids = new Map<string, number>()
  mapWireReferences(
    snapshot,
    (value) => {
      const b = record(value),
        lower = record(b.lower),
        upper = record(b.upper),
        width = record(b.arithmeticWidth)
      const row = [
        lower.numerator,
        lower.denominator,
        upper.numerator,
        upper.denominator,
        width.numerator,
        width.denominator,
        b.exact,
        b.confirmedPercent,
      ]
      const key = JSON.stringify(row)
      if (!ids.has(key)) {
        ids.set(key, table.length)
        table.push(row)
      }
      return ids.get(key)!
    },
    (id, names) => {
      const index = names.indexOf(id)
      if (index < 0) fail()
      return index
    },
  )
  if (!compactCards) return { bounds: table, snapshot }
  const headers: unknown[] = [],
    lines: unknown[] = [],
    headerIds = new Map<string, number>(),
    lineIds = new Map<string, number>()
  const reference = (
    value: unknown,
    ids: Map<string, number>,
    table: unknown[],
  ) => {
    const key = JSON.stringify(value)
    if (!ids.has(key)) {
      ids.set(key, table.length)
      table.push(value)
    }
    return ids.get(key)!
  }
  for (const value of Object.values(record(record(snapshot).cards))) {
    const languages = record(value)
    for (const locale of Object.keys(languages)) {
      const { modifiers, ...header } = record(languages[locale])
      languages[locale] = [
        reference(header, headerIds, headers),
        array(modifiers, 64).map((line) => reference(line, lineIds, lines)),
      ]
    }
  }
  return { bounds: table, snapshot, headers, lines }
}
export function expandScenario(raw: unknown): Scenario {
  safeTree(raw)
  const wire = object(raw, ['bounds', 'snapshot'], ['headers', 'lines'])
  const table = array(wire.bounds, 100000).map((value) => {
    const row = array(value, 8)
    if (row.length !== 8) fail()
    const valueBounds = {
      lower: { numerator: row[0], denominator: row[1] },
      upper: { numerator: row[2], denominator: row[3] },
      arithmeticWidth: { numerator: row[4], denominator: row[5] },
      exact: row[6],
      confirmedPercent: row[7],
    }
    bounds(valueBounds, false)
    return valueBounds
  })
  mapWireReferences(
    wire.snapshot,
    (value) => {
      integer(value, table.length - 1)
      const found = table[value as number]
      if (!found) fail()
      return found
    },
    (value, names) => {
      integer(value, names.length - 1)
      const name = names[value as number]
      if (typeof name !== 'string') fail()
      return name
    },
  )
  if ((wire.headers === undefined) !== (wire.lines === undefined)) fail()
  if (wire.headers !== undefined) {
    const headers = array(wire.headers, 30000),
      lines = array(wire.lines, 100000)
    for (const header of headers)
      card({
        ...object(header, [
          'rarity',
          'name',
          'base',
          'itemClass',
          'itemLevel',
          'properties',
          'requirements',
          'flags',
        ]),
        modifiers: [],
      })
    for (const line of lines) card({ ...record(headers[0]), modifiers: [line] })
    for (const value of Object.values(record(record(wire.snapshot).cards))) {
      const languages = record(value)
      for (const locale of Object.keys(languages)) {
        const refs = array(languages[locale], 2)
        if (refs.length !== 2) fail()
        integer(refs[0], headers.length - 1)
        languages[locale] = {
          ...record(headers[refs[0] as number]),
          modifiers: array(refs[1], 64).map((id) => {
            integer(id, lines.length - 1)
            return lines[id as number]
          }),
        }
      }
    }
  }
  const scenario = validateScenario(wire.snapshot)
  if (
    new TextEncoder().encode(JSON.stringify(scenario)).length >
    SCENARIO_RAW_LIMIT
  )
    fail('SCENARIO_TOO_LARGE')
  return scenario
}

async function collect(stream: ReadableStream<Uint8Array>, limit: number) {
  const reader = stream.getReader(),
    chunks: Uint8Array[] = []
  let total = 0
  try {
    for (;;) {
      const { done, value } = await reader.read()
      if (done) break
      total += value.length
      if (total > limit) fail('SCENARIO_TOO_LARGE')
      chunks.push(value)
    }
  } finally {
    await reader.cancel()
  }
  const out = new Uint8Array(total)
  let offset = 0
  for (const chunk of chunks) {
    out.set(chunk, offset)
    offset += chunk.length
  }
  return out
}
async function digest(data: Uint8Array<ArrayBuffer>) {
  return [...new Uint8Array(await crypto.subtle.digest('SHA-256', data))]
    .map((x) => x.toString(16).padStart(2, '0'))
    .join('')
}
export async function encodeScenario(s: Scenario) {
  validateScenario(s)
  const original = new TextEncoder().encode(JSON.stringify(s))
  if (original.length > SCENARIO_RAW_LIMIT) fail('SCENARIO_TOO_LARGE')
  const compress = (bytes: Uint8Array<ArrayBuffer>) =>
    collect(
      new Blob([bytes]).stream().pipeThrough(new CompressionStream('gzip')),
      SCENARIO_RAW_LIMIT + 65536,
    )
  let bytes = original,
    compressed = await compress(original),
    version = 1
  for (const compactCards of [false, true]) {
    const packed = new TextEncoder().encode(
      JSON.stringify(compactScenario(s, compactCards)),
    )
    if (packed.length > SCENARIO_RAW_LIMIT) continue
    const candidate = await compress(packed)
    if (candidate.length < compressed.length) {
      bytes = packed
      compressed = candidate
      version = 2
    }
  }
  if (compressed.length > SCENARIO_COMPRESSED_LIMIT) fail('SCENARIO_TOO_LARGE')
  let binary = ''
  for (let i = 0; i < compressed.length; i += 8192)
    binary += String.fromCharCode(...compressed.subarray(i, i + 8192))
  return `EHSC${version}.${await digest(bytes)}.${btoa(binary).replaceAll('+', '-').replaceAll('/', '_').replace(/=+$/, '')}`
}

let decoding = 0
export async function decodeScenario(code: string) {
  if (decoding >= 2) fail('SCENARIO_BUSY')
  decoding++
  try {
    if (code.length > SCENARIO_CODE_LIMIT) fail('SCENARIO_TOO_LARGE')
    const match = /^EHSC([12])\.([0-9a-f]{64})\.([A-Za-z0-9_-]+)$/.exec(
      code.trim(),
    )
    if (!match) fail('INVALID_SCENARIO_CODE')
    const compressed = Uint8Array.from(
      atob(match[3]!.replaceAll('-', '+').replaceAll('_', '/')),
      (c) => c.charCodeAt(0),
    )
    if (compressed.length > SCENARIO_COMPRESSED_LIMIT)
      fail('SCENARIO_TOO_LARGE')
    const bytes = await collect(
      new Blob([compressed])
        .stream()
        .pipeThrough(new DecompressionStream('gzip')),
      SCENARIO_RAW_LIMIT,
    )
    if ((await digest(bytes)) !== match[2]) fail('SCENARIO_CHECKSUM')
    const text = new TextDecoder('utf-8', { fatal: true }).decode(bytes),
      value: unknown = JSON.parse(text)
    if (JSON.stringify(value) !== text) fail()
    return match[1] === '2' ? expandScenario(value) : validateScenario(value)
  } finally {
    decoding--
  }
}
