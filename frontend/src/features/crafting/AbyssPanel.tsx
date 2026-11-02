import { validAbyssResponse } from './abyssResponse'
import { useEffect, useRef, useState } from 'react'
import {
  materializeAbyss,
  flowMessages,
  type FlowTransition,
} from './method-search/craftingFlow'
import type { Item } from './path-tree/types'
import { create } from 'zustand'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { gameName, useI18n } from '../../shared/i18n/i18n'
import { localizedAction } from './localizedCrafting'
import { localizedModifierText } from './localizedModifiers'
import { rulesetHeaders, verifyRulesetResponse } from './rulesetIdentity'
import type { ConcreteItem, WorkbenchAction } from './workbenchApi'
import type { Definition, Initial } from './craftingApi'

type Modifier = ConcreteItem['explicits'][number]
type Slot = {
  minimumModifierLevel?: 40 | null
  id: string
  side: 'PREFIX' | 'SUFFIX'
  modifier: Modifier | null
  desecrated: boolean
}
export type AbyssState = {
  item: ConcreteItem
  slots: Slot[]
  offer: { slotId: string; candidates: Modifier[] } | null
}
type Chance = { targetWeight: number; totalWeight: number }
type Comparison = {
  baseline: Chance
  afterBlocking: Chance
  blockerModifierId: string
} | null
type Plan = {
  available: boolean
  branches: { removedSlotId: string | null; removalProbability: number }[]
}
const texts = {
  en: [
    'Abyss crafting',
    'Unrevealed desecrated modifier',
    'Prefix',
    'Suffix',
    'Add unrevealed slot',
    'Desecrated slot',
    'Enter three observed candidates',
    'Reveal / replace observed candidates',
    'Choose',
    'Next currency',
    'Apply',
    'Target modifier',
    'Compare',
    'Next-step probability; parenthesis assumes the highest-weight non-target blocker. Reveal probabilities are unknown.',
    'This action or input is unavailable. Check slots, item level and conflicts.',
    'Could not save this scenario.',
    'Saved scenario restored',
    'Candidate',
    'Removal probability',
    'No eligible comparison',
    'No reveal probability is calculated. Rework the same slot manually, including at six modifiers.',
  ],
  ko: [
    '심연 제작',
    '미공개 훼손 속성',
    '접두',
    '접미',
    '미공개 훼손 슬롯 추가',
    '훼손 슬롯',
    '실제로 본 후보 3개 입력',
    '드러내기 / 관측 후보 교체',
    '선택',
    '다음 화폐',
    '적용',
    '목표 옵션',
    '비교',
    '다음 제작 단계 확률입니다. 괄호는 최고 가중치 비목표 옵션 차단을 가정하며, 드러내기 확률은 알 수 없습니다.',
    '실행할 수 없는 행동 또는 입력입니다. 슬롯·레벨·충돌군을 확인하세요.',
    '이 시나리오를 저장하지 못했습니다.',
    '저장된 시나리오 복원',
    '후보',
    '제거 확률',
    '계산 가능한 비교 없음',
    '드러내기 확률은 계산하지 않습니다. 6옵션에서도 같은 슬롯을 수동 재작업할 수 있습니다.',
  ],
  'zh-CN': [
    '深渊制作',
    '未揭示的亵渎词缀',
    '前缀',
    '后缀',
    '添加未揭示槽位',
    '亵渎槽位',
    '输入已观察到的三个候选',
    '揭示 / 替换观察候选',
    '选择',
    '下一种通货',
    '应用',
    '目标词缀',
    '比较',
    '下一步概率；括号假定阻挡最高权重的非目标词缀。揭示概率未知。',
    '操作或输入不可用。请检查槽位、等级和冲突。',
    '无法保存此方案。',
    '已恢复保存的方案',
    '候选',
    '移除概率',
    '无可计算的比较',
    '不计算揭示概率。六词缀时也可手动重作同一槽位。',
  ],
  'zh-TW': [
    '深淵製作',
    '未揭示的褻瀆詞綴',
    '前綴',
    '後綴',
    '新增未揭示欄位',
    '褻瀆欄位',
    '輸入已觀察到的三個候選',
    '揭示 / 替換觀察候選',
    '選擇',
    '下一種通貨',
    '套用',
    '目標詞綴',
    '比較',
    '下一步機率；括號假定阻擋最高權重的非目標詞綴。揭示機率未知。',
    '操作或輸入不可用。請檢查欄位、等級和衝突。',
    '無法儲存此方案。',
    '已還原儲存的方案',
    '候選',
    '移除機率',
    '無可計算的比較',
    '不計算揭示機率。六詞綴時也可手動重作同一欄位。',
  ],
  ja: [
    'アビス製作',
    '未公開の冒涜モッド',
    'プレフィックス',
    'サフィックス',
    '未公開スロットを追加',
    '冒涜スロット',
    '確認した候補を3つ入力',
    '公開 / 確認した候補を更新',
    '選択',
    '次の通貨',
    '適用',
    '目標モッド',
    '比較',
    '次の製作段階の確率です。括弧は最大ウェイトの非目標モッドを除外した場合です。公開確率は不明です。',
    '操作または入力が無効です。スロット、レベル、競合を確認してください。',
    'シナリオを保存できませんでした。',
    '保存したシナリオを復元',
    '候補',
    '除去確率',
    '計算可能な比較なし',
    '公開確率は計算しません。6モッドでも同じスロットを手動で再加工できます。',
  ],
  es: [
    'Fabricación abisal',
    'Modificador profanado sin revelar',
    'Prefijo',
    'Sufijo',
    'Añadir espacio sin revelar',
    'Espacio profanado',
    'Introduce los tres candidatos observados',
    'Revelar / sustituir candidatos observados',
    'Elegir',
    'Siguiente moneda',
    'Aplicar',
    'Modificador objetivo',
    'Comparar',
    'Probabilidad del siguiente paso; el paréntesis supone bloquear el modificador ajeno al objetivo de mayor peso. La probabilidad de revelar es desconocida.',
    'Acción o datos no disponibles. Comprueba espacios, nivel y conflictos.',
    'No se pudo guardar el escenario.',
    'Escenario guardado restaurado',
    'Candidato',
    'Probabilidad de eliminación',
    'No hay comparación calculable',
    'No se calcula la probabilidad de revelar. Puedes rehacer manualmente el mismo espacio incluso con seis modificadores.',
  ],
} as const

