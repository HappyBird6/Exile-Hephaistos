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
beforeEach(() => {
  vi.stubGlobal('crypto', webcrypto)
  vi.stubGlobal('CompressionStream', CompressionStream)
  vi.stubGlobal('DecompressionStream', DecompressionStream)
  vi.stubGlobal('Blob', Blob)
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
