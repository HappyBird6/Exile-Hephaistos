import { useRef } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useI18n, gameName } from '../../shared/i18n/i18n'
import { AppLink } from '../../shared/navigation/AppLink'
import { navigate, useAppLocation } from '../../shared/navigation/navigation'
import { HistoryWorkspaceLayout } from '../crafting/session-library/HistoryWorkspaceLayout'
import { SessionListCard } from '../crafting/session-library/SessionListCard'
import { SessionDetail } from '../crafting/session-detail/SessionDetail'
import { validStoredFilm } from '../crafting/workbenchHistory'
import { AuthorLink } from './AuthorLink'
import { listParams, listQuery, authorReturn } from './listNavigation'
import { CommunityBoardsNav } from './CommunityBoardsNav'
import { revisionText } from './revisionMessages'
import { communityText } from './messages'
import { CommunityShell } from './CommunityShell'
import type { Page } from './api'
import { communityQueries } from './queries'
export function CommunityList() {
  useAppLocation()
  const { locale } = useI18n(),
    t = communityText(locale),
    r = revisionText(locale)
  const preview = useRef<HTMLElement>(null)
  const options = useQuery(communityQueries.options())
  const params = listParams(location.search, options.data?.currentVersion)
  const posts = useQuery({
    ...communityQueries.list(listQuery(params)),
    enabled: !!options.data,
  })
  const selected =
    posts.data?.posts.find((p) => p.id === params.get('selected')) ??
    posts.data?.posts[0]
  const detail = useQuery({
    ...communityQueries.post(selected?.id ?? ''),
    enabled: !!selected,
  })
  function filter(key: string, value: string) {
    const next = new URLSearchParams(params)
    if (value || key === 'version') next.set(key, value)
    else next.delete(key)
    next.delete('page')
    next.delete('selected')
    if (key === 'itemClass') next.delete('base')
    navigate('/community?' + next, { scroll: false })
  }
  function select(id: string) {
    const next = new URLSearchParams(params)
    next.set('selected', id)
    navigate('/community?' + next, { replace: true, scroll: false })
    if (window.innerWidth <= 760)
      preview.current?.scrollIntoView({ block: 'nearest' })
  }
  const detailsHref = selected
    ? '/community/post/' +
      selected.id +
      '?' +
      new URLSearchParams({
        from:
          '?' +
          new URLSearchParams({
            ...Object.fromEntries(params),
            selected: selected.id,
          }),
      })
    : '/community'
  return (
    <CommunityShell>
      <div className="workspace-panel stash-panel wb-history-panel">
        <HistoryWorkspaceLayout
          navigation={
            <CommunityBoardsNav
              classes={options.data?.classes ?? []}
              selected={params.get('itemClass') ?? ''}
              onSelect={(value) => filter('itemClass', value)}
            />
          }
          heading={
            <>
              {params.get('author') && (
                <AppLink
                  className="community-author-back"
                  href={authorReturn(location.search)}
                >
                  <span aria-hidden="true">←</span> {r.backToList}
                </AppLink>
              )}
              <h2>
                {params.get('author') &&
                (posts.data?.author?.name || posts.data?.posts[0]?.author)
                  ? r.authorSessions.replace(
                      '{name}',
                      posts.data?.author?.name ||
                        posts.data?.posts[0]?.author ||
                        '',
                    )
                  : t.board}
              </h2>
            </>
          }
          beforeColumns={
            <>
              <div className="community-filters">
                <label>
                  {r.base}
                  <select
                    aria-label={r.base}
                    value={params.get('base') ?? ''}
                    onChange={(e) => filter('base', e.target.value)}
                  >
                    <option value="">{t.all}</option>
                    {options.data?.bases
                      .filter(
                        (id) =>
                          !params.get('itemClass') ||
                          options.data?.baseClasses[id] ===
                            params.get('itemClass'),
                      )
                      .map((id) => (
                        <option key={id} value={id}>
                          {gameName(id, id, locale)}
                        </option>
                      ))}
                  </select>
                </label>
                <label>
                  {t.version}
                  <select
                    aria-label={t.version}
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
                <label>
                  {r.sort}
                  <select
                    aria-label={r.sort}
                    value={params.get('sort') ?? 'newest'}
                    onChange={(e) => filter('sort', e.target.value)}
                  >
                    <option value="newest">{t.newest}</option>
                    <option value="popular">{t.popular}</option>
                  </select>
                </label>
              </div>
              {posts.isPending && <p role="status">…</p>}
              {(posts.isError || options.isError) && (
                <p role="alert">{t.error}</p>
              )}
            </>
          }
          list={
            <>
              <Pagination page={posts.data} params={params} />
              <div className="wb-history-list wb-scrollbar">
                <div className="wb-session-library">
                  {posts.data?.posts.length === 0 && <p>{t.empty}</p>}
                  {posts.data?.posts.map((post) => {
                    const baseName = gameName(post.baseId, post.baseId, locale),
                      title = post.title || baseName
                    return (
                      <SessionListCard
                        key={post.id}
                        title={title}
                        baseId={post.baseId}
                        baseName={baseName}
                        selected={selected?.id === post.id}
                        selectLabel={title}
                        onSelect={() => select(post.id)}
                        metadata={
                          <>
                            <small>
                              {baseName} · {post.gameVersion}
                            </small>
                            <small>
                              {t.downloads} {post.downloads} · {t.votes}{' '}
                              {post.recommendations}
                            </small>
                          </>
                        }
                        actions={
                          <>
                            <span className="community-card-date">
                              {new Date(post.createdAt).toLocaleDateString(
                                locale,
                              )}
                            </span>
                            {post.authorId ? (
                              <AuthorLink
                                id={post.authorId}
                                name={post.author ?? ''}
                              />
                            ) : (
                              <span>{t.anonymous}</span>
                            )}
                          </>
                        }
                      />
                    )
                  })}
                </div>
              </div>
            </>
          }
          preview={
            selected ? (
              <section
                ref={preview}
                className="wb-history-preview"
                aria-label={t.session}
              >
                <div className="wb-history-preview__heading">
                  <div className="wb-history-title">
                    <h3>
                      {selected.title ||
                        gameName(selected.baseId, selected.baseId, locale)}
                    </h3>
                  </div>
                </div>
                {detail.isPending ? (
                  <p role="status">…</p>
                ) : detail.data && validStoredFilm(detail.data.film) ? (
                  <SessionDetail
                    film={detail.data.film}
                    cursor={detail.data.sharedStep}
                    itemClassName="wb-history-preview__item"
                    actions={
                      <div className="wb-history-preview__actions">
                        <AppLink href={detailsHref}>
                          {r.details} <span aria-hidden="true">→</span>
                        </AppLink>
                      </div>
                    }
                  />
                ) : (
                  <p role="alert">{t.error}</p>
                )}
              </section>
            ) : null
          }
        />
      </div>
    </CommunityShell>
  )
}
function Pagination({
  page,
  params,
}: {
  page: Page | undefined
  params: URLSearchParams
}) {
  const { locale } = useI18n(),
    t = communityText(locale)
  if (!page) return null
  const pages = Math.max(1, Math.ceil(page.total / page.pageSize))
  const go = (n: number) => {
    const p = new URLSearchParams(params)
    p.set('page', String(n))
    p.delete('selected')
    navigate('/community?' + p, { scroll: false })
  }
  return (
    <nav className="wb-history-pagination" aria-label={t.board}>
      <button
        disabled={page.page <= 1}
        aria-label={t.previous}
        onClick={() => go(page.page - 1)}
      >
        ‹
      </button>
      {Array.from(
        new Set([
          1,
          pages,
          ...Array.from({ length: 5 }, (_, i) => page.page + i - 2),
        ]),
      )
        .filter((n) => n >= 1 && n <= pages)
        .sort((a, b) => a - b)
        .map((n) => (
          <button
            key={n}
            aria-current={n === page.page ? 'page' : undefined}
            onClick={() => go(n)}
          >
            {n}
          </button>
        ))}
      <button
        disabled={page.page >= pages}
        aria-label={t.next}
        onClick={() => go(page.page + 1)}
      >
        ›
      </button>
    </nav>
  )
}
