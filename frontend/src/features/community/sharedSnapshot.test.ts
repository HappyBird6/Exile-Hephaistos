import { expect, it } from 'vitest'
import { publicFilm, exceedsShareLimit } from './sharedSnapshot'
import { initialFixture } from '../../shared/test/craftingFixtures'
import { concreteInitial } from '../crafting/workbenchApi'
import { startFilm, emptyFilms } from '../crafting/workbenchHistory'
it('measures exactly the public UTF-8 snapshot without uploading private source data', () => {
  const film = startFilm(
    emptyFilms(),
    concreteInitial(initialFixture),
    'id',
    'rules',
  ).films[0]!
  const withPrivate = {
    ...film,
    sourceDocument: 'private'.repeat(100000),
    title: '제작 기록',
  }
  const snapshot = publicFilm(withPrivate as typeof film)
  expect(snapshot).not.toHaveProperty('sourceDocument')
  const bytes = new TextEncoder().encode(JSON.stringify(snapshot)).byteLength
  const limits = {
    sessionBytes: bytes,
    frames: 1,
    bodyLength: 5000,
    commentLength: 2000,
  }
  expect(exceedsShareLimit(film, limits)).toBe(false)
  expect(exceedsShareLimit(film, { ...limits, sessionBytes: bytes - 1 })).toBe(
    true,
  )
  expect(
    exceedsShareLimit(
      { ...film, frames: [...film.frames, ...film.frames] },
      limits,
    ),
  ).toBe(true)
})
