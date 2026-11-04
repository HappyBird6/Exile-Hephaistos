import type { Locale } from '../../../shared/i18n/i18n'

type Messages = {
  title: string
  empty: string
  step: string
  omitted: string
  omensMissing: string
  incomplete: string
  unknownCurrency: string
  unknownOmen: string
  imageUnavailable: string
  repeatUses: (count: string) => string
}

export const sessionNotationMessages: Record<Locale, Messages> = {
  en: {
    title: 'History',
    empty: 'No recorded currency applications at this point.',
    step: 'Step',
    omitted:
      'Some saved steps lack application evidence or an identifiable currency and are omitted.',
    omensMissing:
      'Omen consumption is incomplete in some saved steps. Missing details are not inferred.',
    incomplete: 'Incomplete record',
    unknownCurrency: 'Unidentified currency',
    unknownOmen: 'Unidentified omen',
    imageUnavailable: 'Image unavailable',
    repeatUses: (count) => `${count} ${count === '1' ? 'use' : 'uses'}`,
  },
  ko: {
    title: 'History',
    empty: '이 시점에 기록된 화폐 적용이 없습니다.',
    step: '단계',
    omitted:
      '일부 저장 단계는 적용 증거 또는 화폐 식별 정보가 없어 생략했습니다.',
    omensMissing:
      '일부 저장 단계의 징조 소비 정보가 불완전합니다. 누락 정보는 추정하지 않습니다.',
    incomplete: '불완전한 기록',
    unknownCurrency: '식별되지 않은 화폐',
    unknownOmen: '식별되지 않은 징조',
    imageUnavailable: '이미지 없음',
    repeatUses: (count) => `${count}회`,
  },
  'zh-CN': {
    title: 'History',
    empty: '此时没有已记录的通货使用。',
    step: '步骤',
    omitted: '部分已保存步骤缺少使用凭据或通货识别信息，已省略。',
    omensMissing: '部分已保存步骤的预兆消耗信息不完整，不会推测缺失信息。',
    incomplete: '记录不完整',
    unknownCurrency: '无法识别的通货',
    unknownOmen: '无法识别的预兆',
    imageUnavailable: '无可用图片',
    repeatUses: (count) => `${count}次`,
  },
  'zh-TW': {
    title: 'History',
    empty: '此時沒有已記錄的通貨使用。',
    step: '步驟',
    omitted: '部分已儲存步驟缺少使用憑據或通貨識別資訊，已省略。',
    omensMissing: '部分已儲存步驟的預兆消耗資訊不完整，不會推測缺少的資訊。',
    incomplete: '記錄不完整',
    unknownCurrency: '無法識別的通貨',
    unknownOmen: '無法識別的預兆',
    imageUnavailable: '無可用圖片',
    repeatUses: (count) => `${count}次`,
  },
  ja: {
    title: 'History',
    empty: 'この時点で記録された通貨の適用はありません。',
    step: 'ステップ',
    omitted: '適用の証拠または通貨の識別情報がない保存ステップを省略しました。',
    omensMissing:
      '一部の保存ステップではオーメンの消費情報が不完全です。不足情報は推測しません。',
    incomplete: '不完全な記録',
    unknownCurrency: '識別できない通貨',
    unknownOmen: '識別できないオーメン',
    imageUnavailable: '画像なし',
    repeatUses: (count) => `${count}回`,
  },
  es: {
    title: 'History',
    empty:
      'No hay aplicaciones de objetos monetarios registradas en este punto.',
    step: 'Paso',
    omitted:
      'Se omiten pasos guardados sin evidencia de aplicación o sin un objeto monetario identificable.',
    omensMissing:
      'Falta información de consumo de presagios en algunos pasos guardados. No se deducen los datos ausentes.',
    incomplete: 'Registro incompleto',
    unknownCurrency: 'Objeto monetario sin identificar',
    unknownOmen: 'Presagio sin identificar',
    imageUnavailable: 'Imagen no disponible',
    repeatUses: (count) => `${count} ${count === '1' ? 'uso' : 'usos'}`,
  },
}
