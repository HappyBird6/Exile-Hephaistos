import { afterEach, expect, it, vi } from 'vitest'
import {
  initialFixture,
  jsonResponse,
} from '../../../shared/test/craftingFixtures'
import { concreteInitial } from '../workbenchApi'
import {
  applyBone,
  boneConsumedOmens,
  boneRemainingOmens,
} from '../desecrationCurrency'
import { applyAbyssCurrency, abyssFromConcrete } from './applyAbyssCurrency'
import type { AbyssState } from '../AbyssPanel'
import {
  LocalFilmRepository,
  emptyFilms,
  startFilm,
  recordAbyss,
  currentFrame,
  storedAbyssItem,
} from '../workbenchHistory'

const root = { ...concreteInitial(initialFixture), rarity: 'RARE' as const }
const hidden: AbyssState = {
  item: { ...root, explicits: [] },
  slots: [
    {
      id: 'hidden',
      side: 'SUFFIX',
      modifier: null,
      desecrated: true,
      minimumModifierLevel: 40,
    },
  ],
  offer: null,
}
const signal = () => new AbortController().signal
afterEach(() => {
  vi.unstubAllGlobals()
  localStorage.clear()
})

it('accepts a random side with no required side input and never sends a reveal selection', async () => {
  const fetch = vi.fn<
    (input: RequestInfo | URL, init?: RequestInit) => Promise<Response>
  >(() => Promise.resolve(jsonResponse(hidden)))
  vi.stubGlobal('fetch', fetch)
  const next = await applyBone(
    root,
    'Ancient_Collarbone',
    '',
    '',
    [],
    initialFixture,
    signal(),
    'unchanged',
  )
  expect(next).toEqual(hidden)
  expect(fetch).toHaveBeenCalledTimes(1)
  expect(String(fetch.mock.calls[0]?.[0])).not.toContain('/select')
  const body = JSON.parse(fetch.mock.calls[0]?.[1]?.body as string)
  expect(body).toMatchObject({ side: null, removedSlotId: null })
})

it('sends and consumes only bone omens, preserving Sanctification and other active omens', async () => {
  const omens = ['Omen_of_Dextral_Necromancy', 'Omen_of_Sanctification']
  const fetch = vi.fn<
    (input: RequestInfo | URL, init?: RequestInit) => Promise<Response>
  >(() => Promise.resolve(jsonResponse(hidden)))
  vi.stubGlobal('fetch', fetch)
  await applyBone(
    root,
    'Ancient_Collarbone',
    '',
    '',
    omens,
    initialFixture,
    signal(),
    'unchanged',
  )
  expect(
    JSON.parse(fetch.mock.calls[0]?.[1]?.body as string).activeOmens,
  ).toEqual(['Omen_of_Dextral_Necromancy'])
  expect(boneConsumedOmens(omens)).toEqual(['Omen_of_Dextral_Necromancy'])
  expect(boneRemainingOmens(omens)).toEqual(['Omen_of_Sanctification'])
  expect(omens).toEqual([
    'Omen_of_Dextral_Necromancy',
    'Omen_of_Sanctification',
  ])
})

it('restores common hidden slots without leaking common-only fields into the Abyss request', () => {
  const common = storedAbyssItem(hidden)
  const restored = abyssFromConcrete(common, initialFixture)
  expect(restored.slots).toEqual(hidden.slots)
  expect(restored.slots[0]).not.toHaveProperty('modifierId')
  expect(restored.item.observations).toEqual(common.observations)
  expect(restored.item.conditions).toEqual(common.conditions)
})

