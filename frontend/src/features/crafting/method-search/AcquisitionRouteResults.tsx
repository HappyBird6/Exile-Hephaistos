import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useMemo, useRef, useState } from 'react'
import {
  formatNumber,
  gameName,
  getLocale,
  useI18n,
} from '../../../shared/i18n/i18n'
import { materialNames } from '../catalogData'
import type { Initial } from '../craftingApi'
import { useCurrencyMarket } from '../currency-prices/context'
import { abyssWorkbenchMessages } from '../abyss-workbench/messages'
import {
  workbenchActionNames,
  type WorkbenchAction,
} from '../currencyDefinitions'
import { GoalFilterApiError } from '../goal-filter/api'
import { craftStartMessages } from '../goal-filter/craftStartMessages'
import { localizedAction } from '../localizedCrafting'
import { localizedModifierText } from '../localizedModifiers'
import { pathTreeMessages } from '../path-tree/messages'
import type { CreateRequest } from '../path-tree/types'
import { SessionDetail } from '../session-detail/SessionDetail'
import { historyResources } from '../session-history/resources'
import { SessionListCard } from '../session-library/SessionListCard'
import { sessionLibraryMessages } from '../session-library/sessionLibraryMessages'
import { socketText } from '../socketAugments'
import {
  SessionPlanOverviewLink,
  SessionPlansLayout,
} from './RankedCraftingSessions'
import {
  acquisitionRouteQuery,
  type AcquisitionResult,
  type AcquisitionRoute,
  type AcquisitionStep,
} from './acquisitionRouteApi'
import {
  acquisitionOverviewHref,
  acquisitionRouteCost,
  acquisitionRouteFilm,
  acquisitionRouteTitle,
} from './acquisitionRouteFilm'
import { acquisitionRouteMessages } from './acquisitionRouteMessages'
import { graphStateMessages } from './graphStateMessages'
import { planTitle } from './probabilityPresentation'
import { policyCandidateMessages } from './policyCandidateMessages'

type Props = {
  request: CreateRequest
  definitions: Initial['modifiers']
  baseName: string
  active: boolean
}

function materialLabel(id: string) {
  if (id === 'REVEAL') {
    const { desecration, reveal } = abyssWorkbenchMessages[getLocale()]
    return `${desecration} ${reveal}`
  }
  if (Object.hasOwn(workbenchActionNames, id))
    return localizedAction(id as WorkbenchAction)
  return gameName(id, materialNames[id] ?? id.replaceAll('_', ' '))
}

function stepLabel(step: AcquisitionStep) {
  const materials = [step.action, ...step.omens].map(materialLabel).join(' + ')
  return step.socketIndex == null
    ? materials
    : `${materials} (${socketText().socket} ${step.socketIndex + 1})`
}

export function AcquisitionRouteResults({
  request,
  definitions,
  baseName,
  active,
}: Props) {
  const { locale } = useI18n()
  const messages = pathTreeMessages[locale]
  const query = useQuery({
    ...acquisitionRouteQuery(request),
    enabled: active,
  })
  if (query.isError) {
    const changed =
      query.error instanceof GoalFilterApiError &&
      query.error.issues.some(
        (issue) => issue.code === 'RULESET_IDENTITY_MISMATCH',
      )
    return (
      <p role="alert">
        {changed ? messages.changed : messages.error}{' '}
        <button type="button" onClick={() => void query.refetch()}>
          {craftStartMessages[locale].retry}
        </button>
      </p>
    )
  }
  if (!query.data) return <p role="status">{messages.calculating}</p>
  return (
    <RouteList
      key={request.clientRequestId}
      result={query.data}
      definitions={definitions}
      request={request}
      baseName={baseName}
    />
  )
}

