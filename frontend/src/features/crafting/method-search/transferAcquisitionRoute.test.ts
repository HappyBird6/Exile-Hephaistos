import { IDBFactory } from 'fake-indexeddb'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { webcrypto } from 'node:crypto'
import { initialFixture } from '../../../shared/test/craftingFixtures'
import * as auth from '../../account/auth'
import { publicFilm } from '../../community/sharedSnapshot'
import { IndexedFilmRepository } from '../session-library/IndexedFilmRepository'
import { concreteInitial } from '../workbenchApi'
import type { Film } from '../workbenchHistory'
import {
  transferAcquisitionRoute,
  type RouteDestination,
} from './transferAcquisitionRoute'

beforeEach(() => {
  localStorage.clear()
  vi.stubGlobal('indexedDB', new IDBFactory())
  vi.stubGlobal('crypto', webcrypto)
})
afterEach(() => {
  vi.restoreAllMocks()
  vi.unstubAllGlobals()
})

const root = concreteInitial(initialFixture)
const identity = initialFixture.rulesetIdentity
function recipe(currencyId = 'Perfect_Exalted_Orb'): Film {
  const prefix = { modifierId: 'p', values: { life: 18 } }
  return {
    id: 'request-0',
    createdAt: '',
    rulesetIdentity: identity,
    title: 'Target 1 · 1 → 2',
    frames: [
      {
        action: null,
        state: { ...root, rarity: 'RARE' },
        rulesetIdentity: identity,
      },
      {
        state: { ...root, rarity: 'RARE', explicits: [prefix] },
        action: 'SESSION_PLAN',
        rulesetIdentity: identity,
        projectedCurrencies: [{ currencyId, omenIds: [] }],
        projectedChance: { probability: 0.25 },
      },
      {
        state: {
          ...root,
          rarity: 'RARE',
          explicits: [prefix, { modifierId: 's', values: { strength: 6 } }],
        },
        action: 'SESSION_PLAN',
        rulesetIdentity: identity,
        projectedCurrencies: [
          {
            currencyId: 'Chaos_Orb',
            omenIds: ['Omen_of_Whittling'],
            omensPerUse: true,
            expectedUses: 2.6,
            maxUses: 1000,
          },
        ],
        projectedChance: { probability: 0.5 },
      },
    ],
  }
}
function open(film: Film, cursor: number, destination: RouteDestination) {
  return transferAcquisitionRoute(
    film,
    cursor,
    destination,
    initialFixture,
    null,
    new AbortController().signal,
  )
}

it('saves the full verified grade once for the library and composer without switching the active craft', async () => {
  const film = recipe()
  const link = await open(film, 1, 'library')
  const id = new URL(link, location.origin).searchParams.get('session')
  const repository = new IndexedFilmRepository()
  const first = await repository.load()
  expect(first.active).toBeNull()
  expect(first.films).toHaveLength(1)
  const saved = first.films[0]!
  expect(new Date(saved.createdAt).toISOString()).toBe(saved.createdAt)
  expect(saved.id).toBe(id)
  expect(saved.id.length).toBeLessThanOrEqual(160)
  expect(saved.frames).toEqual(film.frames)
  expect(film.createdAt).toBe('')
  expect(publicFilm(saved).frames[2]).toEqual(film.frames[2])
  await repository.save({
    ...first,
    films: [{ ...saved, favorite: true, title: 'My version' }],
  })
  expect(await open(film, 0, 'community')).toBe('/community/new?session=' + id)
  const reopened = await repository.load()
  expect(reopened.films).toHaveLength(1)
  expect(reopened.films[0]).toMatchObject({
    title: 'My version',
    favorite: true,
    createdAt: saved.createdAt,
  })
  expect(await open(recipe('Exalted_Orb'), 1, 'library')).not.toBe(link)
  expect((await repository.load()).films).toHaveLength(2)
  await repository.close()
})

it('forks at the selected verified checkpoint, keeps the full source and preserves user edits on retry', async () => {
  const film = recipe()
  expect(await open(film, 1, 'simulation')).toBe('/?workbench=simulation')
  const repository = new IndexedFilmRepository()
  const next = await repository.load()
  const [source, branch] = next.films
  expect(next.active).toBe(branch!.id)
  expect(next.cursor).toBe(1)
  expect(source!.frames).toEqual(film.frames)
  expect(branch!.frames).toEqual(film.frames.slice(0, 2))
  expect(branch!.continuationOf).toEqual({
    sessionId: source!.id,
    frameIndex: 1,
    rulesetIdentity: identity,
  })
  const modified = { ...source!, frames: source!.frames.slice(0, 1) }
  await repository.save({ ...next, films: [modified, branch!] })
  const resaved = await open(film, 0, 'library')
  expect(resaved.endsWith('session=' + source!.id)).toBe(false)
  expect(await open(film, 2, 'library')).toBe(resaved)
  const retrieved = await repository.load()
  expect(retrieved.films).toHaveLength(3)
  expect(retrieved.films[0]).toEqual(modified)
  expect(retrieved.active).toBe(branch!.id)
  expect(retrieved.films[2]!.frames).toEqual(film.frames)
  await repository.close()
})

it('rejects stale scopes, mismatched rulesets and an invalid later checkpoint before changing the library', async () => {
  const film = recipe()
  await expect(
    transferAcquisitionRoute(
      film,
      0,
      'library',
      initialFixture,
      'previous-account',
      new AbortController().signal,
    ),
  ).rejects.toThrow('ACCOUNT_CHANGED')
  await expect(
    open({ ...film, rulesetIdentity: 'obsolete' }, 0, 'library'),
  ).rejects.toThrow('INVALID_ROUTE_SESSION')
  film.frames[2]!.state.explicits[1]!.values.strength = 999
  await expect(open(film, 0, 'community')).rejects.toThrow(
    'INVALID_ROUTE_STATE',
  )
  const repository = new IndexedFilmRepository()
  expect((await repository.load()).films).toHaveLength(0)
  await repository.close()
})

it('stores a signed-in selection in that account alone and retains the workbench quality default', async () => {
  vi.spyOn(auth, 'accountScope').mockReturnValue('owner-A')
  const baseItemId = 'Metadata/Items/Weapons/TwoHandWeapons/Bows/FourBow1'
  const catalog = {
    ...initialFixture,
    equipmentQuality: 0,
    state: { ...initialFixture.state, baseItemId, implicits: [] },
  }
  const film = recipe()
  film.frames = film.frames.map((frame) => ({
    ...frame,
    state: { ...frame.state, baseItemId, implicits: [] },
  }))
  await transferAcquisitionRoute(
    film,
    2,
    'community',
    catalog,
    'owner-A',
    new AbortController().signal,
  )
  const guest = new IndexedFilmRepository()
  const owner = new IndexedFilmRepository(localStorage, indexedDB, 'owner-A')
  expect((await guest.load()).films).toHaveLength(0)
  const [saved] = (await owner.load()).films
  expect(
    saved!.frames.every((frame) => frame.state.equipmentQuality === 0),
  ).toBe(true)
  expect(
    film.frames.every((frame) => frame.state.equipmentQuality === undefined),
  ).toBe(true)
  await guest.close()
  await owner.close()
})