export function AbyssPanel({
  source,
  initial,
  prepared,
  initialAction = 'EXALTED',
  initialOmens = [],
  embedded = false,
  observationOnly = false,
  localScenario = true,
  allowAdd = true,
  restoredState,
  onTransition,
  onContinue,
}: {
  source: ConcreteItem
  initial: Initial
  prepared?: AbyssState
  initialAction?: WorkbenchAction
  initialOmens?: string[]
  restoredState?: AbyssState | undefined
  embedded?: boolean
  observationOnly?: boolean
  localScenario?: boolean
  allowAdd?: boolean
  onTransition?: (state: AbyssState, transition: FlowTransition) => void
  onContinue?: (state: AbyssState, item: Item) => void
}) {
  const { locale } = useI18n()
  const t = texts[locale]
  const [open, setOpen] = useState(embedded || !!prepared)
  const [saveError, setSaveError] = useState(false)
  const focus = useRef<HTMLSelectElement>(null)
  const client = useQueryClient()
  const [useDraft] = useState(() =>
    create<{
      side: 'PREFIX' | 'SUFFIX'
      slotId: string
      candidates: (Modifier | null)[]
      action: WorkbenchAction
      target: string
      edit: (
        patch: Partial<{
          side: 'PREFIX' | 'SUFFIX'
          slotId: string
          candidates: (Modifier | null)[]
          action: WorkbenchAction
          target: string
        }>,
      ) => void
    }>((set) => ({
      side: 'PREFIX',
      slotId: prepared?.slots.find((s) => s.desecrated)?.id ?? '',
      candidates: [null, null, null],
      action: initialAction,
      target: '',
      edit: (patch) => set(patch),
    })),
  )
  const draft = useDraft()
  const activeOmens = draft.action === initialAction ? initialOmens : []
  const identity = initial.rulesetIdentity
  const seed = JSON.stringify(
    restoredState ??
      (prepared ? [prepared, initialAction, initialOmens] : source),
  )
  const key = ['abyss-v1', identity, seed, localScenario]
  const storageKey = `hephaistos.abyss.v1:${identity}:${seed}`
  async function request<T>(
    operation: string,
    body: unknown,
    signal?: AbortSignal,
  ): Promise<T> {
    const response = await fetch(
      `/api/v1/crafting/workbench/abyss/${operation}`,
      {
        method: 'POST',
        headers: rulesetHeaders(identity),
        body: JSON.stringify(body),
        ...(signal ? { signal } : {}),
      },
    )
    verifyRulesetResponse(response, identity)
    if (!response.ok) throw new Error(t[14])
    const value: unknown = await response.json()
    if (value && typeof value === 'object' && 'rulesetIdentity' in value)
      delete value.rulesetIdentity
    if (!validAbyssResponse(operation, value, source, initial))
      throw new Error(t[14])
    return value as T
  }
  const query = useQuery({
    queryKey: key,
    enabled: open,
    staleTime: Infinity,
    retry: false,
    queryFn: async ({ signal }) => {
      const saved = localScenario
        ? window.localStorage.getItem(storageKey)
        : null
      if (restoredState)
        return request<AbyssState>('validate', { state: restoredState }, signal)
      return saved
        ? request<AbyssState>(
            'validate',
            { state: JSON.parse(saved) as unknown },
            signal,
          )
        : prepared
          ? request<AbyssState>('validate', { state: prepared }, signal)
          : request<AbyssState>('import', { state: source }, signal)
    },
  })
  const state = query.data
  const transitionCallback = useRef(onTransition)
  useEffect(() => {
    transitionCallback.current = onTransition
  }, [onTransition])
  const initialReported = useRef(false)
  useEffect(() => {
    if (state && !initialReported.current) {
      initialReported.current = true
      transitionCallback.current?.(state, { operation: 'restore' })
    }
  }, [state])
  const mounted = useRef(true)
  useEffect(() => {
    mounted.current = true
    return () => {
      mounted.current = false
    }
  }, [])
  const mutation = useMutation({
    mutationFn: ({
      operation,
      payload,
    }: {
      operation: string
      payload: Record<string, unknown>
    }) => request<AbyssState>(operation, { state, ...payload }),
    onSuccess: (next, variables) => {
      if (!mounted.current) return
      const operation = variables.operation as FlowTransition['operation']
      transitionCallback.current?.(next, {
        operation,
        ...(operation === 'apply'
          ? {
              action: draft.action,
              removal:
                plan.data?.branches
                  .filter((b) => b.removedSlotId)
                  .map((b) => ({
                    slotId: b.removedSlotId!,
                    probability: b.removalProbability,
                  })) ?? [],
            }
          : {}),
      })
      client.setQueryData(key, next)
      try {
        if (localScenario)
          window.localStorage.setItem(storageKey, JSON.stringify(next))
        setSaveError(false)
      } catch {
        setSaveError(true)
      }
      focus.current?.focus()
    },
  })
  const plan = useQuery({
    queryKey: [...key, 'plan', state, draft.action, activeOmens],
    enabled: !!state && !observationOnly,
    queryFn: ({ signal }) =>
      request<Plan>(
        'plan',
        { state, action: draft.action, activeOmens },
        signal,
      ),
    retry: false,
  })
  const comparison = useQuery({
    queryKey: [...key, 'comparison', state, draft.slotId, draft.target],
    enabled: false,
    retry: false,
    queryFn: ({ signal }) =>
      request<Comparison>(
        'compare',
        {
          state,
          slotId: draft.slotId,
          action: 'EXALTED',
          activeOmens: [],
          targetIds: [draft.target],
        },
        signal,
      ),
  })
  const definitions = Object.values(initial.modifiers).filter(
    (d) =>
      d.layer !== 'IMPLICIT' &&
      d.affixType !== 'NONE' &&
      (d.requiredItemLevel ?? 1) <= source.itemLevel,
  )
  const selected = state?.slots.find((s) => s.id === draft.slotId)
  const candidateDefinitions = definitions.filter(
    (d) =>
      d.affixType === selected?.side &&
      (d.requiredItemLevel ?? 1) >= (selected?.minimumModifierLevel ?? 0),
  )
  const label = (d: Definition, values?: Record<string, number>) =>
    localizedModifierText(d, values)
  const busy = mutation.isPending || query.isFetching
  const submit = (operation: string, payload: Record<string, unknown>) =>
    mutation.mutate({ operation, payload })
  return (
    <section className="abyss-panel">
      {!embedded && (
        <button
          type="button"
          aria-expanded={open}
          onClick={() => setOpen(!open)}
        >
          {t[0]}
        </button>
      )}
      {open && (
        <>
          <p>{t[20]}</p>
          {(query.isError ||
            mutation.isError ||
            plan.isError ||
            comparison.isError) && <p role="alert">{t[14]}</p>}
          {saveError && <p role="alert">{t[15]}</p>}
          {state && (
            <>
              {onContinue && (
                <button
                  disabled={busy || !materializeAbyss(state)}
                  onClick={() => {
                    const item = materializeAbyss(state)
                    if (item) onContinue(state, item)
                  }}
                >
                  {flowMessages[locale].resume}
                </button>
              )}
              {!embedded && (
                <ul>
                  {state.slots.map((slot) => (
                    <li
                      key={slot.id}
                      className={slot.desecrated ? 'abyss-slot' : undefined}
                    >
                      {slot.side === 'PREFIX' ? t[2] : t[3]}:{' '}
                      {slot.modifier &&
                      initial.modifiers[slot.modifier.modifierId]
                        ? label(
                            initial.modifiers[slot.modifier.modifierId]!,
                            slot.modifier.values,
                          )
                        : t[1]}
                    </li>
                  ))}
                </ul>
              )}
              <label>
                {t[5]}{' '}
                <select
                  ref={focus}
                  value={draft.slotId}
                  onChange={(e) => {
                    draft.edit({
                      slotId: e.target.value,
                      candidates: [null, null, null],
                    })
                  }}
                >
                  <option value="">—</option>
                  {state.slots
                    .filter((s) => s.desecrated)
                    .map((s) => (
                      <option key={s.id} value={s.id}>
                        {s.side === 'PREFIX' ? t[2] : t[3]}{' '}
                      </option>
                    ))}
                </select>
              </label>
              {!observationOnly && allowAdd && (
                <>
                  <label>
                    {t[4]}{' '}
                    <select
                      value={draft.side}
                      onChange={(e) =>
                        draft.edit({
                          side: e.target.value as 'PREFIX' | 'SUFFIX',
                        })
                      }
                    >
                      <option value="PREFIX">{t[2]}</option>
                      <option value="SUFFIX">{t[3]}</option>
                    </select>
                  </label>
                  <button
                    disabled={busy || state.slots.length >= 6}
                    onClick={() =>
                      submit('add', {
                        slotId: crypto.randomUUID(),
                        side: draft.side,
                      })
                    }
                  >
                    {t[4]}
                  </button>
                </>
              )}
              {selected && (
                <fieldset disabled={busy}>
                  <legend>{t[6]}</legend>
                  {draft.candidates.map((candidate, index) => (
                    <div key={index}>
                      <label>
                        {t[17]} {index + 1}{' '}
                        <select
                          value={candidate?.modifierId ?? ''}
                          onChange={(e) => {
                            const d = initial.modifiers[e.target.value]
                            const candidates = [...draft.candidates]
                            candidates[index] = d
                              ? {
                                  modifierId: d.id,
                                  values: Object.fromEntries(
                                    (d.stats ?? []).map((s) => [s.id, s.min]),
                                  ),
                                  fractured: false,
                                }
                              : null
                            draft.edit({ candidates })
                          }}
                        >
                          <option value="">—</option>
                          {candidateDefinitions.map((d) => (
                            <option key={d.id} value={d.id}>
                              {label(d)}
                            </option>
                          ))}
                        </select>
                      </label>
                      {candidate &&
                        initial.modifiers[candidate.modifierId]?.stats?.map(
                          (stat) => (
                            <label key={stat.id}>
                              {label(initial.modifiers[candidate.modifierId]!)}{' '}
                              ({stat.min}–{stat.max})
                              <input
                                type="number"
                                min={stat.min}
                                max={stat.max}
                                step={1}
                                value={candidate.values[stat.id]}
                                onChange={(e) => {
                                  const candidates = [...draft.candidates]
                                  candidates[index] = {
                                    ...candidate,
                                    values: {
                                      ...candidate.values,
                                      [stat.id]: e.target.valueAsNumber,
                                    },
                                  }
                                  draft.edit({ candidates })
                                }}
                              />
                            </label>
                          ),
                        )}
                    </div>
                  ))}
                  <button
                    disabled={draft.candidates.some((c) => !c)}
                    onClick={() =>
                      submit('observe', {
                        slotId: draft.slotId,
                        candidates: draft.candidates,
                      })
                    }
                  >
                    {t[7]}
                  </button>
                </fieldset>
              )}
              {state.offer && (
                <ol>
                  {state.offer.candidates.map((candidate, index) => (
                    <li key={index} className="abyss-slot">
                      {label(
                        initial.modifiers[candidate.modifierId]!,
                        candidate.values,
                      )}{' '}
                      <button
                        disabled={busy}
                        onClick={() =>
                          submit('choose', { candidateIndex: index })
                        }
                      >
                        {t[8]}
                      </button>
                    </li>
                  ))}
                </ol>
              )}
              {!observationOnly && (
                <>
                  {activeOmens.map((omen) => (
                    <p key={omen}>{gameName(omen, omen)}</p>
                  ))}
                  <label>
                    {t[9]}{' '}
                    <select
                      value={draft.action}
                      onChange={(e) =>
                        draft.edit({
                          action: e.target.value as WorkbenchAction,
                        })
                      }
                    >
                      {[
                        ...new Set([
                          initialAction,
                          'EXALTED',
                          'ANNULMENT',
                          'ESSENCE_HYSTERIA',
                          'PERFECT_ESSENCE_INFINITE',
                          'PERFECT_ESSENCE_ENHANCEMENT',
                          'RUNIC_ALLOY',
                          'EXPANSIVE_ALLOY',
                          'CYCLONIC_ALLOY',
                          'MYSTIC_ALLOY',
                          'ADAPTIVE_ALLOY',
                          'SWIFT_ALLOY',
                          'SOVEREIGN_ALLOY',
                          'PRISMATIC_ALLOY',
                        ] as WorkbenchAction[]),
                      ].map((a) => (
                        <option key={a} value={a}>
                          {localizedAction(a)}
                        </option>
                      ))}
                    </select>
                  </label>
                  <button
                    disabled={busy || !plan.data?.available || plan.isFetching}
                    onClick={() =>
                      submit('apply', {
                        action: draft.action,
                        activeOmens,
                      })
                    }
                  >
                    {t[10]}
                  </button>
                  {plan.data?.available && (
                    <ul>
                      {plan.data.branches
                        .filter((b) => b.removedSlotId)
                        .map((b) => (
                          <li key={b.removedSlotId}>
                            {t[18]} ·{' '}
                            {(() => {
                              const m = state.slots.find(
                                (s) => s.id === b.removedSlotId,
                              )?.modifier
                              return m
                                ? label(
                                    initial.modifiers[m.modifierId]!,
                                    m.values,
                                  )
                                : t[1]
                            })()}
                            : {(100 * b.removalProbability).toFixed(2)}%
                          </li>
                        ))}
                    </ul>
                  )}
                </>
              )}
              {selected?.modifier === null && (
                <>
                  <label>
                    {t[11]}{' '}
                    <select
                      value={draft.target}
                      onChange={(e) => {
                        draft.edit({ target: e.target.value })
                      }}
                    >
                      <option value="">—</option>
                      {definitions.map((d) => (
                        <option key={d.id} value={d.id}>
                          {label(d)}
                        </option>
                      ))}
                    </select>
                  </label>
                  <button
                    disabled={!draft.target || comparison.isFetching || busy}
                    onClick={() => void comparison.refetch()}
                  >
                    {t[12]}
                  </button>
                  {comparison.isSuccess && (
                    <p>
                      {comparison.data
                        ? `${((100 * comparison.data.baseline.targetWeight) / comparison.data.baseline.totalWeight).toFixed(2)}%(~${((100 * comparison.data.afterBlocking.targetWeight) / comparison.data.afterBlocking.totalWeight).toFixed(2)}%)`
                        : t[19]}
                    </p>
                  )}
                  <p>{t[13]}</p>
                </>
              )}
            </>
          )}
        </>
      )}
    </section>
  )
}
