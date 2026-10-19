import { GraphItemState } from './GraphItemState'
import { graphStateMessages } from './graphStateMessages'
import type { Scenario } from './scenarioCode'
import { operationKind } from './operationGrouping'
import { operationMessages } from './operationMessages'
import { methodSearchMessages } from './messages'
import { guideMessages } from './guideMessages'
import { methodPoint } from './methodObservation'
import { useLayoutEffect, useRef, useState } from 'react'
import type { AutomaticAction, AutomaticView } from './automaticApi'
import type { Selection } from './supportStorage'
import type { PathTreeProps } from '../path-tree/PathTree'
import { graphProjection, groupMass, exaltedGroups } from './outcomeGrouping'
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
    common = pathTreeMessages[locale]
  const [opened, setOpened] = useState<string | null>(null),
    [expanded, setExpanded] = useState<Set<string>>(new Set())
  const host = useRef<HTMLDivElement>(null)
  const [viewportWidth, setViewportWidth] = useState(340)
  useLayoutEffect(() => {
    const element = host.current
    if (!element) return
    const measure = () => setViewportWidth(element.clientWidth || 340)
    measure()
    if (typeof ResizeObserver === 'undefined') return
    const observer = new ResizeObserver(measure)
    observer.observe(element)
    return () => observer.disconnect()
  }, [])
  const nodeWidth = Math.min(320, Math.max(240, viewportWidth - 20))
  const [measured, setMeasured] = useState<Record<string, number>>({})
  const cardFor = (id: number) => {
    const n = view.nodes.find((node) => node.id === id)!
    return (
      frozenCards?.[String(id)]?.[locale] ??
      presentRollCell(n.item, n.rolls, locale, presentItem)
    )
  }
  const allGroups = exaltedGroups(view, cycles)
  const effective = expanded
  const projection = graphProjection(view, cycles, effective, route)
  const group = allGroups.find((g) => g.id === opened)
  const groupTitle = group
    ? operationMessages[locale][
        operationKind(group.method) === 'add' ? 'add' : 'replace'
      ]
    : guide.details
  const vertices = [
    ...projection.nodes.map((n) => ({
      id: `state:${n.id}`,
      depth: n.depth,
      label:
        n.id === 0
          ? guide.start
          : n.progress.goal === 'MATCH'
            ? common.hit
            : guide.result,
      state: n.id,
    })),
    ...projection.groups.map((g) => ({
      id: g.id,
      depth:
        (view.nodes.find((n) => n.id === g.method.source)?.depth ?? 0) + 0.5,
      label: `${operationMessages[locale][operationKind(g.method) === 'add' ? 'add' : 'replace']} (${g.point.outcomes.length})`,
      state: null,
    })),
  ]
  const positions = new Map<string, { x: number; y: number; height: number }>()
  let levelY = 20
  for (const depth of [...new Set(vertices.map((v) => v.depth))].sort(
    (a, b) => a - b,
  )) {
    const level = vertices.filter((v) => v.depth === depth)
    let maxHeight = 0
    level.forEach((v, index) => {
      const height = v.state === null ? 180 : (measured[v.id] ?? 420)
      maxHeight = Math.max(maxHeight, height)
      positions.set(v.id, {
        x: nodeWidth / 2 + 10 + index * (nodeWidth + 30),
        y: levelY,
        height,
      })
    })
    levelY += maxHeight + 160
  }
  const width = Math.max(
      nodeWidth + 20,
      ...[...positions.values()].map((p) => p.x + nodeWidth / 2 + 10),
    ),
    height = Math.max(100, levelY)
  const actionLabels = new Set<string>()
  const labelRows = new Map<string, number>()
  const kindLabel = (kind: string) =>
    kind === 'GOAL'
      ? common.hit
      : kind === 'LOSS'
        ? methodSearchMessages[locale].loss
        : kind === 'UNRESOLVED'
          ? common.unknown
          : kind === 'DEAD'
            ? common.unavailable
            : guide.result
  return (
    <div ref={host} className="method-map__overview">
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
        {projection.edges.map((e) => {
          const source = positions.get(e.from),
            target = positions.get(e.to)
          if (!source || !target) return null
          const click = () =>
            e.target === null
              ? setOpened(e.to)
              : choose({
                  method: e.method.id,
                  source: e.method.source,
                  target: e.target,
                  kind: e.kind,
                })
          const actionKey = e.from + e.to + operationKind(e.method)
          const showAction = !actionLabels.has(actionKey)
          actionLabels.add(actionKey)
          const pair = e.from + e.to
          const labelRow = labelRows.get(pair) ?? 0
          if (showAction) labelRows.set(pair, labelRow + 1)
          return (
            <g key={`${e.method.id}:${e.to}:${e.kind}`}>
              <path
                key={`${e.method.id}:${e.to}:${e.kind}`}
                role="button"
                tabIndex={0}
                aria-label={e.method.initial
                  .concat(e.method.cycle)
                  .map(action)
                  .join(' → ')}
                onClick={click}
                onKeyDown={(event) => {
                  if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault()
                    click()
                  }
                }}
                className={
                  route.some(
                    (r) => r.method === e.method.id && r.target === e.target,
                  )
                    ? 'automatic-edge is-selected'
                    : 'automatic-edge'
                }
                d={`M${source.x},${source.y + source.height} Q${(source.x + target.x) / 2 + 12},${(source.y + source.height + target.y) / 2} ${target.x},${target.y}`}
                fill="none"
                stroke="currentColor"
                strokeDasharray={e.kind === 'UNRESOLVED' ? '5 4' : undefined}
                markerEnd={`url(#automatic-arrow-${view.id})`}
              >
                <title>
                  {e.method.initial
                    .concat(e.method.cycle)
                    .map(action)
                    .join(' → ')}
                  :{' '}
                  {(() => {
                    const o = methodPoint(e.method, cycles)?.outcomes.find(
                      (o) => o.target === e.target && o.kind === e.kind,
                    )
                    return o
                      ? probabilityText(o.probability.lower, o.probability)
                      : guide.details
                  })()}
                </title>
              </path>
              {showAction && (
                <foreignObject
                  x={(source.x + target.x) / 2 - 85}
                  y={source.y + source.height + 12 + labelRow * 42}
                  width={170}
                  height={38}
                >
                  <div className="graph-action-label">
                    {operationKind(e.method) === 'other'
                      ? e.method.cycle.map(action).join(' → ')
                      : operationMessages[locale][
                          operationKind(e.method) as
                            'add' | 'replace' | 'prepare'
                        ]}
                  </div>
                </foreignObject>
              )}
            </g>
          )
        })}
        {vertices.map((v) => {
          const position = positions.get(v.id)!
          if (v.state === null)
            return (
              <foreignObject
                key={v.id}
                x={position.x - nodeWidth / 2}
                y={position.y}
                width={nodeWidth}
                height={position.height}
              >
                <div className="graph-operation-node">
                  <span>{graphStateMessages[locale].step}</span>
                  <button aria-label={v.label} onClick={() => setOpened(v.id)}>
                    {v.label}
                  </button>
                  <p>{graphStateMessages[locale].candidates}</p>
                </div>
              </foreignObject>
            )
          const selectedEdge = [...route]
            .reverse()
            .find((e) => e.target === v.state)
          const parents = [
            ...new Set(
              projection.edges
                .filter((e) => e.target === v.state)
                .map((e) => e.method.source),
            ),
          ]
          const parent =
            selectedEdge?.source ??
            (parents.length === 1 ? parents[0] : undefined)
          return (
            <foreignObject
              key={v.id}
              data-state-id={v.state}
              x={position.x - nodeWidth / 2}
              y={position.y}
              width={nodeWidth}
              height={position.height + 2}
            >
              <GraphItemState
                card={cardFor(v.state)}
                previous={parent === undefined ? undefined : cardFor(parent)}
                badge={v.label}
                selected={v.state === selected}
                select={() => select(v.state!)}
                onSize={(height) =>
                  setMeasured((previous) =>
                    previous[v.id] === height
                      ? previous
                      : { ...previous, [v.id]: height },
                  )
                }
              />
            </foreignObject>
          )
        })}
      </svg>
      {group && (
        <section className="exalted-outcomes" aria-label={groupTitle}>
          <h3>
            {groupTitle}:
            {group.method.initial
              .concat(group.method.cycle)
              .map(action)
              .join(' → ')}
          </h3>
          <p>{copy.detail}</p>
          <ul>
            {groupMass(group).map((o) => (
              <li key={o.kind}>
                {kindLabel(o.kind)}:{' '}
                {probabilityText(o.probability.lower, o.probability)}
              </li>
            ))}
          </ul>
          <button
            onClick={() => {
              setExpanded((previous) => {
                const next = new Set(previous)
                if (effective.has(group.id)) next.delete(group.id)
                else next.add(group.id)
                return next
              })
            }}
          >
            {effective.has(group.id) ? copy.collapse : copy.expand}
          </button>
          <ul>
            {group.point.outcomes.map((o, index) => (
              <li key={`${o.target}:${o.kind}`}>
                {o.target !== null ? (
                  <button
                    onClick={() =>
                      choose({
                        method: group.method.id,
                        source: group.method.source,
                        target: o.target!,
                        kind: o.kind,
                      })
                    }
                  >
                    {guide.result} {index + 1}
                  </button>
                ) : (
                  common.unknown
                )}{' '}
                · {kindLabel(o.kind)} · {copy.probability}:{' '}
                {probabilityText(o.probability.lower, o.probability)}
                {o.target !== null && (
                  <details>
                    <summary>
                      {guide.result} {index + 1}
                    </summary>
                    <ItemCard
                      item={
                        frozenCards?.[String(o.target)]?.[locale] ??
                        presentRollCell(
                          view.nodes.find((n) => n.id === o.target)!.item,
                          view.nodes.find((n) => n.id === o.target)!.rolls,
                          locale,
                          presentItem,
                        )
                      }
                    />
                  </details>
                )}
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  )
}
