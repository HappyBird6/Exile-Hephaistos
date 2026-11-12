import type { Locale } from '../../shared/i18n/i18n'
const rows: Record<Locale, readonly [string, string]> = {
  ko: ['기록 관리', '세션 {sessions}개 · 베이스 즐겨찾기 {favorites}개'],
  en: ['Manage records', '{sessions} sessions · {favorites} base favorites'],
  'zh-CN': ['管理记录', '{sessions} 个会话 · {favorites} 个基底收藏'],
  'zh-TW': ['管理紀錄', '{sessions} 個紀錄 · {favorites} 個基底收藏'],
  ja: [
    '記録の管理',
    'セッション {sessions} 件・ベースのお気に入り {favorites} 件',
  ],
  es: [
    'Gestionar registros',
    '{sessions} sesiones · {favorites} bases favoritas',
  ],
  ru: [
    'Управление записями',
    'Сессий: {sessions} · Избранных баз: {favorites}',
  ],
  th: ['จัดการประวัติ', '{sessions} เซสชัน · {favorites} เบสโปรด'],
}
export const libraryMessages = (locale: Locale) => ({
  manage: rows[locale][0],
  count: rows[locale][1],
})
