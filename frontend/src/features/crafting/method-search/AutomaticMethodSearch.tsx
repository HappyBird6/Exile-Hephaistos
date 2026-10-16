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
}: PathTreeProps) {
  const { locale, name } = useI18n(),
    copy = methodSearchMessages[locale],
    common = pathTreeMessages[locale],
    policy = policyMessages[locale],
    quality = qualityWorkMessages[locale]
  const [view, setView] = useState<AutomaticView | null>(null),
    [failed, setFailed] = useState(false),
    [busy, setBusy] = useState(false),
    [cycles, setCycles] = useState(100),
    [selected, setSelected] = useState(0)
  const current = useRef<AutomaticView | null>(null),
    alive = useRef(false),
    token = useRef(0),
    running = useRef(true)
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
    const storageKey = `automatic-method:${request.clientRequestId}`
    const start = async () => {
      try {
        const saved = sessionStorage.getItem(storageKey)
        let next: AutomaticView
        try {
          next = saved
            ? await automaticApi.get(
                saved,
                request.start.provenance.rulesetIdentity,
                controller.signal,
              )
            : await automaticApi.create(
                request,
                controller.signal,
                abstractQuality40,
              )
        } catch (error) {
          if (saved && error instanceof AutomaticError && error.status === 404)
            next = await automaticApi.create(
              request,
              controller.signal,
              abstractQuality40,
            )
          else throw error
        }
        if (!alive.current) {
          void cancelAutomatic(
            next.id,
            request.start.provenance.rulesetIdentity,
          )
          return
        }
        if (turn !== token.current) return
        sessionStorage.setItem(storageKey, next.id)
        accept(next)
      } catch (error) {
        if (
          alive.current &&
          turn === token.current &&
          !(error instanceof DOMException && error.name === 'AbortError')
        )
          setFailed(true)
      } finally {
        controllers.current.delete(controller)
      }
    }
    void start()
    const pending = controllers.current
    return () => {
      alive.current = false
      token.current = turn + 1
      for (const c of pending) c.abort()
      pending.clear()
      if (current.current)
        void cancelAutomatic(
          current.current.id,
          request.start.provenance.rulesetIdentity,
        )
    }
    // A committed request is immutable; the outer key owns subsequent generations.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])
  const command = async (operation: 'ADVANCE' | 'RESUME') => {
    const previous = current.current
    if (!previous) return
    setBusy(true)
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
      controllers.current.delete(controller)
      if (alive.current) setBusy(false)
    }
  }
  useEffect(() => {
    if (!view || view.status !== 'READY' || !running.current || busy || failed)
      return
    const timeout = setTimeout(() => void command('ADVANCE'), 150)
    return () => clearTimeout(timeout)
    // Each accepted revision schedules exactly one bounded slice.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [view, busy, failed])
  const stop = () => {
    running.current = false
    if (current.current)
      void cancelAutomatic(
        current.current.id,
        request.start.provenance.rulesetIdentity,
      ).then((next) => {
        if (next) accept(next)
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
  const recommendations = view?.recommendations.find(
    (r) => r.node === selectedNode?.id && r.cycles === cycles,
  )
  const positions = new Map<number, { x: number; y: number }>(),
    rows = new Map<number, number>()
  for (const n of view?.nodes ?? []) {
    const row = rows.get(n.depth) ?? 0
    rows.set(n.depth, row + 1)
    positions.set(n.id, { x: 55 + n.depth * 180, y: 35 + row * 65 })
  }
  const width = Math.max(320, ...[...positions.values()].map((p) => p.x + 80)),
    height = Math.max(100, ...[...positions.values()].map((p) => p.y + 45))
  return (
    <section className="method-map" aria-label={common.title}>
      <h2>{common.title}</h2>
      {failed && (
        <p role="alert">
          {common.error}
          <button
            onClick={() => {
              setFailed(false)
              if (current.current) void command('ADVANCE')
              else onRecalculate()
            }}
          >
            {common.recalculate}
          </button>
        </p>
      )}
      <p role="status">
        {!view || view.status === 'READY'
          ? common.calculating
          : view.status === 'COMPLETE'
            ? copy.complete
            : view.status === 'CANCELLED'
              ? common.stopped
              : common.partial}
      </p>
      <div className="method-map__controls">
        <button onClick={stop} disabled={!view || view.status === 'CANCELLED'}>
          {common.stop}
        </button>
        {view && ['PAUSED', 'CANCELLED'].includes(view.status) && (
          <button
            disabled={busy}
            onClick={() => {
              running.current = true
              void command('RESUME')
            }}
          >
            {common.resume}
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
        <label>
          {copy.cycles}
          <select
            value={cycles}
            onChange={(e) => setCycles(Number(e.target.value))}
          >
            {[100, 300, 500].map((n) => (
              <option key={n}>{n}</option>
            ))}
          </select>
        </label>
      </div>
      {view && (
        <>
          <div
            className="method-map__overview"
            style={{ overflow: 'auto', maxHeight: 360 }}
          >
            <svg width={width} height={height} aria-label={common.title}>
              <defs>
                <marker
                  id={`automatic-arrow-${view.id}`}
                  markerWidth="8"
                  markerHeight="8"
                  refX="7"
                  refY="4"
                  orient="auto"
                >
                  <path d="M0,0 L8,4 L0,8 Z" fill="currentColor" />
                </marker>
              </defs>
              {view.methods.flatMap((m, index) => {
                const source = positions.get(m.source)
                const point = m.points.find((p) => p.cycles === cycles)
                if (!source || !point) return []
                return point.outcomes
                  .filter(
                    (o) =>
                      o.target !== null &&
                      o.kind !== 'UNRESOLVED' &&
                      o.kind !== 'DEAD',
                  )
                  .map((o, i) => {
                    const target = positions.get(o.target!)
                    if (!target) return null
                    const offset = 12 + (index % 7) * 5
                    return (
                      <path
                        key={`${m.id}:${i}`}
                        d={`M${source.x + 18},${source.y} Q${(source.x + target.x) / 2 + offset},${(source.y + target.y) / 2 - offset} ${target.x - 18},${target.y}`}
                        fill="none"
                        stroke="currentColor"
                        opacity={m.source === selectedNode?.id ? 1 : 0.3}
                        markerEnd={`url(#automatic-arrow-${view.id})`}
                      >
                        <title>
                          {m.cycle.map(action).join(' → ')}:{' '}
                          {probabilityText(o.probability.lower, o.probability)}
                        </title>
                      </path>
                    )
                  })
              })}
              {view.nodes.map((n) => {
                const p = positions.get(n.id)!
                return (
                  <g
                    key={n.id}
                    role="button"
                    tabIndex={0}
                    aria-label={`${copy.state} ${n.id}`}
                    onClick={() => setSelected(n.id)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' || e.key === ' ') setSelected(n.id)
                    }}
                  >
                    <circle
                      cx={p.x}
                      cy={p.y}
                      r="18"
                      fill={n.id === selectedNode?.id ? '#c69b57' : '#242424'}
                      stroke="currentColor"
                    />
                    <text x={p.x} y={p.y + 5} textAnchor="middle" fill="white">
                      {n.id}
                    </text>
                  </g>
                )
              })}
            </svg>
          </div>
          <label>
            {copy.state}
            <select
              value={selectedNode?.id ?? 0}
              onChange={(e) => setSelected(Number(e.target.value))}
            >
              {view.nodes.map((n) => (
                <option key={n.id} value={n.id}>
                  {copy.state} {n.id}
                </option>
              ))}
            </select>
          </label>
          {selectedNode && (
            <div className="method-map__states">
              <article>
                <h3>
                  {copy.state} {selectedNode.id}
                </h3>
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
                      {policy[22]}
                    </button>
                    <p>{policy[23]}</p>
                  </>
                )}
              </article>
            </div>
          )}
          {recommendations && (
            <p>
              {view.complete ? copy.final : copy.eventual}:{' '}
              {probabilityText(
                recommendations.strategy.lower,
                recommendations.strategy,
              )}
            </p>
          )}
          <div className="method-map__methods">
            {view.methods
              .filter((m) => m.source === selectedNode?.id)
              .map((m) => {
                const point = m.points.find((p) => p.cycles === cycles)
                const local = recommendations?.local.includes(m.id),
                  final =
                    view.complete &&
                    recommendations?.strategyMethods.includes(m.id)
                return (
                  <article
                    key={m.id}
                    className={final || local ? 'method-map__recommended' : ''}
                  >
                    {local && <strong>{copy.local}</strong>}
                    {final && <strong>{copy.final}</strong>}
                    {m.reasons.includes('QUALITY_CAP_REMOVAL_BEFORE_40') && (
                      <p>{policy[18]}</p>
                    )}
                    {m.initial.length > 0 && (
                      <p>
                        {copy.initial}: {m.initial.map(action).join(' → ')}
                      </p>
                    )}
                    <h3>{m.cycle.map(action).join(' → ')}</h3>
                    {!point ? (
                      <p>{common.calculating}</p>
                    ) : (
                      <>
                        <ul>
                          {point.outcomes.map((o, i) => (
                            <li key={i}>
                              {o.target === null ? (
                                common.unknown
                              ) : (
                                <button onClick={() => setSelected(o.target!)}>
                                  {copy.state} {o.target}
                                </button>
                              )}{' '}
                              —{' '}
                              {o.kind === 'GOAL'
                                ? common.hit
                                : o.kind === 'LOSS'
                                  ? copy.loss
                                  : o.kind === 'UNRESOLVED'
                                    ? common.unknown
                                    : o.kind === 'DEAD'
                                      ? common.unavailable
                                      : copy.checkpoint}
                              :{' '}
                              {!point.complete && o.kind !== 'UNRESOLVED'
                                ? '≥ '
                                : ''}
                              {probabilityText(
                                o.probability.lower,
                                o.probability,
                              )}
                            </li>
                          ))}
                        </ul>
                        <details>
                          <summary>{copy.uses}</summary>
                          {[point.initialUses, point.cycleUses].map(
                            (uses, i) => (
                              <p key={i}>
                                {i === 0 ? copy.initial : copy.cycles}:{' '}
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
                        {[point.initialUses, point.cycleUses].map((uses, i) => {
                          const value = uses['WORK:QUALITY_40']
                          return value ? (
                            <p key={i}>
                              {quality.work}: {quantityText(value.lower, value)}
                            </p>
                          ) : null
                        })}
                      </>
                    )}
                  </article>
                )
              })}
          </div>
        </>
      )}
    </section>
  )
}
