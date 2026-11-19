import type { GoalFilterLanguage } from './i18n'
import type { GroupType } from './types'

export const tradeGroupNames: Record<GroupType, string> = {
  AND: 'And',
  NOT: 'Not',
  IF: 'If',
  COUNT: 'Count',
  WEIGHTED_V1: 'Weighted Sum',
  WEIGHTED_V2: 'Weighted Sum v2',
}

type Copy = {
  rarity: string
  normal: string
  magic: string
  rare: string
  blocked: string
  limits: string
  explicit: string
  implicit: string
  pseudo: string
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
    explicit: 'Explicit modifier',
    implicit: 'Implicit modifier',
    pseudo: 'Combined total',
  },
  ko: {
    rarity: '시작 아이템 희귀도',
    normal: '일반',
    magic: '마법',
    rare: '희귀',
    blocked:
      '현재 속성 수 또는 특수 속성과 충돌합니다. 충돌하는 속성을 먼저 제거하세요.',
    limits: '접두 / 접미 한도',
    explicit: '일반 속성',
    implicit: '고정 속성',
    pseudo: '유사 능력치 합계',
  },
  'zh-CN': {
    rarity: '起始物品稀有度',
    normal: '普通',
    magic: '魔法',
    rare: '稀有',
    blocked: '与当前词缀数量或特殊属性冲突。请先移除冲突词缀。',
    limits: '前缀 / 后缀上限',
    explicit: '显式词缀',
    implicit: '固定词缀',
    pseudo: '相似属性总和',
  },
  'zh-TW': {
    rarity: '起始物品稀有度',
    normal: '普通',
    magic: '魔法',
    rare: '稀有',
    blocked: '與目前詞綴數量或特殊屬性衝突。請先移除衝突詞綴。',
    limits: '前綴 / 後綴上限',
    explicit: '一般詞綴',
    implicit: '固定詞綴',
    pseudo: '相似屬性總和',
  },
  ja: {
    rarity: '開始アイテムのレアリティ',
    normal: 'ノーマル',
    magic: 'マジック',
    rare: 'レア',
    blocked:
      '現在のモッド数または特殊属性と競合します。競合するモッドを先に削除してください。',
    limits: 'プレフィックス / サフィックス上限',
    explicit: '明示モッド',
    implicit: '暗黙モッド',
    pseudo: '類似ステータスの合計',
  },
  es: {
    rarity: 'Rareza inicial',
    normal: 'Normal',
    magic: 'Mágico',
    rare: 'Raro',
    blocked:
      'Incompatible con los modificadores o propiedades actuales. Elimina primero los modificadores incompatibles.',
    limits: 'Límites de prefijos / sufijos',
    explicit: 'Modificador explícito',
    implicit: 'Modificador implícito',
    pseudo: 'Total combinado',
  },

  ru: {
    rarity: 'Стартовая редкость',
    normal: 'Нормальный',
    magic: 'Магический',
    rare: 'Редкий',
    blocked:
      'Недоступно с текущими модификаторами или специальными свойствами. Сначала удалите конфликтующие модификаторы.',
    limits: 'Ограничения на префиксы/суффиксы',
    explicit: 'Явный модификатор',
    implicit: 'Неявный модификатор',
    pseudo: 'Общий итог',
  },
  th: {
    rarity: 'เริ่มหายาก.',
    normal: 'ปกติ',
    magic: 'เมจิก',
    rare: 'แรร์',
    blocked:
      'ไม่สามารถใช้งานได้กับม็อดปัจจุบันหรือคุณสมบัติพิเศษ ลบม็อดที่ขัดแย้งกันออกก่อน',
    limits: 'พรีฟิกซ์ / ซัฟฟิกซ์จำกัด',
    explicit: 'ม็อดที่ชัดเจน',
    implicit: 'ม็อดโดยนัย',
    pseudo: 'รวมกันทั้งสิ้น',
  },
}
