import { fireEvent, render, screen, within } from '@testing-library/react'
import { useRef } from 'react'
import { expect, it, vi } from 'vitest'
import { concreteInitial } from '../workbenchApi'
import { initialFixture } from '../../../shared/test/craftingFixtures'
import { emptyFilms, startFilm, updateFilmMetadata } from '../workbenchHistory'
import { SessionLibrary } from './SessionLibrary'

it('renders small image cards, passes explicit metadata changes and confirms only the selected deletion', () => {
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
    rename = vi.fn(),
    favorite = vi.fn(),
    remove = vi.fn(),
    undo = vi.fn()
  function Example() {
    const anchor = useRef<HTMLButtonElement>(null)
    return (
      <>
        <button ref={anchor}>History</button>
        <SessionLibrary
          id="sessions"
          anchor={anchor}
          open
          onOpenChange={vi.fn()}
          history={history}
          onSelect={select}
          onRename={rename}
          onFavoriteChange={favorite}
          onDelete={remove}
          undo={{ title: 'Removed', onUndo: undo }}
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
  fireEvent.click(first.getByRole('button', { name: 'Rename: My amulet' }))
  fireEvent.change(first.getByRole('textbox'), {
    target: { value: 'Untrimmed title ' },
  })
  fireEvent.click(first.getByRole('button', { name: 'Save' }))
  expect(rename).toHaveBeenCalledWith('one', 'Untrimmed title ')
  fireEvent.click(screen.getByRole('button', { name: 'Delete: Session 2' }))
  expect(remove).not.toHaveBeenCalled()
  fireEvent.click(screen.getByRole('button', { name: 'Cancel' }))
  expect(remove).not.toHaveBeenCalled()
  fireEvent.click(screen.getByRole('button', { name: 'Delete: Session 2' }))
  fireEvent.click(screen.getByRole('button', { name: 'Delete this session' }))
  expect(remove).toHaveBeenCalledExactlyOnceWith('two')
  fireEvent.click(screen.getByRole('button', { name: 'Undo deletion' }))
  expect(undo).toHaveBeenCalledOnce()
})
