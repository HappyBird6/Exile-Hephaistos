import type { Locale } from '../../../shared/i18n/i18n'

/** User-authorized example only; it neither allocates a real tree node nor changes crafting. */
export const temporaryAmuletPassive = (locale: Locale): string =>
  ({
    en: 'Temporary passive allocation example (user policy): Heavy Ammunition; effects are not simulated',
    ko: '임시 패시브 할당 예시 (사용자 정책): Heavy Ammunition · 효과는 계산하지 않음',
    ja: '仮のパッシブ割り当て例（ユーザー方針）: Heavy Ammunition・効果は計算しません',
    'zh-CN': '临时天赋分配示例（用户策略）：Heavy Ammunition；不模拟效果',
    'zh-TW': '暫時天賦配置範例（使用者方針）：Heavy Ammunition；不模擬效果',
    es: 'Ejemplo temporal de pasiva (política del usuario): Heavy Ammunition; no se simulan sus efectos',
  })[locale]
