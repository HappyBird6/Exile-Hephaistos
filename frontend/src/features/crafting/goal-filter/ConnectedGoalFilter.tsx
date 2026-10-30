import { qualityWorkGoal } from './qualityWorkGoal'
import { goalSources } from '../method-search/graphDisplay'
import { resetMessages } from './resetMessages'
import {
  readSupport,
  saveSupport,
  validGoal,
} from '../method-search/supportStorage'
import { workspaceMessages } from '../method-search/workspaceMessages'
import { useQuery } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { useStore } from 'zustand'
import { concreteInitial } from '../workbenchApi'
import type { ConcreteItem } from '../workbenchApi'
import { loadInitial } from '../craftingApi'
import { httpGoalFilterAdapter } from './api'
import { presentNumericStats } from './numericApi'
import { useGoalFilterCatalog } from './useGoalFilter'
import { createGoalFilterEditor, emptyGoal } from './editor'
import { GoalFilterPanel } from './GoalFilterPanel'
import type { GoalFilterLanguage } from './i18n'
import type { Catalog, Context, GoalFilter } from './types'
import { startBases } from './startItem'
import { useI18n } from '../../../shared/i18n/i18n'
import { goalFilterMessages } from './i18n'

const adapter = httpGoalFilterAdapter
export type SearchGoalSnapshot = {
  sourceStatIds?: string[] | undefined
  goal: GoalFilter
  catalogVersion: string | null
  ready: boolean
}
export function ConnectedGoalFilter({
  item,
  rulesetIdentity,
  context,
  activeOmens,
  maxMillis,
  compact = false,
  allowQualityWork = true,
  language,
  onSearchGoalChange,
  onResetGoal,
}: {
  item: ConcreteItem | null
  rulesetIdentity?: string | undefined
  context: Context
  language: GoalFilterLanguage
  activeOmens: string[]
  maxMillis: number
  compact?: boolean
  allowQualityWork?: boolean
  onResetGoal?: (() => void) | undefined
  onSearchGoalChange?: (snapshot: SearchGoalSnapshot) => void
}) {
  const [baseKey, setBaseKey] = useState('support')
  const inventory = useQuery({
    enabled: !compact,
    queryKey: ['goalFilter', 'base-inventory', context.itemLevel],
    queryFn: async ({ signal }) => {
      const response = await fetch('/api/v1/crafting/workbench/registry', {
        signal,
      })
      if (!response.ok) throw new Error('Base inventory unavailable')
      const registry = (await response.json()) as {
        workbenchBases: Record<string, unknown>
        entries: { serviceScope?: string; supportedBases?: string[] }[]
      }
      return Promise.all(
        [
          ...new Set([
            ...Object.keys(registry.workbenchBases),
            ...registry.entries
              .filter((entry) => entry.serviceScope === 'ACTIVE')
              .flatMap((entry) => entry.supportedBases ?? []),
          ]),
        ].map(async (key) => ({
          key,
          initial: await loadInitial(
            context.itemLevel,
            signal,
            key as Parameters<typeof loadInitial>[2],
          ),
        })),
      )
    },
    retry: false,
  })
  const selected = inventory.data?.find((base) => base.key === baseKey)
  const actualItem =
    baseKey === 'support'
      ? item
      : selected
        ? concreteInitial(selected.initial)
        : null
  const actualContext = actualItem ?? context
  const catalog = useQuery({
    queryKey: [
      'goalFilter',
      adapter.id,
      'catalog',
      {
        snapshotId: actualContext.snapshotId,
        baseItemId: actualContext.baseItemId,
        itemLevel: actualContext.itemLevel,
      },
    ],
    queryFn: ({ signal }) =>
      adapter.catalog(
        {
          snapshotId: actualContext.snapshotId,
          baseItemId: actualContext.baseItemId,
          itemLevel: actualContext.itemLevel,
        },
        signal,
      ),
    retry: false,
  })
  const [initialCatalog, setInitialCatalog] = useState<Catalog | null>(null)
  if (!initialCatalog && catalog.data) setInitialCatalog(catalog.data)
  if (!initialCatalog)
    return (
      <p role={catalog.isError ? 'alert' : 'status'}>
        {catalog.isError
          ? goalFilterMessages[language].error
          : goalFilterMessages[language].loading}{' '}
        {catalog.isError && (
          <button onClick={() => void catalog.refetch()}>Retry</button>
        )}
      </p>
    )
  return (
    <div
      className="goal-filter-connected"
      onChange={(event) => event.stopPropagation()}
    >
      {!compact && (
        <details open className="goal-filter-source-settings">
          <summary>Numeric input settings</summary>
          <label>
            {'Numeric starting item'}{' '}
            <select
              value={baseKey}
              onChange={(event) => setBaseKey(event.target.value)}
            >
              <option value="support">{'Support input'}</option>
              {inventory.data?.map((base) => (
                <option key={base.key} value={base.key}>
                  Server base: {base.key}
                </option>
              ))}
            </select>
          </label>
          {inventory.isError && (
            <p role="alert">
              {'Base inventory unavailable.'}{' '}
              <button onClick={() => void inventory.refetch()}>Retry</button>
            </p>
          )}
          {baseKey !== 'support' && (
            <p>
              {
                'Evaluates the selected server base with its real implicit and empty explicits. The family starting item below remains separate.'
              }
            </p>
          )}
        </details>
      )}
      {compact && inventory.isError && (
        <p role="alert">
          Base inventory unavailable.{' '}
          <button onClick={() => void inventory.refetch()}>Retry</button>
        </p>
      )}
      <ConnectedEditor
        onResetGoal={onResetGoal}
        onSearchGoalChange={onSearchGoalChange}
        initialCatalog={initialCatalog}
        rulesetIdentity={
          baseKey === 'support'
            ? rulesetIdentity
            : selected?.initial.rulesetIdentity
        }
        item={actualItem}
        context={{
          snapshotId: actualContext.snapshotId,
          baseItemId: actualContext.baseItemId,
          itemLevel: actualContext.itemLevel,
        }}
        bases={
          inventory.data?.map((base) => ({
            id: base.initial.state.baseItemId,
            label:
              startBases.find((entry) => entry[0] === base.key)?.[2] ??
              base.key,
          })) ?? [{ id: context.baseItemId, label: context.baseItemId }]
        }
        language={language}
        activeOmens={activeOmens}
        maxMillis={maxMillis}
        allowQualityWork={allowQualityWork}
        compact={compact}
      />
    </div>
  )
}
function ConnectedEditor({
  initialCatalog,
  item,
  context,
  bases,
  language,
  compact,
  allowQualityWork,
  onSearchGoalChange,
  onResetGoal,
}: {
  initialCatalog: Catalog
  item: ConcreteItem | null
  rulesetIdentity?: string | undefined
  context: Context
  bases: { id: string; label: string }[]
  language: GoalFilterLanguage
  activeOmens: string[]
  maxMillis: number
  compact: boolean
  allowQualityWork: boolean
  onResetGoal?: (() => void) | undefined
  onSearchGoalChange?: ((snapshot: SearchGoalSnapshot) => void) | undefined
}) {
  const { locale } = useI18n()
  const [editor] = useState(() =>
    createGoalFilterEditor(
      (compact ? readSupport('goal', validGoal) : null) ??
        emptyGoal(initialCatalog.context, initialCatalog.catalogVersion),
    ),
  )
  const goal = useStore(editor, (state) => state.goal)
  const [storageFailed, setStorageFailed] = useState(false)
  useEffect(() => {
    if (!compact) return
    const persist = () =>
      setStorageFailed(!saveSupport('goal', editor.getState().goal))
    const timer = setTimeout(persist, 250)
    window.addEventListener('pagehide', persist)
    return () => {
      clearTimeout(timer)
      window.removeEventListener('pagehide', persist)
    }
  }, [compact, goal, editor])
  useEffect(() => {
    if (
      compact &&
      editor.getState().goal.general.baseItemId !== context.baseItemId
    ) {
      editor.getState().edit((goal) => {
        goal.general.baseItemId = context.baseItemId
      })
    }
  }, [compact, context.baseItemId, editor])
  const currentCatalog = useGoalFilterCatalog(adapter, context)
  const [resetGeneration, setResetGeneration] = useState(0)
  const [inputReady, setInputReady] = useState(false)
  useEffect(() => {
    const catalog = currentCatalog.data
    onSearchGoalChange?.({
      goal: structuredClone(qualityWorkGoal(goal, allowQualityWork)),
      sourceStatIds: catalog ? goalSources(goal, catalog) : undefined,
      catalogVersion: catalog?.catalogVersion ?? null,
      ready:
        inputReady &&
        currentCatalog.isSuccess &&
        !currentCatalog.isFetching &&
        catalog?.catalogVersion === goal.catalogVersion &&
        catalog.context.snapshotId === context.snapshotId &&
        catalog.context.baseItemId === context.baseItemId &&
        catalog.context.itemLevel === context.itemLevel,
    })
  }, [
    goal,
    allowQualityWork,
    currentCatalog.data,
    currentCatalog.isSuccess,
    currentCatalog.isFetching,
    context.snapshotId,
    context.baseItemId,
    context.itemLevel,
    onSearchGoalChange,
    inputReady,
  ])
  const ko = language === 'ko'
  return (
    <div className="goal-filter-connected">
      {!compact && (
        <details open className="goal-filter-support-notes">
          <summary>Evaluation support and limitations</summary>
          <p>
            {ko
              ? '수치 필터는 아래 family/tier 목표와 별도로 판정합니다. 화폐를 적용하거나 기존 추천 목표를 변경하지 않습니다.'
              : 'Numeric filters evaluate separately from the family/tier goal below.'}
          </p>
          {!item && (
            <p role="status">
              {ko
                ? '실제 수치가 없습니다. 수동 tier 입력은 roll 값을 보존하지 않으므로 수치 판정할 수 없습니다. 서버 base 또는 검증된 아이템 텍스트를 선택하세요.'
                : 'Actual rolls are unavailable. Manual tiers cannot be evaluated numerically. Select a server base or verified item text.'}
            </p>
          )}
          <p>
            {ko
              ? '품질·특수 조건의 수치 효과는 미지원입니다. 수치 판정과 확률 지원 상태를 따로 확인하세요.'
              : 'Quality and special numeric effects are unsupported. Evaluation and probability support are reported separately.'}
          </p>
          {item?.catalystQuality && (
            <p role="status">
              {ko
                ? '품질 효과의 수치 판정은 미지원입니다. 입력 품질을 보존하며 적용된 것으로 표시하지 않습니다.'
                : 'Quality projection is unsupported; input quality is preserved.'}
            </p>
          )}
        </details>
      )}
      {!compact && goal.general.baseItemId !== context.baseItemId && (
        <p role="alert">
          {ko
            ? '목표 base와 시작 아이템이 다릅니다. 기존 목표 행은 보존됩니다.'
            : 'Goal base differs from the starting item. Existing rows are preserved.'}{' '}
          <button
            onClick={() =>
              editor.getState().edit((g) => {
                g.general.baseItemId = context.baseItemId
              })
            }
          >
            {ko
              ? '현재 시작 base를 목표에 사용'
              : 'Use current starting base in goal'}
          </button>
        </p>
      )}
      {storageFailed && (
        <p role="status">{workspaceMessages[locale].storage}</p>
      )}
      <GoalFilterPanel
        headerActions={
          <button
            disabled={
              !currentCatalog.data ||
              currentCatalog.isFetching ||
              currentCatalog.isError
            }
            title={resetMessages[language].goalHelp}
            onClick={() => {
              if (!currentCatalog.data) return
              const goal = emptyGoal(
                context,
                currentCatalog.data.catalogVersion,
              )
              onResetGoal?.()
              setInputReady(false)
              editor.setState((state) => ({
                goal,
                revision: state.revision + 1,
                collapsedByGroupId: {},
                presentedTypes: {},
              }))
              if (compact) setStorageFailed(!saveSupport('goal', goal))
              setResetGeneration((n) => n + 1)
            }}
          >
            {resetMessages[language].goal}
          </button>
        }
        key={resetGeneration}
        onSearchReadinessChange={setInputReady}
        presentStatIds={presentNumericStats(
          item,
          currentCatalog.data ?? initialCatalog,
        )}
        adapter={adapter}
        context={context}
        editor={editor}
        bases={bases}
        language={language}
        allowQualityWork={allowQualityWork}
        compact={compact}
      />
    </div>
  )
}
