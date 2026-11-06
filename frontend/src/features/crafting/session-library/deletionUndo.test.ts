import { expect, it } from 'vitest'
import { initialFixture } from '../../../shared/test/craftingFixtures'
import { concreteInitial } from '../workbenchApi'
import {
  emptyFilms,
  startFilm,
  LocalFilmRepository,
  historyStorageKey,
} from '../workbenchHistory'
import {
  queueSessionDeletion,
  restoreDeletedSession,
  clearDeletionUndo,
} from './deletionUndo'
function history() {
  const state = concreteInitial(initialFixture)
  return ['one', 'two', 'three'].reduce(
    (films, id) =>
      startFilm(
        films,
        state,
        id,
        initialFixture.rulesetIdentity,
        'Original item text',
      ),
    emptyFilms(),
  )
}
it('keeps the latest removal recoverable after reload with the original deadline and exact data', () => {
  const original = history()
  const staged = queueSessionDeletion(original, 'three', 1000)!
  const repository = new LocalFilmRepository(localStorage)
  repository.save(staged)
  const reloaded = repository.load()
  expect(reloaded.pendingDeletion?.expiresAt).toBe(61000)
  expect(reloaded.films.map((film) => film.id)).toEqual(['one', 'two'])
  const restored = restoreDeletedSession(reloaded, 60999)
  expect(restored.films).toEqual(
    staged.films.concat(staged.pendingDeletion!.deletion.film),
  )
  expect(restored.films[2]).toMatchObject(original.films[2]!)
  expect(restored.active).toBe('three')
  expect(restored.cursor).toBe(original.cursor)
  expect(restored.pendingDeletion).toBeUndefined()
})
it('expires exactly at one minute and cannot restore an older removal', () => {
  const first = queueSessionDeletion(history(), 'one', 1000)!
  const latest = queueSessionDeletion(first, 'two', 2000)!
  expect(restoreDeletedSession(latest, 61999).films.map((f) => f.id)).toEqual([
    'two',
    'three',
  ])
  expect(restoreDeletedSession(latest, 62000).films.map((f) => f.id)).toEqual([
    'three',
  ])
  expect(restoreDeletedSession(latest, 62000).pendingDeletion).toBeUndefined()
  expect(clearDeletionUndo(latest).films).toEqual(latest.films)
})
it('keeps a newly selected session when restoring a removed record', () => {
  const staged = queueSessionDeletion(history(), 'three', 1000)!
  const next = startFilm(
    staged,
    concreteInitial(initialFixture),
    'new',
    initialFixture.rulesetIdentity,
  )
  const restored = restoreDeletedSession(next, 2000)
  expect(restored.active).toBe('new')
  expect(restored.films.map((f) => f.id)).toEqual([
    'one',
    'two',
    'three',
    'new',
  ])
})
it('rejects a corrupt persisted recovery record without overwriting it', () => {
  const staged = queueSessionDeletion(history(), 'one', 1000)!
  const raw = JSON.stringify({
    ...staged,
    pendingDeletion: {
      ...staged.pendingDeletion,
      deletion: { ...staged.pendingDeletion!.deletion, film: { id: 'bad' } },
    },
  })
  localStorage.setItem(historyStorageKey, raw)
  expect(() => new LocalFilmRepository(localStorage).load()).toThrow(/invalid/)
  expect(localStorage.getItem(historyStorageKey)).toBe(raw)
})
it('does not overwrite a record imported with the removed ID', () => {
  const original = history()
  const staged = queueSessionDeletion(original, 'one', 1000)!
  const imported = {
    ...staged,
    films: [
      ...staged.films,
      { ...original.films[0]!, title: 'Imported replacement' },
    ],
  }
  const restored = restoreDeletedSession(imported, 2000)
  expect(restored.films.find((f) => f.id === 'one')?.title).toBe(
    'Imported replacement',
  )
  expect(restored.pendingDeletion).toBeUndefined()
})

it('rejects a recovery cursor that could produce an invalid active session', () => {
  const staged = queueSessionDeletion(history(), 'three', 1000)!
  staged.pendingDeletion!.deletion.cursor = 999
  localStorage.setItem(historyStorageKey, JSON.stringify(staged))
  expect(() => new LocalFilmRepository(localStorage).load()).toThrow(/invalid/)
})
