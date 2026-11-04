import { createHash, webcrypto } from 'node:crypto'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { locales } from '../../../../shared/i18n/i18n'
import { applyChaosRepeat } from './applyChaosRepeat'
import {
  batchResponse,
  envelopeFor,
  repeatInitial,
  requestFor,
} from './fixtures.test-support'
import type { TestEnvelope } from './fixtures.test-support'
import type { ChaosRepeatRequest } from './types'

beforeEach(() => vi.stubGlobal('crypto', webcrypto))
const signal = () => new AbortController().signal
function mockBatch(
  change?: (value: TestEnvelope, request: ChaosRepeatRequest) => void,
  status = 200,
) {
  const fetch = vi.fn(async (_url: RequestInfo | URL, init?: RequestInit) => {
    const body = init!.body as string
    const request = JSON.parse(body) as ChaosRepeatRequest
    const value = envelopeFor(request, body)
    change?.(value, request)
    return batchResponse(
      status === 500 ? { status: 500, partialOutcome: value } : value,
      status,
    )
  })
  vi.stubGlobal('fetch', fetch)
  return fetch
}

describe('bound repeat transport', () => {
  it.each(locales)(
    'binds the exact Unicode UTF8 body and captured locale %s without client regex matching',
    async (locale) => {
      const request = requestFor()
      request.locale = locale
      const fetch = mockBatch()
      const result = await applyChaosRepeat(request, repeatInitial, signal())
      expect(result.kind).toBe('completed')
      expect(fetch).toHaveBeenCalledOnce()
      const [url, init] = fetch.mock.calls[0]!
      expect(url).toBe('/api/v1/crafting/workbench/chaos-repeat')
      expect(init!.body).toBe(JSON.stringify(request))
      expect(init!.headers).toMatchObject({
        'X-Crafting-Ruleset': repeatInitial.rulesetIdentity,
      })
      expect(JSON.parse(init!.body as string).pattern).toBe(request.pattern)
      expect(
        createHash('sha256')
          .update(init!.body as string, 'utf8')
          .digest('hex'),
      ).toHaveLength(64)
    },
  )

  it.each(['CHAOS', 'GREATER_CHAOS', 'PERFECT_CHAOS'] as const)(
    'accepts both actual witness types for %s',
    async (action) => {
      mockBatch()
      for (const kind of ['CONCRETE', 'ABYSS'] as const) {
        const result = await applyChaosRepeat(
          requestFor(kind, action),
          repeatInitial,
          signal(),
        )
        expect(result.kind).toBe('completed')
        if (result.kind !== 'completed')
          throw new Error('Expected validated batch')
        expect(result.finalState.kind).toBe(kind)
        expect(result.actualUses).toBe(3)
        expect(result.consumedOmens).toEqual(['Omen_of_Sinistral_Erasure'])
        expect(result.remainingOmens).toEqual(['Omen_of_Sinistral_Necromancy'])
      }
    },
  )

  it('uses default 100 and explicit 1000, while retaining the omitted field in the exact body', async () => {
    const request = requestFor()
    delete request.maximumUses
    const fetch = mockBatch((value) => {
      value.termination = 'LIMIT'
      value.actualUses = value.requestBinding.maximumUses
    })
    expect(
      (await applyChaosRepeat(request, repeatInitial, signal())).kind,
    ).toBe('completed')
    expect(
      JSON.parse(fetch.mock.calls[0]![1]!.body as string),
    ).not.toHaveProperty('maximumUses')
    request.maximumUses = 1000
    expect(
      (await applyChaosRepeat(request, repeatInitial, signal())).kind,
    ).toBe('completed')
  })

  it('captures the catalog and request before await and never mutates the caller item', async () => {
    const request = requestFor()
    const initial = structuredClone(repeatInitial)
    mockBatch(() => {
      request.action = 'PERFECT_CHAOS'
      initial.rulesetIdentity = 'changed-later'
    })
    const result = await applyChaosRepeat(request, initial, signal())
    expect(result.kind).toBe('completed')
    if (request.state.kind === 'CONCRETE')
      expect(request.state.item.explicits[0]!.values.life).toBe(10)
  })

  it.each([0, -1, 1.5, 1001, NaN, Infinity, null, '100'])(
    'rejects invalid local maximum %j before transport',
    async (maximumUses) => {
      const fetch = mockBatch()
      const request = requestFor()
      Object.assign(request, { maximumUses })
      expect(
        await applyChaosRepeat(request, repeatInitial, signal()),
      ).toMatchObject({ kind: 'rejected', source: 'client' })
      expect(fetch).not.toHaveBeenCalled()
    },
  )

  it('distinguishes preflight rejection, uncertain network/abort, generic 500 and malformed 200 without retry', async () => {
    const request = requestFor()
    for (const status of [400, 409, 422, 429]) {
      const fetch = vi.fn(async () =>
        batchResponse({ status, detail: 'not a public UI message' }, status),
      )
      vi.stubGlobal('fetch', fetch)
      expect(
        await applyChaosRepeat(request, repeatInitial, signal()),
      ).toMatchObject({ kind: 'rejected', source: 'server', status })
      expect(fetch).toHaveBeenCalledOnce()
    }
    const fetch = vi.fn(async () => {
      throw new TypeError('network private detail')
    })
    vi.stubGlobal('fetch', fetch)
    expect(
      await applyChaosRepeat(request, repeatInitial, signal()),
    ).toMatchObject({ kind: 'uncertain', reason: 'NETWORK' })
    expect(fetch).toHaveBeenCalledOnce()
    const controller = new AbortController()
    controller.abort()
    expect(
      await applyChaosRepeat(request, repeatInitial, controller.signal),
    ).toMatchObject({ kind: 'uncertain', reason: 'ABORTED' })
    expect(fetch).toHaveBeenCalledOnce()
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => batchResponse({ status: 500 }, 500)),
    )
    expect(
      (await applyChaosRepeat(request, repeatInitial, signal())).kind,
    ).toBe('uncertain')
    vi.stubGlobal(
      'fetch',
      vi.fn(
        async () =>
          new Response('broken JSON', {
            headers: { 'X-Crafting-Ruleset': repeatInitial.rulesetIdentity },
          }),
      ),
    )
    expect(
      (await applyChaosRepeat(request, repeatInitial, signal())).kind,
    ).toBe('invalid-response')
  })
})

