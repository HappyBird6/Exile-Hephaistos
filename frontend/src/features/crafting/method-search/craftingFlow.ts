import type { AbyssState } from '../AbyssPanel'
import type { ConcreteItem, WorkbenchAction } from '../workbenchApi'
import type { CreateRequest, Item } from '../path-tree/types'
import { assertShape } from '../path-tree/validation'
import type { ItemCardData } from '../itemCardData'
import type { ItemPresentation } from '../path-tree/presentation'
import type { Locale } from '../../../shared/i18n/i18n'
import { localizedAction } from '../localizedCrafting'

export type FlowTransition = {
  operation:
    'prepare' | 'observe' | 'choose' | 'add' | 'apply' | 'restore' | 'select'
  action?: WorkbenchAction
  removal?: { slotId: string; probability: number }[]
}
export type FlowStep = {
  id: string
  state: AbyssState
  transition: FlowTransition
}
export type FlowBranch = {
  sourceNode: number
  source: ConcreteItem
  prepared: AbyssState
  action: WorkbenchAction
  omens: string[]
  steps: FlowStep[]
}
export type CraftingFlow = {
  origin: string
  request: CreateRequest
  prefix: FlowStep[]
  anchor: ConcreteItem | null
  carry: AbyssState | null
  branch: FlowBranch | null
}
export const flowMessages: Record<
  Locale,
  {
    prepare: string
    observe: string
    choose: string
    next: string
    resume: string
    unknown: string
    conditional: string
    current: string
    stored: string
    select: string
    history: string
    safe: string
  }
