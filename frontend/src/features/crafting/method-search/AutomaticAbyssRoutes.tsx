import { localizedModifierText } from '../localizedModifiers'
import {
  flowMessages,
  type FlowBranch,
  type FlowTransition,
} from './craftingFlow'
import type { Item } from '../path-tree/types'
import { useQuery } from '@tanstack/react-query'
import { gameName, useI18n } from '../../../shared/i18n/i18n'
import { AbyssPanel, type AbyssState } from '../AbyssPanel'
import { loadInitial } from '../craftingApi'
import type { WorkbenchBaseKey } from '../baseRegistry'
import type { ConcreteItem, WorkbenchAction } from '../workbenchApi'
import { validAbyssResponse } from '../abyssResponse'
import { verifyRulesetResponse } from '../rulesetIdentity'
import { localizedAction } from '../localizedCrafting'

const messages = {
  en: [
    'Abyss routes',
    'Complete one side',
    'Block a high-weight modifier',
    'Share removal risk',
    'Open route',
    'These routes require manual reveal choices and are excluded from automatic goal probabilities. Percentages compare the next modifier selection, not its numeric rolls or reveal candidates.',
    'Could not load routes.',
    'Preserve useful modifiers',
  ],
  ko: [
    '심연 제작 경로',
    '한쪽 속성 채우기',
    '고가중치 옵션 차단',
    '제거 위험 분산',
    '경로 열기',
    '이 경로는 드러내기 수동 선택이 필요하며 자동 목표 확률에 합산하지 않습니다. 확률은 다음 옵션 종류의 선택 확률이며 수치 범위나 드러내기 후보 확률이 아닙니다.',
    '경로를 불러오지 못했습니다.',
    '유효 옵션 보존',
  ],
  'zh-CN': [
    '深渊制作路线',
    '填满一侧',
    '阻挡高权重词缀',
    '分散移除风险',
    '打开路线',
    '这些路线需要手动选择揭示结果，不计入自动目标概率。百分比仅比较下一词缀类型的选择，不代表数值或揭示候选概率。',
    '无法加载路线。',
    '保留有效词缀',
  ],
  'zh-TW': [
    '深淵製作路線',
    '填滿一側',
    '阻擋高權重詞綴',
    '分散移除風險',
    '開啟路線',
    '這些路線需要手動選擇揭示結果，不計入自動目標機率。百分比僅比較下一詞綴類型的選擇，不代表數值或揭示候選機率。',
    '無法載入路線。',
    '保留有效詞綴',
  ],
  ja: [
    'アビスクラフト経路',
    '片側を埋める',
    '高ウェイトのモッドを除外',
    '削除リスクを分散',
    '経路を開く',
    'この経路は公開候補の手動選択が必要で、自動目標確率には含まれません。割合は次のモッド種類の選択確率で、数値や公開候補の確率ではありません。',
    '経路を読み込めませんでした。',
    '有効モッドを維持',
  ],
  es: [
    'Rutas del abismo',
    'Completar un lado',
    'Bloquear un modificador de peso alto',
    'Repartir el riesgo de eliminación',
    'Abrir ruta',
    'Estas rutas requieren elegir manualmente los candidatos revelados y no se incluyen en la probabilidad automática del objetivo. Los porcentajes comparan el tipo de modificador siguiente, no sus valores ni los candidatos revelados.',
    'No se pudieron cargar las rutas.',
    'Conservar modificadores útiles',
  ],

  ru: [
    'Бездна маршруты',
    'Завершить одну сторону',
    'Блокировать модификатор с большим весом',
    'Риск удаления доли',
    'Открытый маршрут',
    'Эти маршруты требуют ручного выбора и исключены из автоматического определения вероятности достижения цели. Проценты сравнивают следующий выбор модификатора, а не его числовые значения или выявляют кандидатов.',
    'Не удалось загрузить маршруты.',
    'Сохраните полезные модификаторы',
  ],
  th: [
    'เส้นทางเหว',
    'เสร็จด้านหนึ่ง',
    'บล็อกม็อดที่มีน้ำหนักสูง',
    'แบ่งปันความเสี่ยงในการถอดออก',
    'เปิดเส้นทาง',
    'เส้นทางเหล่านี้จำเป็นต้องเปิดเผยตัวเลือกด้วยตนเอง และไม่รวมอยู่ในความน่าจะเป็นของเป้าหมายอัตโนมัติ เปอร์เซ็นต์จะเปรียบเทียบตัวเลือกม็อดถัดไป ไม่ใช่การม้วนตัวเลขหรือเปิดเผยตัวเลือก',
    'ไม่สามารถโหลดเส้นทางได้',
    'รักษาม็อดที่มีประโยชน์ไว้',
  ],
}
type Chance = { targetWeight: number; totalWeight: number }
type Route = {
  kind: 'COMPLETE_SIDE' | 'BLOCK_HIGH_WEIGHT' | 'SHARE_REMOVAL_RISK'
  prepared: AbyssState
  nextCurrency: WorkbenchAction
  activeOmens: string[]
  modifierSelection: {
    baseline: Chance
    afterBlocking: Chance
    blockerModifierId: string
  } | null
  removalProtection: {
    safeBefore: number
    totalBefore: number
    safeAfter: number
    totalAfter: number
  } | null
}
const sides = {
  en: ['Prefix', 'Suffix'],
  ko: ['접두', '접미'],
  'zh-CN': ['前缀', '后缀'],
  'zh-TW': ['前綴', '後綴'],
  ja: ['プレフィックス', 'サフィックス'],
  es: ['Prefijo', 'Sufijo'],

  ru: ['Префикс', 'Суффикс'],
  th: ['พรีฟิกซ์', 'ซัฟฟิกซ์'],
}
const pct = (a: number, b: number) => `${((100 * a) / b).toFixed(2)}%`
type Props = {
  jobId: string
  nodeId: number
  base: string
  identity: string
  source: ConcreteItem
  branch?: FlowBranch | null
  onStart: (branch: FlowBranch) => void
  onTransition: (state: AbyssState, transition: FlowTransition) => void
  onContinue: (state: AbyssState, item: Item) => void
}
export function AutomaticAbyssRoutes({
  jobId,
  nodeId,
  base,
  identity,
  source,
  branch,
  onStart,
  onTransition,
  onContinue,
}: Props) {
  const { locale } = useI18n()
  const t = messages[locale]
  const query = useQuery({
    queryKey: ['automatic-abyss', identity, jobId, nodeId],
    staleTime: Infinity,
    retry: false,
    queryFn: async ({ signal }) => {
      const initial = await loadInitial(
        source.itemLevel,
        signal,
        base as WorkbenchBaseKey,
      )
      if (initial.rulesetIdentity !== identity) throw new Error(t[6])
      const response = await fetch(
        `/api/v2/crafting/method-searches/automatic/${encodeURIComponent(jobId)}/nodes/${nodeId}/abyss`,
        { signal },
      )
      verifyRulesetResponse(response, identity)
      if (!response.ok) throw new Error(t[6])
      const data: unknown = await response.json()
      if (
        !Array.isArray(data) ||
        !data.every((r: unknown) => {
          if (!r || typeof r !== 'object') return false
          const v = r as Record<string, unknown>
          return (
            [
              'COMPLETE_SIDE',
              'BLOCK_HIGH_WEIGHT',
              'SHARE_REMOVAL_RISK',
            ].includes(String(v.kind)) &&
            typeof v.nextCurrency === 'string' &&
            Array.isArray(v.activeOmens) &&
            v.activeOmens.every((o) => typeof o === 'string') &&
            validAbyssResponse('validate', v.prepared, source, initial) &&
            validAbyssResponse(
              'compare',
              v.modifierSelection,
              source,
              initial,
            ) &&
            (v.removalProtection === null ||
              (typeof v.removalProtection === 'object' &&
                Object.values(v.removalProtection).length === 4 &&
                Object.values(v.removalProtection).every(
                  (n) => Number.isSafeInteger(n) && Number(n) >= 0,
                ) &&
                Number(
                  (v.removalProtection as Record<string, unknown>).totalBefore,
                ) > 0 &&
                Number(
                  (v.removalProtection as Record<string, unknown>).totalAfter,
                ) > 0))
          )
        })
      )
        throw new Error(t[6])
      return { initial, routes: data as Route[] }
    },
  })
  return (
    <div>
      {query.isError && <p role="alert">{t[6]}</p>}
      {!branch &&
        query.data?.routes.map((r, i) => (
          <div key={i}>
            <p>
              {
                t[
                  r.kind === 'COMPLETE_SIDE'
                    ? 1
                    : r.kind === 'BLOCK_HIGH_WEIGHT'
                      ? 2
                      : 3
                ]
              }{' '}
              · {localizedAction(r.nextCurrency)}
            </p>
            <p>
              {
                sides[locale][
                  r.prepared.slots.find((s) => s.desecrated)?.side === 'PREFIX'
                    ? 0
                    : 1
                ]
              }
              {r.activeOmens.map((o) => ` · ${gameName(o, o)}`).join('')}
            </p>
            {r.modifierSelection && query.data && (
              <p>
                {localizedModifierText(
                  query.data.initial.modifiers[
                    r.modifierSelection.blockerModifierId
                  ]!,
                )}
              </p>
            )}
            {r.modifierSelection && (
              <p>
                {pct(
                  r.modifierSelection.baseline.targetWeight,
                  r.modifierSelection.baseline.totalWeight,
                )}
                (~
                {pct(
                  r.modifierSelection.afterBlocking.targetWeight,
                  r.modifierSelection.afterBlocking.totalWeight,
                )}
                )
              </p>
            )}
            {r.removalProtection && (
              <p>
                {t[7]}:{' '}
                {pct(
                  r.removalProtection.safeBefore,
                  r.removalProtection.totalBefore,
                )}{' '}
                →{' '}
                {pct(
                  r.removalProtection.safeAfter,
                  r.removalProtection.totalAfter,
                )}
              </p>
            )}
            <button
              onClick={() =>
                onStart({
                  sourceNode: nodeId,
                  source,
                  prepared: r.prepared,
                  action: r.nextCurrency,
                  omens: r.activeOmens,
                  steps: [
                    {
                      id: crypto.randomUUID(),
                      state: r.prepared,
                      transition: { operation: 'prepare' },
                    },
                  ],
                })
              }
            >
              {flowMessages[locale].prepare} · {localizedAction(r.nextCurrency)}
            </button>
          </div>
        ))}
      {branch && query.data && (
        <AbyssPanel
          key={JSON.stringify([branch.prepared, branch.action, branch.omens])}
          source={branch.source}
          initial={query.data.initial}
          prepared={branch.prepared}
          restoredState={branch.steps.at(-1)?.state}
          initialAction={branch.action}
          initialOmens={branch.omens}
          embedded
          onTransition={onTransition}
          onContinue={onContinue}
        />
      )}
      {!!query.data?.routes.length && <p>{t[5]}</p>}
    </div>
  )
}
