import type { GoalFilterLanguage } from './i18n'
import type { GroupType } from './types'

type Copy = {
  rarity: string
  normal: string
  magic: string
  rare: string
  blocked: string
  limits: string
  advanced: string
  explicit: string
  implicit: string
  pseudo: string
  groups: Record<GroupType, string>
}
export const setupMessages: Record<GoalFilterLanguage, Copy> = {
  en: {
    rarity: 'Starting rarity',
    normal: 'Normal',
    magic: 'Magic',
    rare: 'Rare',
    blocked:
      'Unavailable with the current modifiers or special properties. Remove conflicting modifiers first.',
    limits: 'Prefix / suffix limits',
    advanced: 'Advanced conditions',
    explicit: 'Explicit modifier',
    implicit: 'Implicit modifier',
    pseudo: 'Combined total',
    groups: {
      AND: 'All required',
      COUNT: 'Match a number of conditions',
      NOT: 'Exclude matches',
      IF: 'If present, within range',
      WEIGHTED_V1: 'Weighted total · all present in range',
      WEIGHTED_V2: 'Weighted total · matching rows',
    },
  },
  ko: {
    rarity: '시작 아이템 희귀도',
    normal: '일반',
    magic: '마법',
    rare: '희귀',
    blocked:
      '현재 속성 수 또는 특수 속성과 충돌합니다. 충돌하는 속성을 먼저 제거하세요.',
    limits: '접두 / 접미 한도',
    advanced: '고급 조건',
    explicit: '일반 속성',
    implicit: '고정 속성',
    pseudo: '유사 능력치 합계',
    groups: {
      AND: '모두 꼭 필요',
      COUNT: '여러 조건 중 지정 개수 충족',
      NOT: '일치하는 조건 제외',
      IF: '있다면 범위 충족',
      WEIGHTED_V1: '가중 합계 · 있는 속성 모두 범위 충족',
      WEIGHTED_V2: '가중 합계 · 충족한 속성만',
    },
  },
  'zh-CN': {
    rarity: '起始物品稀有度',
    normal: '普通',
    magic: '魔法',
    rare: '稀有',
    blocked: '与当前词缀数量或特殊属性冲突。请先移除冲突词缀。',
    limits: '前缀 / 后缀上限',
    advanced: '高级条件',
    explicit: '显式词缀',
    implicit: '固定词缀',
    pseudo: '相似属性总和',
    groups: {
      AND: '全部必需',
      COUNT: '满足指定数量的条件',
      NOT: '排除匹配条件',
      IF: '存在时须在范围内',
      WEIGHTED_V1: '加权总和 · 已有属性均在范围内',
      WEIGHTED_V2: '加权总和 · 仅匹配属性',
    },
  },
  'zh-TW': {
    rarity: '起始物品稀有度',
    normal: '普通',
    magic: '魔法',
    rare: '稀有',
    blocked: '與目前詞綴數量或特殊屬性衝突。請先移除衝突詞綴。',
    limits: '前綴 / 後綴上限',
    advanced: '進階條件',
    explicit: '一般詞綴',
    implicit: '固定詞綴',
    pseudo: '相似屬性總和',
    groups: {
      AND: '全部必需',
      COUNT: '符合指定數量的條件',
      NOT: '排除符合條件',
      IF: '存在時須在範圍內',
      WEIGHTED_V1: '加權總和 · 已有屬性均在範圍內',
      WEIGHTED_V2: '加權總和 · 僅符合屬性',
    },
  },
  ja: {
    rarity: '開始アイテムのレアリティ',
    normal: 'ノーマル',
    magic: 'マジック',
    rare: 'レア',
    blocked:
      '現在のモッド数または特殊属性と競合します。競合するモッドを先に削除してください。',
    limits: 'プレフィックス / サフィックス上限',
    advanced: '詳細条件',
    explicit: '明示モッド',
    implicit: '暗黙モッド',
    pseudo: '類似ステータスの合計',
    groups: {
      AND: 'すべて必須',
      COUNT: '指定数の条件を満たす',
      NOT: '一致する条件を除外',
      IF: '存在する場合は範囲内',
      WEIGHTED_V1: '加重合計 · 存在する全属性が範囲内',
      WEIGHTED_V2: '加重合計 · 一致した属性のみ',
    },
  },
  es: {
    rarity: 'Rareza inicial',
    normal: 'Normal',
    magic: 'Mágico',
    rare: 'Raro',
    blocked:
      'Incompatible con los modificadores o propiedades actuales. Elimina primero los modificadores incompatibles.',
    limits: 'Límites de prefijos / sufijos',
    advanced: 'Condiciones avanzadas',
    explicit: 'Modificador explícito',
    implicit: 'Modificador implícito',
    pseudo: 'Total combinado',
    groups: {
      AND: 'Todos obligatorios',
      COUNT: 'Cumplir un número de condiciones',
      NOT: 'Excluir coincidencias',
      IF: 'Si existe, dentro del rango',
      WEIGHTED_V1: 'Total ponderado · presentes en rango',
      WEIGHTED_V2: 'Total ponderado · filas coincidentes',
    },
  },
}
