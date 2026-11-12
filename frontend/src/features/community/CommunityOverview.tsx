import { useQuery } from '@tanstack/react-query'
import { useI18n, gameName } from '../../shared/i18n/i18n'
import { AppLink } from '../../shared/navigation/AppLink'
import { validStoredFilm } from '../crafting/workbenchHistory'
import { SessionOverviewPage } from '../crafting/session-history/SessionOverviewPage'
import { communityQueries } from './queries'
import { communityText } from './messages'
import { revisionText } from './revisionMessages'
/** Public snapshots share the detail cache; viewing never imports a personal session. */
export function CommunityOverview({ id }: { id: string }) {
  const { locale } = useI18n(),
    t = communityText(locale),
    r = revisionText(locale)
  const query = useQuery(communityQueries.post(id))
  const backHref = '/community/post/' + id + location.search
  const post = query.data
  if (!post || !validStoredFilm(post.film))
    return (
      <main className="craft-page craft-page--framed">
        <AppLink href={backHref}>← {r.details}</AppLink>
        <p role={query.isPending ? 'status' : 'alert'}>
          {query.isPending ? '…' : t.error}
        </p>
      </main>
    )
  return (
    <SessionOverviewPage
      id={id}
      source={{
        film: post.film,
        title: post.title || gameName(post.baseId, post.baseId, locale),
        backHref,
        backLabel: r.details,
      }}
    />
  )
}
