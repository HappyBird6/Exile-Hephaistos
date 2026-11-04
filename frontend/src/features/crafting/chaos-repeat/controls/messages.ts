import type { Locale } from '../../../../shared/i18n/i18n'

type Messages = {
  mode: string
  continueUsing: string
  pattern: string
  maximumUses: string
  maximumUsesHelp: string
  invalidMaximumUses: string
  patternHelp: string
  hint: string
  running: string
  serverLimit: string
}

export const chaosRepeatControlMessages: Record<Locale, Messages> = {
  en: {
    mode: 'Chaos repeat mode',
    continueUsing: 'Continue using',
    pattern: 'Pattern',
    maximumUses: 'Maximum uses',
    maximumUsesHelp: '1–{maximum} uses (default: {default}).',
    invalidMaximumUses: 'Enter a whole number from 1 to {maximum}.',
    patternHelp:
      'Basic regular expressions, case insensitive. Match each modifier separately; keep a multiline modifier together.',
    hint: 'Use Chaos until the pattern appears in the modifiers.',
    running: 'Applying Chaos…',
    serverLimit: 'Server limit: {maximum}',
  },
  ko: {
    mode: '카오스 반복 모드',
    continueUsing: '계속사용',
    pattern: '패턴',
    maximumUses: '최대 사용 횟수',
    maximumUsesHelp: '1–{maximum}회 (기본 {default}회).',
    invalidMaximumUses: '1부터 {maximum}까지의 정수를 입력하세요.',
    patternHelp:
      '기본 정규식, 대소문자 무시. 옵션별로 비교하며 여러 줄로 된 복합 옵션은 한 묶음으로 비교합니다.',
    hint: '옵션에 패턴이 등장할때까지 카오스 사용',
    running: '카오스 적용 중…',
    serverLimit: '서버 한도: {maximum}',
  },
  'zh-CN': {
    mode: '混沌重复模式',
    continueUsing: '继续使用',
    pattern: '匹配模式',
    maximumUses: '最大使用次数',
    maximumUsesHelp: '1–{maximum} 次（默认 {default} 次）。',
    invalidMaximumUses: '请输入 1 到 {maximum} 之间的整数。',
    patternHelp:
      '基本正则表达式，不区分大小写。逐条匹配词缀；多行复合词缀作为一组匹配。',
    hint: '持续使用混沌石，直到词缀中出现匹配内容。',
    running: '正在使用混沌石…',
    serverLimit: '服务器上限：{maximum}',
  },
  'zh-TW': {
    mode: '混沌重複模式',
    continueUsing: '繼續使用',
    pattern: '比對模式',
    maximumUses: '最大使用次數',
    maximumUsesHelp: '1–{maximum} 次（預設 {default} 次）。',
    invalidMaximumUses: '請輸入 1 到 {maximum} 之間的整數。',
    patternHelp:
      '基本正規表示式，不區分大小寫。逐條比對詞綴；多行複合詞綴作為一組比對。',
    hint: '持續使用混沌石，直到詞綴中出現符合的內容。',
    running: '正在使用混沌石…',
    serverLimit: '伺服器上限：{maximum}',
  },
  ja: {
    mode: 'カオス反復モード',
    continueUsing: '使用を続ける',
    pattern: 'パターン',
    maximumUses: '最大使用回数',
    maximumUsesHelp: '1–{maximum}回（初期値: {default}回）。',
    invalidMaximumUses: '1から{maximum}までの整数を入力してください。',
    patternHelp:
      '基本的な正規表現で大文字と小文字を区別しません。モッドごとに照合し、複数行の複合モッドは一つのまとまりとして扱います。',
    hint: 'モッドにパターンが現れるまでカオスを使用します。',
    running: 'カオスを適用中…',
    serverLimit: 'サーバー上限：{maximum}',
  },
  es: {
    mode: 'Modo de caos repetido',
    continueUsing: 'Seguir usando',
    pattern: 'Patrón',
    maximumUses: 'Usos máximos',
    maximumUsesHelp: '1–{maximum} usos (predeterminado: {default}).',
    invalidMaximumUses: 'Introduce un entero entre 1 y {maximum}.',
    patternHelp:
      'Expresiones regulares básicas, sin distinguir mayúsculas. Compara cada modificador por separado y agrupa sus líneas si tiene varias.',
    hint: 'Usa caos hasta que el patrón aparezca en los modificadores.',
    running: 'Aplicando caos…',
    serverLimit: 'Límite del servidor: {maximum}',
  },
}
