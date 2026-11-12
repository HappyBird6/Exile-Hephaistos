import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { afterEach, expect, it, vi } from 'vitest'
import { PostEditor } from './PostEditor'
import type { Post } from './api'
const send = vi.hoisted(() => vi.fn())
const load = vi.hoisted(() => vi.fn())
vi.mock('./api', () => ({ boardRequest: send }))
vi.mock('../account/auth', () => ({
  useAccount: () => ({ user: { id: 'owner' } }),
  accountScope: () => 'owner',
}))
vi.mock('../../shared/i18n/i18n', () => ({ useI18n: () => ({ locale: 'en' }) }))
vi.mock('../crafting/session-library/IndexedFilmRepository', () => ({
  IndexedFilmRepository: class {
    load = load
    close = vi.fn()
  },
}))
afterEach(() => {
  cleanup()
  vi.clearAllMocks()
})
const post = {
  id: 'post',
  authorId: 'owner',
  sourceId: 'source',
  title: 'Old title',
  body: 'Old body',
  revision: 2,
  gameVersion: '0.5.5',
} as Post
function mount() {
  send.mockResolvedValue({
    limits: { bodyLength: 5000, sessionBytes: 200000, frames: 2000 },
  })
  render(
    <QueryClientProvider
      client={
        new QueryClient({ defaultOptions: { queries: { retry: false } } })
      }
    >
      <PostEditor post={post} />
    </QueryClientProvider>,
  )
}
it('edits text without loading or publishing the local session', async () => {
  mount()
  fireEvent.click(screen.getByRole('button', { name: 'Edit text' }))
  fireEvent.change(screen.getByLabelText('Description'), {
    target: { value: 'Edited only' },
  })
  fireEvent.click(screen.getByRole('button', { name: 'Save' }))
  await waitFor(() =>
    expect(send).toHaveBeenCalledWith('posts/post/text', {
      method: 'PUT',
      body: JSON.stringify({
        title: 'Old title',
        body: 'Edited only',
        revision: 2,
      }),
    }),
  )
  expect(load).not.toHaveBeenCalled()
})
it('does not pretend to update a session when the original library record is missing', async () => {
  load.mockResolvedValue({ films: [] })
  mount()
  fireEvent.click(screen.getByRole('button', { name: 'Update session' }))
  await screen.findByText(
    'The source session is not available in this library.',
  )
  expect(screen.getByRole('button', { name: 'Update session' })).toBeDisabled()
})
