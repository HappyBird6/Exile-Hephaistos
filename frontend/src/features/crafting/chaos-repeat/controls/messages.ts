import type { Locale } from '../../../../shared/i18n/i18n'

type Messages = {
  mode: string
  continueUsing: string
  pattern: string
  maximumUses: string
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
    hint: 'Use Chaos until the pattern appears in the modifiers.',
    running: 'Applying Chaos…',
    serverLimit: 'Server limit: {maximum}',
  },
  ko: {
    mode: '카오스 반복 모드',
    continueUsing: '계속사용',
    pattern: '패턴',
    maximumUses: '최대 사용 횟수',
    hint: '옵션에 패턴이 등장할때까지 카오스 사용',
    running: '카오스 적용 중…',
    serverLimit: '서버 한도: {maximum}',
  },
  'zh-CN': {
    mode: '混沌重复模式',
    continueUsing: '继续使用',
    pattern: '匹配模式',
    maximumUses: '最大使用次数',
    hint: '持续使用混沌石，直到词缀中出现匹配内容。',
    running: '正在使用混沌石…',
    serverLimit: '服务器上限：{maximum}',
  },
  'zh-TW': {
    mode: '混沌重複模式',
    continueUsing: '繼續使用',
    pattern: '比對模式',
    maximumUses: '最大使用次數',
    hint: '持續使用混沌石，直到詞綴中出現符合的內容。',
    running: '正在使用混沌石…',
    serverLimit: '伺服器上限：{maximum}',
  },
  ja: {
    mode: 'カオス反復モード',
    continueUsing: '使用を続ける',
    pattern: 'パターン',
    maximumUses: '最大使用回数',
    hint: 'モッドにパターンが現れるまでカオスを使用します。',
    running: 'カオスを適用中…',
    serverLimit: 'サーバー上限：{maximum}',
  },
  es: {
    mode: 'Modo de caos repetido',
    continueUsing: 'Seguir usando',
    pattern: 'Patrón',
    maximumUses: 'Usos máximos',
    hint: 'Usa caos hasta que el patrón aparezca en los modificadores.',
    running: 'Aplicando caos…',
    serverLimit: 'Límite del servidor: {maximum}',
  },
}
