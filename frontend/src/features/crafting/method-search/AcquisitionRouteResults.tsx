import { useQueries, useQuery, useQueryClient } from '@tanstack/react-query'
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
import {
  currencyId,
  type NotationStep,
} from '../session-notation/extractSessionNotation'
import { revealAction } from '../abyss-workbench/revealAction'
import { abyssWorkbenchMessages } from '../abyss-workbench/messages'
import {
  workbenchActionNames,
  type WorkbenchAction,
} from '../currencyDefinitions'
import { GoalFilterApiError } from '../goal-filter/api'
import { craftStartMessages } from '../goal-filter/craftStartMessages'
import { localizedAction } from '../localizedCrafting'
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
  type RouteOption,
} from './acquisitionRouteApi'
import {
  acquisitionOverviewHref,
  acquisitionRouteCost,
  acquisitionRouteFilm,
  acquisitionRouteTitle,
} from './acquisitionRouteFilm'
import { acquisitionRouteMessages } from './acquisitionRouteMessages'
import { acquisitionTierText } from './acquisitionTierText'
import {
  evaluatedRouteResult,
  fixedRouteQuery,
  fixedSteps,
  gradeActions,
  gradeSearch,
  hasGradeOverride,
  readRouteGrades,
  routeGrades,
} from './fixedRouteGrades'
import { AcquisitionRouteActions } from './AcquisitionRouteActions'
import { RouteGradePicker } from './RouteGradePicker'
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
  const [selection, setSelection] = useState(() => {
    const params = new URLSearchParams(location.search)
    const raw = params.get('route') ?? ''
    const index = Number(raw)
    return params.get('request') === request.clientRequestId &&
      /^(0|[1-9]\d*)$/.test(raw) &&
      result.routes[index]
      ? index
      : 0
  })
  const [choices, setChoices] = useState<Record<number, number[]>>(() => {
    const params = new URLSearchParams(location.search)
    if (
      params.get('request') !== request.clientRequestId ||
      params.get('route') !== String(selection)
    )
      return {}
    const grades = readRouteGrades(location.search, result.routes[selection])
    return grades && hasGradeOverride(result.routes[selection], grades)
      ? { [selection]: grades }
      : {}
  })
  const [cursor, setCursor] = useState<number | null>(null)
  const changedIndices = Object.keys(choices).map(Number)
  const replays = useQueries({
    queries: changedIndices.map((index) =>
      fixedRouteQuery(request, result, index, choices[index]!),
    ),
  })
  const updated = new Map(
    changedIndices.map((index, i) => [index, replays[i]!]),
  )
  const replay = updated.get(selection)
  const base = result.routes[selection]
  const replayData = replay?.data
  const changed = !!replay
  const selectedView = changed
    ? replayData && evaluatedRouteResult(result, selection, replayData)
    : result
  const viewIndex = changed ? 0 : selection
  const selected = selectedView?.routes[viewIndex]
  const film = selectedView
    ? acquisitionRouteFilm(request, selectedView, viewIndex, locale, selection)
    : null
  const baseCosts = useMemo(
    () => result.routes.map((route) => acquisitionRouteCost(route, market)),
    [result.routes, market],
  )
  if (!base)
    return (
      <p role="status">{result.complete ? copy.empty : messages.unknown}</p>
    )
  const card = result.cards[base.cardIndex]!
  const grades = choices[selection] ?? routeGrades(base)
  const steps = selected?.steps ?? fixedSteps(base, grades)
  const pendingSteps: NotationStep[] = film
    ? []
    : steps.map((step, index) => ({
        number: index + 1,
        frameIndex: index + 1,
        currencyId:
          step.action === 'REVEAL'
            ? revealAction
            : (currencyId(step.action) ?? step.action),
        consumedOmenIds: step.omens,
        omensKnown: true,
        uses: 1,
        repeated: false,
        ...(step.action === 'REVEAL' ? { kind: 'reveal' as const } : {}),
      }))
  const optionText = (option: RouteOption) =>
    acquisitionTierText(option.modifierIds, definitions, locale, option)
  const selectedStep = selected?.steps[(cursor ?? steps.length) - 1]
  const title = (route: AcquisitionRoute) =>
    acquisitionRouteTitle(route, locale)
  const href = (index: number) =>
    acquisitionOverviewHref(request.clientRequestId, index, choices[index])
  function writeSelection(index: number, grades?: number[]) {
    if (location.pathname !== '/support-test') return
    const url = new URL(location.href)
    url.searchParams.set('request', request.clientRequestId)
    url.searchParams.set('route', String(index))
    const encoded = gradeSearch(grades)
    if (encoded) url.searchParams.set('grades', encoded)
    else url.searchParams.delete('grades')
    history.replaceState(history.state, '', url)
  }
  function select(index: number) {
    if (selection !== index) setCursor(null)
    setSelection(index)
    writeSelection(index, choices[index])
  }
  function changeGrade(stepIndex: number, grade: number) {
    const next = [...grades]
    next[stepIndex] = grade
    const override = hasGradeOverride(base, next)
    setChoices((stored) => {
      const edited = { ...stored }
      if (override) edited[selection] = next
      else delete edited[selection]
      return edited
    })
    writeSelection(selection, override ? next : undefined)
  }
  function openOverview(index: number) {
    const entry = updated.get(index)
    const view = entry
      ? entry.data && evaluatedRouteResult(result, index, entry.data)
      : result
    const example =
      index === selection
        ? film
        : view &&
          acquisitionRouteFilm(request, view, entry ? 0 : index, locale, index)
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
  const rows = result.routes.map((route, index) => {
    const entry = updated.get(index)
    const active = entry ? entry.data?.routes[0] : route
    let summary = messages.unknown
    if (active)
      summary = planTitle(
        entry ? acquisitionRouteCost(active, market) : baseCosts[index]!,
        locale,
        chance(active),
      )
    else if (entry?.isPending) summary = messages.calculating
    else if (entry?.isError) summary = messages.error
    else if (entry?.data?.complete) summary = copy.empty
    return { index, route, active, summary }
  })
  const summary = rows[selection]!.summary
  const status = replay?.isError ? (
    <p role="alert">
      {messages.error}{' '}
      <button type="button" onClick={() => void replay.refetch()}>
        {craftStartMessages[locale].retry}
      </button>
    </p>
  ) : (
    <p role="status">{selected ? messages.unknown : summary}</p>
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
                <h3>{selected ? summary : title(base)}</h3>
              </div>
            </div>
            {film && <h4>{copy.example}</h4>}
            <SessionDetail
              film={film}
              cursor={cursor ?? steps.length}
              onNavigate={setCursor}
              actions={
                film && (
                  <AcquisitionRouteActions
                    key={href(selection) + '/' + (cursor ?? steps.length)}
                    film={film}
                    cursor={cursor ?? steps.length}
                  />
                )
              }
              {...(!film ? { item: status, pendingSteps } : {})}
              stepControls={(frameIndex) => {
                const action = steps[frameIndex - 1]!.action
                const available = gradeActions(action)
                return (
                  available && (
                    <RouteGradePicker
                      choices={available}
                      current={action}
                      step={frameIndex}
                      onSelect={(grade) => changeGrade(frameIndex - 1, grade)}
                    />
                  )
                )
              }}
              resources={historyResources}
              overviewHref={href(selection)}
              itemClassName="wb-history-preview__item"
            />
            {selectedStep && selectedStep.acquired.length > 0 && (
              <p>
                {changes.added}{' '}
                {card.options
                  .filter((option) =>
                    selectedStep.acquired.includes(option.number),
                  )
                  .map((option) => optionText(option))
                  .join(' · ')}
              </p>
            )}
            {selected && (
              <p>
                {copy.chance} · {chance(selected)}
              </p>
            )}
            {card.options.length + steps.length > 0 && (
              <details>
                <summary>{copy.order}</summary>
                <div role="region" aria-label={copy.order}>
                  <p>{title(base)}</p>
                  {card.options.length > 0 && (
                    <dl>
                      {card.options.map((option) => (
                        <div key={option.number}>
                          <dt>{option.number}</dt>
                          <dd>{optionText(option)}</dd>
                        </div>
                      ))}
                    </dl>
                  )}
                  {steps.length > 0 && (
                    <ol>
                      {steps.map((step, index) => (
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
        {rows
          .sort(
            (a, b) =>
              (b.active?.probability ?? -1) - (a.active?.probability ?? -1),
          )
          .map(({ index, route, active, summary }, rank) => {
            const options = [
              title(route),
              ...route.steps
                .filter((s) => s.socketIndex != null)
                .map(stepLabel),
            ].join(' · ')
            return (
              <div role="listitem" key={index}>
                <SessionListCard
                  title={summary}
                  baseId={request.start.item.baseItemId}
                  baseName={baseName}
                  selected={index === selection}
                  selectLabel={`${rank + 1}. ${options} · ${summary}`}
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
                    active && (
                      <SessionPlanOverviewLink
                        href={href(index)}
                        onClick={() => openOverview(index)}
                      />
                    )
                  }
                />
              </div>
            )
          })}
      </SessionPlansLayout>
    </>
  )
}
