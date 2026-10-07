import { useCallback, useEffect, useRef, useState } from 'react'
import { useStore } from 'zustand'
import type { StoreApi } from 'zustand/vanilla'
import { addStat, changeGroupType } from './editor'
import { GoalFilterApiError } from './api'
import type { Editor } from './editor'
import { goalFilterMessages } from './i18n'
import type { GoalFilterLanguage } from './i18n'
import { groupTypes, weighted } from './types'
import type {
  Context,
  GoalFilter,
  GoalFilterAdapter,
  Range,
  Recommendation,
} from './types'
import { useGoalFilterCatalog, useGoalFilterValidation } from './useGoalFilter'
import './goal-filter.css'
function NumberField({
  fieldId,
  invalidLabel,
  onValidityChange,
  label,
  value,
  onChange,
}: {
  fieldId: string
  invalidLabel: string
  onValidityChange: (id: string, invalid: boolean) => void
  label: string
  value: number | null
  onChange: (value: number | null) => void
}) {
  const [draft, setDraft] = useState<string | null>(null)
  const invalid =
    draft !== null && draft !== '' && !Number.isFinite(Number(draft))
  useEffect(
    () => () => onValidityChange(fieldId, false),
    [fieldId, onValidityChange],
  )
  return (
    <label>
      {label}
      <input
        type="text"
        inputMode="decimal"
        value={draft ?? (value === null ? '' : String(value))}
        aria-invalid={invalid}
        onChange={(event) => {
          const text = event.target.value
          setDraft(text)
          onValidityChange(
            fieldId,
            text.trim() !== '' && !Number.isFinite(Number(text)),
          )
          if (text.trim() === '') onChange(null)
          else if (Number.isFinite(Number(text))) onChange(Number(text))
        }}
        onBlur={() => {
          if (!invalid) setDraft(null)
        }}
      />
      {invalid && <span role="alert">{invalidLabel}</span>}
    </label>
  )
}
export function GoalFilterPanel({
  adapter,
  context,
  editor,
  bases,
  language = 'ko',
  recommendation,
  recommendationGoal,
  onEvaluate,
  onInputEdit,
  evaluationPending = false,
  evaluationAvailable = true,
  presentStatIds = new Set<string>(),
}: {
  adapter: GoalFilterAdapter
  context: Context
  editor: StoreApi<Editor>
  bases: { id: string; label: string }[]
  language?: GoalFilterLanguage
  recommendation?: Recommendation | undefined
  recommendationGoal?: GoalFilter | undefined
  onEvaluate?: (goal: GoalFilter) => void
  onInputEdit?: () => void
  evaluationPending?: boolean
  presentStatIds?: ReadonlySet<string>
  evaluationAvailable?: boolean
}) {
  const t = goalFilterMessages[language]
  const { goal, edit, collapse, collapsedByGroupId } = useStore(editor)
  const catalog = useGoalFilterCatalog(adapter, {
    ...context,
    baseItemId: goal.general.baseItemId,
  })
  const [invalidFields, setInvalidFields] = useState<Record<string, boolean>>(
    {},
  )
  const onValidityChange = useCallback((id: string, invalid: boolean) => {
    setInvalidFields((fields) => {
      if (fields[id] === invalid) return fields
      return { ...fields, [id]: invalid }
    })
  }, [])
  const currentRecommendation =
    recommendationGoal &&
    JSON.stringify(recommendationGoal) === JSON.stringify(goal) &&
    recommendation?.catalogVersion === goal.catalogVersion
      ? recommendation
      : undefined
  const validInput = !Object.values(invalidFields).some(Boolean)
  const validation = useGoalFilterValidation(adapter, context, goal, validInput)
  const [search, setSearch] = useState('')
  const [category, setCategory] = useState('ALL')
  const [target, setTarget] = useState(goal.groups[0]?.id ?? '')
  const searchRef = useRef<HTMLInputElement>(null)
  const addGroupRef = useRef<HTMLButtonElement>(null)
  const targetId = goal.groups.some((g) => g.id === target)
    ? target
    : goal.groups[0]?.id
  function editRange(range: Range, bound: keyof Range, value: number | null) {
    range[bound] = value
  }
  const stats =
    catalog.data?.stats.filter(
      (s) =>
        (category === 'ALL' || s.kind === category) &&
        `${s.label} ${s.statId}`.toLowerCase().includes(search.toLowerCase()),
    ) ?? []
  return (
    <section
      className="goal-filter"
      aria-label={t.title}
      onChangeCapture={onInputEdit}
    >
      <header>
        <h2>{t.title}</h2>
        {adapter.mock && <p>{t.mock}</p>}
      </header>
      <div className="goal-filter-layout">
        <aside>
          <h3>{t.equipment}</h3>
          <label>
            {t.base}
            <select
              value={goal.general.baseItemId}
              onChange={(e) =>
                edit((g) => {
                  g.general.baseItemId = e.target.value
                })
              }
            >
              {!bases.some((b) => b.id === goal.general.baseItemId) && (
                <option value={goal.general.baseItemId}>
                  {goal.general.baseItemId}
                </option>
              )}
              {bases.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.label}
                </option>
              ))}
            </select>
          </label>
          <fieldset>
            <legend>{t.level}</legend>
            {(['min', 'max'] as const).map((bound) => (
              <NumberField
                fieldId={`general:${bound}`}
                invalidLabel={t.invalid}
                onValidityChange={onValidityChange}
                key={bound}
                label={t[bound]}
                value={goal.general.itemLevel[bound]}
                onChange={(n) =>
                  edit((g) => editRange(g.general.itemLevel, bound, n))
                }
              />
            ))}
          </fieldset>
          <fieldset>
            <legend>{t.rarity}</legend>
            {['NORMAL', 'MAGIC', 'RARE'].map((rarity) => (
              <label key={rarity}>
                <input
                  type="checkbox"
                  checked={goal.general.rarities.includes(rarity)}
                  onChange={(e) =>
                    edit((g) => {
                      g.general.rarities = e.target.checked
                        ? [...g.general.rarities, rarity]
                        : g.general.rarities.filter((r) => r !== rarity)
                    })
                  }
                />
                {rarity}
              </label>
            ))}
          </fieldset>
        </aside>
        <div className="goal-filter-stats">
          <label>
            {t.search}
            <input
              ref={searchRef}
              type="search"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </label>
          <label>
            {t.category}
            <select
              value={category}
              onChange={(e) => setCategory(e.target.value)}
            >
              {['ALL', 'EXPLICIT', 'IMPLICIT', 'PSEUDO'].map((kind) => (
                <option key={kind} value={kind}>
                  {kind === 'ALL' ? t.all : kind}
                </option>
              ))}
            </select>
          </label>
          <label>
            {t.group}
            <select
              value={targetId ?? ''}
              onChange={(e) => setTarget(e.target.value)}
            >
              {goal.groups.map((g, i) => (
                <option key={g.id} value={g.id}>
                  {i + 1}: {g.type}
                </option>
              ))}
            </select>
          </label>
          {catalog.isPending && <p role="status">{t.loading}</p>}
          {catalog.isError && <p role="alert">{t.error}</p>}
          <ul className="goal-filter-candidates">
            {stats.map((stat) => (
              <li key={stat.statId}>
                <span>
                  {stat.label} ({stat.unit}){' '}
                  {stat.kind === 'PSEUDO' && <strong>{t.pseudo}</strong>}
                </span>
                <small>
                  {stat.kind} · {stat.support.evaluation} ·{' '}
                  {stat.eligibilityReason}
                </small>
                {presentStatIds.has(stat.statId) && (
                  <span>
                    {language === 'ko'
                      ? '현재 아이템에 존재'
                      : 'Present on current item'}
                  </span>
                )}
                <button
                  type="button"
                  disabled={
                    (!stat.eligible && !presentStatIds.has(stat.statId)) ||
                    !targetId ||
                    goal.groups
                      .find((g) => g.id === targetId)
                      ?.entries.some((e) => e.statId === stat.statId)
                  }
                  onClick={() => {
                    edit((g) => {
                      const group = g.groups.find(
                        (item) => item.id === targetId,
                      )
                      if (group)
                        addStat(group, stat, presentStatIds.has(stat.statId))
                    })
                    searchRef.current?.focus()
                  }}
                >
                  {t.add} {stat.label}
                </button>
              </li>
            ))}
          </ul>
          {catalog.data && !stats.length && <p>{t.empty}</p>}
          {goal.groups.map((group, index) => (
            <fieldset key={group.id} className="goal-filter-group">
              <legend>
                {t.group} {index + 1}
              </legend>
              <div className="goal-filter-group-controls">
                <label>
                  {t.group}
                  <select
                    value={group.type}
                    onChange={(e) =>
                      edit((g) =>
                        changeGroupType(
                          g.groups[index]!,
                          e.target.value as typeof group.type,
                        ),
                      )
                    }
                  >
                    {groupTypes.map((type) => (
                      <option key={type}>{type}</option>
                    ))}
                  </select>
                </label>
                <label>
                  <input
                    type="checkbox"
                    checked={!group.disabled}
                    onChange={(e) =>
                      edit((g) => {
                        g.groups[index]!.disabled = !e.target.checked
                      })
                    }
                  />
                  {t.active}
                </label>
                <button
                  type="button"
                  aria-expanded={!collapsedByGroupId[group.id]}
                  aria-controls={`goal-group-${group.id}`}
                  onClick={() => collapse(group.id)}
                >
                  {collapsedByGroupId[group.id] ? t.expand : t.collapse}
                </button>
                <button
                  type="button"
                  onClick={() => {
                    edit((g) => {
                      g.groups.splice(index, 1)
                    })
                    addGroupRef.current?.focus()
                  }}
                >
                  {t.remove} {t.group} {index + 1}
                </button>
              </div>
              <div
                id={`goal-group-${group.id}`}
                hidden={collapsedByGroupId[group.id]}
              >
                <p>{t.operations[group.type]}</p>
                {group.range && (
                  <fieldset>
                    <legend>
                      {group.type === 'COUNT' ? t.count : t.score}
                    </legend>
                    {(['min', 'max'] as const).map((bound) => (
                      <NumberField
                        fieldId={`${group.id}:${bound}`}
                        invalidLabel={t.invalid}
                        onValidityChange={onValidityChange}
                        key={bound}
                        label={t[bound]}
                        value={group.range![bound]}
                        onChange={(n) =>
                          edit((g) =>
                            editRange(g.groups[index]!.range!, bound, n),
                          )
                        }
                      />
                    ))}
                  </fieldset>
                )}
                {group.entries.map((entry, row) => {
                  const stat = catalog.data?.stats.find(
                    (s) => s.statId === entry.statId,
                  )
                  return (
                    <fieldset key={entry.id} className="goal-filter-row">
                      <legend>
                        {stat?.label ?? entry.statId} ({entry.unit}){' '}
                        {stat?.kind === 'PSEUDO' ? t.pseudo : ''}
                      </legend>
                      {catalog.data && (!stat || !stat.eligible) && (
                        <p role="status">
                          {stat?.eligibilityReason ?? 'UNKNOWN_STAT'}
                        </p>
                      )}
                      {(['min', 'max'] as const).map((bound) => (
                        <NumberField
                          fieldId={`${entry.id}:${bound}`}
                          invalidLabel={t.invalid}
                          onValidityChange={onValidityChange}
                          key={bound}
                          label={t[bound]}
                          value={entry.range[bound]}
                          onChange={(n) =>
                            edit((g) =>
                              editRange(
                                g.groups[index]!.entries[row]!.range,
                                bound,
                                n,
                              ),
                            )
                          }
                        />
                      ))}
                      {weighted(group.type) && (
                        <NumberField
                          fieldId={`${entry.id}:weight`}
                          invalidLabel={t.invalid}
                          onValidityChange={onValidityChange}
                          label={t.weight}
                          value={entry.weight}
                          onChange={(n) =>
                            edit((g) => {
                              g.groups[index]!.entries[row]!.weight = n
                            })
                          }
                        />
                      )}
                      <label>
                        <input
                          type="checkbox"
                          checked={!entry.disabled}
                          onChange={(e) =>
                            edit((g) => {
                              g.groups[index]!.entries[row]!.disabled =
                                !e.target.checked
                            })
                          }
                        />
                        {t.active}
                      </label>
                      <button
                        type="button"
                        onClick={() => {
                          edit((g) => {
                            g.groups[index]!.entries.splice(row, 1)
                          })
                          searchRef.current?.focus()
                        }}
                      >
                        {t.remove} {stat?.label ?? entry.statId}
                      </button>
                    </fieldset>
                  )
                })}
              </div>
            </fieldset>
          ))}
          {!goal.groups.length && <p>{t.noGroups}</p>}
          <button
            ref={addGroupRef}
            type="button"
            onClick={() => {
              const id = crypto.randomUUID()
              edit((g) => {
                g.groups.push({
                  id,
                  type: 'AND',
                  disabled: false,
                  range: null,
                  entries: [],
                })
              })
              setTarget(id)
            }}
          >
            {t.addGroup}
          </button>
        </div>
      </div>
      {onEvaluate && (
        <button
          type="button"
          disabled={
            !evaluationAvailable ||
            evaluationPending ||
            !validInput ||
            !validation.data?.valid ||
            catalog.data?.catalogVersion !== goal.catalogVersion
          }
          onClick={() => onEvaluate(structuredClone(goal))}
        >
          {language === 'ko'
            ? evaluationPending
              ? '판정 중'
              : '현재 아이템 수치 판정'
            : evaluationPending
              ? 'Evaluating'
              : 'Evaluate current item'}
        </button>
      )}
      <div aria-live="polite">
        {!validInput && <p role="alert">{t.invalid}</p>}
        {catalog.data &&
          catalog.data.catalogVersion !== goal.catalogVersion && (
            <p>{t.stale}</p>
          )}
        {[
          ...(catalog.data?.issues ?? []),
          ...(validInput
            ? (validation.data?.issues ??
              (validation.error instanceof GoalFilterApiError
                ? validation.error.issues
                : []))
            : []),
        ].map((issue, i) => (
          <p key={`${issue.path}-${i}`}>
            {issue.code}: {issue.message} ({issue.path})
          </p>
        ))}
        {validation.isError && <p>{t.error}</p>}
      </div>
      <p role="status">
        {validInput &&
        (currentRecommendation?.probability.status === 'COMPLETE' ||
          currentRecommendation?.probability.status === 'PARTIAL')
          ? `${currentRecommendation.probability.status} · ${currentRecommendation.probability.reasonCode ?? ''}`
          : currentRecommendation?.probability.status === 'UNKNOWN'
            ? t.unknownProbability
            : t.probability}
        {currentRecommendation?.probability.reasonCode && (
          <span> · {currentRecommendation.probability.reasonCode}</span>
        )}
      </p>
      {validInput &&
        currentRecommendation &&
        (currentRecommendation.probability.status === 'COMPLETE' ||
          currentRecommendation.probability.status === 'PARTIAL') && (
          <div>
            <p>
              Declared model probability; the game roll distribution has not
              been verified.
            </p>
            <p>
              Model: {currentRecommendation.probability.modelVersion}; ledger:{' '}
              {currentRecommendation.probability.ledgerVersion}
            </p>
            <p>
              {currentRecommendation.rankingCertified
                ? 'Ranking certified within this model and sequence scope.'
                : 'Ranking unresolved: budgets may leave better sequences unexplored.'}
            </p>
            <p>
              Compared sequences: {currentRecommendation.comparedSequences} /{' '}
              {currentRecommendation.totalSequences}
            </p>
            <ul>
              {currentRecommendation.comparisons.map((comparison, i) => (
                <li key={i}>
                  {comparison.sequence.length
                    ? comparison.sequence.join(' / ')
                    : 'Stop'}
                  : success {(comparison.successLower * 100).toFixed(4)}% to{' '}
                  {(comparison.successUpper * 100).toFixed(4)}%; failure{' '}
                  {(comparison.failureProbability * 100).toFixed(4)}%;
                  unresolved{' '}
                  {(comparison.unresolvedProbability * 100).toFixed(4)}% (
                  {comparison.complete ? 'complete' : 'partial'})
                </li>
              ))}
            </ul>
          </div>
        )}
    </section>
  )
}
