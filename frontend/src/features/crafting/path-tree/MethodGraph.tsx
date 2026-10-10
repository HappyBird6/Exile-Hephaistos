import { useId, useLayoutEffect, useRef } from 'react'
import { useI18n } from '../../../shared/i18n/i18n'
import { ItemCard } from '../ItemCard'
import { CurrencyImage } from '../CurrencyImage'
import { currencies } from '../currencies'
import { localizedAction } from '../localizedCrafting'
import { workbenchCurrencyActions } from '../workbenchApi'
import type { WorkbenchAction } from '../workbenchApi'
import type { ItemPresentation } from './presentation'
import type { GraphPage, Recommendation } from './types'
import { methodMessages } from './methodMessages'
import { pathTreeMessages, numbered } from './messages'
import { percent } from './validation'

export function MethodGraph({
  graph,
  recommendation,
  attempts,
  recommended,
  recovery,
  presentItem,
}: {
  graph: GraphPage
  recommendation: Recommendation
  attempts: string
  recommended: boolean
  recovery: boolean
  presentItem: ItemPresentation
}) {
  const { locale } = useI18n()
  const copy = methodMessages[locale],
    common = pathTreeMessages[locale]
  const id = useId()
  const viewport = useRef<HTMLDivElement>(null)
  const method = recommendation.method!
  const nodes = new Map(graph.nodes.map((n) => [n.id, n]))
  const root = nodes.get(method.fromStateId)!
  const point = recommendation.points.find((p) => p.attempts === attempts)!
  const omitted = method.omitted.find((p) => p.attempts === attempts)!
  const observed = method.exits
    .map((exit) => ({
      ...exit,
      probability: exit.points.find((p) => p.attempts === attempts)!
        .probability,
    }))
    .filter((exit) => BigInt(exit.probability.numerator) > 0n)
  const returned = observed.find((exit) => exit.stateId === root.id)
  const exits = observed.filter((exit) => exit.stateId !== root.id)
  const width = Math.max(680, exits.length * 324)
  useLayoutEffect(() => {
    const element = viewport.current
    if (element)
      element.scrollLeft = Math.max(0, (width - element.clientWidth) / 2)
  }, [width, recommendation.policy.id])
  const methods = recommendation.policy.actions.map((action) => {
    const currency = currencies.find(
      (c) => workbenchCurrencyActions[c.id] === action,
    )
    return {
      action,
      currency,
      name: localizedAction(action as WorkbenchAction),
    }
  })
  return (
    <section
      className={`method-flow ${recommended ? 'method-flow--recommended' : ''} ${recovery ? 'method-flow--recovery' : ''}`}
    >
      <p>{copy.single}</p>
      <p>
        {copy.until} ·{' '}
        {numbered(
          recovery ? common.conditionalWithin : common.within,
          attempts,
        )}
      </p>
      <div
        className="method-flow__viewport"
        ref={viewport}
        tabIndex={0}
        role="region"
        aria-label={copy.outcomes}
      >
        <div className="method-flow__canvas" style={{ width }}>
          <section
            className="method-flow__root path-tree__node"
            aria-label={copy.start}
          >
            <h3>{copy.start}</h3>
            <ItemCard
              item={presentItem(root.item, locale)}
              baseItemId={root.item.baseItemId}
            />
            {returned && (
              <div className="method-flow__return">
                <svg viewBox="0 0 280 65" aria-hidden="true">
                  <defs>
                    <marker
                      id={`${id}-return`}
                      markerWidth="10"
                      markerHeight="10"
                      refX="8"
                      refY="5"
                      orient="auto"
                    >
                      <path d="M0 0 L10 5 L0 10 Z" />
                    </marker>
                  </defs>
                  <path
                    data-method-edge="ACTIVE"
                    d="M 40 0 V 42 H 240 V 4"
                    markerEnd={`url(#${id}-return)`}
                  />
                </svg>
                <span className="method-flow__actions">
                  {methods.map(({ action, currency, name }) => (
                    <span key={action}>
                      {currency && (
                        <CurrencyImage image={currency.image} name={name} />
                      )}
                      {name}
                    </span>
                  ))}
                </span>
                <p>
                  ↻ {common.repeat} · {copy.active} ·{' '}
                  {percent(returned.probability)}
                </p>
              </div>
            )}
          </section>
          <div className="method-flow__connections" style={{ height: 240 }}>
            <svg
              viewBox={`0 0 ${width} 240`}
              aria-hidden="true"
              className="method-flow__arrows"
            >
              <defs>
                <marker
                  id={`${id}-arrow`}
                  markerWidth="10"
                  markerHeight="10"
                  refX="8"
                  refY="5"
                  orient="auto"
                >
                  <path d="M0 0 L10 5 L0 10 Z" />
                </marker>
              </defs>
              {exits.map((exit, i) => {
                const x = (width * (i + 0.5)) / exits.length
                return (
                  <path
                    key={`${exit.stateId}:${exit.kind}`}
                    data-method-edge={exit.kind}
                    d={`M ${width / 2} 0 V 30 H ${x} V 235`}
                    markerEnd={`url(#${id}-arrow)`}
                  />
                )
              })}
            </svg>
            {exits.map((exit, i) => (
              <a
                key={`${exit.stateId}:${exit.kind}`}
                className="method-flow__edge-label"
                style={{ left: `${(100 * (i + 0.5)) / exits.length}%` }}
                href={`#${id}-exit-${i}`}
                onClick={(event) => {
                  event.preventDefault()
                  document.getElementById(`${id}-exit-${i}`)?.focus()
                }}
              >
                <span className="method-flow__actions">
                  {methods.map(({ action, currency, name }) => (
                    <span key={action}>
                      {currency && (
                        <CurrencyImage image={currency.image} name={name} />
                      )}
                      {name}
                    </span>
                  ))}
                </span>
                <span>↻ {common.repeat}</span>
                <strong>
                  {exit.kind === 'HIT' && point.status !== 'COMPLETE'
                    ? '≥ '
                    : ''}
                  {percent(exit.probability)}
                </strong>
                <span>{exit.kind === 'HIT' ? copy.stopped : copy.active}</span>
              </a>
            ))}
          </div>
          <h3 className="method-flow__depth">{copy.depth}</h3>
          <div
            className="method-flow__exits"
            style={{
              gridTemplateColumns: `repeat(${Math.max(1, exits.length)}, 1fr)`,
            }}
          >
            {exits.map((exit, i) => {
              const node = nodes.get(exit.stateId)!
              return (
                <section
                  key={`${exit.stateId}:${exit.kind}`}
                  id={`${id}-exit-${i}`}
                  tabIndex={-1}
                  className="path-tree__node"
                  aria-label={numbered(common.state, i + 1)}
                >
                  <h4>{exit.kind === 'HIT' ? common.hit : common.noMatch}</h4>
                  <ItemCard
                    item={presentItem(node.item, locale)}
                    baseItemId={node.item.baseItemId}
                  />
                </section>
              )
            })}
          </div>
        </div>
      </div>
      <dl className="method-flow__mass">
        <div>
          <dt>
            {copy.omitted} · {common.hit}
          </dt>
          <dd>{percent(omitted.hit)}</dd>
        </div>
        <div>
          <dt>
            {copy.omitted} · {common.noMatch}
          </dt>
          <dd>{percent(omitted.active)}</dd>
        </div>
        <div>
          <dt>{copy.dead}</dt>
          <dd>{percent(point.dead)}</dd>
        </div>
        <div>
          <dt>{copy.unknown}</dt>
          <dd>{percent(point.unresolved)}</dd>
        </div>
      </dl>
    </section>
  )
}
