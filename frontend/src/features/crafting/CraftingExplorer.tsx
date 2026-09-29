import { useState } from 'react'
import { skipToken, useQuery, useQueryClient } from '@tanstack/react-query'
import { ItemCard } from './ItemCard'
import type { ItemCardData } from './itemCardData'
import {
  actions,
  actionNames,
  explore,
  loadActions,
  loadInitial,
  loadTransition,
} from './craftingApi'
import type { Action, Bucket, Definition } from './craftingApi'
import './crafting-explorer.css'

const percent = (value: number) =>
  value > 0 && value * 100 < 0.000001
    ? `${(value * 100).toExponential(3)}%`
    : `${(value * 100).toLocaleString('en-US', { maximumFractionDigits: 6 })}%`
function describe(state: Bucket, definitions: Record<string, Definition>) {
  return state.modifierIds.length
    ? state.modifierIds
        .map((id) => {
          const d = definitions[id]
          return d
            ? `${d.affixType === 'PREFIX' ? 'P' : 'S'} T${d.tier} ${d.text}`
            : id
        })
        .join(' · ')
    : 'No explicit modifiers'
}
const fallback: ItemCardData = {
  rarity: 'NORMAL',
  name: 'Solar Amulet',
  base: 'Solar Amulet',
  itemClass: 'Amulet',
  itemLevel: 82,
  properties: [],
  requirements: [],
  modifiers: [{ id: 'spirit', text: '+15 to Spirit', kind: 'implicit' }],
  flags: [],
}

