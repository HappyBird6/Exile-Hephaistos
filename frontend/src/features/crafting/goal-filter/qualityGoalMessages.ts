import type { Locale } from '../../../shared/i18n/i18n'
export const qualityGoalMessages: Record<
  Locale,
  { label: string; above20: string; work20: string }
> = {
  en: {
    label: 'Minimum quality',
    above20:
      'Above 20: raise the cap with Essence of the Breach, then complete quality work before removing its modifier. Reviewed equipment only; catalyst type and count are unspecified.',
    work20: 'Work to 20 quality',
  },
  ko: {
    label: '최소 퀄리티',
    above20:
      '20 초과: 균열의 에센스로 최대치를 높인 뒤 해당 속성 제거 전에 퀄리티 작업을 완료합니다. 검증된 장비만 지원하며 기폭제 종류·개수는 지정하지 않습니다.',
    work20: '퀄리티 20 작업',
  },
  'zh-CN': {
    label: '最低品质',
    above20:
      '超过20：先使用裂隙精华提高上限，再于移除该词缀前完成品质步骤。仅支持已验证装备，不指定催化剂类型和数量。',
    work20: '提升至20品质',
  },
  'zh-TW': {
    label: '最低品質',
    above20:
      '超過20：先使用裂痕精髓提高上限，再於移除該詞綴前完成品質步驟。僅支援已驗證裝備，不指定催化劑種類與數量。',
    work20: '提升至20品質',
  },
  ja: {
    label: '最低品質',
    above20:
      '20超：まずブリーチのエッセンスで上限を増やし、その修飾子を除去する前に品質作業を完了します。検証済み装備のみ対応。触媒の種類と個数は未指定です。',
    work20: '品質20への作業',
  },
  es: {
    label: 'Calidad mínima',
    above20:
      'Más de 20: aumenta el máximo con Esencia de la Brecha y completa la calidad antes de eliminar su modificador. Solo equipo verificado; catalizador y cantidad sin especificar.',
    work20: 'Trabajo hasta calidad 20',
  },
}
