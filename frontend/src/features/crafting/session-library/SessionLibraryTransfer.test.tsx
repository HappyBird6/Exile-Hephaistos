import { fireEvent, render, screen } from '@testing-library/react'
import { useRef } from 'react'
import { expect, it, vi } from 'vitest'
import { concreteInitial } from '../workbenchApi'
import { initialFixture } from '../../../shared/test/craftingFixtures'
import {
  emptyFilms,
  startFilm,
  updateFilmMetadata,
  historyStorageKey,
  LocalFilmRepository,
} from '../workbenchHistory'
import { saveBlob } from '../session-history/transfer'
import { SessionLibrary } from './SessionLibrary'

vi.mock('../session-history/transfer', async (original) => ({
  ...(await original<typeof import('../session-history/transfer')>()),
  saveBlob: vi.fn(),
}))
it('exports the selected row rather than the active session, including when storage writes are disabled', async () => {
  const state = concreteInitial(initialFixture)
  const history = updateFilmMetadata(
    startFilm(
      startFilm(emptyFilms(), state, 'one', 'fixture-ruleset'),
      state,
      'two',
      'fixture-ruleset',
    ),
    'one',
    { title: 'First' },
  )
  const first = {
    ...history.films[0]!,
    sourceDocument: 'Original\r\ntext',
    future: { kept: true },
  }
  history.films[0] = first
  new LocalFilmRepository(localStorage).save(history)
  const saved = localStorage.getItem(historyStorageKey)
  const select = vi.fn()
  function Example() {
    const anchor = useRef<HTMLButtonElement>(null)
    return (
      <>
        <button ref={anchor}>History</button>
        <SessionLibrary
          id="transfer"
          anchor={anchor}
          open
          disabled
          history={history}
          onOpenChange={vi.fn()}
          onSelect={select}
          onRename={vi.fn()}
          onFavoriteChange={vi.fn()}
          onDelete={vi.fn()}
        />
      </>
    )
  }
  render(<Example />)
  const button = screen.getByRole('button', { name: 'Export session: First' })
  expect(button).toBeEnabled()
  fireEvent.click(button)
  expect(saveBlob).toHaveBeenCalledExactlyOnceWith(
    expect.any(Blob),
    'session.json',
  )
  const blob = vi.mocked(saveBlob).mock.calls[0]![0]
  const raw = await new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result))
    reader.onerror = reject
    reader.readAsText(blob)
  })
  expect(JSON.parse(raw).film).toEqual(first)
  expect(history.active).toBe('two')
  expect(select).not.toHaveBeenCalled()
  expect(localStorage.getItem(historyStorageKey)).toBe(saved)
})
