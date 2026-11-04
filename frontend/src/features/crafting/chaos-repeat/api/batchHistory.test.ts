import { webcrypto } from 'node:crypto'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  currentFilm,
  currentFrame,
  emptyFilms,
  filmRulesetStatus,
  LocalFilmRepository,
  recordChaosRepeat,
  startFilm,
  storedAbyssItem,
  viewFrame,
  verifiedFrameEvidence,
} from '../../workbenchHistory'
import { applyChaosRepeat } from './applyChaosRepeat'
import {
  batchResponse,
  envelopeFor,
  repeatInitial,
  requestFor,
} from './fixtures.test-support'
import { repeatItem } from './stateValidation'
import { extractSessionNotation } from '../../session-notation/extractSessionNotation'
import type { ChaosRepeatRequest } from './types'

beforeEach(() => {
  vi.stubGlobal('crypto', webcrypto)
  vi.stubGlobal(
    'fetch',
    vi.fn(async (_url: RequestInfo | URL, init?: RequestInit) => {
      const body = init!.body as string
      return batchResponse(
        envelopeFor(JSON.parse(body) as ChaosRepeatRequest, body),
      )
    }),
  )
})
async function completed(request = requestFor()) {
  const result = await applyChaosRepeat(
    request,
    repeatInitial,
    new AbortController().signal,
  )
  if (result.kind !== 'completed')
    throw new Error('Expected completed batch fixture')
  return result
}
function start(request: ChaosRepeatRequest) {
  const history = startFilm(
    emptyFilms(),
    repeatItem(request.state),
    'first',
    repeatInitial.rulesetIdentity,
    'synthetic\r\nsource',
  )
  if (request.state.kind === 'ABYSS')
    history.films[0]!.frames[0]!.abyss = structuredClone(request.state.abyss)
  return history
}

