import type { Locale } from '../../../shared/i18n/i18n'

export const methodMessages: Record<
  Locale,
  {
    start: string
    depth: string
    until: string
    stopped: string
    active: string
    omitted: string
    unknown: string
    dead: string
    single: string
    outcomes: string
  }
> = {
  en: {
    start: 'Starting item',
    depth: 'After one crafting method',
    until: 'Repeat until the goal or the selected currency limit',
    stopped: 'Stopped at this item',
    active: 'Still retrying at this item',
    omitted: 'Other item states not shown',
    unknown: 'Unresolved',
    dead: 'Cannot continue',
    single:
      'Each arrow repeats one method. Currency uses do not add crafting stages.',
    outcomes: 'Item outcomes — scroll horizontally to explore branches',
  },
  ko: {
    start: '시작 아이템',
    depth: '제작 방법 1단계 후',
    until: '목표 달성 또는 선택한 화폐 사용 한도까지 반복',
    stopped: '이 아이템에서 목표 달성 후 종료',
    active: '이 아이템에서 재시도 중',
    omitted: '상세가 표시되지 않은 다른 아이템 상태',
    unknown: '미해결',
    dead: '더 진행할 수 없음',
    single:
      '각 화살표는 한 방법의 반복입니다. 화폐 사용 횟수는 제작 단계 수가 아닙니다.',
    outcomes: '아이템 결과 — 가로로 스크롤하여 분기를 확인하세요',
  },
  'zh-CN': {
    start: '起始物品',
    depth: '一个制作方法之后',
    until: '重复直到达成目标或达到所选通货使用上限',
    stopped: '在此物品达成目标后停止',
    active: '仍在此物品上重试',
    omitted: '未显示详情的其他物品状态',
    unknown: '尚未确定',
    dead: '无法继续',
    single: '每条箭头重复一种方法。通货使用次数不是制作阶段数。',
    outcomes: '物品结果 — 横向滚动查看分支',
  },
  'zh-TW': {
    start: '起始物品',
    depth: '一個製作方法之後',
    until: '重複直到達成目標或達到所選通貨使用上限',
    stopped: '在此物品達成目標後停止',
    active: '仍在此物品上重試',
    omitted: '未顯示詳情的其他物品狀態',
    unknown: '尚未確定',
    dead: '無法繼續',
    single: '每條箭頭重複一種方法。通貨使用次數不是製作階段數。',
    outcomes: '物品結果 — 橫向捲動查看分支',
  },
  ja: {
    start: '開始アイテム',
    depth: '制作方法を1段階実行後',
    until: '目標達成または選択した通貨使用上限まで繰り返す',
    stopped: 'このアイテムで目標を達成して終了',
    active: 'このアイテムで再試行中',
    omitted: '詳細を表示していない他のアイテム状態',
    unknown: '未確定',
    dead: '続行できません',
    single:
      '各矢印は一つの方法の繰り返しです。通貨使用回数は制作段階数ではありません。',
    outcomes: 'アイテムの結果 — 横にスクロールして分岐を確認',
  },
  es: {
    start: 'Objeto inicial',
    depth: 'Después de un método de fabricación',
    until:
      'Repetir hasta alcanzar el objetivo o el límite de moneda seleccionado',
    stopped: 'Objetivo alcanzado en este objeto',
    active: 'Se sigue intentando con este objeto',
    omitted: 'Otros estados sin detalles visibles',
    unknown: 'Sin resolver',
    dead: 'No se puede continuar',
    single:
      'Cada flecha repite un método. Los usos de moneda no añaden etapas de fabricación.',
    outcomes: 'Resultados — desplázate horizontalmente para explorar las ramas',
  },
}
