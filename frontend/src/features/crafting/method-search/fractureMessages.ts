import type { Locale } from '../../../shared/i18n/i18n'
type Copy = {
  total: string
  baseline: string
  outcome: string
  roll: string
  completion: string
  branch: string
}
export const fractureMessages: Record<Locale, Copy> = {
  en: {
    total: 'Fracture strategy',
    baseline: 'Without fracturing',
    outcome: 'Fractured modifier',
    roll: 'Chance of this fracture',
    completion: 'Completion after this fracture',
    branch: 'Selected branch',
  },
  ko: {
    total: '분열 전략 전체',
    baseline: '분열 미사용',
    outcome: '분열된 옵션',
    roll: '이 옵션의 분열 확률',
    completion: '이 옵션 분열 후 완주 확률',
    branch: '선택한 분기',
  },
  'zh-CN': {
    total: '分裂策略整体',
    baseline: '不使用分裂',
    outcome: '被分裂的词缀',
    roll: '此词缀的分裂概率',
    completion: '此词缀分裂后的完成概率',
    branch: '所选分支',
  },
  'zh-TW': {
    total: '破裂策略整體',
    baseline: '不使用破裂',
    outcome: '被破裂的詞綴',
    roll: '此詞綴的破裂機率',
    completion: '此詞綴破裂後的完成機率',
    branch: '所選分支',
  },
  ja: {
    total: 'フラクチャー戦略全体',
    baseline: 'フラクチャーなし',
    outcome: 'フラクチャーされたモッド',
    roll: 'このモッドが選ばれる確率',
    completion: 'このモッドのフラクチャー後の達成率',
    branch: '選択した分岐',
  },
  es: {
    total: 'Estrategia de fractura',
    baseline: 'Sin fracturar',
    outcome: 'Modificador fracturado',
    roll: 'Probabilidad de esta fractura',
    completion: 'Probabilidad de completar tras esta fractura',
    branch: 'Rama seleccionada',
  },
  ru: {
    total: 'Стратегия раскола',
    baseline: 'Без раскола',
    outcome: 'Расколотое свойство',
    roll: 'Шанс раскола этого свойства',
    completion: 'Шанс завершения после такого раскола',
    branch: 'Выбранная ветвь',
  },
  th: {
    total: 'แผนการแตกร้าวโดยรวม',
    baseline: 'เมื่อไม่ใช้การแตกร้าว',
    outcome: 'ม็อดที่แตกร้าว',
    roll: 'โอกาสที่ม็อดนี้จะแตกร้าว',
    completion: 'โอกาสสำเร็จหลังจากม็อดนี้แตกร้าว',
    branch: 'เส้นทางที่เลือก',
  },
}
