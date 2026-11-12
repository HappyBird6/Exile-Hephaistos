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
      />
    </CurrencyPriceContext.Provider>
  )
}