describe('batch response validation', () => {
  const invalid: Array<[string, (value: TestEnvelope) => void]> = [
    [
      'schema',
      (v) => {
        v.schemaVersion = 2
      },
    ],
    [
      'request ID',
      (v) => {
        v.requestBinding.requestId = 'other'
      },
    ],
    [
      'raw digest',
      (v) => {
        v.requestBinding.requestSha256 = '0'.repeat(64)
      },
    ],
    [
      'locale',
      (v) => {
        v.requestBinding.locale = 'ja'
      },
    ],
    [
      'action',
      (v) => {
        v.requestBinding.action = 'PERFECT_CHAOS'
      },
    ],
    [
      'N',
      (v) => {
        v.requestBinding.maximumUses = 100
      },
    ],
    [
      'matcher',
      (v) => {
        v.requestBinding.matcherVersion = 'javascript'
      },
    ],
    [
      'ruleset',
      (v) => {
        v.rulesetIdentity = 'other'
      },
    ],
    [
      'fraction count',
      (v) => {
        v.actualUses = 1.5
      },
    ],
    [
      'above N',
      (v) => {
        v.actualUses = 4
      },
    ],
    [
      'negative count',
      (v) => {
        v.actualUses = -1
      },
    ],
    [
      'short LIMIT',
      (v) => {
        v.termination = 'LIMIT'
        v.actualUses = 2
      },
    ],
    [
      'non-string termination',
      (v) => {
        Object.assign(v, { termination: ['MATCH'] })
      },
    ],
    [
      'wrong rejection',
      (v) => {
        v.lastRejection = { code: 'OTHER', reason: 'bad' }
      },
    ],
    [
      'extra trace',
      (v) => {
        Object.assign(v, { intermediateStates: [] })
      },
    ],
    [
      'unknown omen',
      (v) => {
        v.consumedOmens.unknown = 1
      },
    ],
    [
      'duplicate consumption',
      (v) => {
        v.consumedOmens.Omen_of_Sinistral_Erasure = 2
      },
    ],
    [
      'fraction consumption',
      (v) => {
        v.consumedOmens.Omen_of_Sinistral_Erasure = 0.5
      },
    ],
    [
      'omitted consumption',
      (v) => {
        v.consumedOmens = {}
      },
    ],
    [
      'duplicate remaining',
      (v) => {
        v.remainingOmens.push(v.remainingOmens[0]!)
      },
    ],
    [
      'witness omitted',
      (v) => {
        v.lastAppliedWitness = null
      },
    ],
    [
      'witness action',
      (v) => {
        v.lastAppliedWitness!.action = 'GREATER_CHAOS'
      },
    ],
    [
      'witness applied false',
      (v) => {
        Object.assign(v.lastAppliedWitness!, { applied: false })
      },
    ],
    [
      'witness remaining',
      (v) => {
        v.lastAppliedWitness!.remainingOmens = []
      },
    ],
    [
      'fabricated witness state',
      (v) => {
        Object.assign(v.lastAppliedWitness!, { state: {} })
      },
    ],
    [
      'witness kind',
      (v) => {
        Object.assign(v.lastAppliedWitness!, { kind: 'ABYSS' })
      },
    ],
    [
      'out of range roll',
      (v) => {
        if (v.finalState.kind === 'CONCRETE')
          v.finalState.item.explicits[0]!.values.life = 999
      },
    ],
    [
      'item level changed',
      (v) => {
        if (v.finalState.kind === 'CONCRETE') v.finalState.item.itemLevel = 81
      },
    ],
    [
      'implicit changed',
      (v) => {
        if (v.finalState.kind === 'CONCRETE')
          v.finalState.item.implicits[0]!.values.base_spirit_from_equipment = 10
      },
    ],
    [
      'unknown canonical property',
      (v) => {
        if (v.finalState.kind === 'CONCRETE')
          Object.assign(v.finalState.item, { futureField: true })
      },
    ],
    [
      'snapshot changed',
      (v) => {
        if (v.finalState.kind === 'CONCRETE')
          v.finalState.item.snapshotId = 'other'
      },
    ],
    [
      'invalid source',
      (v) => {
        if (v.lastAppliedWitness?.kind === 'CONCRETE')
          v.lastAppliedWitness.assumptions.push({
            id: 'bad',
            candidateUnit: 'p',
            n: 1,
            candidates: [],
            min: null,
            max: null,
            sourceUrl: 'javascript:bad',
            reason: 'bad',
          })
      },
    ],
    [
      'probability zero',
      (v) => {
        if (v.lastAppliedWitness?.kind === 'CONCRETE')
          v.lastAppliedWitness.events[0]!.selectionProbability = 0
      },
    ],
    [
      'wrong final event values',
      (v) => {
        if (v.lastAppliedWitness?.kind === 'CONCRETE')
          v.lastAppliedWitness.events[1]!.values.life = 18
      },
    ],
    [
      'empty success events',
      (v) => {
        if (v.lastAppliedWitness?.kind === 'CONCRETE')
          v.lastAppliedWitness.events = []
      },
    ],
    [
      'wrong omen trigger',
      (v) => {
        v.consumedOmens = { Omen_of_Sinistral_Necromancy: 1 }
        v.remainingOmens = ['Omen_of_Sinistral_Erasure']
        v.lastAppliedWitness!.remainingOmens = [...v.remainingOmens]
      },
    ],
  ]
  it.each(invalid)(
    'refuses %s without a receipt or fallback',
    async (_name, change) => {
      mockBatch(change)
      const result = await applyChaosRepeat(
        requestFor(),
        repeatInitial,
        signal(),
      )
      expect(result).toEqual({
        kind: 'invalid-response',
        requestId: requestFor().requestId,
      })
      expect(result).not.toHaveProperty('receipt')
    },
  )

  it.each(['CONCRETE', 'ABYSS'] as const)(
    'keeps positive %s partial completion distinct from complete and zero-use outcomes',
    async (kind) => {
      const request = requestFor(kind)
      mockBatch((v) => {
        v.actualUses = 2
        v.termination = 'INAPPLICABLE'
        v.lastRejection = {
          code: 'CURRENCY_INAPPLICABLE',
          reason: 'NO_ELIGIBLE_REMOVAL',
        }
      })
      expect(
        await applyChaosRepeat(request, repeatInitial, signal()),
      ).toMatchObject({
        kind: 'partial',
        actualUses: 2,
        termination: 'INAPPLICABLE',
      })
      mockBatch((v) => {
        v.actualUses = 2
        v.termination = 'ERROR'
        v.failure = { code: 'REPEAT_EXECUTION_FAILED', stage: 'APPLY' }
      }, 500)
      expect(
        await applyChaosRepeat(request, repeatInitial, signal()),
      ).toMatchObject({ kind: 'partial', actualUses: 2, termination: 'ERROR' })
      mockBatch((v) => {
        v.actualUses = 0
        v.finalState = structuredClone(request.state)
        v.consumedOmens = {}
        v.remainingOmens = [...request.activeOmens]
        v.lastAppliedWitness = null
        v.termination = 'INAPPLICABLE'
        v.lastRejection = {
          code: 'CURRENCY_INAPPLICABLE',
          reason: 'NO_ELIGIBLE_REMOVAL',
        }
      })
      const zero = await applyChaosRepeat(request, repeatInitial, signal())
      expect(zero.kind).toBe('no-use')
      expect(zero).not.toHaveProperty('receipt')
    },
  )

  it.each(['CONCRETE', 'ABYSS'] as const)(
    'accepts explicit one-use %s batches and ERROR at the final matcher without inventing a zero count',
    async (kind) => {
      const request = requestFor(kind)
      mockBatch((v) => {
        v.actualUses = 1
        v.lastAppliedWitness!.consumedOmens = ['Omen_of_Sinistral_Erasure']
      })
      expect(
        await applyChaosRepeat(request, repeatInitial, signal()),
      ).toMatchObject({ kind: 'completed', actualUses: 1 })
      mockBatch((v) => {
        v.termination = 'ERROR'
        v.failure = { code: 'REPEAT_EXECUTION_FAILED', stage: 'MATCHER' }
      }, 500)
      expect(
        await applyChaosRepeat(request, repeatInitial, signal()),
      ).toMatchObject({ kind: 'partial', actualUses: 3 })
      mockBatch((v) => {
        v.termination = 'ERROR'
        v.actualUses = 0
        v.finalState = structuredClone(request.state)
        v.consumedOmens = {}
        v.remainingOmens = [...request.activeOmens]
        v.lastAppliedWitness = null
        v.failure = { code: 'REPEAT_EXECUTION_FAILED', stage: 'APPLY' }
      }, 500)
      expect(
        await applyChaosRepeat(request, repeatInitial, signal()),
      ).toMatchObject({ kind: 'no-use', termination: 'ERROR' })
    },
  )

  it.each([
    ['invalid failure code', { code: 'OTHER', stage: 'APPLY' }],
    [
      'non-string failure stage',
      { code: 'REPEAT_EXECUTION_FAILED', stage: ['MATCHER'] },
    ],
  ])('rejects %s in a 500 partial envelope', async (_label, failure) => {
    mockBatch((v) => {
      v.actualUses = 2
      v.termination = 'ERROR'
      Object.assign(v, { failure })
    }, 500)
    expect(
      (await applyChaosRepeat(requestFor(), repeatInitial, signal())).kind,
    ).toBe('invalid-response')
  })

  it.each([
    [
      'hidden slot constraint',
      (v: TestEnvelope) => {
        if (v.finalState.kind === 'ABYSS')
          v.finalState.abyss.slots[1]!.minimumModifierLevel = null
      },
    ],
    [
      'invented offer',
      (v: TestEnvelope) => {
        if (v.finalState.kind === 'ABYSS')
          v.finalState.abyss.offer = {
            slotId: 'hidden',
            candidates: Array.from({ length: 3 }, () => ({
              modifierId: 's',
              values: { strength: 5 },
            })),
          }
      },
    ],
    [
      'unknown slot property',
      (v: TestEnvelope) => {
        if (v.finalState.kind === 'ABYSS')
          Object.assign(v.finalState.abyss.slots[0]!, { extra: true })
      },
    ],
    [
      'fabricated Abyss events',
      (v: TestEnvelope) => {
        Object.assign(v.lastAppliedWitness!, { events: [] })
      },
    ],
  ] as const)('refuses %s in an Abyss response', async (_label, change) => {
    mockBatch(change)
    expect(
      (await applyChaosRepeat(requestFor('ABYSS'), repeatInitial, signal()))
        .kind,
    ).toBe('invalid-response')
  })
})
