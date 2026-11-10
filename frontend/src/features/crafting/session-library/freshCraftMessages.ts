import type { Locale } from '../../../shared/i18n/i18n'
export const freshCraftMessages: Record<
  Locale,
  { fromItem: string; fromCurrent: string; reset: string }
> = {
  en: {
    reset: 'Reset',
    fromItem: 'Start a new craft with this item',
    fromCurrent: 'Start a new craft from the current state',
  },
  ko: {
    reset: '초기화',
    fromItem: '이 아이템으로 새 제작',
    fromCurrent: '현재 상태에서 새 제작',
  },
  'zh-CN': {
    reset: '重置',
    fromItem: '使用此物品开始新制作',
    fromCurrent: '从当前状态开始新制作',
  },
  'zh-TW': {
    reset: '重設',
    fromItem: '使用此物品開始新製作',
    fromCurrent: '從目前狀態開始新製作',
  },
  ja: {
    reset: 'リセット',
    fromItem: 'このアイテムで新しくクラフト',
    fromCurrent: '現在の状態から新しくクラフト',
  },
  es: {
    reset: 'Restablecer',
    fromItem: 'Nueva fabricación con este objeto',
    fromCurrent: 'Nueva fabricación desde el estado actual',
  },
  ru: {
    reset: 'Сбросить',
    fromItem: 'Начать новое создание с этим предметом',
    fromCurrent: 'Начать новое создание из текущего состояния',
  },
  th: {
    reset: 'รีเซ็ต',
    fromItem: 'เริ่มคราฟต์ใหม่ด้วยไอเทมนี้',
    fromCurrent: 'เริ่มคราฟต์ใหม่จากสถานะปัจจุบัน',
  },
}
