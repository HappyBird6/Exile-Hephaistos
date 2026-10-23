import { beforeEach, expect, it, vi } from 'vitest'
import { webcrypto } from 'node:crypto'
import { CompressionStream, DecompressionStream } from 'node:stream/web'
import { Blob } from 'node:buffer'
import {
  compactScenario,
  expandScenario,
  decodeScenario,
  encodeScenario,
  validateScenario,
  SCENARIO_CODE_LIMIT,
  SCENARIO_RAW_LIMIT,
  scenarioLocales,
} from './scenarioCode'
import type { Scenario } from './scenarioCode'
import { requestFixture } from '../path-tree/fixtures.test-support'
import { createItemPresentation } from '../path-tree/presentation'
const present = createItemPresentation(
  { name: 'Solar Amulet', itemClass: 'Amulet' },
  {},
)
function fixture(): Scenario {
  const request = requestFixture(),
    item = request.start.item
  return {
    format: 'exile-scenario',
    version: 1,
    createdAt: '2026-10-19T00:00:00.000Z',
    baseKey: 'solar',
    request,
    abstractQuality40: false,
    result: {
      version: 'automatic-method-job-v1',
      id: '00000000-0000-4000-8000-000000000000',
      revision: 1,
      generation: 1,
      status: 'COMPLETE',
      reason: 'SUPPORTED_PLANS_COMPLETE',
      complete: true,
      operations: 1,
      retainedStates: 1,
      nodes: [
        {
          id: 0,
          depth: 0,
          quality40: false,
          item,
          progress: { goal: 'MATCH', entries: [] },
          discoveryFinished: true,
          directFinished: true,
        },
      ],
      methods: [],
      recommendations: [],
    },
    selected: 0,
    cycles: 300,
    route: [],
    cards: {
      '0': Object.fromEntries(
        scenarioLocales.map((locale) => [locale, present(item, locale)]),
      ) as Scenario['cards'][string],
    },
  }
}

