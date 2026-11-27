import {
  useQueries,
  useQuery,
  useQueryClient,
  type UseQueryResult,
} from '@tanstack/react-query'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { gameName, getLocale, useI18n } from '../../../shared/i18n/i18n'
import { manualRevealDefinitions, materialNames } from '../catalogData'
import type { Initial } from '../craftingApi'
import type { ConcreteItem } from '../workbenchApi'
import type { Film } from '../workbenchHistory'
import { HistoryCard } from '../session-history/HistoryCard'
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
  qualityMaterialQuantity,
} from './acquisitionRouteFilm'
import { qualityWorkMessages } from './qualityWorkMessages'
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
import { acquisitionGroupKey } from './acquisitionRouteGroups'
import { AcquisitionRouteFilters } from './AcquisitionRouteFilters'
import { graphStateMessages } from './graphStateMessages'
import { planCostText, planTitle } from './probabilityPresentation'
import { RouteTreeGraph } from './RouteTreeGraph'
import {
  buildAcquisitionRouteTree,
  projectAcquisitionRouteTree,
  type AcquisitionRouteTree,
  type AcquisitionTreeSession,
} from './acquisitionRouteTree'

type Props = {
  request: CreateRequest
  definitions: Initial['modifiers']
  baseName: string
  active: boolean
}

function filterStops(values: number[]) {
  return [...new Set(values)].sort((a, b) => a - b)
}

function replayStates(queries: UseQueryResult<AcquisitionResult>[]) {
  return queries.map(({ data, isPending, isError, refetch }) => ({
    data,
    isPending,
    isError,
    refetch,
  }))
}

function nodeAtFrame(tree: AcquisitionRouteTree, index: number, frame: number) {
  const terminal = tree.terminalNodes.get(index)
  if (terminal === undefined) return undefined
  let node = tree.nodes[terminal]
  while (node && node.depth > frame && node.parentId !== null)
    node = tree.nodes[node.parentId]
  return node?.depth === frame ? node : undefined
}

