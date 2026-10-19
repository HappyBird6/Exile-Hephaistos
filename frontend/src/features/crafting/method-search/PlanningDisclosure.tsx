import { useI18n } from '../../../shared/i18n/i18n'
import type { AutomaticView } from './automaticApi'
import { noPlan, planningNotes } from './planningNotes'
export function PlanningDisclosure({
  view,
  rarities,
}: {
  view: AutomaticView | null
  rarities: string[]
}) {
  const { locale } = useI18n()
  const text = planningNotes[locale]
  return (
    <aside className="method-map__scope">
      <details>
        <summary>{text[3]}</summary>
        <p>
          {text[3]}: {rarities.join(', ')}
        </p>
      </details>
      {view?.reason === 'NO_PLAN_IN_SUPPORTED_SCOPE' && (
        <p role="status">{noPlan[locale]}</p>
      )}
      {view?.status === 'PAUSED' && view.canResume === false && (
        <p role="status">{text[4]}</p>
      )}
    </aside>
  )
}