it('preserves optional quality goals and work amounts through scenario export/import', async () => {
  const value = fixture()
  value.request.goal.general.quality = 30
  value.abstractQuality = 20
  value.result.nodes[0]!.abstractQuality = 20
  const restored = await decodeScenario(await encodeScenario(value))
  expect(restored.request.goal.general.quality).toBe(30)
  expect(restored.abstractQuality).toBe(20)
  expect(restored.result.nodes[0]!.abstractQuality).toBe(20)
})
it('preserves an unsupported quality goal without changing it into a successful result', async () => {
  const source = fixture()
  source.request.goal.general.quality = 30
  source.result.status = 'PARTIAL'
  source.result.complete = false
  source.result.nodes[0]!.progress = {
    goal: 'UNSUPPORTED',
    entries: ['UNSUPPORTED'],
  }
  expect(await decodeScenario(await encodeScenario(source))).toEqual(source)
})
beforeEach(() => {
  vi.stubGlobal('crypto', webcrypto)
  vi.stubGlobal('CompressionStream', CompressionStream)
  vi.stubGlobal('DecompressionStream', DecompressionStream)
  vi.stubGlobal('Blob', Blob)
})
it('round trips historical alias observations above the live method count without adding quality', async () => {
  const source = fixture()
  source.result.status = 'PARTIAL'
  source.result.complete = false
  source.result.reason = 'LEGACY_READ_ONLY'
  source.result.canResume = false
  source.result.methods = Array.from({ length: 257 }, (_, index) => ({
    id: `historical-alias-${index}`,
    source: 0,
    initial: [],
    cycle: [{ currency: 'CHAOS', omens: [], purpose: 'GOAL_ACQUISITION' }],
    points: [],
    reasons: [],
    finished: false,
  }))
  const restored = await decodeScenario(await encodeScenario(source))
  expect(restored).toEqual(source)
  expect(restored.request.goal.general).not.toHaveProperty('quality')
  expect(restored).not.toHaveProperty('abstractQuality')
})
it.each(['COMPLETE', 'PARTIAL'] as const)(
  'losslessly preserves %s and catalog versions without server access',
  async (status) => {
    const source = fixture()
    source.result.status = status
    source.result.complete = status === 'COMPLETE'
    source.request.goal.catalogVersion = 'historical-catalog'
    const fetch = vi.fn(() => {
      throw Error('network forbidden')
    })
    vi.stubGlobal('fetch', fetch)
    const code = await encodeScenario(source)
    expect(code).toMatch(/^EHSC[12]\./)
    expect(await decodeScenario(code)).toEqual(source)
    expect(fetch).not.toHaveBeenCalled()
  },
)
it('rejects changed checksum, truncated data, unsupported format and oversized codes', async () => {
  const code = await encodeScenario(fixture())
  await expect(
    decodeScenario(
      code.slice(0, 6) + (code[6] === 'f' ? 'e' : 'f') + code.slice(7),
    ),
  ).rejects.toThrow()
  await expect(decodeScenario(code.slice(0, -10))).rejects.toThrow()
  await expect(
    decodeScenario(code.replace(/^EHSC[12]/, 'EHSC9')),
  ).rejects.toThrow()
  await expect(
    decodeScenario('x'.repeat(SCENARIO_CODE_LIMIT + 1)),
  ).rejects.toThrow('SCENARIO_TOO_LARGE')
})
it.each([
  'unknown-field',
  'version',
  'reference',
  'negative',
  'too-deep',
  'extra-card',
  'root-mismatch',
])('rejects invalid %s', (kind) => {
  const s = fixture() as unknown as Record<string, unknown>
  if (kind === 'unknown-field') s.serverPath = '/private'
  if (kind === 'version') s.version = 2
  if (kind === 'reference') s.selected = 99
  if (kind === 'negative') (s.result as Scenario['result']).operations = -1
  if (kind === 'too-deep') {
    let value: unknown = {}
    for (let i = 0; i < 40; i++) value = { value }
    s.extra = value
  }
  if (kind === 'extra-card')
    (s.cards as Scenario['cards'])['999'] = (s.cards as Scenario['cards'])['0']!
  if (kind === 'root-mismatch') {
    s.request = structuredClone(s.request)
    ;(s.request as Scenario['request']).start.item.itemLevel = 1
  }
  expect(() => validateScenario(s)).toThrow()
})
it('stops a compressed expansion bomb before JSON processing', async () => {
  const compressed = new Uint8Array(
    await new Response(
      new Blob(['x'.repeat(SCENARIO_RAW_LIMIT + 1)])
        .stream()
        .pipeThrough(
          new CompressionStream('gzip'),
        ) as unknown as ReadableStream,
    ).arrayBuffer(),
  )
  const code =
    'EHSC1.' +
    '0'.repeat(64) +
    '.' +
    Buffer.from(compressed).toString('base64url')
  await expect(decodeScenario(code)).rejects.toThrow('SCENARIO_TOO_LARGE')
})
it('bounds concurrent decoding and releases the slot after failure', async () => {
  const code = await encodeScenario(fixture())
  const one = decodeScenario(code),
    two = decodeScenario(code)
  await expect(decodeScenario(code)).rejects.toThrow('SCENARIO_BUSY')
  await Promise.all([one, two])
  expect(await decodeScenario(code)).toEqual(fixture())
})

it('preserves every exact bound through shared table references', () => {
  const original = fixture()
  const one = {
    lower: { numerator: '1', denominator: '1' },
    upper: { numerator: '1', denominator: '1' },
    arithmeticWidth: { numerator: '0', denominator: '1' },
    exact: true,
    confirmedPercent: '100',
  }
  original.result.methods = [
    {
      id: 'one',
      source: 0,
      initial: [],
      cycle: [{ currency: 'EXALTED', omens: [], purpose: 'GOAL_ACQUISITION' }],
      points: [
        {
          cycles: 1,
          complete: true,
          outcomes: [{ target: 0, kind: 'GOAL', probability: one }],
          initialUses: {},
          cycleUses: { EXALTED: one },
        },
      ],
      reasons: [],
      finished: true,
      executionMode: 'SINGLE_PASS',
    },
  ]
  original.result.recommendations = [
    {
      node: 0,
      cycles: 100,
      local: ['one'],
      strategy: one,
      strategyStatus: 'COMPLETE',
      strategyMethods: ['one'],
    },
  ]
  const packed = compactScenario(original) as {
    bounds: unknown[]
    snapshot: unknown
  }
  expect(packed.bounds).toHaveLength(1)
  expect(expandScenario(packed)).toEqual(original)
  expect(
    original.result.methods[0]!.points[0]!.outcomes[0]!.probability,
  ).toEqual(one)
})
it('rejects malformed compact references and unknown fields before showing a snapshot', () => {
  const packed = compactScenario(fixture()) as {
    bounds: unknown[]
    snapshot: Scenario
  }
  packed.snapshot.result.recommendations = [
    {
      node: 0,
      cycles: 100,
      local: [],
      strategy: 0 as never,
      strategyStatus: 'COMPLETE',
      strategyMethods: [],
    },
  ]
  expect(() => expandScenario(packed)).toThrow()
  expect(() =>
    expandScenario({ ...(compactScenario(fixture()) as object), extra: true }),
  ).toThrow()
  expect(() => expandScenario({ bounds: [[1]], snapshot: fixture() })).toThrow()
})

