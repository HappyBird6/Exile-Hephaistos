import { methodSearchMessages } from './messages'
import { guideMessages } from './guideMessages'
import { methodPoint } from './methodObservation'
import { useState } from 'react'
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
}: {
  view: AutomaticView
  cycles: number
  selected: number
  route: Selection[]
  choose: (s: Selection) => void
  select: (id: number) => void
  action: (a: AutomaticAction) => string
  presentItem: PathTreeProps['presentItem']
}) {
  const { locale } = useI18n(),
    guide = guideMessages[locale],
    copy = outcomeMessages[locale],
    common = pathTreeMessages[locale]
  const [opened, setOpened] = useState<string | null>(null),
    [expanded, setExpanded] = useState<Set<string>>(new Set())
  const allGroups = exaltedGroups(view, cycles)
  const effective = expanded
  const projection = graphProjection(view, cycles, effective, route)
  const group = allGroups.find((g) => g.id === opened)
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
      label: `${copy.group} (${g.point.outcomes.length})`,
      state: null,
    })),
  ]
  const rows = new Map<number, number>(),
    positions = new Map<string, { x: number; y: number }>()
  for (const v of vertices) {
    const row = rows.get(v.depth) ?? 0
    rows.set(v.depth, row + 1)
    positions.set(v.id, { x: 110 + row * 240, y: 35 + v.depth * 180 })
  }
  const width = Math.max(320, ...[...positions.values()].map((p) => p.x + 115)),
    height = Math.max(100, ...[...positions.values()].map((p) => p.y + 40))
  const activate = (id: string, state: number | null) => {
    if (state === null) setOpened(id)
    else select(state)
  }
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
    <div className="method-map__overview">
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
        {projection.edges.map((e, i) => {
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
          return (
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
              d={`M${source.x},${source.y + (e.from.startsWith('group:') ? 18 : 30)} Q${(source.x + target.x) / 2 + 12 + (i % 7) * 5},${(source.y + target.y) / 2} ${target.x},${target.y - (e.to.startsWith('group:') ? 18 : 30)}`}
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
                    : copy.group
                })()}
              </title>
            </path>
          )
        })}
        {vertices.map((v) => {
          const p = positions.get(v.id)!
          return (
            <g
              key={v.id}
              role="button"
              tabIndex={0}
              aria-label={v.label}
              onClick={() => activate(v.id, v.state)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault()
                  activate(v.id, v.state)
                }
              }}
            >
              {v.state === null ? (
                <rect
                  x={p.x - 100}
                  y={p.y - 18}
                  width={200}
                  height={36}
                  rx={7}
                  fill="#343024"
                  stroke="currentColor"
                />
              ) : (
                <rect
                  x={p.x - 90}
                  y={p.y - 30}
                  width={180}
                  height={60}
                  rx={10}
                  fill={v.state === selected ? '#c69b57' : '#242424'}
                  stroke="currentColor"
                />
              )}
              <text x={p.x} y={p.y + 5} textAnchor="middle" fill="white">
                {v.label}
              </text>
            </g>
          )
        })}
      </svg>
      {group && (
        <section className="exalted-outcomes" aria-label={copy.group}>
          <h3>
            {copy.group}:
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
                      item={presentRollCell(
                        view.nodes.find((n) => n.id === o.target)!.item,
                        view.nodes.find((n) => n.id === o.target)!.rolls,
                        locale,
                        presentItem,
                      )}
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
