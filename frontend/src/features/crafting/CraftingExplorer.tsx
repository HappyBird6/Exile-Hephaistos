import { ServiceMessage } from './ServiceMessage'
import {
  useI18n,
  formatPercent,
  formatNumber,
  formatDate,
  localizedSource,
} from '../../shared/i18n/i18n'
import { useRef, useState } from 'react'
import { localizedAction } from './localizedCrafting'
import { CurrencyImage } from './CurrencyImage'
import { currencies } from './currencies'
import { localizedModifierText } from './localizedModifiers'
import { groupOutcomes, modifierSummary } from './outcomeGroups'
import { skipToken, useQuery, useQueryClient } from '@tanstack/react-query'
import { ItemCard } from './ItemCard'
import type { ItemCardData } from './itemCardData'
import {
  actions,
  currencyActions,
  explore,
  loadActions,
  loadInitial,
  loadTransition,
} from './craftingApi'
import type { Action, Bucket, Definition } from './craftingApi'
import './crafting-explorer.css'

const percent = (value: number) =>
  value > 0 && value * 100 < 0.000001
    ? `${formatNumber(value * 100, { notation: 'scientific', minimumFractionDigits: 3, maximumFractionDigits: 3 }).replace(/E(?=-?\d)/, 'e')}%`
    : formatPercent(value)
