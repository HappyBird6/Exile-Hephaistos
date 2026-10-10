import { useCallback, useEffect, useRef, useState } from 'react'
import { useStore } from 'zustand'
import type { StoreApi } from 'zustand/vanilla'
import { addStat } from './editor'
import { presentedGroupType, orHelp } from './groupPresentation'
import type { GroupPresentation } from './groupPresentation'
import { GoalFilterApiError } from './api'
import type { Editor } from './editor'
import { goalFilterMessages, goalFilterActions } from './i18n'
import type { GoalFilterLanguage } from './i18n'
import { groupTypes, weighted } from './types'
import { setupMessages } from './setupMessages'
import { startInputMessages } from './startInputMessages'
import { PickerPopover } from './PickerPopover'
import { PickerSearchInput } from './PickerSearchInput'
import { craftStartMessages } from './craftStartMessages'
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
    <label className="goal-filter-number">
      <span className="goal-filter-sr-only">{label}</span>
      <input
        type="text"
        inputMode="decimal"
        placeholder={label}
        title={label}
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
  compact = false,
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
  compact?: boolean
}) {
  const t = goalFilterMessages[language]
  const actions = goalFilterActions[language]
  const setup = setupMessages[language]
  const unitLabel = (unit: string) =>
    unit === 'percent'
      ? '%'
      : unit === 'flat'
        ? startInputMessages[language].value
        : unit
  const {
    goal,
    edit,
    collapse,
    collapsedByGroupId,
    presentedTypes,
    chooseGroupType,
  } = useStore(editor)
  const groupPresentation = (group: GoalFilter['groups'][number]) =>
    presentedGroupType(group, presentedTypes[group.id])
  const groupHelp = (group: GoalFilter['groups'][number]) =>
    groupPresentation(group) === 'OR'
      ? orHelp[language]
      : t.operations[group.type]
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
  const [searchOpen, setSearchOpen] = useState(false)
  const [activeStat, setActiveStat] = useState(0)
  const [generalCollapsed, setGeneralCollapsed] = useState(false)
  const [category, setCategory] = useState('ALL')
  const [target, setTarget] = useState(goal.groups[0]?.id ?? '')
  const searchRef = useRef<HTMLInputElement>(null)
  const candidatesRef = useRef<HTMLUListElement>(null)
  const addGroupRef = useRef<HTMLButtonElement>(null)
  const targetId = goal.groups.some((g) => g.id === target)
    ? target
    : goal.groups[0]?.id
  useEffect(() => {
    if (searchOpen) searchRef.current?.focus()
  }, [targetId, searchOpen])
  function editRange(range: Range, bound: keyof Range, value: number | null) {
    range[bound] = value
  }
  const stats =
    catalog.data?.stats.filter(
      (s) =>
        (category === 'ALL' || s.kind === category) &&
        `${s.label} ${s.statId}`
          .normalize('NFC')
          .toLocaleLowerCase(language)
          .includes(search.normalize('NFC').toLocaleLowerCase(language).trim()),
    ) ?? []
  const activeIndex = Math.min(activeStat, Math.max(0, stats.length - 1))
  const canAdd = (stat: (typeof stats)[number]) =>
    (stat.eligible || presentStatIds.has(stat.statId)) &&
    !!targetId &&
    !goal.groups
      .find((g) => g.id === targetId)
      ?.entries.some((e) => e.statId === stat.statId)
  function chooseStat(index: number) {
    const stat = stats[index]
    if (!stat || !canAdd(stat)) return
    edit((g) => {
      const group = g.groups.find((item) => item.id === targetId)
      if (group) addStat(group, stat, presentStatIds.has(stat.statId))
    })
    setSearch('')
    searchRef.current?.focus()
    setSearchOpen(false)
  }
  useEffect(() => {
    if (searchOpen)
      candidatesRef.current?.children[activeIndex]?.scrollIntoView?.({
        block: 'nearest',
      })
  }, [activeIndex, searchOpen])
  const searchPanel = (
    <div
      className="goal-filter-search"
      onBlur={(event) => {
        if (!event.currentTarget.contains(event.relatedTarget))
          setSearchOpen(false)
      }}
    >
      <PickerSearchInput
        label={craftStartMessages[language].add}
        aria-label={t.search}
        inputRef={searchRef}
        type="search"
        role="combobox"
        autoComplete="off"
        aria-autocomplete="list"
        aria-activedescendant={
          searchOpen && stats.length ? `goal-stat-${activeIndex}` : undefined
        }
        placeholder={`+ ${craftStartMessages[language].add}`}
        aria-expanded={searchOpen}
        aria-controls="goal-stat-candidates"
        value={search}
        onFocus={() => setSearchOpen(true)}
        onClick={() => setSearchOpen(true)}
        onKeyDown={(event) => {
          if (event.nativeEvent.isComposing || event.keyCode === 229) return
          if (event.key === 'Escape') {
            event.preventDefault()
            event.stopPropagation()
            setSearchOpen(false)
            setSearch('')
          }
          if (event.key === 'Enter') {
            event.preventDefault()
            if (searchOpen) chooseStat(activeIndex)
            else setSearchOpen(true)
          }
          if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
            event.preventDefault()
            setSearchOpen(true)
            setActiveStat(
              searchOpen
                ? (activeIndex +
                    (event.key === 'ArrowDown' ? 1 : -1) +
                    stats.length) %
                    Math.max(1, stats.length)
                : event.key === 'ArrowUp'
                  ? Math.max(0, stats.length - 1)
                  : 0,
            )
          }
        }}
        onChange={(e) => {
          setSearch(e.target.value)
          setSearchOpen(true)
          setActiveStat(0)
        }}
      />
      {searchOpen && (
        <PickerPopover anchor={searchRef}>
          <div className="goal-filter-search-options" hidden={!searchOpen}>
            <label>
              {t.category}
              <select
                value={category}
                onChange={(e) => {
                  setCategory(e.target.value)
                  setActiveStat(0)
                }}
              >
                {['ALL', 'EXPLICIT', 'IMPLICIT', 'PSEUDO'].map((kind) => (
                  <option key={kind} value={kind}>
                    {kind === 'ALL'
                      ? t.all
                      : kind === 'EXPLICIT'
                        ? setup.explicit
                        : kind === 'IMPLICIT'
                          ? setup.implicit
                          : setup.pseudo}
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
                    {i + 1}: {groupPresentation(g)}
                  </option>
                ))}
              </select>
            </label>
          </div>
          {catalog.isPending && <p role="status">{t.loading}</p>}
          {catalog.isError && <p role="alert">{t.error}</p>}
          <ul
            ref={candidatesRef}
            id="goal-stat-candidates"
            className="goal-filter-candidates"
            role="listbox"
            aria-label={t.search}
            hidden={!searchOpen}
            onKeyDown={(event) => {
              if (event.key === 'Escape') {
                searchRef.current?.focus()
                setSearch('')
                setSearchOpen(false)
              }
            }}
          >
            {(searchOpen ? stats : []).map((stat, index) => (
              <li
                key={stat.statId}
                id={`goal-stat-${index}`}
                role="option"
                aria-selected={index === activeIndex}
                aria-disabled={!canAdd(stat)}
                onMouseDown={(event) => event.preventDefault()}
                onClick={() => chooseStat(index)}
              >
                <span className="goal-filter-candidate-label">
                  {stat.label} ({unitLabel(stat.unit)}){' '}
                  <small>
                    {stat.kind === 'PSEUDO'
                      ? setup.pseudo
                      : stat.kind === 'IMPLICIT'
                        ? setup.implicit
                        : setup.explicit}
                  </small>
                </span>
                {!compact && (
                  <small>
                    {stat.kind} · {stat.support.evaluation} ·{' '}
                    {stat.eligibilityReason}
                  </small>
                )}
                {compact && stat.support.evaluation !== 'SUPPORTED' && (
                  <small>{actions.unavailable}</small>
                )}
                {presentStatIds.has(stat.statId) && (
                  <span>{actions.present}</span>
                )}
                <button
                  type="button"
                  aria-label={`${t.add} ${stat.label}`}
                  disabled={!canAdd(stat)}
                  onMouseDown={(event) => event.preventDefault()}
                  onClick={(event) => {
                    event.stopPropagation()
                    chooseStat(index)
                  }}
                >
                  <span aria-hidden="true">+</span>
                </button>
              </li>
            ))}
          </ul>
          {catalog.data && !stats.length && <p>{t.empty}</p>}
        </PickerPopover>
      )}
    </div>
  )
  return (
    <section
      className={`goal-filter${compact ? ' goal-filter-compact' : ''}`}
      aria-label={t.title}
      onChangeCapture={onInputEdit}
    >
      <header className="goal-filter-title">
        <h2>{t.title}</h2>
        {adapter.mock && <p>{t.mock}</p>}
      </header>
      <div className="goal-filter-layout">
        {!compact && (
          <aside className="goal-filter-general">
            <h3 className="goal-filter-section-heading">
              <button
                type="button"
                aria-expanded={!generalCollapsed}
                aria-controls="goal-general-filters"
                onClick={() => setGeneralCollapsed((value) => !value)}
              >
                <span aria-hidden="true">{generalCollapsed ? '▸' : '▾'}</span>{' '}
                {t.equipment}
              </button>
            </h3>
            <div id="goal-general-filters" hidden={generalCollapsed}>
              <label className="goal-filter-general-row">
                <span>{t.base}</span>
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
              <fieldset className="goal-filter-general-row goal-filter-range">
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
              <fieldset className="goal-filter-general-row goal-filter-rarity">
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
            </div>
          </aside>
        )}
        <div className="goal-filter-stats">
          {goal.groups.map((group, index) => (
            <fieldset
              key={group.id}
              className={`goal-filter-group${group.disabled ? ' goal-filter-disabled' : ''}`}
            >
              <legend className="goal-filter-sr-only">
                {t.group} {index + 1}
              </legend>
              <div className="goal-filter-group-controls">
                <label className="goal-filter-group-type">
                  <span className="goal-filter-group-title" aria-hidden="true">
                    {actions.stats}
                  </span>
                  <span className="goal-filter-sr-only">
                    {t.group} {index + 1}
                  </span>
                  <select
                    title={groupHelp(group)}
                    value={groupPresentation(group)}
                    onChange={(e) =>
                      chooseGroupType(
                        group.id,
                        e.target.value as GroupPresentation,
                      )
                    }
                  >
                    {(['AND', 'OR', 'COUNT', 'NOT'] as const).map((type) => (
                      <option key={type} value={type}>
                        {type === 'OR' ? 'OR' : setup.groups[type]}
                      </option>
                    ))}
                    <optgroup label={setup.advanced}>
                      {groupTypes
                        .filter(
                          (type) => !['AND', 'COUNT', 'NOT'].includes(type),
                        )
                        .map((type) => (
                          <option key={type} value={type}>
                            {setup.groups[type]}
                          </option>
                        ))}
                    </optgroup>
                  </select>
                </label>
                <label className="goal-filter-toggle" title={t.active}>
                  <input
                    type="checkbox"
                    checked={!group.disabled}
                    onChange={(e) =>
                      edit((g) => {
                        g.groups[index]!.disabled = !e.target.checked
                      })
                    }
                  />
                  <span className="goal-filter-sr-only">{t.active}</span>
                </label>
                <button
                  type="button"
                  className="goal-filter-icon"
                  aria-label={
                    collapsedByGroupId[group.id] ? t.expand : t.collapse
                  }
                  title={collapsedByGroupId[group.id] ? t.expand : t.collapse}
                  aria-expanded={!collapsedByGroupId[group.id]}
                  aria-controls={`goal-group-${group.id}`}
                  onClick={() => collapse(group.id)}
                >
                  <span aria-hidden="true">
                    {collapsedByGroupId[group.id] ? '▸' : '▾'}
                  </span>
                </button>
                <button
                  type="button"
                  className="goal-filter-icon"
                  aria-label={`${t.remove} ${t.group} ${index + 1}`}
                  title={`${t.remove} ${t.group} ${index + 1}`}
                  onClick={() => {
                    edit((g) => {
                      g.groups.splice(index, 1)
                    })
                    addGroupRef.current?.focus()
                    setSearchOpen(false)
                  }}
                >
                  <span aria-hidden="true">×</span>
                </button>
              </div>
              <div
                id={`goal-group-${group.id}`}
                hidden={collapsedByGroupId[group.id]}
              >
                <p className="goal-filter-group-help">{groupHelp(group)}</p>
                {group.range && groupPresentation(group) !== 'OR' && (
                  <fieldset className="goal-filter-group-range">
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
                    <div
                      key={entry.id}
                      role="group"
                      aria-label={stat?.label ?? entry.statId}
                      className={`goal-filter-row${entry.disabled ? ' goal-filter-disabled' : ''}`}
                    >
                      <div className="goal-filter-row-label">
                        {stat?.kind === 'PSEUDO' && (
                          <strong>{setup.pseudo} </strong>
                        )}
                        <span>
                          {stat?.label ??
                            (compact ? actions.unavailable : entry.statId)}{' '}
                          ({unitLabel(entry.unit)})
                        </span>
                      </div>
                      {catalog.data && (!stat || !stat.eligible) && (
                        <p className="goal-filter-row-warning" role="status">
                          {compact
                            ? actions.unavailable
                            : (stat?.eligibilityReason ?? 'UNKNOWN_STAT')}
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
                      <label className="goal-filter-toggle" title={t.active}>
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
                        <span className="goal-filter-sr-only">{t.active}</span>
                      </label>
                      <button
                        type="button"
                        className="goal-filter-icon"
                        aria-label={`${t.remove} ${stat?.label ?? entry.statId}`}
                        title={`${t.remove} ${stat?.label ?? entry.statId}`}
                        onClick={() => {
                          edit((g) => {
                            g.groups[index]!.entries.splice(row, 1)
                          })
                          searchRef.current?.focus()
                          setSearchOpen(false)
                        }}
                      >
                        <span aria-hidden="true">×</span>
                      </button>
                    </div>
                  )
                })}
              </div>
              {targetId === group.id ? (
                <div hidden={collapsedByGroupId[group.id]}>{searchPanel}</div>
              ) : (
                <button
                  type="button"
                  className="goal-filter-add-stat"
                  hidden={collapsedByGroupId[group.id]}
                  onClick={() => {
                    setTarget(group.id)
                    setSearch('')
                    searchRef.current?.focus()
                    setSearchOpen(true)
                  }}
                >
                  + {actions.add}
                </button>
              )}
            </fieldset>
          ))}
          {!goal.groups.length && <p>{t.noGroups}</p>}
          <button
            ref={addGroupRef}
            className="goal-filter-add-group"
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
            + {t.addGroup}
          </button>
        </div>
      </div>
      {onEvaluate && (
        <button
          type="button"
          className="goal-filter-evaluate"
          disabled={
            !evaluationAvailable ||
            evaluationPending ||
            !validInput ||
            !validation.data?.valid ||
            catalog.data?.catalogVersion !== goal.catalogVersion
          }
          onClick={() => onEvaluate(structuredClone(goal))}
        >
          {evaluationPending ? actions.evaluating : actions.evaluate}
        </button>
      )}
      <div aria-live="polite">
        {!validInput && <p role="alert">{t.invalid}</p>}
        {catalog.data &&
          catalog.data.catalogVersion !== goal.catalogVersion && (
            <p>{t.stale}</p>
          )}
        {(!compact || goal.groups.some((group) => group.entries.length > 0)) &&
          [
            ...(catalog.data?.issues ?? []),
            ...(validInput
              ? (validation.data?.issues ??
                (validation.error instanceof GoalFilterApiError
                  ? validation.error.issues
                  : []))
              : []),
          ].map((issue, i) => (
            <p key={`${issue.path}-${i}`}>
              {compact
                ? actions.unavailable
                : `${issue.code}: ${issue.message} (${issue.path})`}
            </p>
          ))}
        {validation.isError &&
          !(compact && validation.error instanceof GoalFilterApiError) && (
            <p>{t.error}</p>
          )}
      </div>
      {!compact && (
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
      )}
      {!compact &&
        validInput &&
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
