import { fireEvent, render, screen, within } from '@testing-library/react'
import { expect, it, vi } from 'vitest'
import { concreteInitial } from '../workbenchApi'
import { initialFixture } from '../../../shared/test/craftingFixtures'
import { emptyFilms, startFilm, updateFilmMetadata } from '../workbenchHistory'
import { SessionLibrary } from './SessionLibrary'
import { sessionTitle } from './sessionTitle'

it('renders session actions with a styled overview and deletes only the requested card immediately', () => {
  const solar = {
    ...concreteInitial(initialFixture),
    baseItemId: 'Metadata/Items/Amulets/FourAmulet9',
  }
  const history = updateFilmMetadata(
    startFilm(
      startFilm(emptyFilms(), solar, 'one', 'fixture-ruleset'),
      solar,
      'two',
      'fixture-ruleset',
    ),
    'one',
    { title: 'My amulet', favorite: true },
  )
  const select = vi.fn(),
    favorite = vi.fn(),
    remove = vi.fn()
  function Example() {
    return (
      <>
        <SessionLibrary
          history={history}
          onSelect={select}
          onFavoriteChange={favorite}
          onDelete={remove}
        />
      </>
    )
  }
  render(<Example />)
  const first = within(screen.getByRole('article', { name: 'My amulet' }))
  expect(first.getByRole('img', { name: 'Solar Amulet' })).toHaveAttribute(
    'src',
    '/assets/currency/solar-amulet.webp',
  )
  expect(
    first.getByRole('button', { name: 'Remove favorite: My amulet' }),
  ).toHaveAttribute('aria-pressed', 'true')
  fireEvent.click(
    first.getByRole('button', { name: 'Remove favorite: My amulet' }),
  )
  expect(favorite).toHaveBeenCalledWith('one', false)
  fireEvent.click(
    first.getByRole('button', { name: 'Select session: My amulet' }),
  )
  expect(select).toHaveBeenCalledWith('one')
  fireEvent.click(screen.getByRole('article', { name: 'My amulet' }))
  expect(select).toHaveBeenCalledTimes(2)
  expect(first.queryByRole('button', { name: /Rename/ })).toBeNull()
  const actions = screen
    .getByRole('article', { name: 'My amulet' })
    .querySelector('.wb-session-card__actions')!
  expect(actions.firstElementChild).toHaveClass('wb-session-card__overview')
  expect(first.getByRole('link', { name: 'Session overview' })).toHaveAttribute(
    'href',
    '/history/one?from=history',
  )
  fireEvent.click(
    screen.getByRole('button', {
      name: `Delete: ${sessionTitle(history.films[1]!, 'en')}`,
    }),
  )
  expect(remove).toHaveBeenCalledExactlyOnceWith('two')
  expect(
    screen.queryByRole('button', { name: 'Delete this session' }),
  ).toBeNull()
})
