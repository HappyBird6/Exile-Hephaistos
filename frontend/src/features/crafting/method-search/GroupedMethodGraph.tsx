import { attemptLabel } from './attemptLabel'
import { useLayoutEffect, useRef, useState } from 'react'
import type {
  AutomaticAction,
  AutomaticArrow,
  AutomaticView,
} from './automaticApi'
import type { Selection } from './supportStorage'
import type { Scenario } from './scenarioCode'
import type { PathTreeProps } from '../path-tree/PathTree'
import { methodPoint } from './methodObservation'
import { sumBounds } from './outcomeGrouping'
import { stateProjection, type BundleEdge } from './stateProjection'
import { commonStateCard } from './commonStateCard'
import { MeasuredGraphBox } from './MeasuredGraphBox'
import { GraphItemState } from './GraphItemState'
import { graphStateMessages } from './graphStateMessages'
import { graphNavigationMessages } from './graphNavigationMessages'
import { guideMessages } from './guideMessages'
import { outcomeMessages } from './outcomeMessages'
import { probabilityText } from './probabilityPresentation'
import { presentRollCell } from './rollPresentation'
import { ItemCard } from '../ItemCard'
import { useI18n } from '../../../shared/i18n/i18n'
import { pathTreeMessages } from '../path-tree/messages'

