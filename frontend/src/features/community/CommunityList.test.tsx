import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { afterEach, expect, it, vi } from 'vitest'
import { CommunityList } from './CommunityList'
import { initialFixture } from '../../shared/test/craftingFixtures'
import { concreteInitial } from '../crafting/workbenchApi'
import { startFilm, emptyFilms } from '../crafting/workbenchHistory'
import { setLocale } from '../../shared/i18n/i18n'
const send = vi.hoisted(() => vi.fn())
vi.mock('./api', () => ({ boardRequest: send }))
vi.mock('./CommunityShell', () => ({
  CommunityShell: ({ children }: { children: React.ReactNode }) => children,
}))
vi.mock('../crafting/session-detail/SessionDetail', () => ({
  SessionDetail: ({
    film,
    cursor,
    actions,
  }: {
    film: { id: string }
    cursor: number
    actions: React.ReactNode
  }) => (
    <>
      <output aria-label="preview">
        {film.id}:{cursor}
      </output>
      {actions}
    </>
  ),
}))
afterEach(() => {
  cleanup()
  vi.clearAllMocks()
  history.replaceState(null, '', '/')
})
it('selects a card into the shared right preview without opening the detail page or refetching the list', async () => {
  setLocale('en')
  history.replaceState(null, '', '/community')
  const film = startFilm(
    emptyFilms(),
    concreteInitial(initialFixture),
    'one',
    'fixture',
  ).films[0]!
  const items = ['one', 'two'].map((id) => ({
    id,
    title: id,
    baseId: film.frames[0]!.state.baseItemId,
    gameVersion: '0.5.5',
    downloads: 0,
    recommendations: 0,
    createdAt: film.createdAt,
  }))
  send.mockImplementation(async (path: string) =>
    path === 'options'
      ? { classes: [], bases: [], versions: [], currentVersion: '0.5.5' }
      : path.startsWith('posts?')
        ? { posts: items, page: 1, pageSize: 20, total: 2 }
        : {
            ...items.find((p) => path.endsWith(p.id)),
            film: { ...film, id: path.endsWith('two') ? 'two' : 'one' },
            sharedStep: 0,
          },
  )
  render(
    <QueryClientProvider
      client={
        new QueryClient({ defaultOptions: { queries: { retry: false } } })
      }
    >
      <CommunityList />
    </QueryClientProvider>,
  )
  await screen.findByText('one:0')
  expect(screen.queryByRole('link', { name: 'Share session' })).toBeNull()
  expect(document.querySelector('.wb-history-nav select')).toBeNull()
  expect(
    document.querySelector('.wb-history-content .community-filters'),
  ).not.toBeNull()
  const calls = send.mock.calls.filter(([p]) => p.startsWith('posts?')).length
  fireEvent.click(screen.getByRole('button', { name: 'two' }))
  await screen.findByText('two:0')
  expect(location.pathname).toBe('/community')
  expect(send.mock.calls.filter(([p]) => p.startsWith('posts?'))).toHaveLength(
    calls,
  )
  expect(screen.getByRole('link', { name: /View details/ })).toHaveAttribute(
    'href',
    expect.stringContaining('/community/post/two'),
  )
  act(() => {
    history.replaceState(null, '', '/community?selected=one')
    window.dispatchEvent(new PopStateEvent('popstate'))
  })
  await waitFor(() => expect(screen.getByText('one:0')).toBeVisible())
})
