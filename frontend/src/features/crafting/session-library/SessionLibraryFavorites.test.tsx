import { fireEvent, render, screen, within } from '@testing-library/react'
import { useRef, useState } from 'react'
import { expect, it } from 'vitest'
import { setLocale } from '../../../shared/i18n/i18n'
import { initialFixture } from '../../../shared/test/craftingFixtures'
import { concreteInitial } from '../workbenchApi'
import {
  deleteFilm,
  emptyFilms,
  startFilm,
  undoFilmDeletion,
  updateFilmMetadata,
} from '../workbenchHistory'
import type { FilmDeletion } from '../workbenchHistory'
import { SessionLibrary } from './SessionLibrary'
import { SessionLibraryButtons } from './SessionLibraryButtons'
import type { SessionLibraryView } from './SessionLibraryButtons'
import { sessionLibraryMessages } from './sessionLibraryMessages'

function fixtureHistory() {
  const state = concreteInitial(initialFixture)
  const history = updateFilmMetadata(
    startFilm(
      startFilm(
        startFilm(emptyFilms(), state, 'one', 'fixture'),
        state,
        'two',
        'fixture',
      ),
      state,
      'three',
      'fixture',
    ),
    'two',
    { title: 'Loved', favorite: true },
  )
  // v1 legacy records derive their number from the unfiltered list position.
  return {
    ...history,
    films: history.films.map((film) => {
      const legacy = { ...film }
      delete legacy.sessionNumber
      return legacy
    }),
  }
}

function Example({
  favoritesInitially = false,
}: {
  favoritesInitially?: boolean
}) {
  const [history, setHistory] = useState(fixtureHistory)
  const [view, setView] = useState<SessionLibraryView | null>(
    favoritesInitially ? 'favorites' : null,
  )
  const [deletion, setDeletion] = useState<FilmDeletion | null>(null)
  const historyAnchor = useRef<HTMLButtonElement>(null)
  const favoritesAnchor = useRef<HTMLButtonElement>(null)
  return (
    <>
      <output aria-label="active session">{history.active}</output>
      <SessionLibraryButtons
        libraryId="sessions"
        openView={view}
        onOpenChange={setView}
        historyAnchor={historyAnchor}
        favoritesAnchor={favoritesAnchor}
      />
      <SessionLibrary
        id="sessions"
        anchor={view === 'favorites' ? favoritesAnchor : historyAnchor}
        open={view !== null}
        favoritesOnly={view === 'favorites'}
        onOpenChange={(open) => {
          if (!open) setView(null)
        }}
        history={history}
        onSelect={(id) => setHistory({ ...history, active: id })}
        onRename={(id, title) =>
          setHistory(updateFilmMetadata(history, id, { title }))
        }
        onFavoriteChange={(id, favorite) =>
          setHistory(updateFilmMetadata(history, id, { favorite }))
        }
        onDelete={(id) => {
          const result = deleteFilm(history, id)
          if (result) {
            setHistory(result.history)
            setDeletion(result.deletion)
          }
        }}
        {...(deletion
          ? {
              undo: {
                title: deletion.film.title ?? '',
                onUndo: () => {
                  setHistory(undoFilmDeletion(history, deletion))
                  setDeletion(null)
                },
              },
            }
          : {})}
      />
    </>
  )
}

it('opens the same library through History and Favorites, retaining legacy numbers, titles, active state and Undo', () => {
  render(<Example />)
  fireEvent.click(screen.getByRole('button', { name: 'History' }))
  expect(screen.getAllByRole('article')).toHaveLength(3)
  expect(screen.getByRole('button', { name: 'History' })).toHaveAttribute(
    'aria-expanded',
    'true',
  )
  fireEvent.click(screen.getByRole('button', { name: 'Favorites' }))
  const panel = within(screen.getByRole('dialog', { name: 'Favorites' }))
  expect(panel.getAllByRole('article')).toHaveLength(1)
  expect(
    panel
      .getByRole('article', { name: 'Loved' })
      .querySelector('.wb-session-card__number'),
  ).toHaveTextContent('2')
  expect(screen.getByLabelText('active session')).toHaveTextContent('three')
  expect(
    panel.getByRole('button', { name: 'Select session: Loved' }),
  ).toHaveAttribute('aria-pressed', 'false')
  fireEvent.click(panel.getByRole('button', { name: 'Rename: Loved' }))
  fireEvent.change(panel.getByRole('textbox'), { target: { value: 'Renamed' } })
  fireEvent.click(panel.getByRole('button', { name: 'Save' }))
  fireEvent.click(panel.getByRole('button', { name: 'Delete: Renamed' }))
  fireEvent.click(panel.getByRole('button', { name: 'Delete this session' }))
  expect(panel.queryByRole('article')).not.toBeInTheDocument()
  expect(panel.getByText('No favorite crafting sessions yet.')).toBeVisible()
  fireEvent.click(panel.getByRole('button', { name: 'Undo deletion' }))
  expect(panel.getByRole('article', { name: 'Renamed' })).toBeVisible()
  expect(screen.getByLabelText('active session')).toHaveTextContent('three')
  fireEvent.click(
    panel.getByRole('button', { name: 'Remove favorite: Renamed' }),
  )
  expect(panel.queryByRole('article')).not.toBeInTheDocument()
  fireEvent.keyDown(screen.getByRole('dialog'), { key: 'Escape' })
  expect(screen.getByRole('button', { name: 'Favorites' })).toHaveFocus()
  fireEvent.click(screen.getByRole('button', { name: 'History' }))
  expect(screen.getAllByRole('article')).toHaveLength(3)
  expect(
    screen.getByRole('button', { name: 'Favorite: Renamed' }),
  ).toHaveAttribute('aria-pressed', 'false')
})

it('keeps the active favorite selected and closes each trigger by toggling', () => {
  render(<Example favoritesInitially />)
  fireEvent.click(screen.getByRole('button', { name: 'Select session: Loved' }))
  expect(screen.getByRole('article', { name: 'Loved' })).toHaveAttribute(
    'data-active',
    'true',
  )
  fireEvent.click(screen.getByRole('button', { name: 'Favorites' }))
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  fireEvent.click(screen.getByRole('button', { name: 'History' }))
  fireEvent.click(screen.getByRole('button', { name: 'History' }))
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
})

it.each(Object.entries(sessionLibraryMessages))(
  'labels both emoji triggers and the favorite panel in %s',
  (locale, messages) => {
    setLocale(locale as keyof typeof sessionLibraryMessages)
    render(<Example favoritesInitially />)
    expect(
      screen.getByRole('button', { name: messages.history }),
    ).toHaveAttribute('aria-controls', 'sessions')
    expect(
      screen.getByRole('button', { name: messages.favorites }),
    ).toHaveAttribute('aria-expanded', 'true')
    expect(
      screen.getByRole('dialog', { name: messages.favorites }),
    ).toBeVisible()
    expect(
      screen.getByRole('button', { name: messages.closeFavorites }),
    ).toBeVisible()
  },
)
