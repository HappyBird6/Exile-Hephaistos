import type { Locale } from '../../../shared/i18n/i18n'
export const baseFavoriteMessages: Record<
  Locale,
  { list: string; add: string; remove: string; saved: string; error: string }
> = {
  ko: {
    list: '베이스 즐겨찾기',
    add: '이 아이템 베이스 즐겨찾기',
    remove: '베이스 즐겨찾기 제거',
    saved: '베이스 즐겨찾기에 저장했습니다.',
    error: '베이스 즐겨찾기를 저장하거나 불러오지 못했습니다.',
  },
  en: {
    list: 'Favorite bases',
    add: 'Save item as a favorite base',
    remove: 'Remove favorite base',
    saved: 'Base saved to favorites.',
    error: 'Could not save or load favorite bases.',
  },
  'zh-CN': {
    list: '收藏基底',
    add: '将此物品收藏为基底',
    remove: '移除收藏基底',
    saved: '已收藏基底。',
    error: '无法保存或加载收藏基底。',
  },
  'zh-TW': {
    list: '收藏基底',
    add: '將此物品收藏為基底',
    remove: '移除收藏基底',
    saved: '已收藏基底。',
    error: '無法儲存或載入收藏基底。',
  },
  ja: {
    list: 'お気に入りベース',
    add: 'このアイテムをベースとして保存',
    remove: 'お気に入りベースを削除',
    saved: 'ベースを保存しました。',
    error: 'お気に入りベースを保存または読み込めませんでした。',
  },
  es: {
    list: 'Bases favoritas',
    add: 'Guardar objeto como base favorita',
    remove: 'Quitar base favorita',
    saved: 'Base guardada en favoritos.',
    error: 'No se pudieron guardar o cargar las bases favoritas.',
  },
  ru: {
    list: 'Избранные основы',
    add: 'Сохранить предмет как избранную основу',
    remove: 'Удалить избранную основу',
    saved: 'Основа добавлена в избранное.',
    error: 'Не удалось сохранить или загрузить избранные основы.',
  },
  th: {
    list: 'เบสที่ชื่นชอบ',
    add: 'บันทึกไอเท็มนี้เป็นเบสที่ชื่นชอบ',
    remove: 'ลบเบสที่ชื่นชอบ',
    saved: 'บันทึกเบสที่ชื่นชอบแล้ว',
    error: 'ไม่สามารถบันทึกหรือโหลดเบสที่ชื่นชอบได้',
  },
}
