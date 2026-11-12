import { useId, useRef, useState } from 'react'
import { useAccount } from './auth'
import { useI18n } from '../../shared/i18n/i18n'
const labels = {
  ko: [
    '세션 전체 삭제',
    '이 브라우저의 비로그인 세션',
    '계정 세션 · 모든 기기',
    '선택한 범위의 모든 세션을 삭제합니다. 되돌릴 수 없습니다. 즐겨찾기와 공유 글은 유지됩니다.',
    '삭제',
    '취소',
    '삭제하지 못했습니다. 다시 시도해 주세요.',
  ],
  en: [
    'Delete all sessions',
    'Guest sessions in this browser',
    'Account sessions · all devices',
    'Delete every session in the selected scope? This cannot be undone. Favorites and shared posts remain.',
    'Delete',
    'Cancel',
    'Could not delete. Please retry.',
  ],
  'zh-CN': [
    '删除全部会话',
    '此浏览器的访客会话',
    '账户会话 · 所有设备',
    '删除所选范围内的全部会话？无法撤销。收藏和分享帖子保留。',
    '删除',
    '取消',
    '删除失败，请重试。',
  ],
  'zh-TW': [
    '刪除全部工作階段',
    '此瀏覽器的訪客紀錄',
    '帳戶紀錄 · 所有裝置',
    '刪除所選範圍的全部紀錄？無法復原。收藏與分享貼文保留。',
    '刪除',
    '取消',
    '刪除失敗，請重試。',
  ],
  ja: [
    '全セッション削除',
    'このブラウザーのゲスト記録',
    'アカウント記録・全端末',
    '選択範囲の記録をすべて削除します。元に戻せません。お気に入りと共有投稿は残ります。',
    '削除',
    'キャンセル',
    '削除できませんでした。再試行してください。',
  ],
  es: [
    'Eliminar sesiones',
    'Sesiones locales de invitado',
    'Sesiones de la cuenta · todos los dispositivos',
    '¿Eliminar todas las sesiones seleccionadas? No se puede deshacer. Se conservan favoritos y publicaciones.',
    'Eliminar',
    'Cancelar',
    'No se pudo eliminar. Reintenta.',
  ],
  ru: [
    'Удалить все сессии',
    'Гостевые сессии браузера',
    'Сессии аккаунта · все устройства',
    'Удалить все выбранные сессии без возможности восстановления? Избранное и публикации останутся.',
    'Удалить',
    'Отмена',
    'Не удалось удалить. Повторите.',
  ],
  th: [
    'ลบเซสชันทั้งหมด',
    'เซสชันผู้เยี่ยมชมในเบราว์เซอร์นี้',
    'เซสชันบัญชี · ทุกอุปกรณ์',
    'ลบเซสชันทั้งหมดในขอบเขตที่เลือก? ยกเลิกไม่ได้ รายการโปรดและโพสต์ยังอยู่',
    'ลบ',
    'ยกเลิก',
    'ลบไม่สำเร็จ โปรดลองใหม่',
  ],
}
export function ClearSessions({
  browserOnly = false,
}: { browserOnly?: boolean } = {}) {
  const { locale } = useI18n(),
    auth = useAccount(),
    t = labels[locale]
  const group = useId()
  const dialog = useRef<HTMLDialogElement>(null)
  const [scope, setScope] = useState('browser'),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(false)
  return (
    <>
      <button onClick={() => dialog.current?.showModal()}>{t[0]}</button>
      <dialog ref={dialog} className="account-dialog">
        <h2>{t[0]}</h2>
        <p>{t[3]}</p>
        <label>
          <input
            type="radio"
            name={group}
            checked={scope === 'browser'}
            onChange={() => setScope('browser')}
          />
          {t[1]}
        </label>
        {auth?.user && !browserOnly && (
          <label>
            <input
              type="radio"
              name={group}
              checked={scope === 'account'}
              onChange={() => setScope('account')}
            />
            {t[2]}
          </label>
        )}
        {error && <p role="alert">{t[6]}</p>}
        <button
          disabled={busy}
          onClick={async () => {
            setBusy(true)
            try {
              await (
                await import('./librarySync')
              ).clearSessions(scope === 'account' ? auth!.user!.id : null)
              localStorage.setItem(
                'hephaistos.account.changed',
                crypto.randomUUID(),
              )
              location.reload()
            } catch {
              setError(true)
            } finally {
              setBusy(false)
            }
          }}
        >
          {t[4]}
        </button>
        <button disabled={busy} onClick={() => dialog.current?.close()}>
          {t[5]}
        </button>
      </dialog>
    </>
  )
}
