import type { Locale } from '../../shared/i18n/i18n'
export const supportLayoutMessages: Record<
  Locale,
  {
    reload: string
    validate: string
    quality: string
    qualityHelp: string
    start: string
    reset: string
  }
> = {
  en: {
    start: 'Starting item',
    reset: 'Reset',
    reload: 'Refresh catalog',
    validate: 'Validate',
    quality: 'Quality ↑',
    qualityHelp:
      'Use Essence of the Breach to work above the quality cap (jewellery only).',
  },
  ko: {
    start: '시작 아이템',
    reset: '초기화',
    reload: '카탈로그 새로고침',
    validate: '검증',
    quality: '퀄리티 ↑',
    qualityHelp:
      '균열의 에센스로 최대 퀄리티를 초과하는 작업 사용 (장신구 전용).',
  },
  'zh-CN': {
    start: '起始物品',
    reset: '重置',
    reload: '刷新目录',
    validate: '验证',
    quality: '品质 ↑',
    qualityHelp: '使用裂隙精华突破品质上限（仅限首饰）。',
  },
  'zh-TW': {
    start: '起始物品',
    reset: '重設',
    reload: '重新整理目錄',
    validate: '驗證',
    quality: '品質 ↑',
    qualityHelp: '使用裂隙精華突破品質上限（僅限飾品）。',
  },
  ja: {
    start: '開始アイテム',
    reset: 'リセット',
    reload: 'カタログ更新',
    validate: '検証',
    quality: '品質 ↑',
    qualityHelp:
      'ブリーチのエッセンスで品質上限を超える作業を使用（装飾品のみ）。',
  },
  es: {
    start: 'Objeto inicial',
    reset: 'Restablecer',
    reload: 'Actualizar catálogo',
    validate: 'Validar',
    quality: 'Calidad ↑',
    qualityHelp:
      'Usar Esencia de la Brecha para superar el límite de calidad (solo joyería).',
  },

  ru: {
    start: 'Исходный предмет',
    reset: 'Сброс',
    reload: 'Обновить каталог',
    validate: 'Подтвердить',
    quality: 'Качество ↑',
    qualityHelp:
      'Используйте Essence of the Breach, чтобы работать выше верхнего предела качества (только ювелирные изделия).',
  },
  th: {
    start: 'ไอเทมเริ่มต้น',
    reset: 'เริ่มใหม่',
    reload: 'รีเฟรชแคตตาล็อก',
    validate: 'ตรวจสอบ',
    quality: 'คุณภาพ↑',
    qualityHelp:
      'ใช้ Essence of the Breach เพื่อทำงานเหนือระดับคุณภาพ (เฉพาะอัญมณีเท่านั้น)',
  },
}
