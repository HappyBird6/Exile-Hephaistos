import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { useStore } from 'zustand'
import { concreteInitial } from '../workbenchApi'
import type { ConcreteItem } from '../workbenchApi'
import { loadInitial } from '../craftingApi'
import { createHttpGoalFilterAdapter, GoalFilterApiError } from './api'
import { evaluateNumericItem, presentNumericStats } from './numericApi'
import { useGoalFilterCatalog } from './useGoalFilter'
import { createGoalFilterEditor, emptyGoal } from './editor'
import { GoalFilterPanel } from './GoalFilterPanel'
import type { GoalFilterLanguage } from './i18n'
import type { Catalog, Context, GoalFilter } from './types'

const adapter = createHttpGoalFilterAdapter()
export function ConnectedGoalFilter({
  item,
  context,
  activeOmens,
  maxMillis,
}: {
  item: ConcreteItem | null
  context: Context
  language: GoalFilterLanguage
  activeOmens: string[]
  maxMillis: number
}) {
  const [baseKey, setBaseKey] = useState('support')
  const inventory = useQuery({
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
          ? 'Goal catalog unavailable.'
          : 'Loading goal catalog…'}{' '}
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
      <ConnectedEditor
        initialCatalog={initialCatalog}
        item={actualItem}
        context={{
          snapshotId: actualContext.snapshotId,
          baseItemId: actualContext.baseItemId,
          itemLevel: actualContext.itemLevel,
        }}
        bases={
          inventory.data?.map((base) => ({
            id: base.initial.state.baseItemId,
            label: base.key,
          })) ?? [{ id: context.baseItemId, label: context.baseItemId }]
        }
        language="en"
        activeOmens={activeOmens}
        maxMillis={maxMillis}
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
  activeOmens,
  maxMillis,
}: {
  initialCatalog: Catalog
  item: ConcreteItem | null
  context: Context
  bases: { id: string; label: string }[]
  language: GoalFilterLanguage
  activeOmens: string[]
  maxMillis: number
}) {
  const [editor] = useState(() =>
    createGoalFilterEditor(
      emptyGoal(initialCatalog.context, initialCatalog.catalogVersion),
    ),
  )
  const goal = useStore(editor, (state) => state.goal)
  const currentCatalog = useGoalFilterCatalog(adapter, context)
  const [submitted, setSubmitted] = useState<{
    goal: GoalFilter
    identity: string
    nonce: number
  } | null>(null)
  const identity = JSON.stringify({
    item,
    context,
    goal,
    activeOmens,
    maxMillis,
  })
  const current = submitted?.identity === identity && item !== null
  const result = useQuery({
    queryKey: ['goalFilter', 'item-evaluation', identity, submitted],
    enabled: Boolean(current),
    queryFn: ({ signal }) =>
      evaluateNumericItem(
        item!,
        submitted!.goal,
        activeOmens,
        maxMillis,
        signal,
      ),
    retry: false,
  })
  const visible = current ? result.data : undefined
  const ko = language === 'ko'
  return (
    <div className="goal-filter-connected">
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
      {goal.general.baseItemId !== context.baseItemId && (
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
      <GoalFilterPanel
        presentStatIds={presentNumericStats(
          item,
          currentCatalog.data ?? initialCatalog,
        )}
        adapter={adapter}
        context={context}
        editor={editor}
        bases={bases}
        language="en"
        recommendation={visible?.recommendation}
        recommendationGoal={current ? submitted?.goal : undefined}
        evaluationAvailable={item !== null}
        evaluationPending={Boolean(current && result.isFetching)}
        onInputEdit={() => setSubmitted(null)}
        onEvaluate={(selected) =>
          setSubmitted({ goal: selected, identity, nonce: Date.now() })
        }
      />
      {current && result.isError && (
        <p role="alert">
          {ko
            ? '판정 요청 실패. 입력은 보존됩니다.'
            : 'Evaluation failed. Inputs are preserved.'}{' '}
          {result.error.message}
        </p>
      )}
      {current &&
        result.error instanceof GoalFilterApiError &&
        result.error.issues.map((issue, index) => (
          <p role="alert" key={index}>
            {issue.code}: {issue.message} ({issue.path})
          </p>
        ))}
      {visible && (
        <section
          aria-live="polite"
          aria-label={ko ? '수치 판정 결과' : 'Numeric evaluation result'}
        >
          <h3>
            {ko ? '현재 아이템 판정' : 'Current item evaluation'}:{' '}
            {visible.evaluation.status}
          </h3>
          <p>General: {visible.evaluation.generalStatus}</p>
          {visible.evaluation.groups.map((group) => (
            <div key={group.id}>
              <p>
                {ko ? '그룹' : 'Group'}{' '}
                {goal.groups.findIndex((g) => g.id === group.id) + 1}:{' '}
                {group.status}{' '}
                {group.count === null ? '' : `count ${group.count}`}{' '}
                {group.score === null ? '' : `score ${group.score}`}
              </p>
              <ul>
                {group.entries.map((entry) => (
                  <li key={entry.id}>
                    {(currentCatalog.data ?? initialCatalog).stats.find(
                      (stat) =>
                        stat.statId ===
                        goal.groups
                          .flatMap((g) => g.entries)
                          .find((e) => e.id === entry.id)?.statId,
                    )?.label ?? entry.id}
                    : {entry.status} / {entry.presence} /{' '}
                    {entry.value === null ? 'unknown / absent' : entry.value}
                  </li>
                ))}
              </ul>
            </div>
          ))}
          {visible.evaluation.issues.map((issue, index) => (
            <p key={index}>
              {issue.code}: {issue.message} ({issue.path})
            </p>
          ))}
          <p>
            {ko ? '수치 확률' : 'Numeric probability'}:{' '}
            {visible.recommendation.probability.status} /{' '}
            {visible.recommendation.probability.reasonCode}
          </p>
          <p>
            {ko ? '비교한 경로' : 'Compared sequences'}:{' '}
            {visible.recommendation.comparedSequences};{' '}
            {ko
              ? '전체 경로 수 미정'
              : visible.recommendation.totalSequences === null
                ? 'Total sequences unknown'
                : 'Total sequences'}
            : {String(visible.recommendation.totalSequences)}
          </p>
        </section>
      )}
    </div>
  )
}
