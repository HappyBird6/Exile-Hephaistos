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
import { SessionDetail } from '../crafting/session-detail/SessionDetail'
import { historyResources } from '../crafting/session-history/resources'
import { validStoredFilm } from '../crafting/workbenchHistory'
import { CommunityShell } from './CommunityShell'
import { boardRequest } from './api'
import type { Post } from './api'
import { communityText } from './messages'
export function CommunityDetail({ id }: { id: string }) {
  const { locale } = useI18n(),
    t = communityText(locale)
  const query = useQuery({
    queryKey: ['community', 'post', id],
    queryFn: ({ signal }) =>
      boardRequest<Post>('posts/' + encodeURIComponent(id), { signal }),
  })
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
    try {
      const next = await reusePost(
        post,
        cursor,
        action,
        new AbortController().signal,
      )
      await recordDownload(post.id, action)
      void client.invalidateQueries({
        queryKey: ['community', 'stats', post.id],
      })
      if (next) navigate(next)
      else setStatus(t.saved)
    } catch {
      setStatus(t.error)
    } finally {
      setBusy(false)
    }
  }
  return (
    <>
      <AppLink
        className="community-back"
        href={
          '/community' +
          (new URLSearchParams(location.search).get('from')?.startsWith('?')
            ? new URLSearchParams(location.search).get('from')
            : '?selected=' + post.id)
        }
      >
        {t.board} ←
      </AppLink>
      <header className="community-detail-header">
        <h1>{post.title || gameName(post.baseId, post.baseId, locale)}</h1>
        <p className="community-detail-meta">
          {post.authorId ? (
            <AppLink href={'/community?author=' + post.authorId}>
              {post.author}
            </AppLink>
          ) : (
            t.anonymous
          )}{' '}
          · {post.gameVersion}
        </p>
      </header>
      <div className="community-body">{post.body}</div>
      <PostEditor post={post} />
      <PostReactions id={post.id} />
      <p>
        {t.sharedStep}: {post.sharedStep}{' '}
        {post.film.frames.length > post.sharedStep + 1 && (
          <>
            {' '}
            · {t.laterSteps}: {post.film.frames.length - post.sharedStep - 1}
          </>
        )}
      </p>
      <SessionDetail
        film={post.film}
        cursor={cursor}
        onNavigate={setCursor}
        resources={historyResources}
        showOverview={false}
        className="community-preview community-session-detail"
        itemClassName="community-detail-item"
        disabled={busy}
        actions={
          <div className="community-reuse">
            <div className="community-actions">
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
          </div>
        }
      />
      <Comments post={post.id} />
    </>
  )
}
