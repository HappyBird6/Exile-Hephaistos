import { PostEditor } from './PostEditor'
import { reusePost, type ReuseAction } from './reuse'
import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useI18n, gameName } from '../../shared/i18n/i18n'
import { SessionDetail } from '../crafting/session-detail/SessionDetail'
import { historyResources } from '../crafting/session-history/resources'
import { validStoredFilm } from '../crafting/workbenchHistory'
import { CurrencyPriceContext } from '../crafting/currency-prices/context'
import { useCurrencyPrices } from '../crafting/currency-prices/useCurrencyPrices'
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
    market = useCurrencyPrices()
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
      if (next) location.href = next
      else setStatus(t.saved)
    } catch {
      setStatus(t.error)
    } finally {
      setBusy(false)
    }
  }
  return (
    <CurrencyPriceContext.Provider value={market}>
      <h1>{post.title || gameName(post.baseId, post.baseId, locale)}</h1>
      <p>
        {post.authorId ? (
          <a href={'/community?author=' + post.authorId}>{post.author}</a>
        ) : (
          t.anonymous
        )}{' '}
        · {post.gameVersion}
      </p>
      <div className="community-body">{post.body}</div>
      <PostEditor post={post} />
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
        className="community-preview"
        disabled={busy}
        actions={
          <>
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
          </>
        }
      />
    </CurrencyPriceContext.Provider>
  )
}