function RouteList({
  result,
  definitions,
  request,
  baseName,
}: {
  result: AcquisitionResult
  definitions: Initial['modifiers']
  request: CreateRequest
  baseName: string
}) {
  const { locale } = useI18n()
  const copy = acquisitionRouteMessages[locale]
  const messages = pathTreeMessages[locale]
  const changes = graphStateMessages[locale]
  const section = useRef<HTMLElement>(null)
  const cache = useQueryClient()
  const market = useCurrencyMarket()?.data?.snapshot
  const costs = useMemo(
    () => result.routes.map((route) => acquisitionRouteCost(route, market)),
    [result.routes, market],
  )
  const [selection, setSelection] = useState(() => {
    const params = new URLSearchParams(location.search)
    const index = Number(params.get('route'))
    return params.get('request') === request.clientRequestId &&
      Number.isInteger(index) &&
      result.routes[index]
      ? index
      : 0
  })
  const [cursor, setCursor] = useState<number | null>(null)
  const selected = result.routes[selection]
  const film = useMemo(
    () => acquisitionRouteFilm(request, result, selection, locale),
    [request, result, selection, locale],
  )
  if (!selected)
    return (
      <p role="status">{result.complete ? copy.empty : messages.unknown}</p>
    )

  const card = result.cards[selected.cardIndex]!
  const title = (route: AcquisitionRoute) =>
    acquisitionRouteTitle(route, locale)
  const href = (index: number) =>
    acquisitionOverviewHref(request.clientRequestId, index)
  function select(index: number) {
    if (selection !== index) setCursor(null)
    setSelection(index)
    if (location.pathname === '/support-test') {
      const url = new URL(location.href)
      url.searchParams.set('request', request.clientRequestId)
      url.searchParams.set('route', String(index))
      history.replaceState(history.state, '', url)
    }
  }
  function openOverview(index: number) {
    const example =
      index === selection
        ? film
        : acquisitionRouteFilm(request, result, index, locale)
    if (example) cache.setQueryData(['plan-film', href(index)], example)
    select(index)
  }
  const chance = (route: AcquisitionRoute) =>
    route.probability == null
      ? policyCandidateMessages[locale].unresolved
      : formatNumber(
          route.probability,
          { style: 'percent', maximumSignificantDigits: 4 },
          locale,
        )
  return (
    <>
      {!result.complete && <p role="status">{messages.partial}</p>}
      <SessionPlansLayout
        sectionRef={section}
        detail={
          <section
            className="wb-history-preview session-plan-steps"
            aria-label={sessionLibraryMessages[locale].finalState}
            onClickCapture={(event) => {
              if (
                event.target instanceof Element &&
                event.target.closest('a.session-overview-entry')
              )
                openOverview(selection)
            }}
          >
            <div className="wb-history-preview__heading">
              <div className="wb-history-title">
                <h3>
                  {planTitle(costs[selection]!, locale, chance(selected))}
                </h3>
              </div>
            </div>
            {film ? (
              <>
                <h4>{copy.example}</h4>
                <SessionDetail
                  film={film}
                  cursor={cursor ?? film.frames.length - 1}
                  onNavigate={setCursor}
                  resources={historyResources}
                  overviewHref={href(selection)}
                  itemClassName="wb-history-preview__item"
                />
              </>
            ) : (
              <p role="status">{messages.unknown}</p>
            )}
            <p>
              {copy.chance} · {chance(selected)}
            </p>
            {card.options.length + selected.steps.length > 0 && (
              <details>
                <summary>{copy.order}</summary>
                <div role="region" aria-label={copy.order}>
                  <p>{title(selected)}</p>
                  {card.options.length > 0 && (
                    <dl>
                      {card.options.map((option) => (
                        <div key={option.number}>
                          <dt>{option.number}</dt>
                          <dd>
                            {option.modifierIds
                              .map((id) =>
                                definitions[id]
                                  ? localizedModifierText(
                                      definitions[id],
                                      undefined,
                                      locale,
                                    )
                                  : id,
                              )
                              .join(' / ')}
                          </dd>
                        </div>
                      ))}
                    </dl>
                  )}
                  {selected.steps.length > 0 && (
                    <ol>
                      {selected.steps.map((step, index) => (
                        <li key={index}>
                          <strong>{stepLabel(step)}</strong>
                          {step.lost.length > 0 && (
                            <>
                              {' '}
                              · {changes.removed} {step.lost.join(', ')}
                            </>
                          )}
                          {step.acquired.length > 0 && (
                            <>
                              {' '}
                              · {changes.added} {step.acquired.join(', ')}
                            </>
                          )}
                        </li>
                      ))}
                    </ol>
                  )}
                </div>
              </details>
            )}
          </section>
        }
      >
        {result.routes.map((route, index) => {
          const priceAndChance = planTitle(costs[index]!, locale, chance(route))
          const options = [
            title(route),
            ...route.steps
              .filter((step) => step.socketIndex != null)
              .map(stepLabel),
          ].join(' · ')
          return (
            <div role="listitem" key={index}>
              <SessionListCard
                title={priceAndChance}
                baseId={request.start.item.baseItemId}
                baseName={baseName}
                selected={selected === route}
                selectLabel={`${index + 1}. ${options} · ${priceAndChance}`}
                metadata={
                  <small>
                    {baseName} · {options}
                  </small>
                }
                onSelect={() => {
                  select(index)
                  if (window.innerWidth <= 760)
                    section.current
                      ?.querySelector('.wb-history-preview')
                      ?.scrollIntoView?.({ block: 'nearest' })
                }}
                actions={
                  <SessionPlanOverviewLink
                    href={href(index)}
                    onClick={() => openOverview(index)}
                  />
                }
              />
            </div>
          )
        })}
      </SessionPlansLayout>
    </>
  )
}
