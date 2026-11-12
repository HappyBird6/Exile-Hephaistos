import { act, cleanup, render, screen, waitFor } from '@testing-library/react'
import {
  QueryClient,
  QueryClientProvider,
  useQuery,
} from '@tanstack/react-query'
import { afterEach, expect, it, vi } from 'vitest'
import {
  communityQueries as q,
  cachePostStats,
  invalidatePost,
} from './queries'
import { performanceSettings } from '../../shared/performance'
import type { Page } from './api'
const send = vi.hoisted(() => vi.fn())
vi.mock('./api', () => ({ boardRequest: send }))
afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
  send.mockReset()
})
function Reader({ detail }: { detail: boolean }) {
  const options = useQuery(q.options())
  const post = useQuery(q.post('one'))
  const comments = useQuery({ ...q.comments('one', 1), enabled: detail })
  const stats = useQuery({ ...q.stats('one', null), enabled: detail })
  const list = useQuery({ ...q.list(''), enabled: !detail })
  const queries = [options, post, ...(detail ? [comments, stats] : [list])]
  return (
    <output>
      {queries.every((q) => q.isSuccess && !q.isFetching) ? 'ready' : 'loading'}
    </output>
  )
}
it('reuses preview payloads, options, comments and counters across repeated route remounts; expires bounded caches', async () => {
  send.mockResolvedValue({})
  const client = new QueryClient()
  const mount = (detail: boolean) =>
    render(
      <QueryClientProvider client={client}>
        <Reader detail={detail} />
      </QueryClientProvider>,
    )
  let view = mount(false)
  await screen.findByText('ready')
  expect(send).toHaveBeenCalledTimes(3)
  view.unmount()
  view = mount(true)
  await screen.findByText('ready')
  expect(send).toHaveBeenCalledTimes(5)
  for (const detail of [false, true, false, true]) {
    view.unmount()
    view = mount(detail)
    await screen.findByText('ready')
    expect(send).toHaveBeenCalledTimes(5)
  }
  view.unmount()
  vi.spyOn(Date, 'now').mockReturnValue(
    Date.now() + performanceSettings.communityPostFreshMs + 1,
  )
  view = mount(true)
  await screen.findByText('ready')
  expect(send.mock.calls.filter(([path]) => path === 'options')).toHaveLength(1)
  expect(send.mock.calls.filter(([path]) => path === 'posts/one')).toHaveLength(
    2,
  )
  expect(
    send.mock.calls.filter(([path]) => path.endsWith('/stats')),
  ).toHaveLength(2)
  view.unmount()
  client.clear()
})
it('invalidates only the edited post and its listing/link, leaving unrelated comments and catalogs fresh', async () => {
  send.mockResolvedValue({})
  const client = new QueryClient()
  const view = render(
    <QueryClientProvider client={client}>
      <Reader detail />
    </QueryClientProvider>,
  )
  await screen.findByText('ready')
  client.setQueryData(['community', 'post', 'other'], { id: 'other' })
  client.setQueryData(['crafting', 'initial'], { catalog: 'unchanged' })
  send.mockClear()
  await act(() =>
    invalidatePost(client, { id: 'one', sourceId: 'source' }, null),
  )
  await waitFor(() => expect(send).toHaveBeenCalledTimes(1))
  expect(send.mock.calls[0]?.[0]).toBe('posts/one')
  expect(client.getQueryState(q.post('other').queryKey)?.isInvalidated).toBe(
    false,
  )
  expect(client.getQueryState(['crafting', 'initial'])?.isInvalidated).toBe(
    false,
  )
  view.unmount()
  client.clear()
})
it('uses mutation response counts without a stats GET and keeps recommendation state scoped to the viewer', () => {
  const client = new QueryClient()
  client.setQueryData(q.stats('one', 'other-user').queryKey, {
    downloads: 1,
    recommendations: 1,
    recommended: false,
  })
  client.setQueryData(q.list('').queryKey, {
    posts: [{ id: 'one', downloads: 0, recommendations: 0 }],
    total: 1,
    page: 1,
    pageSize: 20,
  } as Page)
  cachePostStats(client, 'one', 'owner', {
    downloads: 1,
    recommendations: 2,
    recommended: true,
  })
  expect(send).not.toHaveBeenCalled()
  expect(
    client.getQueryData(q.stats('one', 'owner').queryKey)?.recommended,
  ).toBe(true)
  expect(
    client.getQueryData(q.stats('one', 'other-user').queryKey)?.recommended,
  ).toBe(false)
  expect(
    client.getQueryData(q.list('').queryKey)?.posts[0]?.recommendations,
  ).toBe(2)
  client.clear()
})
