import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { setLocale } from '../../../shared/i18n/i18n'
import { initialFixture } from '../../../shared/test/craftingFixtures'
import { twoSteps } from '../session-notation/fixtures.test-support'
import { historyStorageKey, LocalFilmRepository } from '../workbenchHistory'
import { SessionHistoryPage } from './SessionHistoryPage'
import { snapshotPages } from './snapshot'
import { saveBlob } from './transfer'

vi.mock('./snapshot', () => ({ snapshotPages: vi.fn() }))
vi.mock('./transfer', async (original) => ({
  ...(await original<typeof import('./transfer')>()),
  saveBlob: vi.fn(),
}))
const originalClipboard = Object.getOwnPropertyDescriptor(
  navigator,
  'clipboard',
)
afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
  vi.clearAllMocks()
  if (originalClipboard)
    Object.defineProperty(navigator, 'clipboard', originalClipboard)
  else Reflect.deleteProperty(navigator, 'clipboard')
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
  client.setQueryData(['session-history', film.id, false], film)
  client.setQueryData(['history-catalog', 'solar', 82], initialFixture)
  const view = render(
    <QueryClientProvider client={client}>
      <SessionHistoryPage id={film.id} />
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
it('copies a created sharing link and retries a blocked clipboard without publishing twice', async () => {
  const { film, saved, view } = await example()
  const post = vi
    .fn()
    .mockResolvedValue(
      new Response(JSON.stringify({ id: 'a'.repeat(48) }), { status: 201 }),
    )
  vi.stubGlobal('fetch', post)
  const copy = vi
    .fn()
    .mockRejectedValueOnce(Error('clipboard'))
    .mockResolvedValue(undefined)
  Object.defineProperty(navigator, 'clipboard', {
    configurable: true,
    value: { writeText: copy },
  })
  fireEvent.click(
    within(view.container.querySelector('header')!).getByRole('button', {
      name: 'Create sharing link',
    }),
  )
  expect(post).not.toHaveBeenCalled()
  const confirm = within(
    screen.getByRole('region', { name: 'Create sharing link' }),
  ).getByRole('button', { name: 'Create sharing link' })
  fireEvent.click(confirm)
  expect(await screen.findByRole('alert')).toBeVisible()
  expect(post).toHaveBeenCalledTimes(1)
  expect(JSON.parse(post.mock.calls[0]![1].body).film).toEqual(film)
  fireEvent.click(confirm)
  const copied = await screen.findByRole('button', { name: 'Link copied' })
  expect(copied.closest('header')).not.toBeNull()
  expect(post).toHaveBeenCalledTimes(1)
  expect(copy).toHaveBeenLastCalledWith(
    new URL('/history/shared/' + 'a'.repeat(48), location.origin).href,
  )
  expect(view.container.querySelector('.history-link')).toBeNull()
  expect(screen.queryByRole('textbox')).not.toBeInTheDocument()
  expect(localStorage.getItem(historyStorageKey)).toBe(saved)
})
