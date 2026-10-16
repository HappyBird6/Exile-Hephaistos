import type { Locale } from '../../../shared/i18n/i18n'
import { useI18n } from '../../../shared/i18n/i18n'
import type { CalculationState } from './calculationStatus'
const labels: Record<Locale, Record<CalculationState, string>> = {
  en: {
    preparing: 'Preparing search',
    calculating: 'Processing calculation request',
    queued: 'Waiting for the next calculation slice',
    stopping: 'Stopping… waiting for confirmation',
    resuming: 'Resuming… waiting for confirmation',
    stopped: 'Calculation stopped.',
    restored: 'Search restored — continue when ready',
    paused: 'Paused — calculation budget reached',
    partial: 'Partial results — unresolved methods remain',
    complete: 'Search complete',
    error: 'Calculation error — input preserved',
    expired: 'Saved search unavailable — input preserved',
  },
  ko: {
    preparing: '탐색 준비 중',
    calculating: '계산 요청 처리 중',
    queued: '다음 계산 대기',
    stopping: '중지 요청 중 · 확인 대기',
    resuming: '재개 요청 중 · 확인 대기',
    stopped: '계산 중지 완료',
    restored: '탐색 복원됨 · 계속 계산을 눌러 재개',
    paused: '일시중단 · 계산 예산 도달',
    partial: '부분 결과 · 미해결 방법 남음',
    complete: '탐색 완료',
    error: '계산 오류 · 입력 보존됨',
    expired: '저장된 탐색 사용 불가 · 입력 보존됨',
  },
  'zh-CN': {
    preparing: '正在准备搜索',
    calculating: '正在处理计算请求',
    queued: '等待下一段计算',
    stopping: '正在停止 · 等待确认',
    resuming: '正在恢复 · 等待确认',
    stopped: '计算已停止',
    restored: '搜索已恢复 · 可继续计算',
    paused: '已暂停 · 达到计算预算',
    partial: '部分结果 · 仍有未解决的方法',
    complete: '搜索完成',
    error: '计算错误 · 已保留输入',
    expired: '已保存的搜索不可用 · 已保留输入',
  },
  'zh-TW': {
    preparing: '正在準備搜尋',
    calculating: '正在處理計算請求',
    queued: '等待下一段計算',
    stopping: '正在停止 · 等待確認',
    resuming: '正在恢復 · 等待確認',
    stopped: '計算已停止',
    restored: '搜尋已恢復 · 可繼續計算',
    paused: '已暫停 · 達到計算預算',
    partial: '部分結果 · 仍有未解決的方法',
    complete: '搜尋完成',
    error: '計算錯誤 · 已保留輸入',
    expired: '已儲存的搜尋不可用 · 已保留輸入',
  },
  ja: {
    preparing: '探索を準備中',
    calculating: '計算リクエストを処理中',
    queued: '次の計算を待機中',
    stopping: '停止要求中・確認待ち',
    resuming: '再開要求中・確認待ち',
    stopped: '計算を停止しました',
    restored: '探索を復元しました・続行できます',
    paused: '一時停止・計算予算に到達',
    partial: '部分結果・未解決の方法があります',
    complete: '探索完了',
    error: '計算エラー・入力を保持',
    expired: '保存した探索は利用不可・入力を保持',
  },
  es: {
    preparing: 'Preparando búsqueda',
    calculating: 'Procesando solicitud de cálculo',
    queued: 'Esperando el siguiente cálculo',
    stopping: 'Deteniendo… esperando confirmación',
    resuming: 'Reanudando… esperando confirmación',
    stopped: 'Cálculo detenido',
    restored: 'Búsqueda restaurada — puedes continuar',
    paused: 'En pausa — presupuesto de cálculo alcanzado',
    partial: 'Resultados parciales — quedan métodos sin resolver',
    complete: 'Búsqueda completa',
    error: 'Error de cálculo — entrada conservada',
    expired: 'Búsqueda guardada no disponible — entrada conservada',
  },
}
const icons: Record<CalculationState, string> = {
  preparing: '◇',
  calculating: '◌',
  queued: '…',
  stopping: '◼',
  resuming: '↻',
  stopped: 'Ⅱ',
  restored: '↩',
  paused: 'Ⅱ',
  partial: '◐',
  complete: '✓',
  error: '!',
  expired: '!',
}
export function CalculationStatus({ state }: { state: CalculationState }) {
  const { locale } = useI18n()
  return (
    <div
      className={`calculation-status calculation-status--${state}`}
      role="status"
      aria-live="polite"
      aria-atomic="true"
      data-state={state}
    >
      <span className="calculation-status__icon" aria-hidden="true">
        {icons[state]}
      </span>
      <span>{labels[locale][state]}</span>
    </div>
  )
}
export function CalculationControlLabel({
  state,
}: {
  state: 'stopping' | 'resuming'
}) {
  const { locale } = useI18n()
  return <>{labels[locale][state]}</>
}
