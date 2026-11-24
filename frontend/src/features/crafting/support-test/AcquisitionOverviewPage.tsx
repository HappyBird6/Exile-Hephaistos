import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useI18n } from '../../../shared/i18n/i18n'
import { craftStartMessages } from '../goal-filter/craftStartMessages'
import { loadStart } from '../goal-filter/savedStart'
import { acquisitionRouteQuery } from '../method-search/acquisitionRouteApi'
import { acquisitionRouteFilm } from '../method-search/acquisitionRouteFilm'
import { pathTreeMessages } from '../path-tree/messages'
import { historyMessages } from '../session-history/messages'
import { SessionOverviewLayout } from '../session-history/SessionOverviewLayout'
import { SessionOverviewPage } from '../session-history/SessionOverviewPage'
import type { Film } from '../workbenchHistory'
import { supportTestTabNames } from './messages'

export default function AcquisitionOverviewPage() {
  const { locale } = useI18n()
  const messages = pathTreeMessages[locale]
  const params = new URLSearchParams(location.search)
  const requestId = params.get('request')
  const indexText = params.get('route') ?? ''
  const index = /^(0|[1-9]\d*)$/.test(indexText) ? Number(indexText) : NaN
  const valid = !!requestId && Number.isSafeInteger(index)
  const cached = useQueryClient().getQueryData<Film>([
    'plan-film',
    location.pathname + location.search,
  ])
  const saved =
    valid && !cached ? loadStart('support-test-start')?.searchRequest : null
  const request = saved && saved.clientRequestId === requestId ? saved : null
  const query = useQuery(acquisitionRouteQuery(request))
  const film =
    valid &&
    (cached ??
      (request &&
        query.data &&
        acquisitionRouteFilm(request, query.data, index)))
  const backLabel = supportTestTabNames[locale]
  const backHref = valid
    ? '/support-test?' +
      new URLSearchParams({ request: requestId, route: indexText })
    : '/support-test'
  const title = historyMessages[locale].title
  if (film)
    return (
      <SessionOverviewPage
        id={film.id}
        source={{ film, title: film.title ?? title, backHref, backLabel }}
      />
    )

  return (
    <SessionOverviewLayout
      title={title}
      backHref={backHref}
      backLabel={backLabel}
    >
      {request && query.isError ? (
        <p role="alert">
          {messages.error}{' '}
          <button type="button" onClick={() => void query.refetch()}>
            {craftStartMessages[locale].retry}
          </button>
        </p>
      ) : (
        <p role="status">
          {request && query.isPending
            ? messages.calculating
            : messages.unavailable}
        </p>
      )}
    </SessionOverviewLayout>
  )
}
