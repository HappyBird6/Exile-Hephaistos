import type { Locale } from '../../../shared/i18n/i18n'

export const startInputMessages = {
  en: {
    value: 'Value',
    invalid: 'Check the item text. Your original text is preserved.',
    unchecked: 'Check the edited text before starting.',
    base: 'This item base is not supported.',
    mapping:
      'Only Solar Amulet text can currently be used for crafting. Your text is preserved.',
    range: 'Enter a whole number in this range:',
    large: 'Item text is too long. Your original text is preserved.',
  },
  ko: {
    value: '수치',
    invalid: '아이템 텍스트를 확인하세요. 원문은 보존됩니다.',
    unchecked: '제작 전에 수정한 텍스트를 확인하세요.',
    base: '아직 지원하지 않는 아이템 베이스입니다.',
    mapping:
      '현재는 태양의 목걸이 텍스트만 제작에 사용할 수 있습니다. 원문은 보존됩니다.',
    range: '이 범위 안의 정수를 입력하세요:',
    large: '아이템 텍스트가 너무 깁니다. 원문은 보존됩니다.',
  },
  'zh-CN': {
    value: '数值',
    invalid: '请检查物品文本。原文已保留。',
    unchecked: '开始前请检查修改后的文本。',
    base: '暂不支持此物品底材。',
    mapping: '目前仅能使用Solar Amulet文本进行制作。原文已保留。',
    range: '请输入此范围内的整数：',
    large: '物品文本过长。原文已保留。',
  },
  'zh-TW': {
    value: '數值',
    invalid: '請檢查物品文字。原文已保留。',
    unchecked: '開始前請檢查修改後的文字。',
    base: '暫不支援此物品基底。',
    mapping: '目前僅能使用Solar Amulet文字進行製作。原文已保留。',
    range: '請輸入此範圍內的整數：',
    large: '物品文字過長。原文已保留。',
  },
  ja: {
    value: '数値',
    invalid: 'アイテムテキストを確認してください。原文は保持されています。',
    unchecked: '開始前に編集したテキストを確認してください。',
    base: 'このアイテムベースは未対応です。',
    mapping:
      '現在クラフトに使用できるのはSolar Amuletのテキストのみです。原文は保持されています。',
    range: 'この範囲の整数を入力してください：',
    large: 'アイテムテキストが長すぎます。原文は保持されています。',
  },
  es: {
    value: 'Valor',
    invalid: 'Comprueba el texto del objeto. Se conservó el original.',
    unchecked: 'Comprueba el texto editado antes de empezar.',
    base: 'Esta base de objeto aún no es compatible.',
    mapping:
      'Por ahora solo se puede fabricar a partir de texto de Solar Amulet. Se conservó el original.',
    range: 'Introduce un entero en este rango:',
    large: 'El texto del objeto es demasiado largo. Se conservó el original.',
  },
}
export function startInputIssue(issue: string, locale: Locale) {
  const t = startInputMessages[locale]
  const range = issue.match(
    /^Roll must be an integer from (-?\d+) to (-?\d+)\.$/,
  )
  if (range) return `${t.range} ${range[1]}–${range[2]}`
  if (issue.startsWith('Unknown or unsupported base.')) return t.base
  if (issue.startsWith('Pasted-item catalog mapping')) return t.mapping
  if (issue.startsWith('Item text exceeds')) return t.large
  if (issue === 'Check the edited text before starting.') return t.unchecked
  if (/^[A-Z_]+$/.test(issue) || issue.startsWith('Item check failed.'))
    return t.invalid
  return issue
}
