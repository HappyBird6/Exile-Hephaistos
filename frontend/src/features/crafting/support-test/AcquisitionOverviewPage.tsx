import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useI18n } from '../../../shared/i18n/i18n'
import { craftStartMessages } from '../goal-filter/craftStartMessages'
import { hasActiveTotalGoal } from '../goal-filter/types'
import { loadStart } from '../goal-filter/savedStart'
import { acquisitionRouteQuery } from '../method-search/acquisitionRouteApi'
import { acquisitionRouteFilm } from '../method-search/acquisitionRouteFilm'
import {
  readRouteGrades,
  fixedRouteQuery,
  evaluatedRouteResult,
} from '../method-search/fixedRouteGrades'
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
  const request =
    saved &&
    saved.clientRequestId === requestId &&
    !hasActiveTotalGoal(saved.goal)
      ? saved
      : null
  const query = useQuery(acquisitionRouteQuery(request))
  const base = query.data
  const recipe = base?.routes[index]
  const grades = recipe ? readRouteGrades(location.search, recipe) : null
  const changed = grades?.some((grade) => grade !== 1) ?? false
  const replay = useQuery(fixedRouteQuery(request, base, index, grades))
  const displayed =
    base && grades
      ? changed
        ? replay.data && evaluatedRouteResult(base, index, replay.data)
        : base
      : null
  const film =
    valid &&
    (cached ??
      (request &&
        displayed &&
        acquisitionRouteFilm(
          request,
          displayed,
          changed ? 0 : index,
          locale,
          index,
        )))
  const activeQuery = changed ? replay : query
  const backLabel = supportTestTabNames[locale]
  const backParams = new URLSearchParams()
  if (valid) {
    backParams.set('request', requestId)
    backParams.set('route', indexText)
    const selectedGrades = params.get('grades')
    if (selectedGrades && (cached || grades))
      backParams.set('grades', selectedGrades)
  }
  const backHref = valid ? '/support-test?' + backParams : '/support-test'
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
      {request && activeQuery.isError ? (
        <p role="alert">
          {messages.error}{' '}
          <button type="button" onClick={() => void activeQuery.refetch()}>
            {craftStartMessages[locale].retry}
          </button>
        </p>
      ) : (
        <p role="status">
          {request && activeQuery.isPending
            ? messages.calculating
            : changed && replay.data && !replay.data.complete
              ? messages.unknown
              : messages.unavailable}
        </p>
      )}
    </SessionOverviewLayout>
  )
}