function materialLabel(id: string) {
  if (id === 'QUALITY_40') return qualityWorkMessages[getLocale()].work
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
  const quantity = qualityMaterialQuantity(step)
  if (quantity !== undefined) return `${materials} ×${quantity}`
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
  const [graphMode, setGraphMode] = useState(false)
  // Keep the same shared grade editable while its replay is pending or unsuccessful.
  const editingBranch = useRef<{
    step: number
    members: readonly number[]
  } | null>(null)
  const changedIndices = useMemo(
    () => Object.keys(choices).map(Number),
    [choices],
  )
  const replayQueries = useMemo(
    () =>
      changedIndices.map((index) =>
        fixedRouteQuery(request, result, index, choices[index]!),
      ),
    [changedIndices, request, result, choices],
  )
  // A shared prefix can have hundreds of members. Only enable the next requests while this view still needs them.
  const states = replayQueries.map((query) =>
    cache.getQueryState(query.queryKey),
  )
  const available = Math.max(
    0,
    4 - states.filter((state) => state?.fetchStatus === 'fetching').length,
  )
  const waiting = states.flatMap((state, index) =>
    !state || (state.status === 'pending' && state.fetchStatus === 'idle')
      ? [index]
      : [],
  )
  const preferredQuery = changedIndices.indexOf(preferred ?? -1)
  if (waiting.includes(preferredQuery)) {
    waiting.splice(waiting.indexOf(preferredQuery), 1)
    waiting.unshift(preferredQuery)
  }
  const deferred = new Set(waiting.slice(available))
  const replays = useQueries({
    queries: replayQueries.map((query, i) => ({
      ...query,
      enabled: !deferred.has(i),
    })),
    combine: replayStates,
  })
  const updated = useMemo(
    () => new Map(changedIndices.map((index, i) => [index, replays[i]!])),
    [changedIndices, replays],
  )
  const graph = useMemo(() => {
    if (!graphMode) return null
    const sessions: AcquisitionTreeSession[] = []
    const views = new Map<number, AcquisitionResult>()
    for (const [index, route] of result.routes.entries()) {
      const replay = updated.get(index)
      if (!replay) {
        sessions.push({ index, route, result })
        continue
      }
      if (replay.isError || !replay.data?.routes[0]) continue
      const view = evaluatedRouteResult(result, index, replay.data)
      sessions.push({ index, result: view, route: view.routes[0]! })
      views.set(index, view)
    }
    const tree = buildAcquisitionRouteTree(
      request.start.item as ConcreteItem,
      sessions,
      market,
    )
    const films = new Map<number, Film | null>()
    return {
      tree,
      filmAt(index: number) {
        if (!films.has(index)) {
          const view = views.get(index)
          films.set(
            index,
            acquisitionRouteFilm(
              request,
              view ?? result,
              view ? 0 : index,
              locale,
              index,
            ),
          )
        }
        return films.get(index)
      },
    }
  }, [graphMode, request, result, updated, market, locale])
  const baseCosts = useMemo(
    () => result.routes.map((route) => acquisitionRouteCost(route, market)),
    [result.routes, market],
  )
  const groupKeys = useMemo(
    () =>
      result.routes.map((route) =>
        acquisitionGroupKey(result, route, request.start.item, definitions),
      ),
    [result, request.start.item, definitions],
  )
  const chance = useCallback(
    (route: AcquisitionRoute) =>
      supportProbabilityText(
        {
          ...route,
          ...(route.steps.some(assumesEligibleReveal)
            ? { assumesEligibleReveal: true }
            : {}),
        },
        locale,
      ),
    [locale],
  )
  const rows = useMemo(
    () =>
      result.routes.map((route, index) => {
        const entry = updated.get(index)
        const active = entry
          ? entry.isError
            ? undefined
            : entry.data?.routes[0]
          : route
        const cost =
          entry && active
            ? acquisitionRouteCost(active, market)
            : baseCosts[index]!
        const lower =
          active?.probability ?? active?.probabilityBounds?.lower ?? 0
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
          group: entry ? index : (groupKeys[index] ?? index),
          lower,
          price,
          lucky: active?.lucky ?? route.lucky ?? false,
        }
      }),
    [
      result.routes,
      updated,
      baseCosts,
      groupKeys,
      market,
      messages,
      copy.empty,
      locale,
      chance,
    ],
  )
  const eligible = useMemo(
    () =>
      rows
        .filter((row) => showLucky || !row.lucky)
        .sort((a, b) => b.lower - a.lower || a.index - b.index),
    [rows, showLucky],
  )
  const rankedGroups = useMemo(() => {
    const priced = eligible.filter(
      (row) =>
        !Number.isFinite(priceCap) ||
        (row.price !== undefined && row.price <= priceCap),
    )
    const groups = new Map<string | number, typeof rows>()
    for (const row of priced) {
      const group = groups.get(row.group)
      if (group) group.push(row)
      else groups.set(row.group, [row])
    }
    return [...groups.values()]
  }, [eligible, priceCap])
  const cutoff = rankedGroups[9]?.[0]?.lower ?? 0
  // Returning from an overview should reopen the route the user explicitly chose.
  const opening = eligible.find((row) => row.index === openingRoute)
  const lower = minimum ?? Math.min(cutoff, opening?.lower ?? cutoff)
  const pendingSelection =
    preferred !== null && !rows[preferred]?.active && updated.has(preferred)
      ? preferred
      : null
  const visibleGroups = useMemo(
    () =>
      rankedGroups.filter(([row]) => {
        if (!row) return false
        if (row.lower >= lower) return true
        // Keep the grade selector available while retrying a previously visible method.
        return (
          !row.active &&
          row.index === pendingSelection &&
          (row.route.probability ?? row.route.probabilityBounds?.lower ?? 0) >=
            lower
        )
      }),
    [rankedGroups, lower, pendingSelection],
  )
  const visible = useMemo(() => visibleGroups.flat(), [visibleGroups])
  const visibleTree = useMemo(
    () =>
      graph &&
      projectAcquisitionRouteTree(
        graph.tree,
        new Set(visible.map((row) => row.index)),
      ),
    [graph, visible],
  )
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
  const sessionNumber = (index: number) =>
    copy.sessionNumber.replace('{n}', String(index + 1))
  const replayIssues = graphMode
    ? rows.filter((row) => {
        const replay = updated.get(row.index)
        return replay?.isError || (replay?.data && !replay.data.routes.length)
      })
    : []
  const filters = (
    <>
      <div className="acquisition-route-views">
        <button
          type="button"
          aria-pressed={!graphMode}
          onClick={() => setGraphMode(false)}
        >
          {copy.sessions}
        </button>
        <button
          type="button"
          aria-pressed={graphMode}
          onClick={() => setGraphMode(true)}
        >
          {copy.graph}
        </button>
      </div>
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
      {replayIssues.length > 0 && (
        <ul className="acquisition-route-issues" aria-live="polite">
          {replayIssues.map((row) => (
            <li key={row.index}>
              <span>
                {sessionNumber(row.index)} ·{' '}
                {acquisitionRouteTitle(row.route, locale)} · {row.summary}
              </span>
              {updated.get(row.index)?.isError && (
                <button
                  type="button"
                  onClick={() => void updated.get(row.index)?.refetch()}
                >
                  {craftStartMessages[locale].retry}
                </button>
              )}
              <button
                type="button"
                onClick={() => {
                  setChoices((previous) => {
                    const next = { ...previous }
                    delete next[row.index]
                    return next
                  })
                  if (row.index === selection) writeSelection(selection)
                }}
              >
                {copy.originalGrades}
              </button>
            </li>
          ))}
        </ul>
      )}
    </>
  )
  const replay = updated.get(selection)
  const base = result.routes[selection]
  const replayData = replay?.isError ? undefined : replay?.data
  const changed = !!replay
  const selectedView = changed
    ? replayData && evaluatedRouteResult(result, selection, replayData)
    : result
  const viewIndex = changed ? 0 : selection
  const selected = selectedView?.routes[viewIndex]
  const film = selectedView
    ? (graph?.filmAt(selection) ??
      acquisitionRouteFilm(request, selectedView, viewIndex, locale, selection))
    : null
  if (!base && result.routes.length)
    return (
      <>
        {!result.complete && <p role="status">{messages.partial}</p>}
        {graphMode ? (
          <section className="session-plans" ref={section}>
            {filters}
            <p role="status">{copy.noFilteredRoutes}</p>
          </section>
        ) : (
          <SessionPlansLayout
            sectionRef={section}
            filters={filters}
            detail={<p role="status">{copy.noFilteredRoutes}</p>}
          >
            {null}
          </SessionPlansLayout>
        )}
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
        uses: qualityMaterialQuantity(base.steps[index]!) ?? 1,
        repeated: false,
        ...(step.action === 'REVEAL' ? { kind: 'reveal' as const } : {}),
        ...(step.action === 'QUALITY_40'
          ? { work: 'UNSPECIFIED_CATALYST_40' as const }
          : {}),
      }))
  const optionText = (option: RouteOption) =>
    acquisitionTierText(option.modifierIds, definitions, locale, option)
  const frameIndex = cursor ?? steps.length
  const selectedState = film?.frames[frameIndex]?.state
  const historySteps = graphMode ? steps.slice(0, frameIndex) : steps
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
    let indices: readonly number[] = [selection]
    if (graph) {
      const node = nodeAtFrame(graph.tree, selection, stepIndex + 1)
      const editing = editingBranch.current
      const pendingGroup =
        editing?.members.includes(selection) && editing.step === stepIndex
          ? editing.members.filter(
              (index) =>
                updated.has(index) && !graph.tree.terminalNodes.has(index),
            )
          : []
      indices = [
        ...new Set([...(node?.routeIndices ?? indices), ...pendingGroup]),
      ]
      editingBranch.current = {
        step: stepIndex,
        members: indices,
      }
    }
    const next = [...grades]
    next[stepIndex] = grade
    const override = hasGradeOverride(base, next)
    setChoices((stored) => {
      const edited = { ...stored }
      for (const index of indices) {
        const recipe = result.routes[index]!
        if (!gradeActions(recipe.steps[stepIndex]!)) continue
        const newGrades = [...(stored[index] ?? routeGrades(recipe))]
        newGrades[stepIndex] = grade
        if (hasGradeOverride(recipe, newGrades)) edited[index] = newGrades
        else delete edited[index]
      }
      return edited
    })
    writeSelection(selection, override ? next : undefined)
  }
  function openOverview(index: number) {
    const entry = updated.get(index)
    const view = entry
      ? !entry.isError &&
        entry.data &&
        evaluatedRouteResult(result, index, entry.data)
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
  const selectedNode = visibleTree
    ? nodeAtFrame(visibleTree, selection, frameIndex)
    : undefined
  const heading =
    graphMode && selectedNode
      ? selectedNode.step
        ? stepLabel(selectedNode.step)
        : baseName
      : selected
        ? summary
        : title(base)
  const endingSessions = selectedNode?.terminals.length
    ? [...new Set([...selectedNode.terminals, selection])]
    : []
  const detail = (
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
          <h3>{heading}</h3>
        </div>
      </div>
      {endingSessions.length > 1 && (
        <label className="acquisition-route-variants">
          {copy.sessions}
          <select
            value={selection}
            onChange={(event) => {
              const index = Number(event.target.value)
              select(index)
              setPosition({ route: index, step: frameIndex })
            }}
          >
            {endingSessions.map((index) => (
              <option key={index} value={index}>
                {sessionNumber(index)} ·{' '}
                {acquisitionRouteTitle(result.routes[index]!, locale)}
              </option>
            ))}
          </select>
        </label>
      )}
      {film && <h4>{copy.example}</h4>}
      <SessionDetail
        film={film}
        {...(graphMode ? { throughFrame: frameIndex } : {})}
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
      {!graphMode && selected && !film?.projectedRouteChance && (
        <p>
          {copy.chance} · {chance(selected)}
        </p>
      )}
      {(graphMode
        ? historySteps.length > 0
        : card.options.length + steps.length > 0) && (
        <details>
          <summary>{copy.order}</summary>
          <div role="region" aria-label={copy.order}>
            {!graphMode && <p>{title(base)}</p>}
            {card.options.length > 0 && (
              <dl>
                {card.options
                  .filter(
                    (option) =>
                      !graphMode ||
                      historySteps.some(
                        (step) =>
                          step.acquired.includes(option.number) ||
                          step.lost.includes(option.number),
                      ),
                  )
                  .map((option) => (
                    <div key={option.number}>
                      <dt>{option.number}</dt>
                      <dd>{optionText(option)}</dd>
                    </div>
                  ))}
              </dl>
            )}
            {historySteps.length > 0 && (
              <ol>
                {historySteps.map((step, index) => (
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
  )
  if (graph && visibleTree) {
    return (
      <section ref={section} className="session-plans">
        {!result.complete && <p role="status">{messages.partial}</p>}
        {filters}
        {visibleTree.terminalNodes.size ? (
          <RouteTreeGraph
            tree={visibleTree}
            selectedId={selectedNode?.id ?? -1}
            onSelect={(id) => {
              const node = visibleTree.nodes[id]
              if (!node || !node.routeIndices.length) return
              const index = node.routeIndices.includes(selection)
                ? selection
                : node.routeIndex
              select(index)
              setPosition({ route: index, step: node.depth })
            }}
            renderItem={(node) => {
              const example = graph.filmAt(node.routeIndex)
              const frame = example?.frames[node.depth]
              return example && frame ? (
                <HistoryCard film={example} frame={frame} />
              ) : null
            }}
            renderDetail={() => detail}
            chanceText={(node) =>
              node.reachChance
                ? supportProbabilityText(node.reachChance, locale)
                : messages.unknown
            }
            costText={(node) => planCostText(node.cost, locale)}
            actionText={(node) => (node.step ? stepLabel(node.step) : baseName)}
          />
        ) : (
          detail
        )}
      </section>
    )
  }
  return (
    <>
      {!result.complete && <p role="status">{messages.partial}</p>}
      <SessionPlansLayout
        sectionRef={section}
        filters={filters}
        detail={detail}
      >
        {visibleGroups.map((group, rank) => {
          const { index, route, active, summary } =
            group.find((row) => row.index === selection) ?? group[0]!
          // Show just the material positions that distinguish variants with the same goal order.
          const differingSteps = route.steps.map((_, i) =>
            group.some(
              (row) =>
                stepLabel(row.route.steps[i]!) !== stepLabel(route.steps[i]!),
            ),
          )

          const options = [
            title(route),
            ...route.steps.filter((s) => s.socketIndex != null).map(stepLabel),
          ].join(' · ')
          return (
            <div role="listitem" key={group[0]!.index}>
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
              {group.length > 1 && (
                <label className="acquisition-route-variants">
                  {copy.variants.replace('{n}', String(group.length))}
                  <select
                    value={index}
                    onChange={(e) => select(Number(e.target.value))}
                  >
                    {group.map((row) => (
                      <option key={row.index} value={row.index}>
                        {[
                          row.route.order.join(' → '),
                          row.route.steps
                            .filter((_, i) => differingSteps[i])
                            .map(stepLabel)
                            .join(' → '),
                        ]
                          .filter(Boolean)
                          .join(' · ')}
                      </option>
                    ))}
                  </select>
                </label>
              )}
            </div>
          )
        })}
      </SessionPlansLayout>
    </>
  )
}