export function CraftingExplorer({
  level,
  revision,
  requestCount,
  requestedAction,
}: {
  level: number
  revision: number
  requestCount: number
  requestedAction: Action | null
}) {
  const client = useQueryClient()
  const initial = useQuery({
    queryKey: ['crafting', 'initial', level],
    queryFn: ({ signal }) => loadInitial(level, signal),
    staleTime: 60_000,
    retry: false,
  })
  const currentKey = ['crafting', 'current', revision] as const
  const current = useQuery<{
    id: string
    state: Bucket
    history: { id: string; state: Bucket; chance: number }[]
  }>({
    queryKey: currentKey,
    queryFn: skipToken,
    gcTime: 60_000,
  })
  const position =
    current.data ??
    (initial.data
      ? { id: initial.data.id, state: initial.data.state }
      : undefined)
  const history = current.data?.history ?? []
  const [selection, setSelection] = useState<{
    count: number
    action: Action | null
  }>({ count: 0, action: null })
  const selected =
    selection.count === requestCount ? selection.action : requestedAction
  const [visible, setVisible] = useState(20)
  const [plan, setPlan] = useState<(Action | '')[]>([
    'TRANSMUTATION',
    'AUGMENTATION',
    'REGAL',
  ])
  const [search, setSearch] = useState<{
    id: number
    plan: Action[]
    stateId: string
  } | null>(null)
  const available = useQuery({
    queryKey: ['crafting', 'actions', position?.id],
    queryFn: position
      ? ({ signal }) => loadActions(position.state, signal)
      : skipToken,
    initialData:
      position?.id === initial.data?.id ? initial.data?.actions : undefined,
    staleTime: 60_000,
    retry: false,
  })
  const transitions = useQuery({
    queryKey: ['crafting', 'transitions', position?.id, selected],
    queryFn:
      position && selected
        ? ({ signal }) => loadTransition(position.state, selected, signal)
        : skipToken,
    staleTime: 60_000,
    retry: false,
  })
  const exploration = useQuery({
    queryKey: ['crafting', 'explore', position?.id, search],
    queryFn:
      position && search && search.stateId === position.id
        ? ({ signal }) => explore(position.state, search.plan, signal)
        : skipToken,
    staleTime: Infinity,
    gcTime: 0,
    retry: false,
  })
  const definitions = initial.data?.modifiers ?? {}
  const card: ItemCardData = position
    ? {
        ...fallback,
        rarity: position.state.rarity,
        itemLevel: position.state.itemLevel,
        modifiers: [
          {
            id: 'spirit',
            text: `+${position.state.implicits[0]?.values.base_spirit_from_equipment ?? 15} to Spirit`,
            kind: 'implicit',
          },
          ...position.state.modifierIds.map((id) => ({
            id,
            text: definitions[id]?.text ?? id,
            kind: 'explicit' as const,
            affixLabel: `${definitions[id]?.affixType === 'PREFIX' ? 'P' : 'S'}${definitions[id]?.tier ?? '?'}`,
          })),
        ],
      }
    : { ...fallback, itemLevel: level }

  function choose(id: string, state: Bucket, probability: number) {
    if (!position) return
    client.setQueryData(currentKey, {
      id,
      state,
      history: [
        ...history,
        { id: position.id, state: position.state, chance: probability },
      ],
    })
    setSelection({ count: requestCount, action: null })
    setVisible(20)
    setSearch(null)
  }
  function back() {
    const previous = history.at(-1)
    if (previous) {
      client.setQueryData(currentKey, {
        id: previous.id,
        state: previous.state,
        history: history.slice(0, -1),
      })
      setSelection({ count: requestCount, action: null })
      setSearch(null)
      setVisible(20)
    }
  }
  const error =
    initial.error ?? available.error ?? transitions.error ?? exploration.error
  return (
    <section
      className="craft-explorer"
      aria-label="Crafting probability explorer"
    >
      <div className="explorer-current">
        <ItemCard item={card} />
        <p>
          Modifier ranges are shown for predicted states; numeric values have
          not been rolled.
        </p>
        <p>
          {history.length} steps · Selected path probability:{' '}
          {percent(history.reduce((p, h) => p * h.chance, 1))}
        </p>
        <button type="button" onClick={back} disabled={!history.length}>
          Previous state
        </button>
        {initial.data && (
          <p className="probability-source">
            Probabilities are calculated using{' '}
            <a
              href="https://poe2db.tw/us/Amulets#ModifiersCalc"
              target="_blank"
              rel="noreferrer"
            >
              PoE2DB modifier weights
            </a>
            .<br />
            Data: {initial.data.metadata.retrievedAt.slice(0, 10)}
          </p>
        )}
      </div>
      <div className="explorer-results">
        <h2>Next possible states</h2>
        <p>Choose a currency, then a possible outcome to explore its future.</p>
        {initial.isPending && <p>Loading modifier data…</p>}
        {error && (
          <p role="alert">
            {error.message}{' '}
            <button
              type="button"
              onClick={() => {
                void initial.refetch()
                if (position) void available.refetch()
                if (selected) void transitions.refetch()
                if (search) void exploration.refetch()
              }}
            >
              Retry
            </button>
          </p>
        )}
        <div className="explorer-actions">
          {(available.data ?? []).map((a) => (
            <div key={a.action}>
              <button
                type="button"
                disabled={!a.available}
                aria-pressed={selected === a.action}
                onClick={() => {
                  setSelection({ count: requestCount, action: a.action })
                  setVisible(20)
                }}
              >
                Preview {actionNames[a.action]}
              </button>
              {!a.available && <small>{a.reason}</small>}
            </div>
          ))}
        </div>
        {transitions.isFetching && (
          <p>
            Calculating outcomes…{' '}
            <button
              type="button"
              onClick={() =>
                setSelection({ count: requestCount, action: null })
              }
            >
              Cancel preview
            </button>
          </p>
        )}
        {selected && transitions.data && (
          <div className="transition-outcomes">
            <h3>{actionNames[selected]}</h3>
            {!transitions.data.available ? (
              <p>{transitions.data.reason}</p>
            ) : (
              <>
                <p>
                  {transitions.data.outcomes.length} distinct states · Total
                  probability:{' '}
                  {percent(
                    transitions.data.outcomes.reduce(
                      (s, o) => s + o.probability,
                      0,
                    ),
                  )}
                </p>
                <ol>
                  {transitions.data.outcomes.slice(0, visible).map((o) => (
                    <li key={o.id}>
                      <strong>{percent(o.probability)}</strong>{' '}
                      <span>
                        {o.state.rarity} · {describe(o.state, definitions)}
                      </span>
                      <button
                        type="button"
                        onClick={() => choose(o.id, o.state, o.probability)}
                      >
                        Explore this state
                      </button>
                    </li>
                  ))}
                </ol>
                {visible < transitions.data.outcomes.length && (
                  <button
                    type="button"
                    onClick={() => setVisible((v) => v + 20)}
                  >
                    Show more outcomes (
                    {transitions.data.outcomes.length - visible} remaining)
                  </button>
                )}
              </>
            )}
          </div>
        )}
        <details className="future-plan">
          <summary>Explore a currency sequence</summary>
          <p>
            Evaluate up to three steps from the current state. Unavailable steps
            stop that branch.
          </p>
          <div className="plan-inputs">
            {plan.map((a, i) => (
              <label key={i}>
                Step {i + 1}
                <select
                  value={a}
                  onChange={(e) => {
                    setPlan((old) =>
                      old.map((v, n) =>
                        n === i ? (e.target.value as Action | '') : v,
                      ),
                    )
                    setSearch(null)
                  }}
                >
                  <option value="">Stop here</option>
                  {actions.map((v) => (
                    <option key={v} value={v}>
                      {actionNames[v]}
                    </option>
                  ))}
                </select>
              </label>
            ))}
          </div>
          <button
            type="button"
            disabled={!position || !plan[0] || exploration.isFetching}
            onClick={() => {
              if (!position) return
              const end = plan.indexOf('')
              const sequence = (end < 0 ? plan : plan.slice(0, end)) as Action[]
              setSearch({
                id: (search?.id ?? 0) + 1,
                plan: sequence,
                stateId: position.id,
              })
            }}
          >
            Explore sequence
          </button>
          {exploration.isFetching && (
            <p>
              Exploring…{' '}
              <button type="button" onClick={() => setSearch(null)}>
                Cancel exploration
              </button>
            </p>
          )}
          {search && exploration.data && (
            <div className="exploration-summary">
              <p>
                {exploration.data.complete
                  ? 'Exploration complete.'
                  : 'Exploration limited; some probability remains unexplored.'}
              </p>
              {!exploration.data.complete && (
                <p>
                  Try fewer steps, or select an outcome before exploring again.
                </p>
              )}
              <dl>
                <dt>Reached the end</dt>
                <dd>{percent(exploration.data.completedProbability)}</dd>
                <dt>Stopped by unavailable currency</dt>
                <dd>{percent(exploration.data.blockedProbability)}</dd>
                <dt>Unexplored</dt>
                <dd>{percent(exploration.data.unexploredProbability)}</dd>
              </dl>
              <p>
                {Object.keys(exploration.data.nodes).length} shared states ·{' '}
                {exploration.data.edges.length} transitions
              </p>
              <ol>
                {exploration.data.terminals.slice(0, 20).map((t, i) => (
                  <li key={`${t.id}-${t.step}-${i}`}>
                    <strong>{percent(t.probability)}</strong> {t.status} ·{' '}
                    {describe(exploration.data.nodes[t.id]!, definitions)}
                  </li>
                ))}
              </ol>
              {exploration.data.terminals.length > 20 && (
                <p>
                  Showing the first 20 endpoints. The totals include every
                  calculated endpoint.
                </p>
              )}
            </div>
          )}
        </details>
      </div>
    </section>
  )
}
