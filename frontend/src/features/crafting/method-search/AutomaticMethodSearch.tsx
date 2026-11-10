import { noPlan, planningNotes } from './planningNotes'
import { create } from 'zustand'
import { createPathSearchRequest } from '../path-tree/request'
import {
  abyssCard,
  materializeAbyss,
  flowMessages,
  flowExportMessages,
  validFlow,
  type CraftingFlow,
  type FlowTransition,
} from './craftingFlow'
import type { AbyssState } from '../AbyssPanel'
import { junkClassifier } from './graphDisplay'
import type { Definition } from '../craftingApi'
import { CommonSlotObservation } from './CommonSlotObservation'
import { compactFrozenCard } from './compactFrozenCard'
import { qualityGoalMessages } from '../goal-filter/qualityGoalMessages'
import { NumericRollNotice } from './NumericRollNotice'
import { ScenarioExport } from './ScenarioCodePanel'
import type { Scenario } from './scenarioCode'
import { OperationCards } from './OperationCards'
import { retryMessages } from './retryMessages'
import { capacityMessages } from './capacityMessages'
import { scopeMessages, invalidItemMessages } from './scopeMessages'
import { legacyResultMessages } from './legacyResultMessages'
import { sumBounds } from './outcomeGrouping'
import { guideMessages } from './guideMessages'
import { removesModifier } from './methodRisk'
import { GroupedMethodGraph } from './GroupedMethodGraph'
import { isRepeatable, methodPoint } from './methodObservation'
import { outcomeMessages } from './outcomeMessages'
import { CalculationStatus, CalculationControlLabel } from './CalculationStatus'
import { calculationState } from './calculationState'
import { CandidateRoute } from './CandidateRoute'
import { routeThrough, validRoute } from './routeSelection'
import { readSupport, saveSupport, validRun } from './supportStorage'
import type { Selection } from './supportStorage'
import { workspaceMessages } from './workspaceMessages'
import { useEffect, useRef, useState } from 'react'
import { useI18n } from '../../../shared/i18n/i18n'
import { ItemCard } from '../ItemCard'
import { localizedAction } from '../localizedCrafting'
import type { ConcreteItem, WorkbenchAction } from '../workbenchApi'
import type { PathTreeProps } from '../path-tree/PathTree'
import { canonical } from '../path-tree/validation'
import { pathTreeMessages } from '../path-tree/messages'
import { methodSearchMessages } from './messages'
import { policyMessages, strictExaltedMessages } from './policyMessages'
import { qualityWorkMessages } from './qualityWorkMessages'
import {
  presentAutomaticItem,
  distributionEditMessages,
  automaticItemIsDistribution,
} from './automaticTierLaws'
import { probabilityText, quantityText } from './probabilityPresentation'
import {
  automaticApi,
  AutomaticError,
  acceptsAutomatic,
  cancelAutomatic,
} from './automaticApi'
import type { AutomaticAction, AutomaticView } from './automaticApi'
import './method-search.css'

