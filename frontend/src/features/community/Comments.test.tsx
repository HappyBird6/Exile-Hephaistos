import { afterEach, expect, it, vi } from 'vitest'
import {
  cleanup,
  render,
  screen,
  fireEvent,
  waitFor,
} from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Comments } from './Comments'
const state = vi.hoisted(() => ({
  user: { id: 'owner' } as { id: string } | null,
}))
const send = vi.hoisted(() => vi.fn())
vi.mock('../account/auth', () => ({ useAccount: () => ({ user: state.user }) }))
vi.mock('../../shared/i18n/i18n', () => ({ useI18n: () => ({ locale: 'en' }) }))
vi.mock('./api', () => ({ boardRequest: send }))
afterEach(() => {
  cleanup()
  vi.resetAllMocks()
  state.user = { id: 'owner' }
})
function mount() {
  send.mockImplementation(async (path: string, init?: RequestInit) => {
    if (path === 'options') return { limits: { commentLength: 2000 } }
    if (init?.method) return undefined
    return {
      comments: [
        {
          id: 'c1',
          authorId: 'owner',
          author: 'Author',
          body: '<img src=x onerror=alert(1)>',
          revision: 3,
          createdAt: '2026-11-12T00:00:00Z',
          updatedAt: '2026-11-12T00:00:00Z',
        },
      ],
      total: 1,
      page: 1,
      pageSize: 20,
    }
  })
  return render(
    <QueryClientProvider
      client={
        new QueryClient({ defaultOptions: { queries: { retry: false } } })
      }
    >
      <Comments post="post" />
    </QueryClientProvider>,
  )
}
it('renders untrusted text safely and submits edits with the existing revision', async () => {
  const view = mount()
  await screen.findByText('<img src=x onerror=alert(1)>')
  expect(view.container.querySelector('img')).toBeNull()
  expect(screen.queryByRole('heading', { name: /Comments/ })).toBeNull()
  expect(screen.getByRole('region', { name: 'Comments' })).toBeInTheDocument()
  fireEvent.click(screen.getByRole('button', { name: 'Edit' }))
  fireEvent.change(screen.getByRole('textbox', { name: 'Write a comment' }), {
    target: { value: 'Updated' },
  })
  fireEvent.click(screen.getByRole('button', { name: 'Save' }))
  await waitFor(() =>
    expect(send).toHaveBeenCalledWith('posts/post/comments/c1', {
      method: 'PUT',
      body: JSON.stringify({ id: 'c1', body: 'Updated', revision: 3 }),
    }),
  )
})
it('does not expose composition or ownership controls to guests', async () => {
  state.user = null
  mount()
  await screen.findByText('<img src=x onerror=alert(1)>')
  expect(screen.queryByRole('textbox')).toBeNull()
  expect(screen.queryByRole('button', { name: 'Edit' })).toBeNull()
  expect(screen.queryByRole('button', { name: 'Delete' })).toBeNull()
})
