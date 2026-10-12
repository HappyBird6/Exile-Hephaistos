import type { Locale } from '../../../shared/i18n/i18n'
type Copy = {
  cycles: string
  local: string
  final: string
  initial: string
  uses: string
  state: string
  complete: string
  loss: string
  checkpoint: string
}
export const methodSearchMessages: Record<Locale, Copy> = {
  en: {
    loss: 'Target modifier lost',
    checkpoint: 'Intermediate state',
    cycles: 'Method repetitions',
    local: 'Best immediate outcome',
    final: 'Recommended strategy',
    initial: 'Preparation',
    uses: 'Expected currency uses',
    state: 'Item state',
    complete: 'Calculation complete',
  },
  ko: {
    loss: '목표 속성 손실',
    checkpoint: '중간 상태',
    cycles: '방법 반복 횟수',
    local: '직계 결과 추천',
    final: '최종 전략 추천',
    initial: '초기 작업',
    uses: '예상 화폐 사용량',
    state: '아이템 상태',
    complete: '계산 완료',
  },
  'zh-CN': {
    loss: '目标词缀丢失',
    checkpoint: '中间状态',
    cycles: '方法重复次数',
    local: '直接结果推荐',
    final: '最终策略推荐',
    initial: '准备步骤',
    uses: '预计通货用量',
    state: '物品状态',
    complete: '计算完成',
  },
  'zh-TW': {
    loss: '目標詞綴遺失',
    checkpoint: '中間狀態',
    cycles: '方法重複次數',
    local: '直接結果推薦',
    final: '最終策略推薦',
    initial: '準備步驟',
    uses: '預計通貨用量',
    state: '物品狀態',
    complete: '計算完成',
  },
  ja: {
    loss: '目標モッドを失った状態',
    checkpoint: '中間状態',
    cycles: '方法の繰り返し回数',
    local: '直近の結果の推奨',
    final: '最終戦略の推奨',
    initial: '準備',
    uses: '通貨の予想使用数',
    state: 'アイテム状態',
    complete: '計算完了',
  },
  es: {
    loss: 'Modificador objetivo perdido',
    checkpoint: 'Estado intermedio',
    cycles: 'Repeticiones del método',
    local: 'Mejor resultado inmediato',
    final: 'Estrategia recomendada',
    initial: 'Preparación',
    uses: 'Uso esperado de moneda',
    state: 'Estado del objeto',
    complete: 'Cálculo completado',
  },
}