> = {
  en: {
    prepare: 'Desecrate',
    observe: 'Observe three reveal candidates',
    choose: 'Choose an observed candidate',
    next: 'Next crafting action',
    resume: 'Continue crafting search from this item',
    unknown: 'User choice · probability unknown',
    conditional:
      'Subsequent probabilities are conditional on this chosen item; no total probability or optimal cost is claimed.',
    current: 'Current crafting state',
    stored: 'Could not save crafting progress.',
    select: 'View crafting state',
    history: 'Crafting steps',
    safe: 'Apply the currency in the current crafting flow to preserve desecrated slots.',
  },
  ko: {
    prepare: '훼손',
    observe: '드러내기 후보 3개 관측',
    choose: '관측 후보 선택',
    next: '다음 제작',
    resume: '이 아이템에서 일반 제작 탐색 계속',
    unknown: '사용자 선택 · 확률 미정',
    conditional:
      '이후 확률은 선택한 아이템을 조건으로 계산합니다. 전체 성공률이나 최적 비용으로 합산하지 않습니다.',
    current: '현재 제작 상태',
    stored: '제작 진행을 저장하지 못했습니다.',
    select: '제작 상태 보기',
    history: '제작 단계',
    safe: '훼손 슬롯을 보존하려면 현재 제작 흐름에서 화폐를 적용하세요.',
  },
  'zh-CN': {
    prepare: '亵渎',
    observe: '观察三个揭示候选',
    choose: '选择已观察候选',
    next: '下一步制作',
    resume: '从此物品继续制作搜索',
    unknown: '用户选择 · 概率未知',
    conditional: '后续概率以所选物品为条件，不代表总成功率或最优成本。',
    current: '当前制作状态',
    stored: '无法保存制作进度。',
    select: '查看制作状态',
    history: '制作步骤',
    safe: '请在当前制作流程应用通货以保留亵渎词缀。',
  },
  'zh-TW': {
    prepare: '褻瀆',
    observe: '觀察三個揭示候選',
    choose: '選擇已觀察候選',
    next: '下一步製作',
    resume: '從此物品繼續製作搜尋',
    unknown: '使用者選擇 · 機率未知',
    conditional: '後續機率以所選物品為條件，不代表總成功率或最佳成本。',
    current: '目前製作狀態',
    stored: '無法儲存製作進度。',
    select: '查看製作狀態',
    history: '製作步驟',
    safe: '請在目前製作流程套用通貨以保留褻瀆詞綴。',
  },
  ja: {
    prepare: '冒涜',
    observe: '公開候補3個を確認',
    choose: '確認した候補を選択',
    next: '次のクラフト',
    resume: 'このアイテムからクラフト探索を続行',
    unknown: 'ユーザー選択・確率不明',
    conditional:
      '以降の確率は選択したアイテムが条件です。全体の成功率や最適費用には加算しません。',
    current: '現在のクラフト状態',
    stored: '進行を保存できませんでした。',
    select: 'クラフト状態を見る',
    history: 'クラフト手順',
    safe: '冒涜スロットを保持するには現在の工程で通貨を適用してください。',
  },
  es: {
    prepare: 'Profanar',
    observe: 'Observar tres candidatos revelados',
    choose: 'Elegir un candidato observado',
    next: 'Siguiente fabricación',
    resume: 'Continuar la búsqueda desde este objeto',
    unknown: 'Elección del usuario · probabilidad desconocida',
    conditional:
      'Las probabilidades posteriores dependen del objeto elegido; no indican éxito total ni coste óptimo.',
    current: 'Estado actual',
    stored: 'No se pudo guardar el progreso.',
    select: 'Ver estado',
    history: 'Pasos de fabricación',
    safe: 'Aplica la moneda en el flujo actual para conservar los espacios profanados.',
  },
}
export function materializeAbyss(
  state: AbyssState,
): (Item & ConcreteItem) | null {
  if (
    state.slots.some(
      (slot) => slot.minimumModifierLevel != null || slot.revealPool != null,
    )
  )
    return null
  if (state.offer || state.item.catalystQuality != null) return null
  return {
    ...state.item,
    implicits: state.item.implicits.map((m) => ({
      ...m,
      fractured: m.fractured ?? false,
    })),
    desecrated: state.slots
      .filter((s) => s.desecrated)
      .map((s) => ({
        id: s.id,
        side: s.side,
        modifierId: s.modifier?.modifierId ?? null,
      })),
    explicits: state.slots
      .filter((s) => s.modifier)
      .map((s) => ({
        ...s.modifier!,
        fractured: s.modifier!.fractured ?? false,
      })),
    augmentSockets: state.item.augmentSockets ?? null,
    catalystQuality: state.item.catalystQuality ?? null,
  } as Item & ConcreteItem
}
export function flowLabel(transition: FlowTransition, locale: Locale) {
  const t = flowMessages[locale]
  return transition.operation === 'apply' && transition.action
    ? localizedAction(transition.action)
    : transition.operation === 'choose'
      ? t.choose
      : transition.operation === 'observe'
        ? t.observe
        : transition.operation === 'restore'
          ? t.current
          : t.prepare
}
export function abyssCard(
  state: AbyssState,
  present: ItemPresentation,
  locale: Locale,
): ItemCardData {
  const known = {
    ...state.item,
    explicits: state.slots.flatMap((s) =>
      s.modifier
        ? [{ ...s.modifier, fractured: s.modifier.fractured ?? false }]
        : [],
    ),
  } as Item
  const card = present(known, locale)
  const labels: Record<Locale, string> = {
    en: 'Unrevealed desecrated modifier',
    ko: '미공개 훼손 속성',
    'zh-CN': '未揭示的亵渎词缀',
    'zh-TW': '未揭示的褻瀆詞綴',
    ja: '未公開の冒涜モッド',
    es: 'Modificador profanado sin revelar',
  }
  return {
    ...card,
    modifiers: [
      ...card.modifiers.map((line) =>
        state.slots.some(
          (s) => s.desecrated && s.modifier?.modifierId === line.id,
        )
          ? { ...line, kind: 'desecrated' as const }
          : line,
      ),
      ...state.slots
        .filter((s) => !s.modifier)
        .map((s) => ({
          id: s.id,
          kind: 'desecrated' as const,
          text: labels[locale],
          unrevealedDesecrated: true,
          affixLabel: s.side === 'PREFIX' ? 'P' : 'S',
        })),
    ],
  }
}
export function validFlow(
  value: unknown,
  origin: string,
): value is CraftingFlow {
  try {
    const v = value as CraftingFlow
    if (
      !v ||
      v.origin !== origin ||
      !Array.isArray(v.prefix) ||
      v.prefix.length > 200
    )
      return false
    assertShape('createRequest', v.request)
    const concrete = (item: ConcreteItem) =>
      assertShape('item', {
        ...item,
        implicits: item.implicits.map((m) => ({
          ...m,
          fractured: m.fractured ?? false,
        })),
        explicits: item.explicits.map((m) => ({
          ...m,
          fractured: m.fractured ?? false,
        })),
        augmentSockets: item.augmentSockets ?? null,
        catalystQuality: item.catalystQuality ?? null,
      })
    if (v.anchor) concrete(v.anchor)
    if (v.branch) concrete(v.branch.source)

    const check = (s: AbyssState) => {
      if (
        !s ||
        !Array.isArray(s.slots) ||
        s.slots.length > 6 ||
        !s.slots.every(
          (x) =>
            typeof x.id === 'string' &&
            ['PREFIX', 'SUFFIX'].includes(x.side) &&
            typeof x.desecrated === 'boolean' &&
            (x.modifier !== null || x.desecrated),
        )
      )
        return false
      if (
        s.item.baseItemId !== v.request.start.item.baseItemId ||
        s.item.snapshotId !== v.request.start.item.snapshotId ||
        s.item.itemLevel !== v.request.start.item.itemLevel ||
        s.item.rarity !== 'RARE' ||
        s.item.explicits.length !== 0 ||
        new Set(s.slots.map((x) => x.id)).size !== s.slots.length ||
        ['PREFIX', 'SUFFIX'].some(
          (side) => s.slots.filter((x) => x.side === side).length > 3,
        )
      )
        return false
      if (
        s.offer &&
        (!Array.isArray(s.offer.candidates) ||
          s.offer.candidates.length !== 3 ||
          !s.slots.some((x) => x.id === s.offer!.slotId && x.desecrated))
      )
        return false
      if (s.offer)
        for (const candidate of s.offer.candidates)
          assertShape('item', {
            ...s.item,
            explicits: [candidate],
            augmentSockets: s.item.augmentSockets ?? null,
            catalystQuality: s.item.catalystQuality ?? null,
          })
      assertShape('item', {
        ...s.item,
        explicits: s.slots.flatMap((x) =>
          x.modifier
            ? [{ ...x.modifier, fractured: x.modifier.fractured ?? false }]
            : [],
        ),
        implicits: s.item.implicits.map((m) => ({
          ...m,
          fractured: m.fractured ?? false,
        })),
        augmentSockets: s.item.augmentSockets ?? null,
        catalystQuality: s.item.catalystQuality ?? null,
      })
      return true
    }
    const steps = [...v.prefix, ...(v.branch?.steps ?? [])]
    return (
      steps.length <= 200 &&
      new Set(steps.map((s) => s.id)).size === steps.length &&
      steps.every(
        (s) =>
          typeof s.id === 'string' &&
          s.id.length > 0 &&
          check(s.state) &&
          (!s.transition.removal ||
            (Array.isArray(s.transition.removal) &&
              s.transition.removal.every(
                (r) =>
                  typeof r.slotId === 'string' &&
                  Number.isFinite(r.probability) &&
                  r.probability > 0 &&
                  r.probability <= 1,
              ))) &&
          ['prepare', 'observe', 'choose', 'add', 'apply', 'restore'].includes(
            s.transition.operation,
          ),
      ) &&
      (!v.carry || check(v.carry)) &&
      (!v.branch ||
        (check(v.branch.prepared) &&
          Number.isSafeInteger(v.branch.sourceNode) &&
          v.branch.sourceNode >= 0 &&
          Array.isArray(v.branch.omens) &&
          v.branch.omens.every((o) => typeof o === 'string')))
    )
  } catch {
    return false
  }
}

export function candidateState(state: AbyssState, index: number): AbyssState {
  const candidate = state.offer?.candidates[index]
  if (!candidate || !state.offer) return state
  return {
    ...state,
    offer: null,
    slots: state.slots.map((slot) =>
      slot.id === state.offer!.slotId ? { ...slot, modifier: candidate } : slot,
    ),
  }
}

export const flowExportMessages: Record<Locale, string> = {
  en: 'The scenario code contains the current automatic search segment. Desecration steps are restored in this browser only.',
  ko: '시나리오 코드는 현재 일반 탐색 구간을 담습니다. 훼손 진행 단계는 이 브라우저에서 복원됩니다.',
  'zh-CN': '方案代码仅包含当前自动搜索阶段。亵渎步骤仅在此浏览器中恢复。',
  'zh-TW': '方案代碼僅包含目前自動搜尋階段。褻瀆步驟僅在此瀏覽器中恢復。',
  ja: 'シナリオコードには現在の自動探索区間が含まれます。冒涜の手順はこのブラウザーでのみ復元されます。',
  es: 'El código incluye el tramo actual de búsqueda automática. Los pasos de profanación se restauran solo en este navegador.',
}
