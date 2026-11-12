import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { CommunityCompose } from './CommunityCompose'
import { initialFixture } from '../../shared/test/craftingFixtures'
import { concreteInitial } from '../crafting/workbenchApi'
import { emptyFilms, startFilm, type Film } from '../crafting/workbenchHistory'
import { setLocale } from '../../shared/i18n/i18n'
const mocks = vi.hoisted(() => ({
  send: vi.fn(),
  load: vi.fn(),
  navigate: vi.fn(),
  user: null as { id: string; displayName: string } | null,
}))
vi.mock('./api', () => ({ boardRequest: mocks.send }))
vi.mock('../account/auth', () => ({
  useAccount: () => ({ user: mocks.user }),
  accountScope: () => mocks.user?.id ?? null,
}))
vi.mock('./CommunityShell', () => ({
  CommunityShell: ({ children }: { children: React.ReactNode }) => children,
}))
vi.mock('../../shared/navigation/navigation', async (original) => ({
  ...(await original<object>()),
  navigate: mocks.navigate,
}))
vi.mock('../crafting/session-library/IndexedFilmRepository', () => ({
  IndexedFilmRepository: class {
    load = mocks.load
    close = vi.fn()
  },
}))
vi.mock('../crafting/session-detail/SessionDetail', () => ({
  SessionDetail: ({
    film,
    cursor,
    onNavigate,
  }: {
    film: Film
    cursor: number
    onNavigate: (cursor: number) => void
  }) => (
    <div>
      <output>
        {film.id}:{cursor}
      </output>
      <button type="button" onClick={() => onNavigate(0)}>
        Inspect first step
      </button>
    </div>
  ),
}))
const selected = startFilm(
  emptyFilms(),
  concreteInitial(initialFixture),
  'selected',
  'test',
).films[0]!
selected.frames.push({
  ...selected.frames[0]!,
  state: { ...selected.frames[0]!.state, itemLevel: 84 },
})
const another = {
  ...selected,
  id: 'another',
  title: 'Do not share this session',
}
beforeEach(async () => {
  await setLocale('en')
  mocks.user = null
  history.replaceState(null, '', '/community/new?session=selected')
  mocks.load.mockResolvedValue({ ...emptyFilms(), films: [another, selected] })
  mocks.send.mockImplementation(async (path: string, init?: RequestInit) => {
    if (path === 'options')
      return {
        currentVersion: '0.5.5',
        versions: ['0.5.5'],
        limits: { bodyLength: 5000, sessionBytes: 1000000, frames: 2000 },
      }
    if (path.startsWith('mine?')) return []
    if (path === 'posts' && init?.body)
      return { ...JSON.parse(String(init.body)), id: 'published' }
    throw Error('Unexpected request: ' + path)
  })
})
afterEach(() => {
  cleanup()
  vi.clearAllMocks()
  history.replaceState(null, '', '/')
})
function mount() {
  return render(
    <QueryClientProvider
      client={
        new QueryClient({ defaultOptions: { queries: { retry: false } } })
      }
    >
      <CommunityCompose />
    </QueryClientProvider>,
  )
}
it('shares the history-selected full session even when inspecting an earlier step, with guest restrictions', async () => {
  const view = mount()
  await screen.findByText('selected:1')
  expect(screen.queryByRole('combobox', { name: 'Select session' })).toBeNull()
  expect(screen.queryByLabelText('Title')).toBeNull()
  expect(screen.queryByLabelText('Description')).toBeNull()
  const reads = mocks.send.mock.calls.length
  fireEvent.click(screen.getByRole('button', { name: 'Inspect first step' }))
  expect(screen.getByText('selected:0')).toBeVisible()
  expect(mocks.send).toHaveBeenCalledTimes(reads)
  fireEvent.change(screen.getByLabelText(/Deletion password/), {
    target: { value: 'temporary-password' },
  })
  await waitFor(() =>
    expect(screen.getByRole('button', { name: 'Publish' })).toBeEnabled(),
  )
  fireEvent.submit(view.container.querySelector('form')!)
  await waitFor(() =>
    expect(mocks.navigate).toHaveBeenCalledWith('/community/post/published'),
  )
  const body = JSON.parse(
    mocks.send.mock.calls.find(([path]) => path === 'posts')![1].body,
  )
  expect(body.sourceId).toBe('selected')
  expect(body.film.frames).toHaveLength(2)
  expect(body.title).toBe('')
  expect(body.body).toBe('')
  expect(body.password).toBe('temporary-password')
})
it('keeps member title/body/version inputs and publishes them without a deletion password', async () => {
  mocks.user = { id: 'owner', displayName: 'Owner' }
  mount()
  await screen.findByLabelText('Title')
  expect(screen.queryByLabelText(/Deletion password/)).toBeNull()
  fireEvent.change(screen.getByLabelText('Title'), {
    target: { value: 'My craft' },
  })
  fireEvent.change(screen.getByLabelText('Description'), {
    target: { value: 'Steps and choices' },
  })
  fireEvent.change(screen.getByLabelText('Game version'), {
    target: { value: '0.5.4' },
  })
  await waitFor(() =>
    expect(screen.getByRole('button', { name: 'Publish' })).toBeEnabled(),
  )
  fireEvent.click(screen.getByRole('button', { name: 'Publish' }))
  await waitFor(() => expect(mocks.navigate).toHaveBeenCalled())
  const body = JSON.parse(
    mocks.send.mock.calls.find(([path]) => path === 'posts')![1].body,
  )
  expect(body).toMatchObject({
    title: 'My craft',
    body: 'Steps and choices',
    gameVersion: '0.5.4',
    password: null,
  })
})
it.each(['/community/new', '/community/new?session=deleted'])(
  'never substitutes another local session when the selected session is absent: %s',
  async (url) => {
    history.replaceState(null, '', url)
    mount()
    await screen.findByText(
      'The source session is not available in this library.',
    )
    expect(screen.queryByRole('button', { name: 'Publish' })).toBeNull()
    expect(screen.queryByText(/another:/)).toBeNull()
    expect(mocks.send).not.toHaveBeenCalled()
    expect(screen.getByRole('link', { name: /History/ })).toHaveAttribute(
      'href',
      '/?workbench=history',
    )
  },
)
it('links to an existing shared post instead of allowing duplicate publication', async () => {
  const fallback = mocks.send.getMockImplementation()!
  mocks.send.mockImplementation((path: string, init?: RequestInit) =>
    path.startsWith('mine?')
      ? Promise.resolve([{ id: 'existing' }])
      : fallback(path, init),
  )
  mount()
  expect(
    await screen.findByRole('link', { name: 'View shared post' }),
  ).toHaveAttribute('href', '/community/post/existing')
  expect(screen.queryByRole('button', { name: 'Publish' })).toBeNull()
})
