import type { Locale } from '../../../shared/i18n/i18n'
export const qualityWorkMessages: Record<
  Locale,
  { work: string; completed: string }
> = {
  en: {
    work: 'Work to 40 quality',
    completed: '40 quality work completed; catalyst type unspecified.',
  },
  ko: {
    work: '품질 40 작업',
    completed: '품질 40 작업 완료 · 기폭제 종류 미지정',
  },
  'zh-CN': {
    work: '提升至40品质',
    completed: '已完成40品质步骤；未指定催化剂类型。',
  },
  'zh-TW': {
    work: '提升至40品質',
    completed: '已完成40品質步驟；未指定催化劑類型。',
  },
  ja: {
    work: '品質40への作業',
    completed: '品質40への作業完了・触媒の種類は未指定',
  },
  es: {
    work: 'Trabajo hasta calidad 40',
    completed:
      'Trabajo de calidad 40 completado; tipo de catalizador sin especificar.',
  },
}
