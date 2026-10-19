import { retryMessages } from './retryMessages'
import { capacityMessages } from './capacityMessages'
import { sumBounds } from './outcomeGrouping'
import { guideMessages } from './guideMessages'
import { removesModifier } from './methodRisk'
import { GroupedMethodGraph } from './GroupedMethodGraph'
import { isRepeatable, methodPoint } from './methodObservation'
import { outcomeMessages } from './outcomeMessages'
import { CalculationStatus, CalculationControlLabel } from './CalculationStatus'
import { PlanningDisclosure } from './PlanningDisclosure'
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
import type { WorkbenchAction } from '../workbenchApi'
import type { PathTreeProps } from '../path-tree/PathTree'
import { canonical } from '../path-tree/validation'
import { pathTreeMessages } from '../path-tree/messages'
import { methodSearchMessages } from './messages'
import { policyMessages } from './policyMessages'
import { qualityWorkMessages } from './qualityWorkMessages'
import { presentRollCell } from './rollPresentation'
import { probabilityText, quantityText } from './probabilityPresentation'
import {
  automaticApi,
  AutomaticError,
  acceptsAutomatic,
  cancelAutomatic,
} from './automaticApi'
import type { AutomaticAction, AutomaticView } from './automaticApi'
import './method-search.css'

export function AutomaticMethodSearch(props: PathTreeProps) {
  return (
    <Run
      key={canonical([
        props.inputGeneration,
        props.request,
        props.abstractQuality40,
      ])}
      {...props}
    />
  )
}
function Run({
  request,
  presentItem,
  onEditState,
  onRecalculate,
  abstractQuality40 = false,
  active = true,
}: PathTreeProps) {
  const { locale, name } = useI18n(),
    copy = methodSearchMessages[locale],
    common = pathTreeMessages[locale],
    policy = policyMessages[locale],
    quality = qualityWorkMessages[locale]
  const [saved] = useState(() => {
    const stored = readSupport('run', validRun)
    return stored?.request === request.clientRequestId ? stored : null
  })
  const [storageFailed, setStorageFailed] = useState(false)
  const [expired, setExpired] = useState(false)
  const [capacity, setCapacity] = useState<AutomaticError | null>(null)
  const commandInFlight = useRef(false)
  const [controlPhase, setControlPhase] = useState<'STOP' | 'RESUME' | null>(
    null,
  )
  const [runEnabled, setRunEnabled] = useState(!saved)
  const [route, setRoute] = useState<Selection[]>(saved?.route ?? [])
  const [view, setView] = useState<AutomaticView | null>(null),
    [failed, setFailed] = useState(false),
    [busy, setBusy] = useState(false),
    [cycles, setCycles] = useState(saved?.cycles ?? 100),
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
            cycles: saved?.cycles ?? 100,
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
          if (error instanceof AutomaticError && error.status === 429)
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
    if (!previous || commandInFlight.current) return
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
        if (operation === 'RESUME') setControlPhase(null)
      }
    }
  }
  useEffect(() => {
    if (
      !view ||
      view.status !== 'READY' ||
      !runEnabled ||
      !active ||
      busy ||
      failed
    )
      return
    const timeout = setTimeout(() => void command('ADVANCE'), 150)
    return () => clearTimeout(timeout)
    // Each accepted revision schedules exactly one bounded slice.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [view, busy, failed, runEnabled, active])
  const stop = () => {
    setRunEnabled(false)
    if (!current.current || controlPhase) return
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
    a.purpose === 'QUALITY_CHECKPOINT'
      ? quality.work
      : `${localizedAction(a.currency as WorkbenchAction)}${
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
    if (!jobId) return
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
  }, [jobId, request.clientRequestId, selected, cycles, route])
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
  return (
    <section className="method-map" aria-label={common.title}>
      <h2>{common.title}</h2>
      <PlanningDisclosure
        view={view}
        rarities={request.goal.general.rarities}
      />
      {storageFailed && (
        <p role="status">{workspaceMessages[locale].storage}</p>
      )}
      {expired && <p role="alert">{workspaceMessages[locale].expired}</p>}
      {failed && <p role="alert">{common.error}</p>}
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
      {!capacity && (
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
      <div className="method-map__controls">
        <button
          onClick={stop}
          disabled={
            !view || view.status === 'CANCELLED' || controlPhase !== null
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
      {view && (
        <>
          <div className="automatic-workspace">
            <GroupedMethodGraph
              view={view}
              cycles={cycles}
              selected={selectedNode?.id ?? 0}
              route={route}
              choose={chooseEdge}
              select={selectNode}
              action={action}
              presentItem={presentItem}
            />
            <aside className="automatic-inspector">
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
                        {n.id === 0 ? guide.start : guide.result + ' ' + index}
                      </option>
                    ))}
                  </select>
                </label>
              </details>
              {selectedNode && (
                <div className="method-map__states">
                  <article>
                    {selectedNode.quality40 && <p>{quality.completed}</p>}
                    <p>
                      {selectedNode.progress.goal === 'MATCH'
                        ? common.hit
                        : selectedNode.progress.goal === 'NO_MATCH'
                          ? common.noMatch
                          : common.unknown}
                    </p>
                    <ItemCard
                      item={presentRollCell(
                        selectedNode.item,
                        selectedNode.rolls,
                        locale,
                        presentItem,
                      )}
                    />
                    {onEditState && (
                      <>
                        <button
                          onClick={() => {
                            stop()
                            onEditState(structuredClone(selectedNode.item), {
                              abstractQuality40: selectedNode.quality40,
                            })
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
              {view.methods.some((m) => m.source === selectedNode?.id) && (
                <h3>{guide.next}</h3>
              )}
              <div className="method-map__methods">
                {view.methods
                  .filter((m) => m.source === selectedNode?.id)
                  .map((m, methodIndex) => {
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
                            {m.initial.concat(m.cycle).map(action).join(' → ')}
                          </strong>
                          <span>
                            {isRepeatable(m)
                              ? (m.initial.length
                                  ? m.initial.map(action).join(' → ') +
                                    ': ' +
                                    guide.once +
                                    ' · ' +
                                    m.cycle.map(action).join(' → ') +
                                    ': '
                                  : '') +
                                guide.within.replace('{n}', String(cycles))
                              : m.executionMode === 'PENDING'
                                ? outcomeMessages[locale].pending
                                : guide.once}
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
                                  (goal.length ? common.hit : copy.checkpoint) +
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
                                onChange={(e) =>
                                  setCycles(Number(e.target.value))
                                }
                              >
                                {[100, 300, 500].map((n) => (
                                  <option key={n}>{n}</option>
                                ))}
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
                  })}
              </div>
            </aside>
          </div>
          {recommendations && (
            <details>
              <summary>{guide.calculations}</summary>
              <p>
                {view.complete ? copy.final : copy.eventual}:{' '}
                {probabilityText(
                  recommendations.strategy.lower,
                  recommendations.strategy,
                )}
              </p>
            </details>
          )}
        </>
      )}
    </section>
  )
}
