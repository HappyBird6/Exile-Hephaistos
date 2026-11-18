import { useI18n } from '../../../shared/i18n/i18n'
import { localizedAction } from '../localizedCrafting'
import type { WorkbenchAction } from '../workbenchApi'
import { policyCandidateMessages } from './policyCandidateMessages'
import { probabilityText, quantityText } from './probabilityPresentation'
import type { PolicyCandidate } from './policyCandidates'
export function CandidateSummary({
  candidate,
}: {
  candidate: PolicyCandidate
}) {
  const { locale, name } = useI18n(),
    copy = policyCandidateMessages[locale]
  return (
    <>
      <dl>
        {(['success', 'rebase', 'failure', 'unresolved'] as const).map(
          (key) => (
            <div key={key}>
              <dt>{copy[key]}</dt>
              <dd>{probabilityText(candidate[key].lower, candidate[key])}</dd>
            </div>
          ),
        )}
      </dl>
      {candidate.rankStatus === 'OVERLAPPING' && <p>{copy.overlap}</p>}
      <details>
        <summary>{copy.uses}</summary>
        {candidate.usageStatus === 'LOWER_BOUND_ONLY' && <p>{copy.lower}</p>}
        <ul>
          {Object.entries(candidate.expectedUses)
            .filter(([material]) => !material.startsWith('WORK:'))
            .map(([material, amount]) => (
              <li key={material}>
                {material.startsWith('OMEN:')
                  ? name(
                      material.slice(5),
                      material.slice(5).replaceAll('_', ' '),
                    )
                  : localizedAction(material as WorkbenchAction)}
                :{' '}
                {amount.upper
                  ? `${quantityText(amount.lower)} – ${quantityText(amount.upper)}`
                  : `≥ ${quantityText(amount.lower)}`}
              </li>
            ))}
        </ul>
      </details>
    </>
  )
}
