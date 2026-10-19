import type { Locale } from '../../../shared/i18n/i18n'
export const guideMessages: Record<
  Locale,
  Record<
    | 'selected'
    | 'otherItems'
    | 'route'
    | 'alternative'
    | 'reset'
    | 'empty'
    | 'result'
    | 'start'
    | 'next'
    | 'details'
    | 'once'
    | 'within'
    | 'chance'
    | 'risk'
    | 'edit'
    | 'scope'
    | 'calculations'
    | 'records'
    | 'supported'
    | 'prepared',
    string
  >
> = {
  en: {
    selected: 'Selected item',
    otherItems: 'Other results',
    route: 'Crafting steps',
    alternative: 'Other routes',
    reset: 'Clear selection',
    empty: 'Choose a crafting method.',
    result: 'Result',
    start: 'Starting item',
    next: 'Next craft',
    details: 'Outcomes and probabilities',
    once: '1 attempt',
    within: 'Up to {n} attempts',
    chance: 'Chance of this result',
    risk: 'An existing modifier may be removed.',
    edit: 'Use as starting item',
    scope: 'Supported crafting',
    calculations: 'Probabilities and retries',
    records: 'Estimates and records',
    supported: 'Available currencies',
    prepared: 'Prepare first',
  },
  ko: {
    selected: '선택한 아이템',
    otherItems: '다른 결과 보기',
    route: '제작 순서',
    alternative: '다른 경로 보기',
    reset: '선택 해제',
    empty: '제작 방법을 선택하세요.',
    result: '결과',
    start: '시작 아이템',
    next: '다음 제작',
    details: '결과별 확률 보기',
    once: '1회 시도',
    within: '최대 {n}회 시도',
    chance: '이 결과를 얻을 확률',
    risk: '기존 옵션이 제거될 수 있습니다.',
    edit: '시작 아이템으로 사용',
    scope: '지원 범위',
    calculations: '확률과 반복',
    records: '예상 비용과 기록',
    supported: '사용 가능한 화폐',
    prepared: '먼저 준비',
  },
  'zh-CN': {
    selected: '所选物品',
    otherItems: '其他结果',
    route: '制作步骤',
    alternative: '其他路线',
    reset: '清除选择',
    empty: '请选择制作方法。',
    result: '结果',
    start: '起始物品',
    next: '下一步制作',
    details: '查看结果概率',
    once: '尝试1次',
    within: '最多尝试{n}次',
    chance: '获得此结果的概率',
    risk: '可能移除已有词缀。',
    edit: '用作起始物品',
    scope: '支持范围',
    calculations: '概率与重试',
    records: '预计成本与记录',
    supported: '可用通货',
    prepared: '先准备',
  },
  'zh-TW': {
    selected: '所選物品',
    otherItems: '其他結果',
    route: '製作步驟',
    alternative: '其他路線',
    reset: '清除選擇',
    empty: '請選擇製作方法。',
    result: '結果',
    start: '起始物品',
    next: '下一步製作',
    details: '查看結果機率',
    once: '嘗試1次',
    within: '最多嘗試{n}次',
    chance: '獲得此結果的機率',
    risk: '可能移除已有詞綴。',
    edit: '用作起始物品',
    scope: '支援範圍',
    calculations: '機率與重試',
    records: '預估成本與紀錄',
    supported: '可用通貨',
    prepared: '先準備',
  },
  ja: {
    selected: '選択したアイテム',
    otherItems: '他の結果',
    route: '製作手順',
    alternative: '他の経路',
    reset: '選択を解除',
    empty: '製作方法を選んでください。',
    result: '結果',
    start: '開始アイテム',
    next: '次の製作',
    details: '結果ごとの確率',
    once: '1回試行',
    within: '最大{n}回試行',
    chance: 'この結果を得る確率',
    risk: '既存のモッドが削除される場合があります。',
    edit: '開始アイテムにする',
    scope: '対応範囲',
    calculations: '確率と再試行',
    records: '予想費用と記録',
    supported: '使用可能な通貨',
    prepared: '先に準備',
  },
  es: {
    selected: 'Objeto seleccionado',
    otherItems: 'Otros resultados',
    route: 'Pasos de fabricación',
    alternative: 'Otras rutas',
    reset: 'Borrar selección',
    empty: 'Elige un método de fabricación.',
    result: 'Resultado',
    start: 'Objeto inicial',
    next: 'Siguiente paso',
    details: 'Resultados y probabilidades',
    once: '1 intento',
    within: 'Hasta {n} intentos',
    chance: 'Probabilidad de este resultado',
    risk: 'Puede eliminarse un modificador existente.',
    edit: 'Usar como objeto inicial',
    scope: 'Alcance compatible',
    calculations: 'Probabilidades y reintentos',
    records: 'Costes estimados y registros',
    supported: 'Monedas disponibles',
    prepared: 'Preparar primero',
  },
}
