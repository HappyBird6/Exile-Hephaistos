import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { expect, it, vi } from 'vitest'
import { setLocale } from '../../../shared/i18n/i18n'
import { initialFixture } from '../../../shared/test/craftingFixtures'
import { twoSteps } from '../session-notation/fixtures.test-support'
import { historyStorageKey, LocalFilmRepository } from '../workbenchHistory'
import { documentFor } from '../session-history/transfer'
import { historyMessages } from '../session-history/messages'
import { SessionImport } from './SessionImport'

async function example(save = true) {
  setLocale('en')
  const { history } = await twoSteps()
  new LocalFilmRepository(localStorage).save(history)
  const saved = localStorage.getItem(historyStorageKey)
  const document = {
    ...documentFor(history.films[0]!),
    future: { preserved: true },
  }
  const onImport = vi.fn(() => save)
  const onKeyDown = vi.fn()
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false, staleTime: Infinity } },
  })
  client.setQueryData(['history-catalog', 'solar', 82], initialFixture)
  render(
    <QueryClientProvider client={client}>
      <div onKeyDown={onKeyDown}>
        <SessionImport disabled={false} onImport={onImport} />
      </div>
    </QueryClientProvider>,
  )
  return { document, saved, onImport, onKeyDown }
}
function choose(raw: string) {
  const file = new File([raw], 'session.json', { type: 'application/json' })
  Object.defineProperty(file, 'text', { value: async () => raw })
  fireEvent.change(screen.getByLabelText('Import session'), {
    target: { files: [file] },
  })
}
it('previews all states before asking the parent to save, preserving unknown data', async () => {
  const { document, onImport, saved } = await example()
  choose(JSON.stringify(document))
  expect(
    await screen.findByRole('heading', { name: 'Import preview' }),
  ).toBeVisible()
  expect(screen.getAllByRole('article')).toHaveLength(
    document.film.frames.length,
  )
  expect(onImport).not.toHaveBeenCalled()
  expect(localStorage.getItem(historyStorageKey)).toBe(saved)
  fireEvent.click(screen.getByRole('button', { name: 'Add this session' }))
  expect(onImport).toHaveBeenCalledExactlyOnceWith(document)
  await waitFor(() =>
    expect(
      screen.queryByRole('heading', { name: 'Import preview' }),
    ).not.toBeInTheDocument(),
  )
})
it('retains the preview and original storage when the parent cannot save', async () => {
  const { document, onImport, saved } = await example(false)
  choose(JSON.stringify(document))
  fireEvent.click(
    await screen.findByRole('button', { name: 'Add this session' }),
  )
  expect(onImport).toHaveBeenCalledOnce()
  expect(await screen.findByRole('alert')).toBeVisible()
  expect(screen.getByRole('heading', { name: 'Import preview' })).toBeVisible()
  expect(localStorage.getItem(historyStorageKey)).toBe(saved)
})
it('cancels a preview with Escape without dismissing the History panel or changing storage', async () => {
  const { document, onImport, onKeyDown, saved } = await example()
  choose(JSON.stringify(document))
  const confirm = await screen.findByRole('button', {
    name: 'Add this session',
  })
  fireEvent.keyDown(confirm, { key: 'Escape' })
  expect(onKeyDown).not.toHaveBeenCalled()
  expect(onImport).not.toHaveBeenCalled()
  expect(screen.getByLabelText('Import session')).toHaveFocus()
  expect(localStorage.getItem(historyStorageKey)).toBe(saved)
})
it('rejects an invalid file without modifying existing sessions', async () => {
  const { onImport, saved } = await example()
  choose('{"format":"wrong"}')
  await waitFor(() => expect(screen.getByRole('alert')).toBeVisible())
  expect(onImport).not.toHaveBeenCalled()
  expect(localStorage.getItem(historyStorageKey)).toBe(saved)
})

it('updates an existing import error when the display language changes', async () => {
  const { onImport } = await example()
  choose('{"format":"invalid"}')
  await waitFor(() =>
    expect(screen.getByRole('alert')).toHaveTextContent(
      historyMessages.en.error,
    ),
  )
  act(() => setLocale('ko'))
  expect(screen.getByRole('alert')).toHaveTextContent(historyMessages.ko.error)
  expect(onImport).not.toHaveBeenCalled()
})
