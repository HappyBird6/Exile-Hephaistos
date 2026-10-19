import type { Locale } from '../../../shared/i18n/i18n'
export const graphStateMessages: Record<
  Locale,
  {
    select: string
    added: string
    changed: string
    removed: string
    candidates: string
    step: string
  }
> = {
  en: {
    select: 'Select this item state',
    added: 'Added',
    changed: 'Changed',
    removed: 'Removed',
    candidates: 'Possible results — not a single item',
    step: 'Crafting action',
  },
  ko: {
    select: '이 아이템 상태 선택',
    added: '추가',
    changed: '변경',
    removed: '제거',
    candidates: '가능한 결과 후보 · 하나의 아이템이 아닙니다',
    step: '제작 작업',
  },
  'zh-CN': {
    select: '选择此物品状态',
    added: '新增',
    changed: '变更',
    removed: '移除',
    candidates: '可能结果，不是单个物品',
    step: '制作操作',
  },
  'zh-TW': {
    select: '選擇此物品狀態',
    added: '新增',
    changed: '變更',
    removed: '移除',
    candidates: '可能結果，不是單一物品',
    step: '製作操作',
  },
  ja: {
    select: 'このアイテム状態を選択',
    added: '追加',
    changed: '変更',
    removed: '除去',
    candidates: '結果候補・1つのアイテムではありません',
    step: 'クラフト操作',
  },
  es: {
    select: 'Seleccionar este estado',
    added: 'Añadido',
    changed: 'Cambiado',
    removed: 'Eliminado',
    candidates: 'Resultados posibles, no un único objeto',
    step: 'Acción de fabricación',
  },
}