function describe(state: Bucket, definitions: Record<string, Definition>) {
  return state.modifierIds.length
    ? state.modifierIds
        .map((id) => {
          const d = definitions[id]
          return d
            ? `${d.affixType === 'PREFIX' ? 'P' : 'S'} T${d.tier} ${localizedModifierText(d)}`
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
  const { t } = useI18n()
  const client = useQueryClient()
  const currentHeading = useRef<HTMLDivElement>(null)
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
    history: {
      id: string
      state: Bucket
      chance: number
      action: Action | null
    }[]
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
      ? ({ signal }) =>
          loadActions(position.state, signal, initial.data!.rulesetIdentity)
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
        ? ({ signal }) =>
            loadTransition(
              position.state,
              selected,
              signal,
              initial.data!.rulesetIdentity,
            )
        : skipToken,
    staleTime: 60_000,
    retry: false,
  })
  const exploration = useQuery({
    queryKey: ['crafting', 'explore', position?.id, search],
    queryFn:
      position && search && search.stateId === position.id
        ? ({ signal }) =>
            explore(
              position.state,
              search.plan,
              signal,
              initial.data!.rulesetIdentity,
            )
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
            text: definitions[id]
              ? localizedModifierText(definitions[id]!)
              : id,
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
        {
          id: position.id,
          state: position.state,
          chance: probability,
          action: selected,
        },
      ],
    })
    setSelection({ count: requestCount, action: null })
    setVisible(20)
    setSearch(null)
    requestAnimationFrame(() => currentHeading.current?.focus())
  }
  function returnTo(index: number) {
    const previous = history[index]
    if (!previous) return
    client.setQueryData(currentKey, {
      id: previous.id,
      state: previous.state,
      history: history.slice(0, index),
    })
    setSelection({ count: requestCount, action: null })
    setSearch(null)
    setVisible(20)
    requestAnimationFrame(() => currentHeading.current?.focus())
  }
  function back() {
    returnTo(history.length - 1)
  }
  const groups = groupOutcomes(transitions.data?.outcomes ?? [], definitions)
  const error =
    initial.error ?? available.error ?? transitions.error ?? exploration.error
  return (
    <section
      className="craft-explorer"
      aria-label={t('ui.crafting_probability_explorer')}
    >
      <ol className="selected-path" aria-label={t('ui.selected_crafting_path')}>
        {history.map((entry, index) => (
          <li key={`${entry.id}-${index}`}>
            <span className="path-step">{index + 1}</span>
            <div>
              <strong>
                {entry.state.rarity} {t('ui.item_level_fragment')}{' '}
                {entry.state.itemLevel}
              </strong>
              <p>{describe(entry.state, definitions)}</p>
              <button type="button" onClick={() => returnTo(index)}>
                {t('explorer.return_step', { index: index + 1 })}
              </button>
              <p className="path-action">
                ↓{' '}
                {entry.action
                  ? localizedAction(entry.action)
                  : t('ui.selected_outcome')}{' '}
                {t('ui.conditional_probability')} {percent(entry.chance)}
              </p>
            </div>
          </li>
        ))}
      </ol>
      <div className="explorer-current">
        <div
          className="current-state-heading"
          ref={currentHeading}
          tabIndex={-1}
        >
          <span className="path-step">{history.length + 1}</span>
          <h2>{t('ui.current_state')}</h2>
          <span className="current-marker">{t('ui.you_are_here')}</span>
        </div>
        <div className="current-overview">
          <ItemCard baseItemId="Solar_Amulet" item={card} />
          <div className="current-context">
            <p>{t('notice.predicted_ranges')}</p>
            <p>
              {history.length} {t('ui.path_probability')}{' '}
              {percent(history.reduce((p, h) => p * h.chance, 1))}
            </p>
            <button type="button" onClick={back} disabled={!history.length}>
              {t('ui.previous_state')}
            </button>
            {initial.data && (
              <p className="probability-source">
                {' '}
                {t('ui.weights_calculation')}{' '}
                <a
                  href={localizedSource(
                    'https://poe2db.tw/us/Amulets#ModifiersCalc',
                  )}
                  target="_blank"
                  rel="noreferrer"
                >
                  {t('ui.poe2db_modifier_weights')}
                </a>
                .<br /> {t('ui.data_colon')}{' '}
                {formatDate(initial.data.metadata.retrievedAt)}
              </p>
            )}
          </div>
        </div>
      </div>
      <div className="explorer-results">
        <h2>{t('ui.next_possible_states')}</h2>
        <p>
          {t(
            'ui.choose_a_currency_then_a_possible_outcome_to_explore_its_future',
          )}
        </p>
        {initial.isPending && <p>{t('ui.loading_modifier_data')}</p>}
        {error && (
          <p role="alert">
            <ServiceMessage text={error.message} />{' '}
            <button
              type="button"
              onClick={() => {
                void initial.refetch()
                if (position) void available.refetch()
                if (selected) void transitions.refetch()
                if (search) void exploration.refetch()
              }}
            >
              {t('ui.retry')}
            </button>
          </p>
        )}
        {available.isFetching && (
          <p role="status">{t('ui.loading_available_currencies')}</p>
        )}
        <div className="explorer-actions" aria-busy={available.isFetching}>
          {(available.data ?? []).map((a) => (
            <div key={a.action} className="explorer-action">
              <button
                type="button"
                disabled={!a.available}
                aria-pressed={selected === a.action}
                aria-label={t('explorer.preview', {
                  name: localizedAction(a.action),
                })}
                aria-describedby={
                  !a.available ? `reason-${a.action}` : undefined
                }
                onClick={() => {
                  setSelection({ count: requestCount, action: a.action })
                  setVisible(20)
                }}
              >
                <CurrencyImage
                  image={
                    currencies.find(
                      (currency) => currencyActions[currency.id] === a.action,
                    )!.image
                  }
                  name={localizedAction(a.action)}
                />
                <span>{localizedAction(a.action)}</span>
              </button>
              {!a.available && (
                <small id={`reason-${a.action}`}>
                  <ServiceMessage text={a.reason} />
                </small>
              )}
            </div>
          ))}
        </div>
        {transitions.isFetching && (
          <p>
            {' '}
            {t('ui.calculating_outcomes')}{' '}
            <button
              type="button"
              onClick={() =>
                setSelection({ count: requestCount, action: null })
              }
            >
              {t('ui.cancel_preview')}
            </button>
          </p>
        )}
        {selected && transitions.data && (
          <div className="transition-outcomes">
            <h3>{localizedAction(selected)}</h3>
            {!transitions.data.available ? (
              <p>
                <ServiceMessage text={transitions.data.reason} />
              </p>
            ) : (
              <>
                <p>
                  {transitions.data.outcomes.length} {t('ui.total_probability')}{' '}
                  {percent(
                    transitions.data.outcomes.reduce(
                      (s, o) => s + o.probability,
                      0,
                    ),
                  )}
                </p>
                <ol>
                  {groups.slice(0, visible).map((group) => (
                    <li key={group.key}>
                      {group.outcomes.length > 1 ? (
                        <details className="outcome-group">
                          <summary>
                            <strong>{percent(group.probability)}</strong> ·{' '}
                            {group.outcomes[0]!.state.rarity} ·{' '}
                            {group.outcomes[0]!.state.modifierIds.map((id) =>
                              definitions[id]
                                ? modifierSummary(
                                    localizedModifierText(definitions[id]!),
                                    definitions[id]!.stats,
                                  )
                                : id,
                            ).join(' · ')}{' '}
                            <span>
                              {group.outcomes.length}{' '}
                              {t('ui.tier_combinations')}{' '}
                            </span>
                          </summary>
                          <p>{t('notice.conditional_probability')}</p>
                          <ol>
                            {group.outcomes
                              .slice()
                              .sort((a, b) =>
                                a.state.modifierIds
                                  .map((id) => definitions[id]?.tier ?? 0)
                                  .join(',')
                                  .localeCompare(
                                    b.state.modifierIds
                                      .map((id) => definitions[id]?.tier ?? 0)
                                      .join(','),
                                    'en-US',
                                    { numeric: true },
                                  ),
                              )
                              .map((o) => (
                                <li key={o.id}>
                                  <strong>{percent(o.probability)}</strong>
                                  <span>{describe(o.state, definitions)}</span>
                                  <button
                                    type="button"
                                    onClick={() =>
                                      choose(o.id, o.state, o.probability)
                                    }
                                  >
                                    {t('ui.explore_this_state')}
                                  </button>
                                </li>
                              ))}
                          </ol>
                        </details>
                      ) : (
                        group.outcomes.map((o) => (
                          <div key={o.id} className="single-outcome">
                            <strong>{percent(o.probability)}</strong>
                            <span>
                              {o.state.rarity} ·{' '}
                              {describe(o.state, definitions)}
                            </span>
                            <button
                              type="button"
                              onClick={() =>
                                choose(o.id, o.state, o.probability)
                              }
                            >
                              {t('ui.explore_this_state')}
                            </button>
                          </div>
                        ))
                      )}
                    </li>
                  ))}
                </ol>
                {visible < groups.length && (
                  <button
                    type="button"
                    onClick={() => setVisible((v) => v + 20)}
                  >
                    {' '}
                    {t('ui.more_outcomes')}
                    {groups.length - visible} {t('ui.groups_remaining')}{' '}
                  </button>
                )}
              </>
            )}
          </div>
        )}
        <details className="future-plan">
          <summary>{t('ui.explore_a_currency_sequence')}</summary>
          <p>{t('notice.explore_three')}</p>
          <div className="plan-inputs">
            {plan.map((a, i) => (
              <label key={i}>
                {' '}
                {t('ui.step')} {i + 1}
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
                  <option value="">{t('ui.stop_here')}</option>
                  {actions.map((v) => (
                    <option key={v} value={v}>
                      {localizedAction(v)}
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
            {t('ui.explore_sequence')}
          </button>
          {exploration.isFetching && (
            <p>
              {' '}
              {t('ui.exploring')}{' '}
              <button type="button" onClick={() => setSearch(null)}>
                {t('ui.cancel_exploration')}
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
              {!exploration.data.complete && <p>{t('notice.try_fewer')}</p>}
              <dl>
                <dt>{t('ui.reached_the_end')}</dt>
                <dd>{percent(exploration.data.completedProbability)}</dd>
                <dt>{t('ui.stopped_by_unavailable_currency')}</dt>
                <dd>{percent(exploration.data.blockedProbability)}</dd>
                <dt>{t('ui.unexplored')}</dt>
                <dd>{percent(exploration.data.unexploredProbability)}</dd>
              </dl>
              <p>
                {Object.keys(exploration.data.nodes).length}{' '}
                {t('ui.shared_states')} {exploration.data.edges.length}{' '}
                {t('ui.transitions')}{' '}
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
                <p>{t('notice.first_twenty')}</p>
              )}
            </div>
          )}
        </details>
      </div>
    </section>
  )
}
