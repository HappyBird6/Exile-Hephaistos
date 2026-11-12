import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useAccount } from '../account/auth'
import { useI18n } from '../../shared/i18n/i18n'
import { communityText } from './messages'
import { boardRequest, type Stats } from './api'
import { communityQueries, cachePostStats } from './queries'
export function PostReactions({ id }: { id: string }) {
  const auth = useAccount(),
    { locale } = useI18n(),
    t = communityText(locale),
    client = useQueryClient()
  const [busy, setBusy] = useState(false),
    [error, setError] = useState(false)
  const query = useQuery(communityQueries.stats(id, auth?.user?.id))
  return (
    <div className="community-reactions">
      <span className="community-downloads">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M12 3v12m-4-4 4 4 4-4M5 16v5h14v-5" />
        </svg>
        <span>{t.downloads}</span>
        <strong>{query.data?.downloads.toLocaleString(locale) ?? '…'}</strong>
      </span>
      <button
        className="community-recommend"
        aria-pressed={query.data?.recommended ?? false}
        disabled={busy || !auth?.user || !query.data}
        title={!auth?.user ? t.loginRequired : undefined}
        onClick={async () => {
          setBusy(true)
          try {
            const stats = await boardRequest<Stats>(
              'posts/' + id + '/recommendation',
              {
                method: 'PUT',
                body: JSON.stringify({ enabled: !query.data?.recommended }),
              },
            )
            cachePostStats(client, id, auth?.user?.id, stats)
            setError(false)
          } catch {
            setError(true)
          } finally {
            setBusy(false)
          }
        }}
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M5 10H2v11h3m4 0h9a2 2 0 0 0 2-2l2-8a2 2 0 0 0-2-2h-6l1-5a2 2 0 0 0-3-2L7 10v11z" />
        </svg>
        <span>{t.votes}</span>
        <strong>
          {query.data?.recommendations.toLocaleString(locale) ?? '…'}
        </strong>
      </button>
      {(error || query.isError) && <span role="alert">{t.error}</span>}
    </div>
  )
}
