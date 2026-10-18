import { useI18n } from '../../../shared/i18n/i18n'
import type { AutomaticView } from './automaticApi'
import type { Locale } from '../../../shared/i18n/i18n'

const disclosureTitle: Record<Locale, string> = {
  en: 'Failure / retries / costs',
  ko: '실패·반복·비용 조건',
  'zh-CN': '失败、重试与费用',
  'zh-TW': '失敗、重試與費用',
  ja: '失敗・再試行・費用',
  es: 'Fallos, reintentos y costes',
}
const noPlan: Record<Locale, string> = {
  en: 'No route found within supported plans.',
  ko: '지원하는 계획 범위에서 해법을 찾지 못했습니다.',
  'zh-CN': '支持的计划范围内未找到路线。',
  'zh-TW': '支援的計畫範圍內未找到路線。',
  ja: '対応する計画の範囲では経路が見つかりませんでした。',
  es: 'No se encontro una ruta dentro de los planes compatibles.',
}

const copy = {
  en: [
    'Plan steps, not affix count or currency uses.',
    'Completion covers supported milestone plans; global optimality is not proven.',
    'Failure stops this method. Recovery or a new starting item has a separate cost. Repeated probabilities use state transitions; identical single-affix Chaos retries share one verified pool.',
    'Target rarities',
    'Maximum resource limit reached; recalculate with a narrower goal.',
  ],
  ko: [
    '세로 단계는 제작 방법 단계이며 속성 수나 화폐 횟수가 아닙니다.',
    '완료는 지원하는 목표 획득 계획의 탐색 완료이며 전역 최적성을 보장하지 않습니다.',
    '실패는 해당 방법을 중단합니다. 복구·새 시작 아이템 비용은 별도입니다. 반복 확률은 상태 전이로 계산하며 단일 속성 카오스는 동일 후보 풀을 확인한 경우만 재시도합니다.',
    '목표 희귀도',
    '최대 자원 한도입니다. 목표 범위를 좁혀 다시 계산하세요.',
  ],
  'zh-CN': [
    '纵向层级是制作方法阶段，不是词缀或货币次数。',
    '完成仅指受支持的目标计划，不保证全局最优。',
    '失败结束当前方法；恢复和重启成本另计。重复概率使用状态转移，单词缀混沌仅在候选池相同时重试。',
    '目标稀有度',
    '已达资源上限，请缩小目标后重算。',
  ],
  'zh-TW': [
    '縱向層級是製作方法階段，不是詞綴或通貨次數。',
    '完成僅指支援的目標計畫，不保證全域最佳。',
    '失敗結束目前方法；恢復與重啟成本另計。重複機率使用狀態轉移，單詞綴混沌僅在候選池相同時重試。',
    '目標稀有度',
    '已達資源上限，請縮小目標後重算。',
  ],
  ja: [
    '縦の段階は製作方法の段階で、修飾子数や通貨回数ではありません。',
    '完了は対応する目標計画の探索完了です。大域的最適性は保証しません。',
    '失敗時はその方法を終了します。復旧と再開始の費用は別です。反復確率は状態遷移を使用し、単一修飾子の混沌は同一候補プールの場合のみ再試行します。',
    '目標レアリティ',
    '資源上限です。目標を絞って再計算してください。',
  ],
  es: [
    'Los niveles son etapas del metodo, no afijos ni usos de moneda.',
    'Completo solo dentro de los planes compatibles; no garantiza un optimo global.',
    'El fallo termina el metodo. Recuperar o reiniciar tiene un coste separado. Las repeticiones usan transiciones de estado; Caos con un afijo solo reintenta con el mismo conjunto verificado.',
    'Rarezas objetivo',
    'Limite maximo de recursos; reduce el objetivo y recalcula.',
  ],
}

export function PlanningDisclosure({
  view,
  rarities,
}: {
  view: AutomaticView | null
  rarities: string[]
}) {
  const { locale } = useI18n()
  const text = copy[locale]
  return (
    <aside className="method-map__scope">
      <p>
        {text[3]}: {rarities.join(', ')}
      </p>
      <p>
        {text[0]} {text[1]}
      </p>
      {view?.reason === 'NO_PLAN_IN_SUPPORTED_SCOPE' && (
        <p role="status">{noPlan[locale]}</p>
      )}
      <details>
        <summary>{disclosureTitle[locale]}</summary>
        <p>{text[2]}</p>
      </details>
      {view?.status === 'PAUSED' && view.canResume === false && (
        <p role="status">{text[4]}</p>
      )}
    </aside>
  )
}
