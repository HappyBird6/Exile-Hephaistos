import type { Locale } from '../../../shared/i18n/i18n'
type Copy = {
  add: string
  replace: string
  prepare: string
  addHelp: string
  replaceHelp: string
  inspect: string
  compare: string
  separate: string
  chance: string
}
export const operationMessages: Record<Locale, Copy> = {
  en: {
    add: 'Add one modifier',
    replace: 'Replace a modifier',
    prepare: 'Prepare, then replace a modifier',
    addHelp:
      'Keep existing modifiers and roll one new modifier. Open results to inspect possible items.',
    replaceHelp:
      'Chaos removes a modifier and rolls a replacement. Existing modifiers may be lost; preparation is shown separately.',
    inspect: 'Inspect the selected method and its results.',
    compare: 'Compare currency and omen options',
    separate:
      'Each option has its own chance, cost, removal risk and outcome pool. Probabilities are not added together.',
    chance: 'Goal chance',
  },
  ko: {
    add: '속성 1개 추가',
    replace: '속성 교체',
    prepare: '속성 준비 후 교체',
    addHelp:
      '기존 속성을 유지하고 새 속성 1개를 뽑습니다. 상세 결과를 열면 가능한 아이템을 확인할 수 있습니다.',
    replaceHelp:
      '카오스는 속성을 제거하고 새 속성을 뽑습니다. 기존 속성을 잃을 수 있으며, 필요한 준비 작업은 따로 표시합니다.',
    inspect: '선택한 방법과 결과를 확인하세요.',
    compare: '화폐·징조별 방법 비교',
    separate:
      '각 방법의 확률·소모·제거 위험·후보 결과는 별개입니다. 서로 다른 방법의 확률을 더하지 않습니다.',
    chance: '목표 획득 확률',
  },
  'zh-CN': {
    add: '添加一个词缀',
    replace: '替换词缀',
    prepare: '准备后替换词缀',
    addHelp: '保留现有词缀并随机添加一个新词缀。展开结果查看可能的物品。',
    replaceHelp:
      '混沌石移除词缀并生成替代词缀。现有词缀可能丢失，准备步骤单独显示。',
    inspect: '查看所选方法和结果。',
    compare: '比较通货和预兆方案',
    separate: '每个方案的概率、消耗、风险和结果池独立，不合并概率。',
    chance: '目标概率',
  },
  'zh-TW': {
    add: '新增一個詞綴',
    replace: '替換詞綴',
    prepare: '準備後替換詞綴',
    addHelp: '保留現有詞綴並隨機新增一個詞綴。展開結果查看可能的物品。',
    replaceHelp:
      '混沌石移除詞綴並產生替代詞綴。現有詞綴可能遺失，準備步驟單獨顯示。',
    inspect: '查看所選方法和結果。',
    compare: '比較通貨與預兆方案',
    separate: '每個方案的機率、消耗、風險和結果池獨立，不合併機率。',
    chance: '目標機率',
  },
  ja: {
    add: 'モッドを1つ追加',
    replace: 'モッドを置換',
    prepare: '準備後にモッドを置換',
    addHelp:
      '既存モッドを保持し、新しいモッドを1つ抽選します。結果を開くと候補を確認できます。',
    replaceHelp:
      'カオスはモッドを除去して置換します。既存モッドを失う可能性があり、準備は別に表示します。',
    inspect: '選択した方法と結果を確認します。',
    compare: '通貨・オーメンの方法を比較',
    separate:
      '確率、消費、リスク、候補は方法ごとに独立しています。確率は合算しません。',
    chance: '目標獲得確率',
  },
  es: {
    add: 'Añadir un modificador',
    replace: 'Sustituir un modificador',
    prepare: 'Preparar y sustituir un modificador',
    addHelp:
      'Conserva los modificadores y sortea uno nuevo. Abre los resultados para ver los objetos posibles.',
    replaceHelp:
      'Caos elimina un modificador y sortea su reemplazo. Puede perderse un modificador; la preparación se muestra aparte.',
    inspect: 'Consulta el método y sus resultados.',
    compare: 'Comparar monedas y presagios',
    separate:
      'Cada opción conserva su probabilidad, consumo, riesgo y resultados. No se suman probabilidades.',
    chance: 'Probabilidad del objetivo',
  },
}
