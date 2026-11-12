import { beforeEach, afterEach, expect, it, vi } from 'vitest'
import { IDBFactory } from 'fake-indexeddb'
import { initialFixture } from '../../shared/test/craftingFixtures'
import { concreteInitial } from '../crafting/workbenchApi'
import { startFilm, emptyFilms } from '../crafting/workbenchHistory'
import { IndexedFilmRepository } from '../crafting/session-library/IndexedFilmRepository'
import {
  baseFavoritesKey,
  readBaseFavorites,
} from '../crafting/base-favorites/repository'
import { reusePost } from './reuse'
import type { Post } from './api'
beforeEach(() => {
  localStorage.clear()
  vi.stubGlobal('indexedDB', new IDBFactory())
})
afterEach(() => vi.unstubAllGlobals())
function post(): Post {
  const film = startFilm(
    emptyFilms(),
    concreteInitial(initialFixture),
    'source',
    'fixture-ruleset',
  ).films[0]!
  film.frames.push({
    ...structuredClone(film.frames[0]!),
    state: { ...structuredClone(film.frames[0]!.state), itemLevel: 84 },
  })
  return {
    id: 'test',
    authorId: null,
    author: null,
    sourceId: 'source',
    baseId: 'solar',
    title: 'shared',
    body: '',
    film,
    revision: 1,
    sharedStep: 0,
    gameVersion: '0.5.5',
    createdAt: film.createdAt,
    updatedAt: film.createdAt,
  }
}
it('imports the full immutable path once, independent of the selected preview step', async () => {
  const p = post(),
    before = JSON.stringify(p)
  const first = await reusePost(p, 0, 'import', new AbortController().signal)
  await reusePost(p, 1, 'import', new AbortController().signal)
  const repo = new IndexedFilmRepository()
  const history = await repo.load()
  expect(history.films).toHaveLength(1)
  expect(history.films[0]?.frames).toHaveLength(2)
  expect(first).toContain('workbench=history')
  expect(JSON.stringify(p)).toBe(before)
  await repo.close()
})
it('favorites only the selected item state and rejects an invalid step without writes', async () => {
  const p = post()
  await reusePost(p, 0, 'favorite', new AbortController().signal)
  expect(
    readBaseFavorites(localStorage.getItem(baseFavoritesKey))[0]?.frames[0]
      ?.state.itemLevel,
  ).toBe(82)
  await expect(
    reusePost(p, 100, 'favorite', new AbortController().signal),
  ).rejects.toThrow()
  expect(
    readBaseFavorites(localStorage.getItem(baseFavoritesKey)),
  ).toHaveLength(1)
})
