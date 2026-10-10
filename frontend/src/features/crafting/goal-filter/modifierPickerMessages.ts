import type { Locale } from '../../../shared/i18n/i18n'

type Messages = {
  group: string
  search: string
  tier: string
  chooseTier: string
  empty: string
  noMatch: string
  prefix: string
  suffix: string
}
export const modifierPickerMessages: Record<Locale, Messages> = {
  en: {
    group: 'Modifier group',
    search: 'Search modifiers…',
    tier: 'Starting modifier tier',
    chooseTier: 'Select tier…',
    empty: 'No more modifiers can be added.',
    noMatch: 'No matching modifiers.',
    prefix: 'Prefix',
    suffix: 'Suffix',
  },
  ko: {
    group: '속성 계열',
    search: '속성 검색…',
    tier: '시작 아이템 속성 티어',
    chooseTier: '티어 선택…',
    empty: '더 추가할 수 있는 속성이 없습니다.',
    noMatch: '일치하는 속성이 없습니다.',
    prefix: '접두어',
    suffix: '접미어',
  },
  'zh-CN': {
    group: '词缀类别',
    search: '搜索词缀…',
    tier: '起始物品词缀阶级',
    chooseTier: '选择阶级…',
    empty: '无法添加更多词缀。',
    noMatch: '没有匹配的词缀。',
    prefix: '前缀',
    suffix: '后缀',
  },
  'zh-TW': {
    group: '詞綴類別',
    search: '搜尋詞綴…',
    tier: '起始物品詞綴階級',
    chooseTier: '選擇階級…',
    empty: '無法新增更多詞綴。',
    noMatch: '沒有符合的詞綴。',
    prefix: '前綴',
    suffix: '後綴',
  },
  ja: {
    group: 'モッドの種類',
    search: 'モッドを検索…',
    tier: '開始アイテムのモッドティア',
    chooseTier: 'ティアを選択…',
    empty: '追加できるモッドはありません。',
    noMatch: '一致するモッドはありません。',
    prefix: 'プレフィックス',
    suffix: 'サフィックス',
  },
  es: {
    group: 'Grupo de modificadores',
    search: 'Buscar modificadores…',
    tier: 'Grado del modificador inicial',
    chooseTier: 'Selecciona un grado…',
    empty: 'No se pueden añadir más modificadores.',
    noMatch: 'No hay modificadores coincidentes.',
    prefix: 'Prefijo',
    suffix: 'Sufijo',
  },
}