type Props = PathTreeProps & {
  graphDefinitions?: Record<string, Definition> | undefined
  targetSources?: string[] | undefined
  snapshot?: Scenario | undefined
  scenarioBaseKey?: string | undefined
  goalLabels?: Scenario['goalLabels']
}
export function AutomaticMethodSearch(props: Props) {
  return (
    <Workflow
      key={canonical([
        props.inputGeneration,
        props.request,
        props.abstractQuality40,
        props.abstractQuality,
      ])}
      {...props}
    />
  )
}
function Workflow(props: Props) {
  const { locale } = useI18n()
  const [useFlow] = useState(() =>
    create<{
      value: CraftingFlow
      saveError: boolean
      change: (fn: (f: CraftingFlow) => CraftingFlow) => void
    }>((set) => ({
      saveError: false,
      value: (props.snapshot
        ? null
        : readSupport(
            'crafting-flow:' + props.request.clientRequestId,
            (v): v is CraftingFlow =>
              validFlow(v, props.request.clientRequestId),
          )) ?? {
        origin: props.request.clientRequestId,
        request: props.request,
        prefix: [],
        anchor: null,
        carry: null,
        branch: null,
      },
      change: (fn) =>
        set((s) => {
          const value = fn(s.value)
          return {
            value,
            saveError:
              !props.snapshot &&
              !saveSupport(
                'crafting-flow:' + props.request.clientRequestId,
                value,
              ),
          }
        }),
    })),
  )
  const { value: flow, change, saveError } = useFlow()
  return (
    <>
      {saveError && <p role="alert">{flowMessages[locale].stored}</p>}
      <Run
        {...props}
        key={flow.request.clientRequestId}
        request={flow.request}
        flow={flow}
        changeFlow={change}
        continueFlow={(state, item) =>
          change((old) => ({
            ...old,
            request: createPathSearchRequest(
              item,
              old.request.goal,
              old.request.start.provenance,
              old.request.activeOmens,
            ),
            prefix: [...old.prefix, ...(old.branch?.steps ?? [])],
            carry: state,
            branch: {
              sourceNode: 0,
              source: materializeAbyss(state)!,
              prepared: state,
              action: 'EXALTED',
              omens: [],
              steps: [],
            },
          }))
        }
      />
    </>
  )
}
type FlowProps = {
  flow: CraftingFlow
  changeFlow: (fn: (f: CraftingFlow) => CraftingFlow) => void
  continueFlow: (
    state: AbyssState,
    item: import('../path-tree/types').Item,
  ) => void
}
function Run({
  flow,
  changeFlow,
  continueFlow,
  graphDefinitions,
  targetSources,
  request,
  presentItem,
  onEditState,
  onRecalculate,
  abstractQuality40 = false,
  abstractQuality = abstractQuality40 ? 40 : 0,
  active = true,
  snapshot,
  scenarioBaseKey = 'solar',
  goalLabels,
}: Props & FlowProps) {
  const { locale, name } = useI18n(),
    copy = methodSearchMessages[locale],
    common = pathTreeMessages[locale],
    policy = policyMessages[locale],
    quality = qualityWorkMessages[locale]
  const [saved] = useState(() => {
    if (snapshot)
      return {
        request: request.clientRequestId,
        job: snapshot.result.id,
        selected: snapshot.selected,
        cycles: snapshot.cycles,
        route: snapshot.route,
      }
    const stored = readSupport('run', validRun)
    return stored?.request === request.clientRequestId ? stored : null
  })
  const [manualSelected, setManualSelected] = useState<string | null>(
    flow.branch?.steps.at(-1)?.id ?? null,
  )
  const manualStep = [...flow.prefix, ...(flow.branch?.steps ?? [])].find(
    (s) => s.id === manualSelected,
  )
  const transition = (state: AbyssState, event: FlowTransition) => {
    const previous = flow.branch?.steps.at(-1)?.state ?? flow.branch?.prepared
    if (!flow.branch || canonical(previous) === canonical(state)) return
    const id = crypto.randomUUID()
    changeFlow((old) => {
      if (
        !old.branch ||
        canonical(old.branch.steps.at(-1)?.state ?? old.branch.prepared) ===
          canonical(state)
      )
        return old
      return {
        ...old,
        branch: {
          ...old.branch,
          steps: [...old.branch.steps, { id, state, transition: event }],
        },
      }
    })
    if (event.operation !== 'restore') setManualSelected(id)
  }
  const [storageFailed, setStorageFailed] = useState(false)
  const [expired, setExpired] = useState(false)
  const [scopeUnsupported, setScopeUnsupported] = useState(false)
  const [invalidItem, setInvalidItem] = useState(false)
  const [capacity, setCapacity] = useState<AutomaticError | null>(null)
  const commandInFlight = useRef(false)
  const automaticResumeRevision = useRef<string | null>(null)
  const [controlPhase, setControlPhase] = useState<'STOP' | 'RESUME' | null>(
    null,
  )
  const [runEnabled, setRunEnabled] = useState(!snapshot && !saved)
  const [route, setRoute] = useState<Selection[]>(saved?.route ?? [])
  const [view, setView] = useState<AutomaticView | null>(
      snapshot?.result ?? null,
    ),
    [failed, setFailed] = useState(false),
    [busy, setBusy] = useState(false),
    [cycles, setCycles] = useState(snapshot?.cycles ?? 100),
    [selected, setSelected] = useState(saved?.selected ?? 0)
  const current = useRef<AutomaticView | null>(null),
    alive = useRef(false),
    token = useRef(0)
  const controllers = useRef(new Set<AbortController>())
  const accept = (next: AutomaticView) => {
    if (alive.current && acceptsAutomatic(current.current, next)) {
      current.current = next
      setView(next)
    }
  }
  useEffect(() => {
    if (snapshot) return
    alive.current = true
    const turn = ++token.current
    const controller = new AbortController()
    controllers.current.add(controller)
    const start = async () => {
      try {
        const job = saved?.job
        let next: AutomaticView
        try {
          next = job
            ? await automaticApi.get(
                job,
                request.start.provenance.rulesetIdentity,
                controller.signal,
              )
            : await automaticApi.create(
                request,
                controller.signal,
                abstractQuality40,
                abstractQuality,
              )
        } catch (error) {
          if (
            job &&
            error instanceof AutomaticError &&
            [404, 409].includes(error.status)
          ) {
            if (alive.current) setExpired(true)
            return
          }
          throw error
        }
        if (!alive.current) {
          if (next.reason === 'LEGACY_READ_ONLY') return
          void cancelAutomatic(
            next.id,
            request.start.provenance.rulesetIdentity,
          )
          return
        }
        if (turn !== token.current) return
        setStorageFailed(
          !saveSupport('run', {
            request: request.clientRequestId,
            job: next.id,
            selected: saved?.selected ?? 0,
            cycles: 100,
            route: saved?.route ?? [],
          }),
        )
        accept(next)
      } catch (error) {
        if (
          alive.current &&
          turn === token.current &&
          !(error instanceof DOMException && error.name === 'AbortError')
        ) {
          if (
            error instanceof AutomaticError &&
            error.code === 'GOAL_EXCEEDS_SIX_EXPLICIT_SCOPE'
          )
            setScopeUnsupported(true)
          else if (
            error instanceof AutomaticError &&
            error.code === 'INVALID_ITEM'
          )
            setInvalidItem(true)
          else if (error instanceof AutomaticError && error.status === 429)
            setCapacity(error)
          else setFailed(true)
        }
      } finally {
        controllers.current.delete(controller)
      }
    }
    const startTimer = setTimeout(() => void start(), 0)
    const pending = controllers.current
    return () => {
      clearTimeout(startTimer)
      alive.current = false
      token.current = turn + 1
      for (const c of pending) c.abort()
      pending.clear()
    }
    // A committed request is immutable; the outer key owns subsequent generations.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])
  const command = async (operation: 'ADVANCE' | 'RESUME') => {
    const previous = current.current
    if (
      !previous ||
      previous.reason === 'LEGACY_READ_ONLY' ||
      commandInFlight.current
    )
      return
    commandInFlight.current = true
    setBusy(true)
    if (operation === 'RESUME') setControlPhase('RESUME')
    setFailed(false)
    const controller = new AbortController()
    controllers.current.add(controller)
    try {
      accept(
        await automaticApi.control(
          previous,
          operation,
          request.start.provenance.rulesetIdentity,
          controller.signal,
        ),
      )
    } catch (error) {
      if (
        alive.current &&
        error instanceof AutomaticError &&
        error.status === 409
      ) {
        try {
          accept(
            await automaticApi.get(
              previous.id,
              request.start.provenance.rulesetIdentity,
              controller.signal,
            ),
          )
        } catch {
          setFailed(true)
        }
      } else if (
        alive.current &&
        !(error instanceof DOMException && error.name === 'AbortError')
      )
        setFailed(true)
    } finally {
      commandInFlight.current = false
      controllers.current.delete(controller)
      if (alive.current) {
        setBusy(false)
        if (operation === 'RESUME')
          setControlPhase((phase) => (phase === 'RESUME' ? null : phase))
      }
    }
  }
  useEffect(() => {
    if (
      !view ||
      !(
        view.status === 'READY' ||
        (view.status === 'PAUSED' &&
          view.reason === 'RESOURCE_BUDGET' &&
          view.canResume === true)
      ) ||
      !runEnabled ||
      !active ||
      busy ||
      failed
    )
      return
    const resumeRevision = [view.id, view.revision, view.generation].join(':')
    if (
      view.status === 'PAUSED' &&
      automaticResumeRevision.current === resumeRevision
    )
      return
    const timeout = setTimeout(() => {
      if (view.status === 'PAUSED')
        automaticResumeRevision.current = resumeRevision
      void command(view.status === 'READY' ? 'ADVANCE' : 'RESUME')
    }, 150)
    return () => clearTimeout(timeout)
    // Each accepted revision schedules exactly one bounded slice.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [view, busy, failed, runEnabled, active])
  const stop = () => {
    setRunEnabled(false)
    if (!current.current || controlPhase === 'STOP') return
    setControlPhase('STOP')
    void cancelAutomatic(
      current.current.id,
      request.start.provenance.rulesetIdentity,
    )
      .then((next) => {
        if (!alive.current) return
        if (next) accept(next)
        else setFailed(true)
      })
      .finally(() => {
        if (alive.current) setControlPhase(null)
      })
  }
  const selectedNode =
    view?.nodes.find((n) => n.id === selected) ?? view?.nodes[0]
  const action = (a: AutomaticAction) =>
    a.operation === 'DESECRATE'
      ? {
          en: 'Desecrate',
          ko: '훼손',
          'zh-CN': '亵渎',
          'zh-TW': '褻瀆',
          ja: '冒涜',
          es: 'Profanar',

          ru: 'Осквернить',
          th: 'ดูหมิ่นศาสนา',
        }[locale] + (a.side === 'PREFIX' ? ' · P' : ' · S')
      : a.purpose === 'QUALITY_BASE_CHECKPOINT'
        ? qualityGoalMessages[locale].work20
        : a.purpose === 'QUALITY_CHECKPOINT'
          ? quality.work
          : `${localizedAction(a.currency as WorkbenchAction)}${a.comparison ? ` · ${((100 * a.comparison.baseline.targetWeight) / a.comparison.baseline.totalWeight).toFixed(2)}%(~${((100 * a.comparison.afterBlocking.targetWeight) / a.comparison.afterBlocking.totalWeight).toFixed(2)}%)` : ''}${
              a.omens.length
                ? ` + ${a.omens
                    .map((o) =>
                      name(
                        `Omen_of_${o
                          .toLowerCase()
                          .split('_')
                          .map((p) => p[0]!.toUpperCase() + p.slice(1))
                          .join('_')}`,
                        o.replaceAll('_', ' '),
                      ),
                    )
                    .join(' + ')}`
                : ''
            }`
  const guide = guideMessages[locale]
  const recommendations = view?.recommendations.find(
    (r) => r.node === selectedNode?.id && r.cycles === cycles,
  )
  const jobId = view?.id
  useEffect(() => {
    if (!jobId || snapshot) return
    const persist = () =>
      setStorageFailed(
        !saveSupport('run', {
          request: request.clientRequestId,
          job: jobId,
          selected,
          cycles,
          route,
        }),
      )
    const timer = setTimeout(persist, 100)
    window.addEventListener('pagehide', persist)
    return () => {
      clearTimeout(timer)
      window.removeEventListener('pagehide', persist)
    }
  }, [jobId, request.clientRequestId, selected, cycles, route, snapshot])
  const selectNode = (id: number) => {
    setSelected(id)
    setRoute((previous) => {
      const i = previous.findIndex((e) => e.target === id)
      return i >= 0 ? previous.slice(0, i + 1) : []
    })
  }
  const chooseEdge = (edge: Selection) => {
    setSelected(edge.target)
    setRoute((previous) =>
      routeThrough(validRoute(view!, previous, cycles) ? previous : [], edge),
    )
  }
  const toolbar = (
    <>
      {!capacity && !scopeUnsupported && !invalidItem && (
        <CalculationStatus
          state={calculationState(
            view,
            runEnabled && active,
            busy,
            controlPhase,
            failed,
            expired,
          )}
        />
      )}
      {!snapshot && view?.reason !== 'LEGACY_READ_ONLY' && (
        <div className="method-map__controls">
          <button
            onClick={stop}
            disabled={
              !view || view.status === 'CANCELLED' || controlPhase === 'STOP'
            }
          >
            {controlPhase === 'STOP' ? (
              <CalculationControlLabel state="stopping" />
            ) : (
              common.stop
            )}
          </button>
          {view &&
            (['PAUSED', 'CANCELLED'].includes(view.status) ||
              (view.status === 'READY' && !runEnabled)) && (
              <button
                disabled={
                  busy || controlPhase !== null || view.canResume === false
                }
                onClick={() => {
                  setRunEnabled(true)
                  if (view.status !== 'READY') void command('RESUME')
                }}
              >
                {controlPhase === 'RESUME' ? (
                  <CalculationControlLabel state="resuming" />
                ) : (
                  common.resume
                )}
              </button>
            )}
          <button
            onClick={() => {
              stop()
              onRecalculate()
            }}
          >
            {common.recalculate}
          </button>
        </div>
      )}
    </>
  )
  return (
    <section className="method-map" aria-label={common.title}>
      {view?.reason === 'NO_PLAN_IN_SUPPORTED_SCOPE' && (
        <p role="status">{noPlan[locale]}</p>
      )}
      {view?.status === 'PAUSED' && view.canResume === false && (
        <p role="status">{planningNotes[locale][4]}</p>
      )}
      {storageFailed && (
        <p role="status">{workspaceMessages[locale].storage}</p>
      )}
      {expired && <p role="alert">{workspaceMessages[locale].expired}</p>}
      {failed && <p role="alert">{common.error}</p>}
      {invalidItem && <p role="alert">{invalidItemMessages[locale]}</p>}
      {scopeUnsupported && <p role="alert">{scopeMessages[locale]}</p>}
      {capacity && (
        <p role="alert">
          {capacity.code === 'JOB_CAPACITY'
            ? capacityMessages[locale].full
            : capacity.code === 'JOB_STORAGE_CAPACITY'
              ? capacityMessages[locale].storage
              : capacityMessages[locale].limited}
          {capacity.retryAt !== null && (
            <>
              {' '}
              {capacityMessages[locale].after}:{' '}
              {new Date(capacity.retryAt).toLocaleString(locale)}
            </>
          )}
        </p>
      )}
      {view?.reason === 'LEGACY_READ_ONLY' && (
        <p role="status">{legacyResultMessages[locale]}</p>
      )}
      {!snapshot && view?.reason === 'LEGACY_READ_ONLY' && (
        <button onClick={onRecalculate}>{common.recalculate}</button>
      )}
      <NumericRollNotice models={view?.rollModels} />
      {!view && toolbar}
      {(flow.branch || flow.prefix.length > 0) && (
        <p>{flowExportMessages[locale]}</p>
      )}
      {view && (
        <ScenarioExport
          request={request}
          view={view}
          selected={selectedNode?.id ?? 0}
          cycles={cycles}
          route={validRoute(view, route, cycles) ? route : []}
          presentItem={presentItem}
          abstractQuality40={abstractQuality40}
          abstractQuality={abstractQuality}
          baseKey={scenarioBaseKey}
          frozen={snapshot}
          goalLabels={goalLabels}
          junk={
            graphDefinitions
              ? junkClassifier(targetSources, graphDefinitions)
              : undefined
          }
        />
      )}
      {view && (
        <>
          <div className="automatic-workspace">
            <GroupedMethodGraph
              toolbar={toolbar}
              detailContent={
                <div className="graph-selection-content">
                  <h3>{guide.selected}</h3>
                  <details className="item-switcher">
                    <summary>{guide.otherItems}</summary>
                    <label>
                      <span className="goal-filter-sr-only">
                        {guide.otherItems}
                      </span>
                      <select
                        aria-label={guide.otherItems}
                        value={selectedNode?.id ?? 0}
                        onChange={(e) => selectNode(Number(e.target.value))}
                      >
                        {view.nodes.map((n, index) => (
                          <option key={n.id} value={n.id}>
                            {n.id === 0
                              ? guide.start
                              : guide.result + ' ' + index}
                          </option>
                        ))}
                      </select>
                    </label>
                  </details>
                  {selectedNode && (
                    <div className="method-map__states">
                      <article>
                        {!snapshot && (
                          <p>{strictExaltedMessages[locale].policy}</p>
                        )}
                        {!snapshot &&
                          selectedNode.discoveryFinished &&
                          selectedNode.progress.goal !== 'MATCH' &&
                          !view.methods.some(
                            (m) => m.source === selectedNode.id,
                          ) && (
                            <p role="status">
                              {strictExaltedMessages[locale].blocked}
                            </p>
                          )}
                        {selectedNode.quality40 && <p>{quality.completed}</p>}
                        {!selectedNode.quality40 &&
                          (selectedNode.abstractQuality ?? 0) >= 20 && (
                            <p>{qualityGoalMessages[locale].work20} ✓</p>
                          )}
                        <p>
                          {manualStep
                            ? common.unknown
                            : selectedNode.progress.goal === 'MATCH'
                              ? common.hit
                              : selectedNode.progress.goal === 'NO_MATCH'
                                ? common.noMatch
                                : selectedNode.progress.goal === 'UNSUPPORTED'
                                  ? common.unsupported
                                  : common.unknown}
                        </p>
                        <ItemCard
                          item={
                            manualStep
                              ? abyssCard(manualStep.state, presentItem, locale)
                              : snapshot?.cards[String(selectedNode.id)]?.[
                                    locale
                                  ]
                                ? compactFrozenCard(
                                    snapshot.cards[String(selectedNode.id)]![
                                      locale
                                    ],
                                  )
                                : presentAutomaticItem(
                                    selectedNode,
                                    locale,
                                    presentItem,
                                    view.nodes,
                                  )
                          }
                        />
                        {onEditState &&
                          automaticItemIsDistribution(selectedNode) && (
                            <p>{distributionEditMessages[locale]}</p>
                          )}
                        {onEditState &&
                          !selectedNode.item.desecrated?.length &&
                          !manualStep &&
                          !flow.carry &&
                          !automaticItemIsDistribution(selectedNode) && (
                            <>
                              <button
                                onClick={() => {
                                  stop()
                                  onEditState(
                                    structuredClone(selectedNode.item),
                                    {
                                      abstractQuality40: selectedNode.quality40,
                                      abstractQuality:
                                        selectedNode.abstractQuality ??
                                        (selectedNode.quality40 ? 40 : 0),
                                    },
                                  )
                                }}
                              >
                                {guide.edit}
                              </button>
                            </>
                          )}
                      </article>
                    </div>
                  )}
                  <CandidateRoute
                    view={view}
                    selected={selectedNode?.id ?? 0}
                    cycles={cycles}
                    route={route}
                    choose={chooseEdge}
                    clear={() => setRoute([])}
                    action={action}
                  />
                  {view.methods.some(
                    (m) =>
                      m.source === selectedNode?.id &&
                      m.cycle.some((a) => a.comparison),
                  ) && (
                    <p>
                      {
                        {
                          en: 'p%(~N%): next modifier selection before / after selecting the highest-weight non-target blocker. This is not a reveal probability or the full goal chance.',
                          ko: 'p%(~N%): 다음 속성 선택의 기준 확률 / 최고 가중치 비목표 차단 옵션 선택 후 확률입니다. 드러내기 추첨 확률이나 전체 목표 성공률이 아닙니다.',
                          'zh-CN':
                            'p%(~N%)：下一条属性的基准概率 / 选择最高权重非目标阻挡属性后的概率。不是揭示抽取概率或完整目标成功率。',
                          'zh-TW':
                            'p%(~N%)：下一條屬性的基準機率 / 選擇最高權重非目標阻擋屬性後的機率。不是揭示抽取機率或完整目標成功率。',
                          ja: 'p%(~N%)：次の特性選択の基準確率 / 最大重みの非目標ブロッカー選択後の確率。公開候補の抽選確率や目標全体の成功率ではありません。',
                          es: 'p%(~N%): probabilidad base del siguiente modificador / tras elegir el bloqueador no objetivo de mayor peso. No es la probabilidad de revelado ni del objetivo completo.',

                          ru: 'p%(~N%): следующий выбор модификатора до/после выбора нецелевого блокирующего с наибольшим весом. Это не вероятность раскрытия или полная вероятность гола.',
                          th: 'p%(~N%): การเลือกม็อดถัดไปก่อน/หลังการเลือกตัวบล็อกที่ไม่ใช่เป้าหมายที่มีน้ำหนักสูงสุด นี่ไม่ใช่การเปิดเผยความน่าจะเป็นหรือโอกาสทำประตูทั้งหมด',
                        }[locale]
                      }
                    </p>
                  )}
                  {view.methods.some((m) => m.source === selectedNode?.id) && (
                    <h3>{guide.next}</h3>
                  )}
                  {(flow.branch || flow.prefix.length > 0) && (
                    <p>{flowMessages[locale].conditional}</p>
                  )}
                  {!snapshot &&
                    selectedNode &&
                    !!selectedNode.item.desecrated?.length &&
                    !automaticItemIsDistribution(selectedNode) && (
                      <CommonSlotObservation
                        item={selectedNode.item}
                        base={scenarioBaseKey}
                        identity={request.start.provenance.rulesetIdentity}
                        onTransition={(state, event) => {
                          if (flow.branch?.sourceNode === selectedNode.id) {
                            transition(state, event)
                            return
                          }
                          changeFlow((old) => ({
                            ...old,
                            anchor:
                              old.anchor ?? (selectedNode.item as ConcreteItem),
                            branch: {
                              sourceNode: selectedNode.id,
                              source: selectedNode.item as ConcreteItem,
                              prepared: state,
                              action: 'EXALTED',
                              omens: [],
                              steps: [
                                {
                                  id: crypto.randomUUID(),
                                  state,
                                  transition: event,
                                },
                              ],
                            },
                          }))
                        }}
                        onContinue={(state, item) => {
                          stop()
                          continueFlow(state, item)
                        }}
                      />
                    )}
                  <div className="method-map__methods">
                    <OperationCards
                      recommendations={view.recommendations}
                      methods={view.methods.filter(
                        (m) => !manualStep && m.source === selectedNode?.id,
                      )}
                      cycles={cycles}
                      action={action}
                      render={(m, methodIndex) => {
                        const point = methodPoint(m, cycles)
                        const local = recommendations?.local.includes(m.id),
                          final =
                            view.complete &&
                            recommendations?.strategyMethods.includes(m.id)
                        return (
                          <details
                            className="craft-choice"
                            key={m.id}
                            open={methodIndex === 0}
                          >
                            <summary>
                              <strong>
                                {m.initial
                                  .concat(m.cycle)
                                  .map(action)
                                  .join(' → ')}
                              </strong>
                              <span>
                                {isRepeatable(m)
                                  ? (m.initial.length
                                      ? m.initial.map(action).join(' → ') +
                                        ' · ' +
                                        m.cycle.map(action).join(' → ') +
                                        ': '
                                      : '') +
                                    guide.within.replace('{n}', String(cycles))
                                  : m.executionMode === 'PENDING'
                                    ? outcomeMessages[locale].pending
                                    : ''}
                              </span>
                              {point && (
                                <span className="craft-chance">
                                  {(() => {
                                    const goal = point.outcomes.filter(
                                      (o) => o.kind === 'GOAL',
                                    )
                                    const next = goal.length
                                      ? goal
                                      : point.outcomes.filter(
                                          (o) => o.kind === 'CHECKPOINT',
                                        )
                                    if (!next.length) return common.unknown
                                    const mass = sumBounds(
                                      next.map((o) => o.probability),
                                    )
                                    return (
                                      (goal.length
                                        ? common.hit
                                        : copy.checkpoint) +
                                      ': ' +
                                      probabilityText(mass.lower, mass)
                                    )
                                  })()}
                                </span>
                              )}
                            </summary>
                            <article
                              className={
                                final || local ? 'method-map__recommended' : ''
                              }
                            >
                              {local && <strong>{copy.local}</strong>}
                              {final && <strong>{copy.final}</strong>}
                              {m.reasons.includes(
                                'QUALITY_CAP_REMOVAL_BEFORE_40',
                              ) && <p>{policy[18]}</p>}

                              {m.initial.length > 0 && (
                                <p>
                                  {guide.prepared}:{' '}
                                  {m.initial.map(action).join(' → ')}
                                </p>
                              )}

                              {removesModifier(m) && (
                                <p className="craft-risk">
                                  {guide.risk}{' '}
                                  <a href="#crafting-policy">{guide.scope}</a>
                                </p>
                              )}
                              {isRepeatable(m) && (
                                <label>
                                  {copy.cycles}
                                  <select
                                    aria-label={copy.cycles}
                                    value={cycles}
                                    onChange={(event) =>
                                      setCycles(
                                        snapshot
                                          ? Number(event.target.value)
                                          : 100,
                                      )
                                    }
                                  >
                                    {(snapshot ? [100, 300, 500] : [100]).map(
                                      (n) => (
                                        <option key={n}>{n}</option>
                                      ),
                                    )}
                                  </select>
                                </label>
                              )}
                              {!point ? (
                                <p>{common.calculating}</p>
                              ) : (
                                <>
                                  {!point.complete && (
                                    <p role="status">{common.calculating}</p>
                                  )}
                                  <details className="craft-outcomes">
                                    <summary>{guide.details}</summary>
                                    <ul>
                                      {point.outcomes.map((o, i) => (
                                        <li key={i}>
                                          {o.target === null ? (
                                            o.kind === 'RETRY' ? (
                                              guide.result
                                            ) : (
                                              common.unknown
                                            )
                                          ) : (
                                            <button
                                              onClick={() =>
                                                chooseEdge({
                                                  method: m.id,
                                                  source: m.source,
                                                  target: o.target!,
                                                  kind: o.kind,
                                                })
                                              }
                                            >
                                              {guide.result} {i + 1}
                                            </button>
                                          )}{' '}
                                          —{' '}
                                          {o.kind === 'RETRY'
                                            ? retryMessages[locale].replace(
                                                'N',
                                                String(cycles),
                                              )
                                            : o.kind === 'GOAL'
                                              ? common.hit
                                              : o.kind === 'LOSS'
                                                ? copy.loss
                                                : o.kind === 'UNRESOLVED'
                                                  ? common.unknown
                                                  : o.kind === 'DEAD'
                                                    ? common.unavailable
                                                    : copy.checkpoint}
                                          :{' '}
                                          {!point.complete &&
                                          o.kind !== 'UNRESOLVED'
                                            ? '≥ '
                                            : ''}
                                          {probabilityText(
                                            o.probability.lower,
                                            o.probability,
                                          )}
                                        </li>
                                      ))}
                                    </ul>
                                  </details>
                                  <details>
                                    <summary>{copy.uses}</summary>
                                    {[point.initialUses, point.cycleUses].map(
                                      (uses, i) => (
                                        <p key={i}>
                                          {i === 0
                                            ? copy.initial
                                            : isRepeatable(m)
                                              ? copy.cycles
                                              : outcomeMessages[locale].single}
                                          :{' '}
                                          {Object.entries(uses)
                                            .filter(
                                              ([material]) =>
                                                !material.startsWith('WORK:'),
                                            )
                                            .map(
                                              ([material, b]) =>
                                                `${material.startsWith('OMEN:') ? material.slice(5).replaceAll('_', ' ') : localizedAction(material as WorkbenchAction)} ${quantityText(b.lower, b)}`,
                                            )
                                            .join(', ') || '0'}
                                        </p>
                                      ),
                                    )}
                                  </details>
                                  {[point.initialUses, point.cycleUses].map(
                                    (uses, i) => {
                                      const value = uses['WORK:QUALITY_40']
                                      return value ? (
                                        <p key={i}>
                                          {quality.work}:{' '}
                                          {quantityText(value.lower, value)}
                                        </p>
                                      ) : null
                                    },
                                  )}
                                </>
                              )}
                            </article>
                          </details>
                        )
                      }}
                    />
                  </div>
                </div>
              }
              view={view}
              cycles={cycles}
              selected={selectedNode?.id ?? 0}
              route={route}
              choose={chooseEdge}
              select={(id) => {
                setManualSelected(null)
                selectNode(id)
              }}
              flow={flow}
              selectedFlow={manualSelected}
              selectFlow={setManualSelected}
              action={action}
              presentItem={presentItem}
              frozenCards={snapshot?.cards}
              junk={
                graphDefinitions
                  ? junkClassifier(targetSources, graphDefinitions)
                  : undefined
              }
            />
          </div>
        </>
      )}
    </section>
  )
}