describe('actual concrete and Abyss repeat frame evidence', () => {
  it.each(['CONCRETE', 'ABYSS'] as const)(
    'records one %s frame and retains the actual state-free witness',
    async (kind) => {
      const request = requestFor(kind)
      const history = start(request)
      const result = await completed(request)
      const next = recordChaosRepeat(
        history,
        result.receipt,
        'new',
        repeatInitial,
      )
      expect(currentFilm(next)!.frames).toHaveLength(2)
      expect(currentFilm(history)!.frames).toHaveLength(1)
      const frame = currentFrame(next)!
      expect(frame.chaosRepeat).toEqual({
        version: 1,
        action: 'CHAOS',
        uses: 3,
      })
      expect(frame.chaosRepeatWitness).toMatchObject({
        kind,
        action: 'CHAOS',
        consumedOmens: [],
      })
      expect(frame.chaosRepeatWitness).not.toHaveProperty('state')
      expect(
        filmRulesetStatus(currentFilm(next)!, repeatInitial.rulesetIdentity),
      ).toBe('current')
      if (kind === 'CONCRETE') {
        expect(frame.evidence?.consumedOmens).toEqual([
          'Omen_of_Sinistral_Erasure',
        ])
        expect(verifiedFrameEvidence(frame, repeatInitial)).toEqual(
          frame.evidence,
        )
        expect(frame.manual).toBeUndefined()
      } else {
        expect(frame.evidence).toBeUndefined()
        expect(verifiedFrameEvidence(frame, repeatInitial)).toBeUndefined()
        expect(frame.manual?.consumedOmens).toEqual([
          'Omen_of_Sinistral_Erasure',
        ])
        expect(frame.chaosRepeatWitness).not.toHaveProperty('events')
        expect(frame.chaosRepeatWitness).not.toHaveProperty('assumptions')
        expect(frame.abyss?.slots.find((slot) => slot.id === 'hidden')).toEqual(
          request.state.kind === 'ABYSS' ? request.state.abyss.slots[1] : null,
        )
        expect(frame.state).toEqual(storedAbyssItem(frame.abyss!))
      }
      expect(
        extractSessionNotation(currentFilm(next), next.cursor).steps,
      ).toMatchObject([
        {
          currencyId: 'Chaos_Orb',
          uses: 3,
          repeated: true,
          consumedOmenIds: ['Omen_of_Sinistral_Erasure'],
        },
      ])
    },
  )

  it('preserves revealed slots and observed offer without inventing a concrete event', async () => {
    const request = requestFor('ABYSS')
    if (request.state.kind !== 'ABYSS') throw new Error('Expected Abyss')
    const candidate = { modifierId: 's', values: { strength: 5 } }
    request.state.abyss.slots[1]!.modifier = candidate
    request.state.abyss.offer = {
      slotId: 'hidden',
      candidates: Array.from({ length: 3 }, () => structuredClone(candidate)),
    }
    const result = await completed(request)
    const next = recordChaosRepeat(
      start(request),
      result.receipt,
      'new',
      repeatInitial,
    )
    expect(currentFrame(next)?.abyss?.offer).toEqual(request.state.abyss.offer)
    expect(currentFrame(next)?.abyss?.slots[1]).toEqual(
      request.state.abyss.slots[1],
    )
  })

  it.each(['CONCRETE', 'ABYSS'] as const)(
    'restores %s batch undo/redo and branches without touching the original future or legacy bytes',
    async (kind) => {
      const request = requestFor(kind)
      const result = await completed(request)
      const first = recordChaosRepeat(
        start(request),
        result.receipt,
        'unused',
        repeatInitial,
      )
      const oldFilm = currentFilm(first)!
      const original = JSON.stringify(oldFilm)
      const undo = viewFrame(first, 0)
      expect(currentFrame(undo)?.state).toEqual(repeatItem(request.state))
      expect(currentFrame(viewFrame(undo, 1))?.chaosRepeat?.uses).toBe(3)
      const branched = recordChaosRepeat(
        undo,
        result.receipt,
        'branch',
        repeatInitial,
      )
      expect(branched.films).toHaveLength(2)
      expect(branched.films[0]).toBe(oldFilm)
      expect(JSON.stringify(oldFilm)).toBe(original)
      expect(currentFilm(branched)!.frames).toHaveLength(2)
      expect(currentFilm(branched)!.sourceDocument).toBe('synthetic\r\nsource')
      if (kind === 'ABYSS')
        expect(currentFilm(branched)!.frames[0]!.abyss).toEqual(
          request.state.kind === 'ABYSS' ? request.state.abyss : null,
        )
      const bytes = new Map<string, string>()
      const storage = {
        getItem: (key: string) => bytes.get(key) ?? null,
        setItem: (key: string, value: string) => {
          bytes.set(key, value)
        },
      }
      const repository = new LocalFilmRepository(storage)
      repository.save(branched)
      expect(repository.load()).toEqual(branched)
      const restored = currentFrame(repository.load())!
      expect(restored.chaosRepeatWitness?.kind).toBe(kind)
      expect(currentFilm(repository.load())!.frames[0]).not.toHaveProperty(
        'chaosRepeat',
      )
    },
  )

  it('rejects corrupted new witness metadata and leaves memory storage bytes intact', async () => {
    const request = requestFor('ABYSS')
    const result = await completed(request)
    const history = recordChaosRepeat(
      start(request),
      result.receipt,
      'new',
      repeatInitial,
    )
    for (const badWitness of [
      undefined,
      null,
      {},
      { ...currentFrame(history)!.chaosRepeatWitness, action: 'EXALTED' },
      { ...currentFrame(history)!.chaosRepeatWitness, events: [] },
    ]) {
      const bad = structuredClone(history)
      Object.assign(currentFrame(bad)!, { chaosRepeatWitness: badWitness })
      expect(
        filmRulesetStatus(currentFilm(bad)!, repeatInitial.rulesetIdentity),
      ).toBe('inconsistent')
      const raw = JSON.stringify(bad)
      const storage = { getItem: () => raw, setItem: vi.fn() }
      const repository = new LocalFilmRepository(storage)
      expect(() => repository.load()).toThrow()
      expect(() => repository.save(bad)).toThrow()
      expect(storage.setItem).not.toHaveBeenCalled()
    }
    const inconsistent = structuredClone(history)
    currentFrame(inconsistent)!.abyss!.slots[0]!.modifier!.values.life = 18
    expect(
      filmRulesetStatus(
        currentFilm(inconsistent)!,
        repeatInitial.rulesetIdentity,
      ),
    ).toBe('inconsistent')
    const storage = {
      getItem: () => JSON.stringify(inconsistent),
      setItem: vi.fn(),
    }
    expect(() => new LocalFilmRepository(storage).load()).toThrow()
    expect(storage.setItem).not.toHaveBeenCalled()
    const concreteRequest = requestFor()
    const concrete = recordChaosRepeat(
      start(concreteRequest),
      (await completed(concreteRequest)).receipt,
      'new',
      repeatInitial,
    )
    const witness = currentFrame(concrete)!.chaosRepeatWitness!
    Object.assign(witness, {
      qualityLimit: { ruleVersion: 'quality-limit-v1', maximumQuality: 999 },
    })
    expect(
      filmRulesetStatus(currentFilm(concrete)!, repeatInitial.rulesetIdentity),
    ).toBe('inconsistent')
  })
})
