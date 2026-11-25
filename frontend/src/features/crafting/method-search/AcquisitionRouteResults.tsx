import { useQueries, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useMemo, useRef, useState } from 'react'
import { gameName, getLocale, useI18n } from '../../../shared/i18n/i18n'
import { manualRevealDefinitions, materialNames } from '../catalogData'
import type { Initial } from '../craftingApi'
import { useCurrencyMarket } from '../currency-prices/context'
import { supportProbabilityText } from '../supportProbability'
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
import { localizedModifierText } from '../localizedModifiers'
import { displayedModifierValues } from '../sanctification/preciseModifierProjection'
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
  assumesEligibleReveal,
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
import { AcquisitionRouteFilters } from './AcquisitionRouteFilters'
import { graphStateMessages } from './graphStateMessages'
import { planTitle } from './probabilityPresentation'

type Props = {
  request: CreateRequest
  definitions: Initial['modifiers']
  baseName: string
  active: boolean
}

function filterStops(values: number[]) {
  return [...new Set(values)].sort((a, b) => a - b)
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
  definitions: startingDefinitions,
  request,
  baseName,
}: {
  result: AcquisitionResult
  definitions: Initial['modifiers']
  request: CreateRequest
  baseName: string
}) {
  const { locale } = useI18n()
  const definitions = useMemo(() => {
    const extra: Initial['modifiers'] = {}
    const reveal: Initial['modifiers'] =
      manualRevealDefinitions.definitions as Initial['modifiers']
    for (const card of result.cards)
      for (const option of card.options)
        for (const id of option.modifierIds)
          if (!startingDefinitions[id] && reveal[id]) extra[id] = reveal[id]
    return { ...startingDefinitions, ...extra }
  }, [startingDefinitions, result.cards])
  const copy = acquisitionRouteMessages[locale]
  const messages = pathTreeMessages[locale]
  const changes = graphStateMessages[locale]
  const section = useRef<HTMLElement>(null)
  const cache = useQueryClient()
  const market = useCurrencyMarket()?.data?.snapshot
  const [preferred, setSelection] = useState<number | null>(() => {
    const params = new URLSearchParams(location.search)
    const raw = params.get('route') ?? ''
    const index = Number(raw)
    return params.get('request') === request.clientRequestId &&
      /^(0|[1-9]\d*)$/.test(raw) &&
      result.routes[index]
      ? index
      : null
  })
  const [choices, setChoices] = useState<Record<number, number[]>>(() => {
    const params = new URLSearchParams(location.search)
    if (
      params.get('request') !== request.clientRequestId ||
      preferred === null ||
      params.get('route') !== String(preferred)
    )
      return {}
    const grades = readRouteGrades(location.search, result.routes[preferred])
    return grades && hasGradeOverride(result.routes[preferred], grades)
      ? { [preferred]: grades }
      : {}
  })
  const [position, setPosition] = useState<{
    route: number
    step: number
  } | null>(null)
  const [showLucky, setShowLucky] = useState(
    () => !!(preferred !== null && result.routes[preferred]?.lucky),
  )
  const [priceCap, setPriceCap] = useState(Infinity)
  const [minimum, setMinimum] = useState<number | null>(null)
  const [openingRoute, setOpeningRoute] = useState(preferred)
  const changedIndices = Object.keys(choices).map(Number)
  const replays = useQueries({
    queries: changedIndices.map((index) =>
      fixedRouteQuery(request, result, index, choices[index]!),
    ),
  })
  const updated = new Map(
    changedIndices.map((index, i) => [index, replays[i]!]),
  )
  const baseCosts = useMemo(
    () => result.routes.map((route) => acquisitionRouteCost(route, market)),
    [result.routes, market],
  )
  const chance = (route: AcquisitionRoute) =>
    supportProbabilityText(
      {
        ...route,
        ...(route.steps.some(assumesEligibleReveal)
          ? { assumesEligibleReveal: true }
          : {}),
      },
      locale,
    )
  const rows = result.routes.map((route, index) => {
    const entry = updated.get(index)
    const active = entry ? entry.data?.routes[0] : route
    const cost =
      entry && active ? acquisitionRouteCost(active, market) : baseCosts[index]!
    const lower = active?.probability ?? active?.probabilityBounds?.lower ?? 0
    const price = active && !cost.incomplete ? cost.exalted : undefined
    let summary = messages.unknown
    if (active) summary = planTitle(cost, locale, chance(active))
    else if (entry?.isPending) summary = messages.calculating
    else if (entry?.isError) summary = messages.error
    else if (entry?.data?.complete) summary = copy.empty
    return {
      index,
      route,
      active,
      summary,
      lower,
      price,
      lucky: active?.lucky ?? route.lucky ?? false,
    }
  })
  const eligible = rows.filter((row) => showLucky || !row.lucky)
  eligible.sort((a, b) => b.lower - a.lower || a.index - b.index)
  const cutoff = eligible[9]?.lower ?? 0
  // Returning from an overview should reopen the route the user explicitly chose.
  const opening = eligible.find((row) => row.index === openingRoute)
  const lower = minimum ?? Math.min(cutoff, opening?.lower ?? cutoff)
  const visible = eligible.filter((row) => {
    if (
      Number.isFinite(priceCap) &&
      (row.price === undefined || row.price > priceCap)
    )
      return false
    if (row.lower >= lower) return true
    // Keep the grade selector available while retrying a previously visible method.
    return (
      !row.active &&
      row.index === preferred &&
      updated.has(preferred) &&
      (row.route.probability ?? row.route.probabilityBounds?.lower ?? 0) >=
        lower
    )
  })
  useEffect(() => {
    console.info('[API] 제작 경로 화면 필터', {
      request: request.clientRequestId,
      received: rows.length,
      afterLuckyFilter: eligible.length,
      visible: visible.length,
      showLucky,
      minimumProbability: lower,
      maximumCost: priceCap,
    })
  }, [
    request.clientRequestId,
    rows.length,
    eligible.length,
    visible.length,
    showLucky,
    lower,
    priceCap,
  ])
  const selection =
    visible.find((row) => row.index === preferred)?.index ??
    visible[0]?.index ??
    -1
  const cursor = position?.route === selection ? position.step : null
  function setCursor(step: number | null) {
    setPosition(step === null ? null : { route: selection, step })
  }
  const filters = (
    <AcquisitionRouteFilters
      lower={lower}
      cap={priceCap}
      lucky={showLucky}
      lowerStops={filterStops([
        0,
        1,
        lower,
        ...eligible.map((row) => row.lower),
      ])}
      costStops={filterStops([
        0,
        Infinity,
        priceCap,
        ...eligible.flatMap((row) =>
          row.price === undefined ? [] : [row.price],
        ),
      ])}
      setLower={setMinimum}
      setCap={setPriceCap}
      setLucky={(checked) => {
        setShowLucky(checked)
        if (!checked && opening?.lucky) setOpeningRoute(null)
        if (!checked && preferred !== null && rows[preferred]?.lucky) {
          setSelection(null)
          writeSelection(null)
        }
      }}
    />
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
  if (!base && result.routes.length)
    return (
      <>
        {!result.complete && <p role="status">{messages.partial}</p>}
        <SessionPlansLayout
          sectionRef={section}
          filters={filters}
          detail={<p role="status">{copy.noFilteredRoutes}</p>}
        >
          {null}
        </SessionPlansLayout>
      </>
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
  const frameIndex = cursor ?? steps.length
  const selectedState = film?.frames[frameIndex]?.state
  const acquiredLabels = (selected?.steps[frameIndex - 1]?.acquired ?? []).map(
    (number) => {
      const option = card.options.find((target) => target.number === number)
      const mod = selectedState?.explicits.find((instance) =>
        option?.modifierIds.includes(instance.modifierId),
      )
      const definition = mod && definitions[mod.modifierId]
      return selectedState && mod && definition
        ? localizedModifierText(
            definition,
            displayedModifierValues(
              selectedState,
              definition,
              mod,
              'explicits',
            ),
            locale,
          )
        : String(number)
    },
  )
  const title = (route: AcquisitionRoute) =>
    acquisitionRouteTitle(route, locale)
  const href = (index: number) =>
    acquisitionOverviewHref(request.clientRequestId, index, choices[index])
  function writeSelection(index: number | null, grades?: number[]) {
    if (location.pathname !== '/support-test') return
    const url = new URL(location.href)
    url.searchParams.set('request', request.clientRequestId)
    if (index === null) url.searchParams.delete('route')
    else url.searchParams.set('route', String(index))
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
    setSelection(selection)
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
        filters={filters}
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
                const available = gradeActions(base.steps[frameIndex - 1]!)
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
            {acquiredLabels.length > 0 && (
              <p>
                {changes.added} {acquiredLabels.join(' · ')}
              </p>
            )}
            {selected && !film?.projectedRouteChance && (
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
        {visible.map(({ index, route, active, summary }, rank) => {
          const options = [
            title(route),
            ...route.steps.filter((s) => s.socketIndex != null).map(stepLabel),
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
