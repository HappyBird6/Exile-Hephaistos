import type { Locale } from '../../../shared/i18n/i18n'

type Copy = {
  graph: string
  detail: string
  chance: string
  cost: string
  complete: string
}

export const routeTreeMessages: Record<Locale, Copy> = {
  en: {
    graph: 'Crafting route tree',
    detail: 'Selected state history',
    chance: 'Cumulative chance',
    cost: 'Cumulative cost',
    complete: 'Completed route',
  },
  ko: {
    graph: '제작 경로 트리',
    detail: '선택한 상태의 기록',
    chance: '누적 확률',
    cost: '누적 비용',
    complete: '완성 경로',
  },
  'zh-CN': {
    graph: '制作路线树',
    detail: '所选状态的记录',
    chance: '累计概率',
    cost: '累计费用',
    complete: '已完成的路线',
  },
  'zh-TW': {
    graph: '製作路線樹',
    detail: '所選狀態的紀錄',
    chance: '累積機率',
    cost: '累積費用',
    complete: '已完成的路線',
  },
  ja: {
    graph: 'クラフト経路ツリー',
    detail: '選択した状態の履歴',
    chance: '累積確率',
    cost: '累積コスト',
    complete: '完成した経路',
  },
  es: {
    graph: 'Árbol de rutas de fabricación',
    detail: 'Historial del estado seleccionado',
    chance: 'Probabilidad acumulada',
    cost: 'Coste acumulado',
    complete: 'Ruta completada',
  },
  ru: {
    graph: 'Дерево путей крафта',
    detail: 'История выбранного состояния',
    chance: 'Суммарная вероятность',
    cost: 'Суммарная стоимость',
    complete: 'Завершённый путь',
  },
  th: {
    graph: 'แผนผังเส้นทางการคราฟต์',
    detail: 'ประวัติของสถานะที่เลือก',
    chance: 'โอกาสสะสม',
    cost: 'ค่าใช้จ่ายสะสม',
    complete: 'เส้นทางที่เสร็จสมบูรณ์',
  },
}
