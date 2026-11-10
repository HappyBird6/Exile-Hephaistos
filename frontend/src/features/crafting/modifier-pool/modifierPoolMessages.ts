import type { Locale } from '../../../shared/i18n/i18n'

type Messages = {
  dynamicEmpty: string
  dynamic: string
  static: string
  mode: string
  dynamicBasis: string
  desecrated: string
  essence: string
  essenceNote: string

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
    dynamicEmpty: 'No modifiers can be added in the current state.',
    dynamic: 'Dynamic',
    static: 'All modifiers',
    mode: 'Modifier view',
    dynamicBasis:
      'Current item level, occupied families and remaining prefix/suffix slots. Each affix side totals 100%; currency and omen effects are excluded.',
    desecrated: 'Desecrated',
    essence: 'Essence',
    essenceNote:
      'Essence outcomes are specified by each currency; they do not use ordinary spawn weights.',

    effect: 'Modifier',
    basis:
      'Published weights for all modifier tiers, with prefixes and suffixes each totaling 100%. Existing modifiers and currency effects are not included. Expand a group to see individual tiers.',
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
    dynamicEmpty: '현재 상태에서 추가할 수 있는 옵션이 없습니다.',
    dynamic: '동적 보기',
    static: '전체 옵션',
    mode: '옵션 보기',
    dynamicBasis:
      '현재 아이템의 레벨·중복 계열·남은 접두/접미 슬롯을 반영합니다. 접두어·접미어별 비중이며 화폐·징조 효과는 반영하지 않습니다.',
    desecrated: '훼손',
    essence: '에센스',
    essenceNote:
      '에센스 옵션은 화폐별 지정 결과이며 일반 옵션의 가중치 추첨을 적용하지 않습니다.',

    effect: '옵션',
    basis:
      '전체 티어의 공개 가중치 기준으로 접두어·접미어를 각각 100%로 계산합니다. 현재 옵션과 화폐 효과는 반영하지 않습니다. 계열을 펼치면 티어별 수치를 볼 수 있습니다.',
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
    dynamicEmpty: '当前状态没有可添加的词缀。',
    dynamic: '动态',
    static: '全部词缀',
    mode: '词缀视图',
    dynamicBasis:
      '根据当前物品等级、已有词缀类别和剩余前后缀槽位计算。前后缀分别归一化，不含通货及预兆效果。',
    desecrated: '亵渎',
    essence: '精华',
    essenceNote: '精华词缀由各通货指定，不使用普通词缀生成权重。',

    effect: '词缀',
    basis:
      '根据所有阶级的公开权重，前缀和后缀分别合计为100%。不考虑已有词缀及通货效果。展开类别可查看各阶级。',
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
    dynamicEmpty: '目前狀態沒有可新增的詞綴。',
    dynamic: '動態',
    static: '全部詞綴',
    mode: '詞綴檢視',
    dynamicBasis:
      '依據目前物品等級、現有詞綴類別與剩餘前後綴欄位計算。前後綴分別正規化，不含通貨與預兆效果。',
    desecrated: '褻瀆',
    essence: '精髓',
    essenceNote: '精髓詞綴由各通貨指定，不使用普通詞綴生成權重。',

    effect: '詞綴',
    basis:
      '依據所有階級的公開權重，前綴和後綴分別合計為100%。不考慮現有詞綴及通貨效果。展開類別可查看各階級。',
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
    dynamicEmpty: '現在の状態で追加できるモッドはありません。',
    dynamic: '動的表示',
    static: '全モッド',
    mode: 'モッド表示',
    dynamicBasis:
      '現在のアイテムレベル、既存モッドの種類、残りの枠を反映します。接頭と接尾は個別に正規化し、通貨・オーメンの効果は含みません。',
    desecrated: '冒涜',
    essence: 'エッセンス',
    essenceNote:
      'エッセンスの結果は通貨ごとに指定され、通常の出現ウェイトは使用しません。',

    effect: 'モッド',
    basis:
      '全ティアの公開された重みに基づき、プレフィックスとサフィックスをそれぞれ100%として計算します。既存のモッドと通貨効果は含みません。グループを展開すると各ティアを確認できます。',
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
    dynamicEmpty: 'No se pueden añadir modificadores en el estado actual.',
    dynamic: 'Dinámico',
    static: 'Todos los mods',
    mode: 'Vista de modificadores',
    dynamicBasis:
      'Según el nivel, familias ocupadas y espacios de prefijos/sufijos disponibles. Cada tipo suma un 100%; no incluye efectos de monedas ni presagios.',
    desecrated: 'Profanado',
    essence: 'Esencia',
    essenceNote:
      'Las esencias especifican resultados por moneda; no usan los pesos de aparición normales.',

    effect: 'Modificador',
    basis:
      'Pesos publicados de todos los grados; prefijos y sufijos suman un 100% por separado. No se incluyen modificadores existentes ni efectos de monedas. Despliega un grupo para ver sus grados.',
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

  ru: {
    dynamicEmpty: 'В текущем состоянии добавление модификаторов невозможно.',
    dynamic: 'Динамический',
    static: 'Все модификаторы',
    mode: 'Режим просмотра модификаторов',
    dynamicBasis:
      'Учитываются уровень предмета, занятые группы и свободные места для префиксов и суффиксов. Вероятности каждой стороны в сумме равны 100%. Эффекты валюты и знамений не учитываются.',
    desecrated: 'Очернение',
    essence: 'Эссенции',
    essenceNote:
      'Результаты эссенций заданы для каждой валюты и не используют обычные веса появления.',

    effect: 'Модификатор',
    basis:
      'Опубликованные веса для всех уровней модификаторов с префиксами и суффиксами, каждый из которых составляет 100 %. Существующие модификаторы и эффекты валюты не включены. Разверните группу, чтобы увидеть отдельные уровни.',
    unpublishedNote:
      'Веса и вероятности для этого предмета не публикуются. Просмотрите его модификаторы и необходимые уровни ниже.',

    title: 'Веса и вероятности модификаторов',
    itemLevel: 'Уровень предмета',
    search: 'Поиск модификаторов…',
    prefix: 'Префиксы',
    suffix: 'Суффиксы',
    groups: 'групп',
    tiers: 'уровней',
    totalWeight: 'Общий вес',
    weight: 'Вес',
    probability: 'Вероятность',
    modifier: 'Группа модификаторов',
    tier: 'Уровень',
    level: 'Уровень',
    source: 'PoE2DB',
    empty: 'На этом уровне предмета модификаторов нет.',
    noMatch: 'Нет соответствующих модификаторов.',
    loading: 'Загрузка модификаторов…',
    unavailable: 'Данные модификатора для этого предмета недоступны.',
    unpublished: 'Не опубликовано',
  },
  th: {
    dynamicEmpty: 'ไม่สามารถเพิ่มม็อดในสถานะปัจจุบันได้',
    dynamic: 'ไดนามิก',
    static: 'ม็อดทั้งหมด',
    mode: 'มุมมองม็อด',
    dynamicBasis:
      'คำนวณตามเลเวลไอเทม กลุ่มม็อดที่มีอยู่ และช่องพรีฟิกซ์หรือซัฟฟิกซ์ที่เหลือ โอกาสของแต่ละฝั่งรวมเป็น 100% โดยไม่รวมผลของเคอร์เรนซีและลาง',
    desecrated: 'สามานย์',
    essence: 'เอสเซนส์',
    essenceNote:
      'เอสเซนส์แต่ละชนิดกำหนดม็อดที่จะได้รับไว้แล้ว จึงไม่ใช้น้ำหนักการสุ่มม็อดทั่วไป',

    effect: 'ม็อด',
    basis:
      'แสดงน้ำหนักที่เปิดเผยของม็อดทุกระดับ โดยโอกาสของพรีฟิกซ์และซัฟฟิกซ์แต่ละฝั่งรวมเป็น 100% ไม่คำนึงถึงม็อดที่มีอยู่หรือผลของเคอร์เรนซี ขยายกลุ่มเพื่อดูแต่ละระดับ',
    unpublishedNote:
      'ยังไม่มีการเปิดเผยน้ำหนักและโอกาสของไอเทมนี้ ดูม็อดและเลเวลที่ต้องการได้ด้านล่าง',

    title: 'น้ำหนักและความน่าจะเป็นของม็อด',
    itemLevel: 'เลเวลไอเทม',
    search: 'ค้นหาม็อด…',
    prefix: 'พรีฟิกซ์',
    suffix: 'ซัฟฟิกซ์',
    groups: 'กลุ่ม',
    tiers: 'ชั้น',
    totalWeight: 'น้ำหนักรวม',
    weight: 'น้ำหนัก',
    probability: 'ความน่าจะเป็น',
    modifier: 'กลุ่มม็อด',
    tier: 'ชั้น',
    level: 'ระดับ',
    source: 'PoE2DB',
    empty: 'ไม่มีม็อดในเลเวลไอเทมนี้',
    noMatch: 'ไม่มีม็อดที่ตรงกัน',
    loading: 'กำลังโหลดม็อด...',
    unavailable: 'ไม่มีข้อมูลม็อดสำหรับไอเทมนี้',
    unpublished: 'ไม่ได้เผยแพร่',
  },
}
