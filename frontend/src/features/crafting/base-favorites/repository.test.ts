import { beforeEach, expect, it, vi } from 'vitest'
import {
  baseFavoritesKey,
  favoriteCheckpoint,
  saveBaseFavorite,
  readBaseFavorites,
  removeBaseFavorite,
} from './repository'
import { startFilm, emptyFilms } from '../workbenchHistory'
import { concreteInitial } from '../workbenchApi'
import { initialFixture } from '../../../shared/test/craftingFixtures'
beforeEach(() => localStorage.removeItem(baseFavoritesKey))
const film = () =>
  startFilm(
    emptyFilms(),
    concreteInitial(initialFixture),
    'original',
    initialFixture.rulesetIdentity,
  ).films[0]!
it('keeps the entire final item and hidden state without copying the crafting history', () => {
  const source = film()
  const first = source.frames[0]!
  source.frames.push({
    ...first,
    state: {
      ...first.state,
      equipmentQuality: 28,
      augmentSockets: 2,
      installedAugments: [{ slotIndex: 0, augmentId: 'Iron_Rune' }],
      explicits: [
        { modifierId: 'fixture', values: { life: 12 }, fractured: true },
      ],
      conditions: ['CORRUPTED'],
    },
    action: 'EXALTED',
  })
  const checkpoint = favoriteCheckpoint(source)
  expect(checkpoint.frames).toHaveLength(1)
  expect(checkpoint.frames[0]!.action).toBeNull()
  expect(checkpoint.frames[0]!.state).toEqual(source.frames[1]!.state)
  checkpoint.frames[0]!.state.explicits[0]!.values.life = 99
  expect(source.frames[1]!.state.explicits[0]!.values.life).toBe(12)
})
it('persists, deduplicates and removes independently of the original session', () => {
  const source = film()
  const id = saveBaseFavorite(source)
  expect(saveBaseFavorite(source)).toBe(id)
  expect(
    readBaseFavorites(localStorage.getItem(baseFavoritesKey)),
  ).toHaveLength(1)
  removeBaseFavorite(id)
  expect(readBaseFavorites(localStorage.getItem(baseFavoritesKey))).toEqual([])
  expect(source.frames).toHaveLength(1)
})
it('does not overwrite corrupt data or pretend a quota failure succeeded', () => {
  localStorage.setItem(baseFavoritesKey, '{broken')
  expect(() => saveBaseFavorite(film())).toThrow()
  expect(localStorage.getItem(baseFavoritesKey)).toBe('{broken')
  localStorage.removeItem(baseFavoritesKey)
  const save = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
    throw new DOMException('full', 'QuotaExceededError')
  })
  expect(() => saveBaseFavorite(film())).toThrow('full')
  save.mockRestore()
  expect(localStorage.getItem(baseFavoritesKey)).toBeNull()
})
