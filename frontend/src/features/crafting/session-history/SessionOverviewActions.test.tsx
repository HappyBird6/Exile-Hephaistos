import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { setLocale } from '../../../shared/i18n/i18n'
import { initialFixture } from '../../../shared/test/craftingFixtures'
import { twoSteps } from '../session-notation/fixtures.test-support'
import { historyStorageKey, LocalFilmRepository } from '../workbenchHistory'
import { SessionOverviewPage } from './SessionOverviewPage'
import { snapshotPages } from './snapshot'
import { saveBlob } from './transfer'
import { historyMessages } from './messages'

vi.mock('./snapshot', () => ({ snapshotPages: vi.fn() }))
vi.mock('./transfer', async (original) => ({
  ...(await original<typeof import('./transfer')>()),
  saveBlob: vi.fn(),
}))
afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
  vi.clearAllMocks()
})
async function example() {
  setLocale('en')
  const { history } = await twoSteps()
  const film = history.films[0]!
  new LocalFilmRepository(localStorage).save(history)
  const saved = localStorage.getItem(historyStorageKey)
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false, staleTime: Infinity } },
  })
  client.setQueryData(['session-overview', film.id], film)
  client.setQueryData(['history-catalog', 'solar', 82], initialFixture)
  const view = render(
    <QueryClientProvider client={client}>
      <SessionOverviewPage id={film.id} />
    </QueryClientProvider>,
  )
  return { film, saved, view }
}
it('immediately downloads every PNG with no download list or history mutation', async () => {
  const { saved, view } = await example()
  const pages = [
    new Blob(['first'], { type: 'image/png' }),
    new Blob(['second'], { type: 'image/png' }),
  ]
  vi.mocked(snapshotPages).mockResolvedValue(pages)
  fireEvent.click(screen.getByRole('button', { name: 'Save PNG snapshots' }))
  await waitFor(() => expect(saveBlob).toHaveBeenCalledTimes(2))
  expect(saveBlob).toHaveBeenNthCalledWith(1, pages[0], 'session-1.png')
  expect(saveBlob).toHaveBeenNthCalledWith(2, pages[1], 'session-2.png')
  expect(view.container.querySelector('.history-pngs')).toBeNull()
  expect(localStorage.getItem(historyStorageKey)).toBe(saved)
})
it('keeps the single-page filename and restores the button when capture fails', async () => {
  await example()
  const page = new Blob(['one'], { type: 'image/png' })
  vi.mocked(snapshotPages)
    .mockResolvedValueOnce([page])
    .mockRejectedValueOnce(Error('capture'))
  const button = screen.getByRole('button', { name: 'Save PNG snapshots' })
  fireEvent.click(button)
  await waitFor(() =>
    expect(saveBlob).toHaveBeenCalledWith(page, 'session.png'),
  )
  await waitFor(() => expect(button).toBeEnabled())
  fireEvent.click(button)
  expect(await screen.findByRole('alert')).toBeVisible()
  expect(button).toBeEnabled()
})
it('does not request server snapshots for a missing local session', async () => {
  setLocale('en')
  const request = vi.fn()
  vi.stubGlobal('fetch', request)
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  render(
    <QueryClientProvider client={client}>
      <SessionOverviewPage id="missing" />
    </QueryClientProvider>,
  )
  expect(await screen.findByText(/Session unavailable/)).toBeVisible()
  expect(
    screen.getByRole('button', { name: 'Save PNG snapshots' }),
  ).toBeDisabled()
  expect(request).not.toHaveBeenCalled()
})

it('updates an existing snapshot error when the language changes', async () => {
  const { saved } = await example()
  vi.mocked(snapshotPages).mockRejectedValue(Error('capture failed'))
  fireEvent.click(
    screen.getByRole('button', { name: historyMessages.en.snapshot }),
  )
  await waitFor(() =>
    expect(screen.getByRole('alert')).toHaveTextContent(
      historyMessages.en.error,
    ),
  )
  act(() => setLocale('ko'))
  expect(screen.getByRole('alert')).toHaveTextContent(historyMessages.ko.error)
  expect(localStorage.getItem(historyStorageKey)).toBe(saved)
})
