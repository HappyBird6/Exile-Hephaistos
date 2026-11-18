import { useI18n } from '../../../shared/i18n/i18n'
import { qualityGoalMessages } from '../goal-filter/qualityGoalMessages'
import { qualityWorkMessages } from './qualityWorkMessages'
import { localizedAction } from '../localizedCrafting'
import type { WorkbenchAction } from '../workbenchApi'
import type { AutomaticAction } from './automaticApi'
const desecrate = {
  en: 'Desecrate',
  ko: '훼손',
  'zh-CN': '亵渎',
  'zh-TW': '褻瀆',
  ja: '冒涜',
  es: 'Profanar',
  ru: 'Осквернить',
  th: 'ดูหมิ่นศาสนา',
}

export function planOmenId(omen: string) {
  return (
    'Omen_of_' +
    omen
      .toLowerCase()
      .split('_')
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
      .join('_')
  )
}

export function usePlanAction() {
  const { locale, name } = useI18n()
  return (action: AutomaticAction) => {
    if (action.operation === 'DESECRATE')
      return desecrate[locale] + (action.side === 'PREFIX' ? ' · P' : ' · S')
    if (action.purpose === 'QUALITY_BASE_CHECKPOINT')
      return qualityGoalMessages[locale].work20
    if (action.purpose === 'QUALITY_CHECKPOINT')
      return qualityWorkMessages[locale].work

    let label = localizedAction(action.currency as WorkbenchAction)
    if (action.comparison) {
      const { baseline, afterBlocking } = action.comparison
      const before = (
        (100 * baseline.targetWeight) /
        baseline.totalWeight
      ).toFixed(2)
      const after = (
        (100 * afterBlocking.targetWeight) /
        afterBlocking.totalWeight
      ).toFixed(2)
      label += ` · ${before}%(~${after}%)`
    }
    const omens = action.omens.map((omen) =>
      name(planOmenId(omen), omen.replaceAll('_', ' ')),
    )
    return [label, ...omens].join(' + ')
  }
}
