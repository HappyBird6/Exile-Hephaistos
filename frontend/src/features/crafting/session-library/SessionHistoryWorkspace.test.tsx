import { fireEvent, render, screen, within } from '@testing-library/react'
import { useState } from 'react'
import { expect, it, vi } from 'vitest'
import { setLocale } from '../../../shared/i18n/i18n'
import { initialFixture } from '../../../shared/test/craftingFixtures'
import { concreteInitial } from '../workbenchApi'
import { appendDocument, documentFor } from '../session-history/transfer'
import {
  deleteFilm,
  emptyFilms,
  startFilm,
  undoFilmDeletion,
  updateFilmMetadata,
} from '../workbenchHistory'
import type { Film, FilmDeletion, Frame } from '../workbenchHistory'
import { SessionHistoryWorkspace } from './SessionHistoryWorkspace'
import { sessionLibraryMessages } from './sessionLibraryMessages'

vi.mock('../session-history/HistoryCard', () => ({
  HistoryCard: ({ film, frame }: { film: Film; frame: Frame }) => (
    <output aria-label="previewed frame">
      {film.id}:{film.frames.indexOf(frame)}
    </output>
  ),
}))
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
  history.films[1]!.frames.push({
    ...history.films[1]!.frames[0]!,
    state: { ...state, rarity: 'RARE' },
  })
  return {
    ...history,
    films: history.films.map((film) => {
      const legacy = { ...film }
      delete legacy.sessionNumber
      return legacy
    }),
  }
}
function Example() {
  const [history, setHistory] = useState(fixtureHistory)
  const [deletion, setDeletion] = useState<FilmDeletion | null>(null)
  return (
    <>
      <output aria-label="active session">
        {history.active}:{history.cursor}
      </output>
      <SessionHistoryWorkspace
        history={history}
        onContinue={(id) =>
          setHistory({
            ...history,
            active: id,
            cursor: history.films.find((f) => f.id === id)!.frames.length - 1,
          })
        }
        onImport={(document) => {
          const added = appendDocument(history, document)
          setHistory(added.history)
          return added.id
        }}
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
it('previews the final frame without changing the active session until Continue', () => {
  setLocale('en')
  render(<Example />)
  const before = localStorage.getItem('hephaistos.workbench.films.v1')
  fireEvent.click(screen.getByRole('button', { name: 'Select session: Loved' }))
  expect(screen.getByLabelText('previewed frame')).toHaveTextContent('two:1')
  expect(screen.getByLabelText('active session')).toHaveTextContent('three:0')
  expect(localStorage.getItem('hephaistos.workbench.films.v1')).toBe(before)
  fireEvent.click(
    screen.getByRole('button', { name: 'Continue in simulation' }),
  )
  expect(screen.getByLabelText('active session')).toHaveTextContent('two:1')
})
it('filters favorites with legacy numbers and preserves rename, delete and Undo', () => {
  setLocale('en')
  render(<Example />)
  const nav = within(screen.getByRole('navigation'))
  fireEvent.click(nav.getByRole('button', { name: 'Favorites' }))
  expect(screen.getAllByRole('article')).toHaveLength(1)
  expect(
    screen
      .getByRole('article', { name: 'Loved' })
      .querySelector('.wb-session-card__number'),
  ).toHaveTextContent('2')
  fireEvent.click(screen.getByRole('button', { name: 'Rename: Loved' }))
  fireEvent.change(screen.getByRole('textbox'), {
    target: { value: 'Renamed' },
  })
  fireEvent.click(screen.getByRole('button', { name: 'Save' }))
  fireEvent.click(screen.getByRole('button', { name: 'Delete: Renamed' }))
  fireEvent.click(screen.getByRole('button', { name: 'Delete this session' }))
  expect(screen.getByText('No favorite crafting sessions yet.')).toBeVisible()
  fireEvent.click(screen.getByRole('button', { name: 'Undo deletion' }))
  expect(screen.getByRole('article', { name: 'Renamed' })).toBeVisible()
  expect(screen.getByLabelText('active session')).toHaveTextContent('three:0')
  fireEvent.click(
    screen.getByRole('button', { name: 'Remove favorite: Renamed' }),
  )
  expect(screen.queryByRole('article')).not.toBeInTheDocument()
  fireEvent.click(nav.getByRole('button', { name: 'Session library' }))
  expect(screen.getAllByRole('article')).toHaveLength(3)
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
})
it('imports into the library preview while retaining the active simulation', async () => {
  setLocale('en')
  render(<Example />)
  const film = {
    ...fixtureHistory().films[0]!,
    id: 'imported',
    title: 'Imported',
  }
  const raw = JSON.stringify(documentFor(film))
  const file = new File([raw], 'session.json', { type: 'application/json' })
  Object.defineProperty(file, 'text', { value: async () => raw })
  fireEvent.change(screen.getByLabelText('Import session'), {
    target: { files: [file] },
  })
  fireEvent.click(
    await screen.findByRole('button', { name: 'Add this session' }),
  )
  expect(screen.getByLabelText('previewed frame')).toHaveTextContent(
    'imported:0',
  )
  expect(screen.getByLabelText('active session')).toHaveTextContent('three:0')
})
it.each(Object.entries(sessionLibraryMessages))(
  'shows emoji and visible navigation labels in %s',
  (locale, messages) => {
    setLocale(locale as keyof typeof sessionLibraryMessages)
    render(<Example />)
    const nav = within(
      screen.getByRole('navigation', { name: messages.navigation }),
    )
    expect(
      nav.getByRole('button', { name: messages.library }),
    ).toHaveTextContent('🕘')
    expect(
      nav.getByRole('button', { name: messages.favorites }),
    ).toHaveTextContent('⭐')
    fireEvent.click(nav.getByRole('button', { name: messages.favorites }))
    expect(
      nav.getByRole('button', { name: messages.favorites }),
    ).toHaveAttribute('aria-current', 'page')
    expect(screen.getAllByRole('article')).toHaveLength(1)
  },
)