it('shares repeated display data without changing any locale or item value', () => {
  const original = fixture(),
    packed = compactScenario(original, true) as {
      headers: unknown[]
      lines: unknown[]
      snapshot: unknown
    }
  expect(packed.headers.length).toBeLessThan(6)
  expect(expandScenario(packed)).toEqual(original)
  const invalid = compactScenario(original, true) as {
    snapshot: { cards: Record<string, Record<string, unknown>> }
  }
  invalid.snapshot.cards['0']!.en = [999, []]
  expect(() => expandScenario(invalid)).toThrow()
})

it('round trips computation accounting while retaining old scenario compatibility', async () => {
  const scenario = fixture()
  scenario.result.resources = {
    computationStates: 0,
    stateLimit: 64,
    methodExecutions: 0,
    methodLimit: 128,
    concreteStates: 1,
    concreteMethods: 0,
    retainedRecords: 1,
    retainedLimit: 20000,
    primitiveOutcomeBudget: 5000,
    blockers: [],
  }
  const decoded = await decodeScenario(await encodeScenario(scenario))
  expect(decoded).toEqual(scenario)
  const corrupt = structuredClone(scenario)
  corrupt.result.resources!.concreteStates = 2
  expect(() => validateScenario(corrupt)).toThrow()
})

it('round trips read-only legacy results and original requests without enabling resume', async () => {
  const original = fixture()
  original.result.reason = 'LEGACY_READ_ONLY'
  original.result.canResume = false
  const restored = await decodeScenario(await encodeScenario(original))
  expect(restored).toEqual(original)
  expect(restored.result.canResume).toBe(false)
})

it('exports and restores conditional tier distributions without changing exact weights', () => {
  const s = fixture(),
    n = s.result.nodes[0]!
  n.item.explicits = [
    { modifierId: 'tier-low', values: { life: 1 }, fractured: false },
  ]
  n.tiers = {
    'tier-low': {
      variants: [
        {
          modifierId: 'tier-low',
          stat: 'life',
          min: 1,
          max: 2,
          probability: {
            numerator: '9007199254740993',
            denominator: '18014398509481986',
          },
        },
        {
          modifierId: 'tier-high',
          stat: 'life',
          min: 4,
          max: 5,
          probability: { numerator: '1', denominator: '2' },
        },
      ],
    },
  }
  expect(expandScenario(compactScenario(s)).result.nodes[0]!.tiers).toEqual(
    n.tiers,
  )
  expect(validateScenario(s).result.nodes[0]!.tiers).toEqual(n.tiers)
})

