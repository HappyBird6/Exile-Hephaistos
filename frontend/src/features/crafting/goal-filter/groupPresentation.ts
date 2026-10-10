import type { Group, GroupType } from './types'
import type { GoalFilterLanguage } from './i18n'

export type GroupPresentation = GroupType | 'OR'

/** OR is a presentation of the existing unbounded at-least-one COUNT contract. */
export function presentedGroupType(
  group: Group,
  preferred?: GroupPresentation,
): GroupPresentation {
  if (
    group.type === 'COUNT' &&
    group.range?.min === 1 &&
    group.range.max === null
  )
    return preferred === 'COUNT' ? 'COUNT' : 'OR'
  return group.type
}

export const orHelp: Record<GoalFilterLanguage, string> = {
  en: 'At least one enabled condition must match.',
  ko: '활성 조건 중 하나 이상이 일치해야 합니다.',
  'zh-CN': '至少一个启用的条件必须匹配。',
  'zh-TW': '至少一個啟用的條件必須符合。',
  ja: '有効な条件のうち少なくとも1つが一致する必要があります。',
  es: 'Debe cumplirse al menos una condición activa.',
}
