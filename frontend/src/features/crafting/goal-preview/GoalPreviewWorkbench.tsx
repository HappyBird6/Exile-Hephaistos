import { useI18n } from '../../../shared/i18n/i18n'
import { CraftStart } from '../goal-filter/CraftStart'
import { GoalPreviewResults } from './GoalPreviewResults'
import { previewGoalHints } from './previewTabNames'

export function GoalPreviewWorkbench({ active }: { active: boolean }) {
  const { locale } = useI18n()
  return (
    <CraftStart
      active={active}
      renderPreview={(input) =>
        input ? (
          <GoalPreviewResults {...input} />
        ) : (
          <p>{previewGoalHints[locale]}</p>
        )
      }
    />
  )
}