function jointScenario(): Scenario {
  const s = fixture(),
    root = s.result.nodes[0]!
  root.item.explicits = []
  const modifier = (stat: string, value: number) => ({
    modifierId: stat + value,
    values: { [stat]: value },
    fractured: false,
  })
  const item = (a: number, b: number, c = false) => ({
    ...root.item,
    explicits: [
      modifier('a', a),
      modifier('b', b),
      ...(c ? [modifier('c', 1)] : []),
    ],
  })
  const bounds = (numerator: string, denominator = '1') => ({
    lower: { numerator, denominator },
    upper: { numerator, denominator },
    arithmeticWidth: { numerator: '0', denominator: '1' },
    exact: true,
    confirmedPercent: '0',
  })
  const members = [
    [1, 4],
    [4, 1],
    [4, 4],
  ].map(([a, b]) => ({ item: item(a!, b!), tiers: {}, rolls: {} }))
  const options = (stat: string) =>
    [1, 4].map((v) => ({
      modifierId: stat + v,
      stat,
      min: v,
      max: v,
      tier: 5 - v,
      role: stat,
    }))
  const source = {
    ...root,
    id: 1,
    depth: 1,
    item: item(1, 4),
    members,
    attributes: { a1: options('a'), b4: options('b') },
    joint: [{ method: 'first', source: 0, kind: 'CHECKPOINT', retained: {} }],
  }
  const target = {
    ...root,
    id: 2,
    depth: 2,
    item: item(1, 4, true),
    members: [{ item: item(1, 4, true), tiers: {}, rolls: {} }],
    attributes: {
      ...source.attributes,
      c1: [{ modifierId: 'c1', stat: 'c', min: 1, max: 1, tier: 1, role: 'c' }],
    },
    joint: [
      {
        method: 'next',
        source: 1,
        kind: 'GOAL',
        retained: { a1: 'a1', b4: 'b4' },
      },
    ],
  }
  s.result.nodes.push(source, target)
  const action = { currency: 'EXALTED', omens: [], purpose: 'GOAL_ACQUISITION' }
  s.result.methods = [
    {
      id: 'first',
      source: 0,
      initial: [],
      cycle: [action],
      reasons: [],
      finished: true,
      points: [
        {
          cycles: 1,
          complete: true,
          initialUses: {},
          cycleUses: {},
          outcomes: [
            {
              target: 1,
              kind: 'CHECKPOINT',
              probability: bounds('3', '8'),
              conditional: members.map((m) => ({
                local: m.item.explicits,
                tiers: {},
                rolls: {},
                retainedFromSource: [],
                probability: bounds('1', '8'),
              })),
            },
            { target: null, kind: 'LOSS', probability: bounds('5', '8') },
          ],
        },
      ],
    },
    {
      id: 'next',
      source: 1,
      initial: [],
      cycle: [action],
      reasons: [],
      finished: true,
      points: [
        {
          cycles: 1,
          complete: true,
          initialUses: {},
          cycleUses: {},
          outcomes: [
            {
              target: 2,
              kind: 'GOAL',
              probability: bounds('1'),
              conditional: [
                {
                  local: [modifier('c', 1)],
                  tiers: {},
                  rolls: {},
                  retainedFromSource: ['a1', 'b4'],
                  probability: bounds('1'),
                },
              ],
            },
          ],
        },
      ],
    },
  ]
  for (const n of [source, target])
    s.cards[String(n.id)] = Object.fromEntries(
      scenarioLocales.map((locale) => [locale, present(n.item, locale)]),
    ) as Scenario['cards'][string]
  return s
}
it('preserves mutually exclusive joint components and source projections through compressed export', async () => {
  const s = jointScenario()
  expect(expandScenario(compactScenario(s))).toEqual(s)
  expect(await decodeScenario(await encodeScenario(s))).toEqual(s)
  expect(s.result.methods[0]!.points[0]!.outcomes[0]!.conditional).toHaveLength(
    3,
  )
})
it('preserves declared numeric model evidence through compressed export and rejects unknown models', async () => {
  const s = fixture()
  s.result.rollModels = ['user-coupled-ratio-half-up-v1']
  expect(await decodeScenario(await encodeScenario(s))).toEqual(s)
  s.result.rollModels = ['unverified-new-model']
  expect(() => validateScenario(s)).toThrow()
})
it.each(['mass', 'projection'] as const)(
  'rejects a corrupted conditional %s without changing stored input',
  (kind) => {
    const s = jointScenario()
    if (kind === 'mass') {
      const probability =
        s.result.methods[0]!.points[0]!.outcomes[0]!.conditional![0]!
          .probability
      probability.lower.numerator = '2'
      probability.upper.numerator = '2'
    } else s.result.nodes[2]!.joint![0]!.retained.a1 = 'b4'
    expect(() => validateScenario(s)).toThrow()
  },
)
