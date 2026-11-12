import { queryOptions, type QueryClient } from '@tanstack/react-query'
import { performanceSettings as budget } from '../../shared/performance'
import { boardRequest } from './api'
import type { Options, Page, Post, Stats, Summary, CommentPage } from './api'

// All board consumers share keys AND freshness rules. A route remount is not a data change.
const publicRead = { staleTime: budget.communityFreshMs, retry: false as const }
export const communityQueries = {
  options: () =>
    queryOptions({
      ...publicRead,
      queryKey: ['community', 'options'],
      staleTime: budget.communityOptionsFreshMs,
      queryFn: ({ signal }) => boardRequest<Options>('options', { signal }),
    }),
  list: (params: string) =>
    queryOptions({
      ...publicRead,
      queryKey: ['community', 'list', params],
      queryFn: ({ signal }) =>
        boardRequest<Page>('posts?' + params, { signal }),
    }),
  post: (id: string) =>
    queryOptions({
      ...publicRead,
      queryKey: ['community', 'post', id],
      staleTime: budget.communityPostFreshMs,
      queryFn: ({ signal }) =>
        boardRequest<Post>('posts/' + encodeURIComponent(id), { signal }),
    }),
  stats: (id: string, viewer: string | null | undefined) =>
    queryOptions({
      ...publicRead,
      queryKey: ['community', 'stats', id, viewer ?? null],
      queryFn: ({ signal }) =>
        boardRequest<Stats>('posts/' + id + '/stats', { signal }),
    }),
  comments: (id: string, page: number) =>
    queryOptions({
      ...publicRead,
      queryKey: ['community', 'comments', id, page],
      queryFn: ({ signal }) =>
        boardRequest<CommentPage>('posts/' + id + '/comments?page=' + page, {
          signal,
        }),
    }),
  linked: (source: string, owner: string | null) =>
    queryOptions({
      ...publicRead,
      queryKey: ['community', 'mine', owner, source],
      queryFn: ({ signal }) =>
        boardRequest<Summary[]>('mine?source=' + encodeURIComponent(source), {
          signal,
        }),
    }),
}

/** A counter mutation already returns the new counts; do not download the film again. */
export function cachePostStats(
  client: QueryClient,
  id: string,
  viewer: string | null | undefined,
  stats: Stats,
) {
  client.setQueryData(communityQueries.stats(id, viewer).queryKey, stats)
  client.setQueriesData<Page>(
    { queryKey: ['community', 'list'] },
    (page) =>
      page && {
        ...page,
        posts: page.posts.map((post) =>
          post.id === id
            ? {
                ...post,
                downloads: stats.downloads,
                recommendations: stats.recommendations,
              }
            : post,
        ),
      },
  )
  // Popular order can change. Mark lists stale without triggering a hidden refetch.
  void client.invalidateQueries({
    queryKey: ['community', 'list'],
    refetchType: 'none',
  })
}

/** Text/session edits affect one post and its listing, never all comments/counters/catalogs. */
export async function invalidatePost(
  client: QueryClient,
  post: Pick<Post, 'id' | 'sourceId'>,
  owner: string | null,
  deleted = false,
) {
  if (deleted)
    client.removeQueries({ queryKey: communityQueries.post(post.id).queryKey })
  await Promise.all([
    ...(!deleted
      ? [
          client.invalidateQueries({
            queryKey: communityQueries.post(post.id).queryKey,
          }),
        ]
      : []),
    client.invalidateQueries({
      queryKey: ['community', 'list'],
      refetchType: 'none',
    }),
    client.invalidateQueries({
      queryKey: communityQueries.linked(post.sourceId, owner).queryKey,
      refetchType: 'none',
    }),
  ])
}