export function GroupedMethodGraph({
  view,
  cycles,
  selected,
  route,
  choose,
  select,
  action,
  presentItem,
  frozenCards,
}: {
  view: AutomaticView
  cycles: number
  selected: number
  route: Selection[]
  choose: (s: Selection) => void
  select: (id: number) => void
  action: (a: AutomaticAction) => string
  presentItem: PathTreeProps['presentItem']
  frozenCards?: Scenario['cards'] | undefined
}) {
  const { locale } = useI18n(),
    guide = guideMessages[locale],
    copy = outcomeMessages[locale],
    common = pathTreeMessages[locale],
    nav = graphNavigationMessages[locale]
  const host = useRef<HTMLDivElement>(null)
  const [viewport, setViewport] = useState({ width: 340, height: 720 }),
    [zoom, setZoom] = useState(1)
  const [measured, setMeasured] = useState<Record<string, number>>({}),
    [opened, setOpened] = useState<string | null>(null)
  useLayoutEffect(() => {
    const el = host.current
    if (!el) return
    const measure = () =>
      setViewport({
        width: el.clientWidth || 340,
        height: el.clientHeight || 720,
      })
    measure()
    if (typeof ResizeObserver === 'undefined') return
    const observer = new ResizeObserver(measure)
    observer.observe(el)
    return () => observer.disconnect()
  }, [])
  const projection = stateProjection(view, cycles)
  const nodeWidth = Math.min(320, Math.max(240, viewport.width - 24))
  const renderedCards = new Map<
    number,
    ReturnType<PathTreeProps['presentItem']>
  >()
  const cardFor = (id: number) => {
    const cached = renderedCards.get(id)
    if (cached) return cached
    const n = view.nodes.find((n) => n.id === id)!
    const card =
      frozenCards?.[String(id)]?.[locale] ??
      presentRollCell(n.item, n.rolls, locale, presentItem)
    renderedCards.set(id, card)
    return card
  }
  const labelFor = (edge: BundleEdge) =>
    edge.variants[0]!.method.initial.concat(edge.variants[0]!.method.cycle)
      .map(action)
      .join(' → ')
  const candidateArrivals = new Map<
    number,
    {
      method: AutomaticArrow
      outcome: AutomaticArrow['points'][number]['outcomes'][number]
    }[]
  >()
  for (const method of view.methods)
    for (const outcome of methodPoint(method, cycles)?.outcomes ?? []) {
      if (outcome.target === null) continue
      const rows = candidateArrivals.get(outcome.target) ?? []
      rows.push({ method, outcome })
      candidateArrivals.set(outcome.target, rows)
    }
  const incoming = new Map(
    projection.bundles.map((b) => [
      b.id,
      projection.edges.filter((e) => e.to === b.id),
    ]),
  )
  const positions = new Map<
    string,
    { x: number; y: number; height: number; labelHeight: number }
  >()
  let levelY = 20
  for (const depth of [...new Set(projection.bundles.map((b) => b.depth))].sort(
    (a, b) => a - b,
  )) {
    const level = projection.bundles.filter((b) => b.depth === depth)
    const labelHeight = Math.max(
      0,
      ...level.map(
        (b) =>
          measured['labels:' + b.id] ?? (incoming.get(b.id)?.length ?? 0) * 64,
      ),
    )
    let maxHeight = 0
    level.forEach((b, index) => {
      const height = measured[b.id] ?? 480
      maxHeight = Math.max(maxHeight, height)
      positions.set(b.id, {
        x: 12 + nodeWidth / 2 + index * (nodeWidth + 48),
        y: levelY + labelHeight + (labelHeight ? 18 : 0),
        height,
        labelHeight:
          measured['labels:' + b.id] ?? (incoming.get(b.id)?.length ?? 0) * 64,
      })
    })
    levelY += labelHeight + (labelHeight ? 18 : 0) + maxHeight + 100
  }
  const width = Math.max(
      nodeWidth + 24,
      ...[...positions.values()].map((p) => p.x + nodeWidth / 2 + 12),
    ),
    height = Math.max(100, levelY)
  const updateSize = (id: string, h: number) =>
    setMeasured((previous) =>
      previous[id] === h ? previous : { ...previous, [id]: h },
    )
  const openEdge = (edge: BundleEdge) => {
    setOpened(edge.id)
    const outcomes = edge.variants.flatMap((v) =>
      v.outcomes.map((o) => ({ method: v.method, o })),
    )
    if (outcomes.length === 1) {
      const { method, o } = outcomes[0]!
      choose({
        method: method.id,
        source: method.source,
        target: o.target!,
        kind: o.kind,
      })
    }
  }
  const chosen = projection.edges.find((e) => e.id === opened)
  const fit = () => {
    setZoom(
      Math.max(
        0.1,
        Math.min(
          1,
          (viewport.width - 8) / width,
          (viewport.height - 8) / height,
        ),
      ),
    )
    host.current?.scrollTo?.({ left: 0, top: 0 })
  }
  return (
    <div className="graph-workspace">
      <div
        className="graph-zoom-controls"
        role="group"
        aria-label={common.title}
      >
        <button
          aria-label={nav.zoomOut}
          disabled={zoom <= 0.1}
          onClick={() =>
            setZoom((z) => Math.max(0.1, Math.round((z - 0.1) * 100) / 100))
          }
        >
          −
        </button>
        <output aria-live="polite">{Math.round(zoom * 100)}%</output>
        <button
          aria-label={nav.zoomIn}
          disabled={zoom >= 2}
          onClick={() =>
            setZoom((z) => Math.min(2, Math.round((z + 0.1) * 100) / 100))
          }
        >
          +
        </button>
        <button onClick={() => setZoom(1)}>{nav.reset}</button>
        <button onClick={fit}>{nav.fit}</button>
      </div>
      <div
        ref={host}
        className="method-map__overview"
        tabIndex={0}
        role="region"
        aria-label={common.title}
      >
        <svg
          width={width * zoom}
          height={height * zoom}
          viewBox={`0 0 ${width} ${height}`}
          aria-label={common.title}
          data-bundles={projection.bundles.length}
        >
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
            <pattern
              id={`graph-dots-${view.id}`}
              width="24"
              height="24"
              patternUnits="userSpaceOnUse"
            >
              <circle cx="1" cy="1" r="1" fill="#776640" opacity="0.5" />
            </pattern>
          </defs>
          <rect
            width={width}
            height={height}
            fill={`url(#graph-dots-${view.id})`}
            pointerEvents="none"
          />
          {projection.edges.map((e) => {
            const source = positions.get(e.from)!,
              target = positions.get(e.to)!
            return (
              <path
                key={e.id}
                role="button"
                tabIndex={0}
                aria-label={labelFor(e)}
                onClick={() => openEdge(e)}
                onKeyDown={(event) => {
                  if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault()
                    openEdge(e)
                  }
                }}
                className={
                  'automatic-edge' +
                  (e.variants.some((v) =>
                    route.some(
                      (r) =>
                        r.method === v.method.id &&
                        v.outcomes.some((o) => o.target === r.target),
                    ),
                  )
                    ? ' is-selected'
                    : '')
                }
                d={`M${source.x},${source.y + source.height} C${source.x},${source.y + source.height + 50} ${target.x},${target.y - 60} ${target.x},${target.y - 3}`}
                fill="none"
                stroke="currentColor"
                strokeDasharray={
                  e.variants.some((v) =>
                    v.outcomes.some((o) => o.kind === 'UNRESOLVED'),
                  )
                    ? '5 4'
                    : undefined
                }
                markerEnd={`url(#automatic-arrow-${view.id})`}
              >
                <title>{labelFor(e)}</title>
              </path>
            )
          })}
          {projection.bundles.map((b) => {
            const p = positions.get(b.id)!,
              edges = incoming.get(b.id)!
            return (
              edges.length > 0 && (
                <foreignObject
                  key={'labels:' + b.id}
                  data-label-target={b.id}
                  x={p.x - nodeWidth / 2}
                  y={p.y - p.labelHeight - 14}
                  width={nodeWidth}
                  height={p.labelHeight + 2}
                >
                  <MeasuredGraphBox
                    className="graph-arrival-labels"
                    onSize={(h) => updateSize('labels:' + b.id, h)}
                  >
                    {edges.map((e) => (
                      <button
                        key={e.id}
                        className="graph-action-label"
                        onClick={() => openEdge(e)}
                      >
                        {labelFor(e)}
                        {e.variants.map(({ method, outcomes }) => {
                          const probability = sumBounds(
                            outcomes.map((o) => o.probability),
                          )
                          const point = methodPoint(method, cycles)
                          return (
                            <span
                              className="graph-method-probability"
                              key={method.id}
                            >
                              {nav.source} {method.source} ·{' '}
                              {point && point.cycles > 1 && (
                                <>{attemptLabel(locale, point.cycles)} · </>
                              )}
                              {copy.probability}:{' '}
                              {probabilityText(probability.lower, probability)}
                            </span>
                          )
                        })}
                      </button>
                    ))}
                  </MeasuredGraphBox>
                </foreignObject>
              )
            )
          })}
          {projection.bundles.map((b) => {
            const p = positions.get(b.id)!,
              multiple = b.nodes.length > 1
            const n = b.nodes.find((n) => n.id === selected) ?? b.nodes[0]!
            const badge = b.nodes.every((n) => n.progress.goal === 'MATCH')
              ? common.hit
              : b.nodes.some((n) => n.id === 0)
                ? guide.start
                : guide.result
            const edge = [...route].reverse().find((e) => e.target === n.id)
            return (
              <foreignObject
                key={b.id}
                data-bundle-id={b.id}
                data-state-ids={b.nodes.map((n) => n.id).join(',')}
                data-terminal={!projection.edges.some((e) => e.from === b.id)}
                x={p.x - nodeWidth / 2}
                y={p.y}
                width={nodeWidth}
                height={p.height + 2}
              >
                <MeasuredGraphBox onSize={(h) => updateSize(b.id, h)}>
                  {multiple ? (
                    <div
                      className={
                        'graph-item-state graph-state-bundle' +
                        (b.nodes.some((n) => n.id === selected)
                          ? ' is-selected'
                          : '')
                      }
                    >
                      <div className="graph-item-badges">
                        <span>{badge}</span>
                        <span>
                          {nav.candidates} ({b.nodes.length})
                        </span>
                      </div>
                      <p>{nav.common}</p>
                      <ItemCard
                        item={commonStateCard(
                          b.nodes.map((n) => cardFor(n.id)),
                        )}
                      />
                      <p>{graphStateMessages[locale].candidates}</p>
                      <details className="graph-candidates" open>
                        <summary>
                          {nav.variants} ({b.nodes.length})
                        </summary>
                        {b.nodes.map((candidate) => (
                          <section
                            key={candidate.id}
                            data-candidate-id={candidate.id}
                          >
                            <div className="graph-candidate-options">
                              {cardFor(candidate.id).modifiers.map((line) => (
                                <div key={line.id}>
                                  {line.affixLabel && (
                                    <span>{line.affixLabel} </span>
                                  )}
                                  {line.text}
                                </div>
                              ))}
                            </div>
                            <button
                              aria-pressed={candidate.id === selected}
                              onClick={() => select(candidate.id)}
                            >
                              {graphStateMessages[locale].select}
                            </button>
                            {(candidateArrivals.get(candidate.id) ?? []).map(
                              ({ method, outcome: o }) => (
                                <button
                                  key={method.id + o.kind}
                                  onClick={() =>
                                    choose({
                                      method: method.id,
                                      source: method.source,
                                      target: candidate.id,
                                      kind: o.kind,
                                    })
                                  }
                                >
                                  {method.initial
                                    .concat(method.cycle)
                                    .map(action)
                                    .join(' → ')}{' '}
                                  · {nav.source} {method.source} ·{' '}
                                  {copy.probability}
                                  {(methodPoint(method, cycles)?.cycles ?? 1) >
                                    1 && (
                                    <>
                                      {' '}
                                      (
                                      {attemptLabel(
                                        locale,
                                        methodPoint(method, cycles)!.cycles,
                                      )}
                                      )
                                    </>
                                  )}
                                  :{' '}
                                  {probabilityText(
                                    o.probability.lower,
                                    o.probability,
                                  )}
                                </button>
                              ),
                            )}
                          </section>
                        ))}
                      </details>
                    </div>
                  ) : (
                    <GraphItemState
                      card={cardFor(n.id)}
                      previous={edge ? cardFor(edge.source) : undefined}
                      badge={badge}
                      selected={n.id === selected}
                      select={() => select(n.id)}
                      onSize={() => {}}
                    />
                  )}
                </MeasuredGraphBox>
              </foreignObject>
            )
          })}
        </svg>
      </div>
      {chosen && (
        <section className="exalted-outcomes" aria-label={nav.variants}>
          <h3>{labelFor(chosen)}</h3>
          <p>{copy.detail}</p>
          {chosen.variants.map(({ method, outcomes }) => (
            <details key={method.id} open={chosen.variants.length === 1}>
              <summary>
                {nav.source} {method.source} · {labelFor(chosen)}
              </summary>
              <ItemCard item={cardFor(method.source)} />
              <ul>
                {outcomes.map((o) => (
                  <li key={`${o.target}:${o.kind}`}>
                    <button
                      onClick={() =>
                        choose({
                          method: method.id,
                          source: method.source,
                          target: o.target!,
                          kind: o.kind,
                        })
                      }
                    >
                      {guide.result} {o.target}
                    </button>{' '}
                    · {copy.probability}:{' '}
                    {probabilityText(o.probability.lower, o.probability)}
                    <ItemCard item={cardFor(o.target!)} />
                  </li>
                ))}
              </ul>
            </details>
          ))}
        </section>
      )}
    </div>
  )
}
