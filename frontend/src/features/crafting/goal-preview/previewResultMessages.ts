import type { Locale } from '../../../shared/i18n/i18n'

type Copy = {
  conditions: string
  examples: string
  sources: string
  tiers: string
  presence: string
  loading: string
  empty: string
  qualityProjection: string
  retry: string
}
export const previewResultMessages: Record<Locale, Copy> = {
  ko: {
    qualityProjection:
      '기폭제 퀄리티가 적용된 옵션 수치는 아직 목표와 대조할 수 없습니다.',
    conditions: '목표 충족 조건',
    examples: '완성 아이템 예시',
    sources: '획득처',
    tiers: '목표를 충족하는 티어 조합',
    presence: '수치 제한 없음',
    loading: '목표에 맞는 아이템을 찾는 중…',
    empty: '현재 조건으로 확인된 완성 예시가 없습니다.',
    retry: '다시 시도',
  },
  en: {
    qualityProjection:
      'Modifier values affected by catalyst quality cannot be checked against the goal yet.',
    conditions: 'Goal conditions',
    examples: 'Matching item examples',
    sources: 'Sources',
    tiers: 'Matching tier combinations',
    presence: 'Any value',
    loading: 'Finding matching items…',
    empty: 'No matching example was found for these conditions.',
    retry: 'Try again',
  },
  'zh-CN': {
    qualityProjection: '暂时无法按目标核对受催化剂品质影响的词缀数值。',
    conditions: '目标条件',
    examples: '满足条件的物品示例',
    sources: '获取途径',
    tiers: '符合目标的阶级组合',
    presence: '数值不限',
    loading: '正在查找符合条件的物品…',
    empty: '尚未找到符合这些条件的物品示例。',
    retry: '重试',
  },
  'zh-TW': {
    qualityProjection: '目前無法依目標核對受催化劑品質影響的詞綴數值。',
    conditions: '目標條件',
    examples: '符合條件的物品範例',
    sources: '取得方式',
    tiers: '符合目標的階級組合',
    presence: '數值不限',
    loading: '正在尋找符合條件的物品…',
    empty: '尚未找到符合這些條件的物品範例。',
    retry: '重試',
  },
  ja: {
    qualityProjection:
      '触媒の品質が影響するモッドの数値は、まだ目標と照合できません。',
    conditions: '目標条件',
    examples: '条件を満たすアイテム例',
    sources: '入手方法',
    tiers: '目標を満たすティアの組み合わせ',
    presence: '数値指定なし',
    loading: '条件を満たすアイテムを検索中…',
    empty: 'この条件を満たす完成例は見つかりませんでした。',
    retry: '再試行',
  },
  es: {
    qualityProjection:
      'Aún no se pueden comprobar los valores afectados por la calidad del catalizador.',
    conditions: 'Condiciones del objetivo',
    examples: 'Ejemplos de objetos que cumplen el objetivo',
    sources: 'Obtención',
    tiers: 'Combinaciones de grados que cumplen el objetivo',
    presence: 'Cualquier valor',
    loading: 'Buscando objetos que cumplan el objetivo…',
    empty: 'No se encontraron ejemplos para estas condiciones.',
    retry: 'Reintentar',
  },
  ru: {
    qualityProjection:
      'Пока нельзя проверить значения свойств с учётом качества от катализатора.',
    conditions: 'Условия цели',
    examples: 'Примеры подходящих предметов',
    sources: 'Источники',
    tiers: 'Подходящие сочетания рангов',
    presence: 'Любое значение',
    loading: 'Поиск подходящих предметов…',
    empty: 'Примеров предметов, удовлетворяющих этим условиям, не найдено.',
    retry: 'Повторить',
  },
  th: {
    qualityProjection:
      'ยังไม่สามารถตรวจค่าม็อดที่ได้รับผลจากคุณภาพของตัวเร่งเทียบกับเป้าหมายได้',
    conditions: 'เงื่อนไขเป้าหมาย',
    examples: 'ตัวอย่างไอเทมที่ตรงเป้าหมาย',
    sources: 'แหล่งที่มา',
    tiers: 'ชุดเทียร์ที่ตรงตามเป้าหมาย',
    presence: 'ค่าใดก็ได้',
    loading: 'กำลังค้นหาไอเทมที่ตรงเป้าหมาย…',
    empty: 'ยังไม่พบตัวอย่างที่ตรงกับเงื่อนไขเหล่านี้',
    retry: 'ลองอีกครั้ง',
  },
}
