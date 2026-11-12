import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useAccount } from '../account/auth'
import { useI18n } from '../../shared/i18n/i18n'
import { communityText } from './messages'
import { boardRequest } from './api'
export type Stats = {
  downloads: number
  recommendations: number
  recommended: boolean
}
export function PostReactions({ id }: { id: string }) {
  const auth = useAccount(),
    { locale } = useI18n(),
    t = communityText(locale),
    client = useQueryClient()
  const [busy, setBusy] = useState(false),
    [error, setError] = useState(false)
  const key = ['community', 'stats', id, auth?.user?.id]
  const query = useQuery({
    queryKey: key,
    queryFn: () => boardRequest<Stats>('posts/' + id + '/stats'),
  })
  return (
    <div className="community-actions">
      <span>
        {t.downloads} {query.data?.downloads ?? '…'}
      </span>
      <button
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
            client.setQueryData(key, stats)
            setError(false)
          } catch {
            setError(true)
          } finally {
            setBusy(false)
          }
        }}
      >
        {t.votes} {query.data?.recommendations ?? '…'}
      </button>
      {error && <span role="alert">{t.error}</span>}
    </div>
  )
}
