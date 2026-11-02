import type { Locale } from '../../../shared/i18n/i18n'

type Messages = {
  history: string
  close: string
  empty: string
  favorite: string
  unfavorite: string
  rename: string
  title: string
  save: string
  cancel: string
  remove: string
  confirm: string
  confirmBody: string
  undo: string
  deleted: string
  select: string
}
export const sessionLibraryMessages: Record<Locale, Messages> = {
  en: {
    history: 'History',
    close: 'Close history',
    empty: 'No crafting sessions yet.',
    favorite: 'Favorite',
    unfavorite: 'Remove favorite',
    rename: 'Rename',
    title: 'Session title',
    save: 'Save',
    cancel: 'Cancel',
    remove: 'Delete',
    confirm: 'Delete this session',
    confirmBody:
      'Delete only this session? You can undo immediately afterwards.',
    undo: 'Undo deletion',
    deleted: 'Session deleted',
    select: 'Select session',
  },
  ko: {
    history: 'History',
    close: '기록 닫기',
    empty: '제작 세션이 없습니다.',
    favorite: '즐겨찾기',
    unfavorite: '즐겨찾기 해제',
    rename: '제목 수정',
    title: '세션 제목',
    save: '저장',
    cancel: '취소',
    remove: '삭제',
    confirm: '이 세션 삭제',
    confirmBody: '이 세션만 삭제할까요? 삭제 후 바로 되돌릴 수 있습니다.',
    undo: '삭제 되돌리기',
    deleted: '세션 삭제됨',
    select: '세션 선택',
  },
  'zh-CN': {
    history: '历史',
    close: '关闭历史',
    empty: '暂无制作会话。',
    favorite: '收藏',
    unfavorite: '取消收藏',
    rename: '重命名',
    title: '会话标题',
    save: '保存',
    cancel: '取消',
    remove: '删除',
    confirm: '删除此会话',
    confirmBody: '仅删除此会话？删除后可立即撤销。',
    undo: '撤销删除',
    deleted: '会话已删除',
    select: '选择会话',
  },
  'zh-TW': {
    history: '歷史',
    close: '關閉歷史',
    empty: '尚無製作工作階段。',
    favorite: '收藏',
    unfavorite: '取消收藏',
    rename: '重新命名',
    title: '工作階段標題',
    save: '儲存',
    cancel: '取消',
    remove: '刪除',
    confirm: '刪除此工作階段',
    confirmBody: '僅刪除此工作階段？刪除後可立即復原。',
    undo: '復原刪除',
    deleted: '工作階段已刪除',
    select: '選擇工作階段',
  },
  ja: {
    history: '履歴',
    close: '履歴を閉じる',
    empty: 'クラフトセッションはありません。',
    favorite: 'お気に入り',
    unfavorite: 'お気に入りを解除',
    rename: '名前を変更',
    title: 'セッション名',
    save: '保存',
    cancel: 'キャンセル',
    remove: '削除',
    confirm: 'このセッションを削除',
    confirmBody: 'このセッションだけ削除しますか？削除直後に元に戻せます。',
    undo: '削除を元に戻す',
    deleted: 'セッションを削除しました',
    select: 'セッションを選択',
  },
  es: {
    history: 'Historial',
    close: 'Cerrar historial',
    empty: 'Aún no hay sesiones de fabricación.',
    favorite: 'Favorito',
    unfavorite: 'Quitar favorito',
    rename: 'Renombrar',
    title: 'Título de la sesión',
    save: 'Guardar',
    cancel: 'Cancelar',
    remove: 'Eliminar',
    confirm: 'Eliminar esta sesión',
    confirmBody:
      '¿Eliminar solo esta sesión? Puedes deshacerlo inmediatamente después.',
    undo: 'Deshacer eliminación',
    deleted: 'Sesión eliminada',
    select: 'Seleccionar sesión',
  },
}