it('preserves hidden constraints, offers and dormant bone omens through a normal currency and save/restore', async () => {
  const next = {
    ...hidden,
    slots: [
      ...hidden.slots,
      {
        id: 'added',
        side: 'PREFIX' as const,
        modifier: { modifierId: 'p', values: { life: 10 } },
        desecrated: false,
      },
    ],
  }
  vi.stubGlobal(
    'fetch',
    vi.fn(() =>
      Promise.resolve(
        jsonResponse({
          state: next,
          action: 'EXALTED',
          applied: true,
          reason: '',
          consumedOmens: [],
          remainingOmens: [],
        }),
      ),
    ),
  )
  const result = await applyAbyssCurrency(
    hidden,
    'EXALTED',
    ['Omen_of_Dextral_Necromancy'],
    initialFixture,
    signal(),
    'unchanged',
  )
  expect(result.remainingOmens).toEqual(['Omen_of_Dextral_Necromancy'])
  expect(result.state.slots[0]).toEqual(hidden.slots[0])
  const history = recordAbyss(
    startFilm(
      emptyFilms(),
      storedAbyssItem(hidden),
      'seed',
      initialFixture.rulesetIdentity,
    ),
    storedAbyssItem(hidden),
    result.state,
    result.action,
    'step',
    initialFixture.rulesetIdentity,
    result.consumedOmens,
  )
  const repo = new LocalFilmRepository(localStorage)
  repo.save(history)
  expect(currentFrame(repo.load())?.abyss).toEqual(result.state)
  expect(currentFrame(repo.load())?.manual?.consumedOmens ?? []).toEqual([])
})

it('rejects failure responses that consume omens or change the state', async () => {
  vi.stubGlobal('fetch', () =>
    Promise.resolve(
      jsonResponse({
        state: hidden,
        action: 'ANNULMENT',
        applied: false,
        reason: 'blocked',
        consumedOmens: ['Omen_of_Dextral_Annulment'],
        remainingOmens: [],
      }),
    ),
  )
  await expect(
    applyAbyssCurrency(
      hidden,
      'ANNULMENT',
      ['Omen_of_Dextral_Annulment'],
      initialFixture,
      signal(),
      'unchanged',
    ),
  ).rejects.toThrow('unchanged')
  expect(hidden.slots[0]?.modifier).toBeNull()
})

it('keeps a rejected result unchanged including a pending offer', async () => {
  const offered = {
    ...hidden,
    offer: {
      slotId: 'hidden',
      candidates: Array(3).fill({
        modifierId: 's',
        values: { strength: 5 },
      }),
    },
  }
  vi.stubGlobal('fetch', () =>
    Promise.resolve(
      jsonResponse({
        state: offered,
        action: 'ANNULMENT',
        applied: false,
        reason: 'RESOLVE_OFFER_OR_QUALITY_BEFORE_CURRENCY',
        consumedOmens: [],
        remainingOmens: ['Omen_of_Dextral_Annulment'],
      }),
    ),
  )
  const result = await applyAbyssCurrency(
    offered,
    'ANNULMENT',
    ['Omen_of_Dextral_Annulment'],
    initialFixture,
    signal(),
    'unchanged',
  )
  expect(result.applied).toBe(false)
  expect(result.state).toEqual(offered)
})

it('discards a response that arrives after cancellation even if fetch resolves it', async () => {
  const controller = new AbortController()
  vi.stubGlobal('fetch', async () => {
    controller.abort()
    return jsonResponse(hidden)
  })
  await expect(
    applyBone(
      root,
      'Ancient_Collarbone',
      '',
      '',
      [],
      initialFixture,
      controller.signal,
      'unchanged',
    ),
  ).rejects.toMatchObject({ name: 'AbortError' })
  vi.stubGlobal('fetch', async () =>
    jsonResponse({
      state: hidden,
      action: 'EXALTED',
      applied: true,
      reason: '',
      consumedOmens: [],
      remainingOmens: [],
    }),
  )
  await expect(
    applyAbyssCurrency(
      hidden,
      'EXALTED',
      [],
      initialFixture,
      controller.signal,
      'unchanged',
    ),
  ).rejects.toMatchObject({ name: 'AbortError' })
})
