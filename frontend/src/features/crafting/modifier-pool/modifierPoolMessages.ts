import type { Locale } from '../../../shared/i18n/i18n'

type Messages = {
  title: string
  itemLevel: string
  search: string
  prefix: string
  suffix: string
  groups: string
  tiers: string
  totalWeight: string
  weight: string
  probability: string
  modifier: string
  tier: string
  level: string
  source: string
  empty: string
  noMatch: string
  loading: string
  unavailable: string
  effect: string
  basis: string
  unpublishedNote: string
  unpublished: string
}

export const modifierPoolMessages: Record<Locale, Messages> = {
  en: {
    effect: 'Modifier',
    basis:
      'Published weights at this item level, with prefixes and suffixes each totaling 100%. Existing modifiers and currency effects are not included. Expand a group to see individual tiers.',
    unpublishedNote:
      'Weights and probabilities for this item are not published. Browse its modifiers and required levels below.',

    title: 'Modifier weights & probabilities',
    itemLevel: 'Item level',
    search: 'Search modifiers…',
    prefix: 'Prefixes',
    suffix: 'Suffixes',
    groups: 'groups',
    tiers: 'tiers',
    totalWeight: 'Total weight',
    weight: 'Weight',
    probability: 'Probability',
    modifier: 'Modifier group',
    tier: 'Tier',
    level: 'Level',
    source: 'PoE2DB',
    empty: 'No modifiers at this item level.',
    noMatch: 'No matching modifiers.',
    loading: 'Loading modifiers…',
    unavailable: 'Modifier data is unavailable for this item.',
    unpublished: 'Not published',
  },
  ko: {
    effect: '옵션',
    basis:
      '이 아이템 레벨의 공개 가중치 기준으로 접두어·접미어를 각각 100%로 계산합니다. 현재 옵션과 화폐 효과는 반영하지 않습니다. 계열을 펼치면 티어별 수치를 볼 수 있습니다.',
    unpublishedNote:
      '이 장비의 가중치와 확률은 미공개입니다. 아래에서 옵션과 요구 레벨을 확인할 수 있습니다.',

    title: '옵션 가중치와 확률',
    itemLevel: '아이템 레벨',
    search: '옵션 검색…',
    prefix: '접두어',
    suffix: '접미어',
    groups: '개 계열',
    tiers: '개 티어',
    totalWeight: '총 가중치',
    weight: '가중치',
    probability: '확률',
    modifier: '옵션 계열',
    tier: '티어',
    level: '레벨',
    source: 'PoE2DB',
    empty: '이 아이템 레벨에 해당하는 옵션이 없습니다.',
    noMatch: '일치하는 옵션이 없습니다.',
    loading: '옵션을 불러오는 중…',
    unavailable: '이 장비의 옵션 정보를 불러올 수 없습니다.',
    unpublished: '미공개',
  },
  'zh-CN': {
    effect: '词缀',
    basis:
      '根据此物品等级的公开权重，前缀和后缀分别合计为100%。不考虑已有词缀及通货效果。展开类别可查看各阶级。',
    unpublishedNote:
      '此物品的权重和概率未公开。您可以在下方查看词缀及所需等级。',

    title: '词缀权重与概率',
    itemLevel: '物品等级',
    search: '搜索词缀…',
    prefix: '前缀',
    suffix: '后缀',
    groups: '个类别',
    tiers: '个阶级',
    totalWeight: '总权重',
    weight: '权重',
    probability: '概率',
    modifier: '词缀类别',
    tier: '阶级',
    level: '等级',
    source: 'PoE2DB',
    empty: '此物品等级没有可用词缀。',
    noMatch: '没有匹配的词缀。',
    loading: '正在加载词缀…',
    unavailable: '无法获取此物品的词缀信息。',
    unpublished: '未公开',
  },
  'zh-TW': {
    effect: '詞綴',
    basis:
      '依據此物品等級的公開權重，前綴和後綴分別合計為100%。不考慮現有詞綴及通貨效果。展開類別可查看各階級。',
    unpublishedNote:
      '此物品的權重和機率未公開。您可以在下方查看詞綴及所需等級。',

    title: '詞綴權重與機率',
    itemLevel: '物品等級',
    search: '搜尋詞綴…',
    prefix: '前綴',
    suffix: '後綴',
    groups: '個類別',
    tiers: '個階級',
    totalWeight: '總權重',
    weight: '權重',
    probability: '機率',
    modifier: '詞綴類別',
    tier: '階級',
    level: '等級',
    source: 'PoE2DB',
    empty: '此物品等級沒有可用詞綴。',
    noMatch: '沒有符合的詞綴。',
    loading: '正在載入詞綴…',
    unavailable: '無法取得此物品的詞綴資訊。',
    unpublished: '未公開',
  },
  ja: {
    effect: 'モッド',
    basis:
      'このアイテムレベルの公開された重みに基づき、プレフィックスとサフィックスをそれぞれ100%として計算します。既存のモッドと通貨効果は含みません。グループを展開すると各ティアを確認できます。',
    unpublishedNote:
      'この装備の重みと確率は非公開です。モッドと必要レベルは下記で確認できます。',

    title: 'モッドの重みと確率',
    itemLevel: 'アイテムレベル',
    search: 'モッドを検索…',
    prefix: 'プレフィックス',
    suffix: 'サフィックス',
    groups: 'グループ',
    tiers: 'ティア',
    totalWeight: '合計重み',
    weight: '重み',
    probability: '確率',
    modifier: 'モッドグループ',
    tier: 'ティア',
    level: 'レベル',
    source: 'PoE2DB',
    empty: 'このアイテムレベルのモッドはありません。',
    noMatch: '一致するモッドはありません。',
    loading: 'モッドを読み込み中…',
    unavailable: 'この装備のモッド情報を取得できません。',
    unpublished: '非公開',
  },
  es: {
    effect: 'Modificador',
    basis:
      'Pesos publicados para este nivel de objeto; prefijos y sufijos suman un 100% por separado. No se incluyen modificadores existentes ni efectos de monedas. Despliega un grupo para ver sus grados.',
    unpublishedNote:
      'Los pesos y las probabilidades de este objeto no están publicados. Puedes consultar sus modificadores y niveles requeridos a continuación.',

    title: 'Pesos y probabilidades de modificadores',
    itemLevel: 'Nivel de objeto',
    search: 'Buscar modificadores…',
    prefix: 'Prefijos',
    suffix: 'Sufijos',
    groups: 'grupos',
    tiers: 'grados',
    totalWeight: 'Peso total',
    weight: 'Peso',
    probability: 'Probabilidad',
    modifier: 'Grupo de modificadores',
    tier: 'Grado',
    level: 'Nivel',
    source: 'PoE2DB',
    empty: 'No hay modificadores para este nivel de objeto.',
    noMatch: 'No hay modificadores coincidentes.',
    loading: 'Cargando modificadores…',
    unavailable:
      'La información de modificadores de este objeto no está disponible.',
    unpublished: 'No publicado',
  },
}
