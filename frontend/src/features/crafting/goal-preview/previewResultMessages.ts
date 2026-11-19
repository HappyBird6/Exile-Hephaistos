import type { Locale } from '../../../shared/i18n/i18n'

type Copy = {
  conditions: string
  examples: string
  sources: string
  ordinary: string
  implicit: string
  augment: string
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
    ordinary: '일반 옵션 추첨',
    implicit: '베이스 고정 옵션',
    augment: '장착 후 추첨',
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
    ordinary: 'Normal modifier pool',
    implicit: 'Base implicit',
    augment: 'Roll while socketed',
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
    ordinary: '普通词缀池',
    implicit: '基底固有词缀',
    augment: '镶嵌后随机生成',
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
    ordinary: '一般詞綴池',
    implicit: '基底固定詞綴',
    augment: '鑲嵌後隨機產生',
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
    ordinary: '通常のモッド抽選',
    implicit: 'ベースの暗黙モッド',
    augment: '装着中に抽選',
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
    ordinary: 'Grupo de modificadores normales',
    implicit: 'Implícito de la base',
    augment: 'Tirada con el aumento engarzado',
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
    ordinary: 'Пул обычных свойств',
    implicit: 'Собственное свойство базы',
    augment: 'Выпадает при установке',
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
    ordinary: 'กลุ่มม็อดปกติ',
    implicit: 'ม็อดติดฐาน',
    augment: 'สุ่มได้เมื่อติดตั้ง',
    presence: 'ค่าใดก็ได้',
    loading: 'กำลังค้นหาไอเทมที่ตรงเป้าหมาย…',
    empty: 'ยังไม่พบตัวอย่างที่ตรงกับเงื่อนไขเหล่านี้',
    retry: 'ลองอีกครั้ง',
  },
}
