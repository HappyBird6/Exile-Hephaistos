import { useQuery } from '@tanstack/react-query'
import { useI18n, gameName } from '../../shared/i18n/i18n'
import { BaseImage } from '../crafting/base-picker/BaseImage'
import { communityText } from './messages'
import { CommunityShell } from './CommunityShell'
import { boardRequest } from './api'
import type { Page, Options } from './api'
export function CommunityList() {
  const { locale } = useI18n(),
    t = communityText(locale),
    params = new URLSearchParams(location.search)
  const options = useQuery({
    queryKey: ['community', 'options'],
    queryFn: () => boardRequest<Options>('options'),
    staleTime: 60000,
  })
  const posts = useQuery({
    queryKey: ['community', 'list', params.toString()],
    queryFn: ({ signal }) => boardRequest<Page>('posts?' + params, { signal }),
  })
  function filter(key: string, value: string) {
    const next = new URLSearchParams(location.search)
    if (value) next.set(key, value)
    else next.delete(key)
    next.delete('page')
    location.href = '/community?' + next
  }
  return (
    <CommunityShell>
      <h1>{t.board}</h1>
      <div className="wb-history-workspace community-layout">
        <aside className="wb-history-nav">
          <label>
            {t.equipment}
            <select
              value={params.get('base') ?? ''}
              onChange={(e) => filter('base', e.target.value)}
            >
              <option value="">{t.all}</option>
              {options.data?.bases.map((id) => (
                <option key={id} value={id}>
                  {gameName(id, id, locale)}
                </option>
              ))}
            </select>
          </label>
          <label>
            {t.version}
            <select
              value={params.get('version') ?? ''}
              onChange={(e) => filter('version', e.target.value)}
            >
              <option value="">{t.all}</option>
              {Array.from(
                new Set([
                  options.data?.currentVersion,
                  ...(options.data?.versions ?? []),
                ]),
              )
                .filter(Boolean)
                .map((v) => (
                  <option key={v} value={v}>
                    {v}
                  </option>
                ))}
            </select>
          </label>
          {params.get('author') && <a href="/community">{t.all}</a>}
        </aside>
        <section className="wb-history-content">
          <div className="community-list-actions">
            <a href="/community/new">{t.newPost}</a>
            <select
              aria-label={t.popular}
              value={params.get('sort') ?? 'newest'}
              onChange={(e) => filter('sort', e.target.value)}
            >
              <option value="newest">{t.newest}</option>
              <option value="popular">{t.popular}</option>
            </select>
            <Pagination page={posts.data} />
          </div>
          {posts.isPending && <p role="status">…</p>}
          {posts.isError && <p role="alert">{t.error}</p>}
          {posts.data?.posts.length === 0 && <p>{t.empty}</p>}
          <div className="community-posts">
            {posts.data?.posts.map((post) => (
              <article className="wb-session-card" key={post.id}>
                <a
                  className="community-post-link"
                  href={'/community/post/' + post.id}
                >
                  <BaseImage
                    baseItemId={post.baseId}
                    label={gameName(post.baseId, post.baseId, locale)}
                  />
                  <div>
                    <h2>
                      {post.title || gameName(post.baseId, post.baseId, locale)}
                    </h2>
                    <p>
                      {post.gameVersion} · {t.session} {post.steps}
                    </p>
                    <p>
                      {t.downloads} {post.downloads} · {t.votes}{' '}
                      {post.recommendations}
                    </p>
                    <time>
                      {new Date(post.createdAt).toLocaleString(locale)}
                    </time>
                  </div>
                </a>
                {post.authorId ? (
                  <a href={'/community?author=' + post.authorId}>
                    {post.author}
                  </a>
                ) : (
                  <span>{t.anonymous}</span>
                )}
              </article>
            ))}
          </div>
        </section>
      </div>
    </CommunityShell>
  )
}
function Pagination({ page }: { page: Page | undefined }) {
  const { locale } = useI18n(),
    t = communityText(locale)
  if (!page) return null
  const pages = Math.max(1, Math.ceil(page.total / page.pageSize))
  const link = (n: number) => {
    const p = new URLSearchParams(location.search)
    p.set('page', String(n))
    return '/community?' + p
  }
  return (
    <nav aria-label={t.board} className="wb-history-pagination">
      {page.page > 1 && <a href={link(page.page - 1)}>{t.previous}</a>}
      {Array.from({ length: pages }, (_, i) => i + 1)
        .filter((n) => n === 1 || n === pages || Math.abs(n - page.page) <= 2)
        .map((n) => (
          <a
            key={n}
            href={link(n)}
            aria-current={n === page.page ? 'page' : undefined}
          >
            {n}
          </a>
        ))}
      {page.page < pages && <a href={link(page.page + 1)}>{t.next}</a>}
    </nav>
  )
}
