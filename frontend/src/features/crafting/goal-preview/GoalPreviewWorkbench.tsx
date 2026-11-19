import { useQuery } from '@tanstack/react-query'
import { useCallback, useState } from 'react'
import { useStore } from 'zustand'
import { createStore } from 'zustand/vanilla'
import { useI18n, uiText } from '../../../shared/i18n/i18n'
import { baseSlugs, type WorkbenchBaseKey } from '../baseRegistry'
import type { Initial } from '../craftingApi'
import { httpGoalFilterAdapter } from '../goal-filter/api'
import { craftStartMessages } from '../goal-filter/craftStartMessages'
import { createGoalFilterEditor, emptyGoal } from '../goal-filter/editor'
import { GoalFilterPanel } from '../goal-filter/GoalFilterPanel'
import { goalFilterMessages } from '../goal-filter/i18n'
import { levelMessages } from '../goal-filter/levelMessages'
import {
  startInitialQuery,
  startInventoryQuery,
} from '../goal-filter/startCatalog'
import { startInputMessages } from '../goal-filter/startInputMessages'
import type { StartBase } from '../goal-filter/startItem'
import type { Catalog, GoalFilter } from '../goal-filter/types'
import {
  hasCompleteGoal,
  useGoalFilterCatalog,
} from '../goal-filter/useGoalFilter'
import { GoalPreviewResults } from './GoalPreviewResults'
import { previewGoalHints } from './previewTabNames'
import './previewInputs.css'

export function GoalPreviewWorkbench({ active }: { active: boolean }) {
  const { locale, name } = useI18n()
  const copy = craftStartMessages[locale]
  const [selection] = useState(() =>
    createStore<{ base: WorkbenchBaseKey }>(() => ({ base: 'solar' })),
  )
  const selected = useStore(selection, (draft) => draft.base)
  const inventory = useQuery({ ...startInventoryQuery, enabled: active })
  const bases = inventory.data?.bases ?? []
  const base = bases.find(([key]) => key === selected) ?? bases[0]
  const initial = useQuery({
    ...startInitialQuery(
      base?.[0] ?? 'solar',
      inventory.data?.generation ?? '',
    ),
    enabled: active && !!base,
  })
  const failed = inventory.isError
    ? inventory
    : initial.isError
      ? initial
      : null
  return (
    <div className="goal-preview-workbench">
      <label className="goal-preview-field">
        {copy.base}
        <select
          value={base?.[0] ?? ''}
          disabled={!base}
          onChange={(event) => {
            const next = bases.find(([key]) => key === event.target.value)
            if (next) selection.setState({ base: next[0] })
          }}
        >
          {[...new Set(bases.map((entry) => entry[1]))].map((type) => (
            <optgroup
              key={type}
              label={uiText(type === 'Amulets' ? 'Amulet' : type, locale)}
            >
              {bases
                .filter((entry) => entry[1] === type)
                .map(([key, , label]) => (
                  <option key={key} value={key}>
                    {name(baseSlugs[key], label)}
                  </option>
                ))}
            </optgroup>
          ))}
        </select>
      </label>
      {failed ? (
        <p role="alert">
          {copy.loadError}{' '}
          <button onClick={() => void failed.refetch()}>{copy.retry}</button>
        </p>
      ) : base && initial.data ? (
        <BaseGoals
          key={
            initial.data.state.baseItemId + ':' + initial.data.rulesetIdentity
          }
          active={active}
          base={base}
          initial={initial.data}
        />
      ) : (
        <p role="status">
          {inventory.isSuccess && !base ? copy.unverified : copy.loading}
        </p>
      )}
    </div>
  )
}

type BaseProps = { active: boolean; base: StartBase; initial: Initial }
function BaseGoals(props: BaseProps) {
  const { locale } = useI18n()
  const { snapshotId, baseItemId, itemLevel } = props.initial.state
  const catalog = useGoalFilterCatalog(httpGoalFilterAdapter, {
    snapshotId,
    baseItemId,
    itemLevel,
  })
  if (!catalog.data && catalog.isError)
    return (
      <p role="alert">
        {goalFilterMessages[locale].error}{' '}
        <button onClick={() => void catalog.refetch()}>
          {craftStartMessages[locale].retry}
        </button>
      </p>
    )
  return catalog.data ? (
    <PreviewEditor {...props} initialCatalog={catalog.data} />
  ) : (
    <p role="status">{goalFilterMessages[locale].loading}</p>
  )
}

function PreviewEditor({
  active,
  base,
  initial,
  initialCatalog,
}: BaseProps & { initialCatalog: Catalog }) {
  const { locale } = useI18n()
  const [editor] = useState(() =>
    createGoalFilterEditor(
      emptyGoal(initialCatalog.context, initialCatalog.catalogVersion),
    ),
  )
  const goal = useStore(editor, (state) => state.goal)
  const [levelStore] = useState(() =>
    createStore(() => ({
      text: String(initial.state.itemLevel),
      value: initial.state.itemLevel,
    })),
  )
  const level = useStore(levelStore)
  const levelValid = level.text !== '' && Number(level.text) === level.value
  const context = { ...initialCatalog.context, itemLevel: level.value }
  const catalog = useGoalFilterCatalog(httpGoalFilterAdapter, context)
  const [ready, setReady] = useState<{
    goal: GoalFilter
    level: number
  } | null>(null)
  const validationChanged = useCallback(
    (valid: boolean) => {
      setReady(valid ? { goal, level: level.value } : null)
    },
    [goal, level.value],
  )
  return (
    <>
      <label className="goal-preview-field">
        {levelMessages[locale].label}
        <input
          type="number"
          min={1}
          max={100}
          step={1}
          required
          value={level.text}
          aria-invalid={!levelValid}
          onChange={(event) => {
            const text = event.target.value
            const value = event.target.valueAsNumber
            levelStore.setState({
              text,
              ...(Number.isInteger(value) && value >= 1 && value <= 100
                ? { value }
                : {}),
            })
          }}
        />
      </label>
      {!levelValid && (
        <p role="alert">{startInputMessages[locale].range} 1–100</p>
      )}
      <GoalFilterPanel
        adapter={httpGoalFilterAdapter}
        context={context}
        editor={editor}
        bases={[]}
        language={locale}
        allowQualityWork={['Amulets', 'Rings', 'Belts'].includes(base[1])}
        compact
        preview
        onSearchReadinessChange={validationChanged}
      />
      {catalog.isError && (
        <p role="alert">
          {goalFilterMessages[locale].error}{' '}
          <button onClick={() => void catalog.refetch()}>
            {craftStartMessages[locale].retry}
          </button>
        </p>
      )}
      {!hasCompleteGoal(goal) && <p>{previewGoalHints[locale]}</p>}
      {active &&
        levelValid &&
        ready?.goal === goal &&
        ready.level === level.value &&
        catalog.isSuccess &&
        !catalog.isFetching &&
        catalog.data.catalogVersion === goal.catalogVersion && (
          <GoalPreviewResults
            context={context}
            goal={goal}
            catalog={catalog.data}
            base={base}
            initial={initial}
          />
        )}
    </>
  )
}
