import type { Locale } from '../../../shared/i18n/i18n'
type Copy = {
  cycles: string
  local: string
  final: string
  initial: string
  uses: string
  state: string
  complete: string
}
export const methodSearchMessages: Record<Locale, Copy> = {
  en: {
    cycles: 'Method repetitions',
    local: 'Best immediate outcome',
    final: 'Recommended strategy',
    initial: 'Preparation',
    uses: 'Expected currency uses',
    state: 'Item state',
    complete: 'Calculation complete',
  },
  ko: {
    cycles: '방법 반복 횟수',
    local: '직계 결과 추천',
    final: '최종 전략 추천',
    initial: '초기 작업',
    uses: '예상 화폐 사용량',
    state: '아이템 상태',
    complete: '계산 완료',
  },
  'zh-CN': {
    cycles: '方法重复次数',
    local: '直接结果推荐',
    final: '最终策略推荐',
    initial: '准备步骤',
    uses: '预计通货用量',
    state: '物品状态',
    complete: '计算完成',
  },
  'zh-TW': {
    cycles: '方法重複次數',
    local: '直接結果推薦',
    final: '最終策略推薦',
    initial: '準備步驟',
    uses: '預計通貨用量',
    state: '物品狀態',
    complete: '計算完成',
  },
  ja: {
    cycles: '方法の繰り返し回数',
    local: '直近の結果の推奨',
    final: '最終戦略の推奨',
    initial: '準備',
    uses: '通貨の予想使用数',
    state: 'アイテム状態',
    complete: '計算完了',
  },
  es: {
    cycles: 'Repeticiones del método',
    local: 'Mejor resultado inmediato',
    final: 'Estrategia recomendada',
    initial: 'Preparación',
    uses: 'Uso esperado de moneda',
    state: 'Estado del objeto',
    complete: 'Cálculo completado',
  },
}
