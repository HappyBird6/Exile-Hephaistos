import { AppLink } from '../../shared/navigation/AppLink'
import { navigate } from '../../shared/navigation/navigation'
import { Comments } from './Comments'
import { PostReactions } from './PostReactions'
import { recordDownload } from './downloads'
import { PostEditor } from './PostEditor'
import { reusePost, type ReuseAction } from './reuse'
import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useI18n, gameName } from '../../shared/i18n/i18n'
import { accountScope } from '../account/auth'
import { SessionDetail } from '../crafting/session-detail/SessionDetail'
import { historyResources } from '../crafting/session-history/resources'
import { validStoredFilm } from '../crafting/workbenchHistory'
import { CommunityShell } from './CommunityShell'
import { communityQueries, cachePostStats } from './queries'
import type { Post } from './api'
import { communityText } from './messages'
export function CommunityDetail({ id }: { id: string }) {
  const { locale } = useI18n(),
    t = communityText(locale)
  const query = useQuery(communityQueries.post(id))
  return (
    <CommunityShell>
      {query.isPending ? (
        <p role="status">…</p>
      ) : query.data && validStoredFilm(query.data.film) ? (
        <Detail key={id + ':' + query.data.revision} post={query.data} />
      ) : (
        <p role="alert">{t.error}</p>
      )}
    </CommunityShell>
  )
}
function Detail({ post }: { post: Post }) {
  const { locale } = useI18n(),
    t = communityText(locale),
    client = useQueryClient()
  const [cursor, setCursor] = useState(post.sharedStep)
  const [busy, setBusy] = useState(false),
    [status, setStatus] = useState('')
  async function reuse(action: ReuseAction) {
    setBusy(true)
    setStatus('')
    const owner = accountScope()
    try {
      const next = await reusePost(
        post,
        cursor,
        action,
        new AbortController().signal,
      )
      const stats = await recordDownload(post.id, action)
      if (stats && owner === accountScope())
        cachePostStats(client, post.id, owner, stats)
      if (next) navigate(next)
      else setStatus(t.saved)
    } catch {
      setStatus(t.error)
    } finally {
      setBusy(false)
    }
  }
  const from = new URLSearchParams(location.search).get('from')
  return (
    <article className="community-post">
      <AppLink
        className="community-back"
        href={
          '/community' + (from?.startsWith('?') ? from : '?selected=' + post.id)
        }
      >
        ← {t.board}
      </AppLink>
      <header className="community-detail-header">
        <h1>{post.title || gameName(post.baseId, post.baseId, locale)}</h1>
        <div className="community-detail-meta">
          {post.authorId ? (
            <AppLink href={'/community?author=' + post.authorId}>
              {post.author}
            </AppLink>
          ) : (
            <span>{t.anonymous}</span>
          )}
          <time dateTime={post.createdAt}>
            {new Date(post.createdAt).toLocaleString(locale)}
          </time>
          <span className="community-version">{post.gameVersion}</span>
        </div>
      </header>
      <div className="community-post-content">
        <SessionDetail
          film={post.film}
          cursor={cursor}
          onNavigate={setCursor}
          resources={historyResources}
          showOverview={false}
          className="community-session-detail"
          disabled={busy}
          actions={
            <>
              <div className="wb-history-preview__actions community-reuse">
                <button disabled={busy} onClick={() => void reuse('import')}>
                  {t.importSession}
                </button>
                <button disabled={busy} onClick={() => void reuse('craft')}>
                  {t.newCraft}
                </button>
                <button disabled={busy} onClick={() => void reuse('favorite')}>
                  {t.favorite}
                </button>
              </div>
              {status && <p role="status">{status}</p>}
            </>
          }
        />
        <div className="community-post-description">
          {post.body && (
            <div className="community-body community-post-body">
              {post.body}
            </div>
          )}
        </div>
      </div>
      <div className="community-post-actions">
        <PostReactions id={post.id} />
        <PostEditor post={post} />
      </div>
      <Comments post={post.id} />
    </article>
  )
}
