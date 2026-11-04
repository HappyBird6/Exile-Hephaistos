import type { Locale } from '../../../../shared/i18n/i18n'

type Messages = {
  mode: string
  use: string
  patternPlaceholder: string
  pattern: string
  hint: string
  running: string
}

export const chaosRepeatControlMessages: Record<Locale, Messages> = {
  en: {
    use: 'Use',
    patternPlaceholder: 'Enter a regular expression pattern',
    mode: 'Chaos repeat mode',
    pattern: 'Pattern',
    hint: 'Use Chaos until the pattern appears in the modifiers.',
    running: 'Applying Chaos…',
  },
  ko: {
    use: '사용',
    patternPlaceholder: '정규식 패턴을 입력해주세요',
    mode: '카오스 반복 모드',
    pattern: '패턴',
    hint: '옵션에 패턴이 등장할때까지 카오스 사용',
    running: '카오스 적용 중…',
  },
  'zh-CN': {
    use: '使用',
    patternPlaceholder: '请输入正则表达式',
    mode: '混沌重复模式',
    pattern: '匹配模式',
    hint: '持续使用混沌石，直到词缀中出现匹配内容。',
    running: '正在使用混沌石…',
  },
  'zh-TW': {
    use: '使用',
    patternPlaceholder: '請輸入正規表示式',
    mode: '混沌重複模式',
    pattern: '比對模式',
    hint: '持續使用混沌石，直到詞綴中出現符合的內容。',
    running: '正在使用混沌石…',
  },
  ja: {
    use: '使用',
    patternPlaceholder: '正規表現パターンを入力してください',
    mode: 'カオス反復モード',
    pattern: 'パターン',
    hint: 'モッドにパターンが現れるまでカオスを使用します。',
    running: 'カオスを適用中…',
  },
  es: {
    use: 'Usar',
    patternPlaceholder: 'Introduce un patrón de expresión regular',
    mode: 'Modo de caos repetido',
    pattern: 'Patrón',
    hint: 'Usa caos hasta que el patrón aparezca en los modificadores.',
    running: 'Aplicando caos…',
  },
}
