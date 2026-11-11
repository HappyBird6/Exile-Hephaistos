import { CurrencyPriceContext } from '../currency-prices/context'
import {
  fireEvent,
  render,
  screen,
  within,
  waitFor,
} from '@testing-library/react'
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
  const [expiresAt] = useState(() => Date.now() + 60000)
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
        onRename={(id, title) => {
          setHistory(updateFilmMetadata(history, id, { title }))
          return true
        }}
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
                expiresAt,
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
  fireEvent.click(screen.getByRole('button', { name: 'Rename: Loved' }))
  const editor = screen.getByRole('textbox')
  editor.textContent = 'Renamed'
  fireEvent.keyDown(editor, { key: 'Enter' })
  fireEvent.click(screen.getByRole('button', { name: 'Delete: Renamed' }))
  expect(screen.getByText('No favorite crafting sessions yet.')).toBeVisible()
  expect(document.querySelector('.wb-history-preview')).toBeNull()
  fireEvent.click(
    screen.getByRole('button', { name: 'Undo deletion: Renamed' }),
  )
  expect(screen.getByRole('article', { name: 'Renamed' })).toBeVisible()
  expect(document.querySelector('.wb-history-preview')).toBeVisible()
  expect(screen.getByLabelText('active session')).toHaveTextContent('three:0')
  fireEvent.click(
    screen.getByRole('button', { name: 'Remove favorite: Renamed' }),
  )
  expect(screen.queryByRole('article')).not.toBeInTheDocument()
  expect(document.querySelector('.wb-history-preview')).toBeNull()
  fireEvent.click(nav.getByRole('button', { name: 'Sessions' }))
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
  await waitFor(() =>
    expect(screen.getByLabelText('previewed frame')).toHaveTextContent(
      'imported:0',
    ),
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

it('previews the selected session total independently from the active simulation cursor', () => {
  const history = fixtureHistory()
  history.active = 'one'
  history.cursor = 0
  const selected = history.films.find((film) => film.id === 'two')!
  selected.frames = [
    selected.frames[0]!,
    ...['EXALTED', 'EXALTED'].map((action) => ({
      state: selected.frames[0]!.state,
      action,
      manual: { rulesetIdentity: 'fixture', action, consumedOmens: [] },
    })),
  ]
  const market = {
    data: {
      snapshot: {
        league: 'Current League',
        updatedAt: '2026-11-09T06:00:00Z',
        exaltedPerDivine: 500,
        prices: {
          'Exalted Orb': { name: 'Exalted Orb', divine: 0.002, exalted: 1 },
        },
      },
      nextRefreshAllowedAt: '2026-11-09T06:05:00Z',
      unavailable: false,
      stale: false,
    },
    pending: false,
    refreshing: false,
    error: false,
    refresh: () => {},
  }
  render(
    <CurrencyPriceContext.Provider value={market}>
      <SessionHistoryWorkspace
        history={history}
        onContinue={vi.fn()}
        onImport={vi.fn()}
        onRename={vi.fn()}
        onFavoriteChange={vi.fn()}
        onDelete={vi.fn()}
      />
    </CurrencyPriceContext.Provider>,
  )
  fireEvent.click(screen.getByRole('button', { name: 'Select session: Loved' }))
  const cost = document.querySelector(
    '.wb-history-preview .crafting-cost-line',
  )!
  expect(
    within(cost as HTMLElement).getByLabelText('2 Exalted Orb'),
  ).toBeVisible()
  expect(history.active).toBe('one')
  expect(history.cursor).toBe(0)
})
