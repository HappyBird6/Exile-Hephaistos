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
import {
  localizedModifierText,
  modifierRollDivisor,
  storedModifierRoll,
} from './localizedModifiers'
import { rulesetHeaders, verifyRulesetResponse } from './rulesetIdentity'
import type { ConcreteItem, WorkbenchAction } from './workbenchApi'
import type { Definition, Initial } from './craftingApi'
import { UnrevealedRune } from './abyss-workbench/UnrevealedRune'
import { RevealIcon } from './abyss-workbench/RevealIcon'
import { RevealPopover } from './abyss-workbench/RevealPopover'
import { abyssWorkbenchMessages } from './abyss-workbench/messages'

type Modifier = ConcreteItem['explicits'][number]
type Slot = {
  revealPool?: string | null
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

  ru: [
    'Бездна крафта',
    'Нераскрытый модификатор осквернения',
    'Префикс',
    'Суффикс',
    'Добавить нераскрытый слот',
    'Оскверненный слот',
    'Введите трех наблюдаемых кандидатов',
    'Выявить/заменить наблюдаемых кандидатов',
    'Выберите',
    'Следующая валюта',
    'Применить',
    'Целевой модификатор',
    'Сравнить',
    'Вероятность следующего шага; в скобках предполагается нецелевой блокатор с наибольшим весом. Вероятности раскрытия неизвестны.',
    'Это действие или ввод недоступны. Проверьте слоты, уровень предметов и конфликты.',
    'Не удалось сохранить этот сценарий.',
    'Сохраненный сценарий восстановлен.',
    'Кандидат',
    'Вероятность удаления',
    'Нет подходящего сравнения',
    'Вероятность обнаружения не рассчитывается. Переработать один и тот же слот вручную, в том числе на шесть модификаторов.',
  ],
  th: [
    'การคราฟต์เหว',
    'ม็อดที่เสื่อมทรามที่ยังไม่เปิดเผย',
    'พรีฟิกซ์',
    'ซัฟฟิกซ์',
    'เพิ่มช่องที่ไม่เปิดเผย',
    'สล็อตที่ถูกทำลาย',
    'ป้อนผู้นำไปใช้ที่สังเกตพบสามคน',
    'เปิดเผย/แทนที่ผู้นำไปใช้ที่สังเกตพบ',
    'เลือก',
    'สกุลเงินถัดไป',
    'นำไปใช้',
    'ม็อดเป้าหมาย',
    'เปรียบเทียบ',
    'ความน่าจะเป็นขั้นต่อไป วงเล็บจะถือว่าตัวบล็อกที่ไม่ใช่เป้าหมายที่มีน้ำหนักสูงสุด ไม่ทราบความน่าจะเป็นที่เปิดเผย',
    'การดำเนินการหรือการป้อนข้อมูลนี้ไม่พร้อมใช้งาน ตรวจสอบช่อง เลเวลไอเทม และข้อขัดแย้ง',
    'ไม่สามารถบันทึกสถานการณ์นี้ได้',
    'กู้คืนสถานการณ์ที่บันทึกไว้แล้ว',
    'ผู้นำไปใช้',
    'ความน่าจะเป็นในการกำจัด',
    'ไม่มีสิทธิ์เปรียบเทียบ',
    'ไม่มีการคำนวณความน่าจะเป็นที่เปิดเผย ทำงานซ้ำช่องเดิมด้วยตนเอง รวมถึงตัวปรับแต่งหกตัว',
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
  directSelection = false,
  compact = false,
  restoredState,
  onTransition,
  onContinue,
  externalBusy = false,
  canMutate,
  onBusyChange,
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
  directSelection?: boolean
  compact?: boolean
  onTransition?: (state: AbyssState, transition: FlowTransition) => void
  onContinue?: (state: AbyssState, item: Item) => void
  externalBusy?: boolean
  canMutate?: () => boolean
  onBusyChange?: (busy: boolean) => void
}) {
  const { locale } = useI18n()
  const t = texts[locale]
  const immutable = source.conditions.includes('SANCTIFIED')
  // A bone-created/restored slot or an existing prepared route opens this panel.
  // The legacy allowAdd prop stays accepted, but cannot create a currency-free slot.
  const open =
    !!prepared ||
    !!restoredState?.slots.some((slot) => slot.desecrated) ||
    !!source.desecrated?.length
  const [revealingSlotId, setRevealingSlotId] = useState<string | null>(null)
  const [saveError, setSaveError] = useState(false)
  const focus = useRef<HTMLSelectElement>(null)
  const revealTrigger = useRef<HTMLButtonElement>(null)
  const client = useQueryClient()
  const [useDraft] = useState(() =>
    create<{
      slotId: string
      candidates: (Modifier | null)[]
      action: WorkbenchAction
      target: string
      edit: (
        patch: Partial<{
          slotId: string
          candidates: (Modifier | null)[]
          action: WorkbenchAction
          target: string
        }>,
      ) => void
    }>((set) => ({
      slotId:
        (restoredState ?? prepared)?.slots.find((s) => s.desecrated)?.id ?? '',
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
    if (operation === 'eligible') {
      if (
        !Array.isArray(value) ||
        !value.every((id) => typeof id === 'string' && !!initial.modifiers[id])
      )
        throw new Error(t[14])
      return value as T
    }
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
  const [transitioned, setTransitioned] = useState<{
    seed: string
    value: AbyssState
  } | null>(null)
  const state = transitioned?.seed === seed ? transitioned.value : query.data
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
    }) => {
      return request<AbyssState>(operation, { state, ...payload }).then(
        (next) => {
          if (operation === 'select' && state) {
            const expected = {
              ...state,
              offer: null,
              slots: state.slots.map((slot) =>
                slot.id === payload.slotId
                  ? { ...slot, modifier: payload.candidate }
                  : slot,
              ),
            }
            const canonical = (value: unknown): string =>
              JSON.stringify(value, (_key, v: unknown) =>
                v && typeof v === 'object' && !Array.isArray(v)
                  ? Object.fromEntries(
                      Object.entries(v)
                        .sort(([a], [b]) => a.localeCompare(b))
                        .filter(
                          ([, n]) =>
                            n !== null && n !== undefined && n !== false,
                        ),
                    )
                  : v,
              )
            if (canonical(next) !== canonical(expected)) throw new Error(t[14])
          }
          return next
        },
      )
    },
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
      // Historical query results are immutable checkpoints. Updating this key would
      // make rewinding to the same hidden slot return its later revealed state.
      if (localScenario) client.setQueryData(key, next)
      else setTransitioned({ seed, value: next })
      try {
        if (localScenario)
          window.localStorage.setItem(storageKey, JSON.stringify(next))
        setSaveError(false)
      } catch {
        setSaveError(true)
      }
      focus.current?.focus()
      if (compact)
        requestAnimationFrame(() => {
          const target =
            document.querySelector<HTMLButtonElement>(
              '.abyss-reveal-bar > button:not(:disabled)',
            ) ?? document.querySelector<HTMLButtonElement>('.item-slot')
          target?.focus()
        })
    },
    onSettled: () => {
      mutationPending.current = false
      onBusyChange?.(false)
    },
  })
  const mutationPending = useRef(false)
  useEffect(() => {
    onBusyChange?.(query.isFetching || mutation.isPending)
  }, [query.isFetching, mutation.isPending, onBusyChange])
  useEffect(
    () => () => {
      onBusyChange?.(false)
    },
    [onBusyChange],
  )
  const plan = useQuery({
    queryKey: [...key, 'plan', state, draft.action, activeOmens],
    enabled:
      open && !!state && !immutable && !observationOnly && !directSelection,
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
  const hiddenSlots =
    state?.slots.filter((s) => s.desecrated && s.modifier === null) ?? []
  const selected = compact
    ? (hiddenSlots.find((s) => s.id === draft.slotId) ?? hiddenSlots[0])
    : state?.slots.find((s) => s.id === draft.slotId)
  const eligible = useQuery({
    queryKey: [...key, 'eligible', state, selected?.id],
    enabled:
      open &&
      directSelection &&
      !!state &&
      !!selected &&
      selected.id === revealingSlotId &&
      selected.modifier === null,
    retry: false,
    queryFn: ({ signal }) =>
      request<string[]>('eligible', { state, slotId: selected?.id }, signal),
  })
  const directCopy = {
    en: [
      'Choose an eligible modifier. This is a manual choice, not a guaranteed in-game outcome.',
      'Choose modifier',
      'Apply chosen modifier',
    ],
    ko: [
      '적용 가능한 옵션을 직접 선택하세요. 실제 게임에서 확정 획득한다는 뜻은 아닙니다.',
      '옵션 선택',
      '선택한 옵션 적용',
    ],
    'zh-CN': [
      '选择可用词缀。这是手动选择，并不保证游戏中的结果。',
      '选择词缀',
      '应用所选词缀',
    ],
    'zh-TW': [
      '選擇可用詞綴。這是手動選擇，不保證遊戲中的結果。',
      '選擇詞綴',
      '套用所選詞綴',
    ],
    ja: [
      '適用可能なモッドを選択します。ゲーム内での確定獲得を保証するものではありません。',
      'モッドを選択',
      '選択したモッドを適用',
    ],
    es: [
      'Elige un modificador válido. Es una elección manual, no un resultado garantizado en el juego.',
      'Elegir modificador',
      'Aplicar modificador elegido',
    ],

    ru: [
      'Выберите подходящий модификатор. Это выбор вручную, а не гарантированный внутриигровой результат.',
      'Выберите модификатор',
      'Применить выбранный модификатор',
    ],
    th: [
      'เลือกม็อดที่มีสิทธิ์ นี่เป็นการตัดสินใจด้วยตนเอง ไม่ใช่การรับประกันผลลัพธ์ในเกม',
      'เลือกม็อด',
      'ใช้ม็อดที่เลือก',
    ],
  }[locale]
  const candidateDefinitions = definitions.filter(
    (d) =>
      (!directSelection || eligible.data?.includes(d.id)) &&
      d.affixType === selected?.side &&
      (d.requiredItemLevel ?? 1) >= (selected?.minimumModifierLevel ?? 0),
  )
  const label = (d: Definition, values?: Record<string, number>) =>
    localizedModifierText(d, values)
  const busy =
    externalBusy || mutation.isPending || query.isFetching || immutable
  const submit = (operation: string, payload: Record<string, unknown>) => {
    if (busy || mutationPending.current || canMutate?.() === false) return
    mutationPending.current = true
    onBusyChange?.(true)
    mutation.mutate({ operation, payload })
  }
  if (!open) return null
  if (compact && directSelection) {
    if (state && !hiddenSlots.length) return null
    const copy = abyssWorkbenchMessages[locale]
    const candidate = draft.candidates[0]
    const choose = (id: string) => {
      const d = initial.modifiers[id]
      draft.edit({
        candidates: [
          d
            ? {
                modifierId: d.id,
                values: Object.fromEntries(
                  (d.stats ?? []).map((stat) => [stat.id, stat.min]),
                ),
                fractured: false,
              }
            : null,
          null,
          null,
        ],
      })
    }
    return (
      <div
        className="abyss-reveal-bar"
        aria-label={`${copy.desecration} ${copy.reveal}`}
        aria-busy={busy}
      >
        <span>{copy.desecration}</span>
        {hiddenSlots.length > 1 && (
          <select
            aria-label={t[5]}
            value={selected?.id ?? ''}
            disabled={busy}
            onChange={(event) => {
              draft.edit({
                slotId: event.target.value,
                candidates: [null, null, null],
              })
              setRevealingSlotId(null)
            }}
          >
            {hiddenSlots.map((slot, index) => (
              <option key={slot.id} value={slot.id}>
                {slot.side === 'PREFIX' ? 'P' : 'S'}
                {index + 1}
              </option>
            ))}
          </select>
        )}
        {(query.isError || mutation.isError || saveError) && (
          <span role="alert" aria-label={t[14]}>
            !
          </span>
        )}
        <button
          type="button"
          ref={revealTrigger}
          disabled={busy || !selected}
          aria-label={`${copy.desecration} ${copy.reveal}`}
          aria-expanded={!!selected && revealingSlotId === selected.id}
          onClick={() => {
            if (
              !busy &&
              !mutationPending.current &&
              canMutate?.() !== false &&
              selected
            )
              setRevealingSlotId(selected.id)
          }}
        >
          <RevealIcon />
          {copy.reveal}
        </button>
        {selected && revealingSlotId === selected.id && (
          <RevealPopover
            anchor={revealTrigger}
            label={`${copy.desecration} ${copy.reveal}`}
            closeLabel={copy.close}
            onClose={() => setRevealingSlotId(null)}
          >
            <fieldset disabled={busy} className="abyss-reveal-picker__controls">
              {eligible.isFetching && (
                <span role="status" aria-label={copy.reveal}>
                  ◌
                </span>
              )}
              {eligible.isError && (
                <span role="alert" aria-label={t[14]}>
                  !
                </span>
              )}
              <select
                aria-label={directCopy[1]}
                value={candidate?.modifierId ?? ''}
                disabled={busy || eligible.isFetching}
                onChange={(event) => choose(event.target.value)}
              >
                <option value="">—</option>
                {candidateDefinitions.map((d) => (
                  <option key={d.id} value={d.id}>
                    {label(d)}
                  </option>
                ))}
              </select>
              <div className="abyss-reveal-picker__rolls">
                {candidate &&
                  initial.modifiers[candidate.modifierId]?.stats?.map(
                    (stat) => {
                      const divisor = modifierRollDivisor(
                        initial.modifiers[candidate.modifierId]!,
                        stat.id,
                      )
                      return (
                        <input
                          key={stat.id}
                          type="number"
                          aria-label={label(
                            initial.modifiers[candidate.modifierId]!,
                          )}
                          title={`${stat.min / divisor}–${stat.max / divisor}`}
                          min={stat.min / divisor}
                          max={stat.max / divisor}
                          step={1 / divisor}
                          value={(candidate.values[stat.id] ?? 0) / divisor}
                          onChange={(event) =>
                            draft.edit({
                              candidates: [
                                {
                                  ...candidate,
                                  values: {
                                    ...candidate.values,
                                    [stat.id]: storedModifierRoll(
                                      event.target.valueAsNumber,
                                      divisor,
                                    ),
                                  },
                                },
                                null,
                                null,
                              ],
                            })
                          }
                        />
                      )
                    },
                  )}
                <button
                  type="button"
                  aria-label={copy.reveal}
                  disabled={
                    busy ||
                    !candidate ||
                    !eligible.data?.includes(candidate.modifierId) ||
                    eligible.isFetching
                  }
                  onClick={() =>
                    submit('select', { slotId: selected.id, candidate })
                  }
                >
                  ✓
                </button>
              </div>
            </fieldset>
          </RevealPopover>
        )}
      </div>
    )
  }
  return (
    <section
      className="abyss-panel"
      aria-busy={busy}
      onClickCapture={(event) => {
        if (
          externalBusy ||
          mutationPending.current ||
          canMutate?.() === false
        ) {
          event.preventDefault()
          event.stopPropagation()
        }
      }}
      onChangeCapture={(event) => {
        if (
          externalBusy ||
          mutationPending.current ||
          canMutate?.() === false
        ) {
          event.preventDefault()
          event.stopPropagation()
        }
      }}
    >
      {open && (
        <>
          {(query.isError ||
            mutation.isError ||
            plan.isError ||
            comparison.isError) && <p role="alert">{t[14]}</p>}
          {saveError && <p role="alert">{t[15]}</p>}
          {state && (
            <>
              {onContinue && (
                <button
                  disabled={
                    busy || query.isFetching || !materializeAbyss(state)
                  }
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
                      initial.modifiers[slot.modifier.modifierId] ? (
                        label(
                          initial.modifiers[slot.modifier.modifierId]!,
                          slot.modifier.values,
                        )
                      ) : (
                        <UnrevealedRune />
                      )}
                    </li>
                  ))}
                </ul>
              )}
              <label>
                {t[5]}{' '}
                <select
                  disabled={busy}
                  ref={focus}
                  value={draft.slotId}
                  onChange={(e) => {
                    setRevealingSlotId(null)
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
              {directSelection && selected?.modifier === null && (
                <button
                  type="button"
                  disabled={busy}
                  aria-expanded={revealingSlotId === selected.id}
                  onClick={() => setRevealingSlotId(selected.id)}
                >
                  {abyssWorkbenchMessages[locale].reveal}
                </button>
              )}
              {selected &&
                (!directSelection ||
                  (selected.modifier === null &&
                    revealingSlotId === selected.id)) && (
                  <fieldset disabled={busy}>
                    <legend>{directSelection ? directCopy[1] : t[6]}</legend>
                    {directSelection && <p>{directCopy[0]}</p>}
                    {directSelection && eligible.isError && (
                      <p role="alert">{t[14]}</p>
                    )}
                    {(directSelection
                      ? draft.candidates.slice(0, 1)
                      : draft.candidates
                    ).map((candidate, index) => (
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
                            (stat) => {
                              const divisor = modifierRollDivisor(
                                initial.modifiers[candidate.modifierId]!,
                                stat.id,
                              )
                              return (
                                <label key={stat.id}>
                                  {label(
                                    initial.modifiers[candidate.modifierId]!,
                                  )}{' '}
                                  ({stat.min}–{stat.max})
                                  <input
                                    type="number"
                                    min={stat.min / divisor}
                                    max={stat.max / divisor}
                                    step={1 / divisor}
                                    value={
                                      (candidate.values[stat.id] ?? 0) / divisor
                                    }
                                    onChange={(e) => {
                                      const candidates = [...draft.candidates]
                                      candidates[index] = {
                                        ...candidate,
                                        values: {
                                          ...candidate.values,
                                          [stat.id]: storedModifierRoll(
                                            e.target.valueAsNumber,
                                            divisor,
                                          ),
                                        },
                                      }
                                      draft.edit({ candidates })
                                    }}
                                  />
                                </label>
                              )
                            },
                          )}
                      </div>
                    ))}
                    <button
                      disabled={
                        directSelection
                          ? !draft.candidates[0] ||
                            !eligible.data?.includes(
                              draft.candidates[0].modifierId,
                            ) ||
                            eligible.isFetching
                          : draft.candidates.some((c) => !c)
                      }
                      onClick={() =>
                        directSelection
                          ? submit('select', {
                              slotId: draft.slotId,
                              candidate: draft.candidates[0],
                            })
                          : submit('observe', {
                              slotId: draft.slotId,
                              candidates: draft.candidates,
                            })
                      }
                    >
                      {directSelection ? directCopy[2] : t[7]}
                    </button>
                  </fieldset>
                )}
              {state.offer && !directSelection && (
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
              {!observationOnly && !directSelection && (
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
                  {plan.data?.available && !directSelection && (
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
              {selected?.modifier === null && !directSelection && (
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
