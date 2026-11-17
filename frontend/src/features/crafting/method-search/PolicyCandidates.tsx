import { useState } from 'react'
import { useI18n } from '../../../shared/i18n/i18n'
import { localizedAction } from '../localizedCrafting'
import type { WorkbenchAction } from '../workbenchApi'
import type { AutomaticAction, AutomaticView } from './automaticApi'
import { probabilityText, quantityText } from './probabilityPresentation'
import {
  defaultRareOutcomePolicy,
  type RareOutcomePolicy,
} from './policyCandidates'
import { policyCandidateMessages } from './policyCandidateMessages'

export function PolicyCandidates({
  view,
  onRestart,
  onPolicy,
  onInspect,
}: {
  view: AutomaticView
  onRestart?: (() => void) | undefined
  onPolicy?: ((policy: RareOutcomePolicy) => void) | undefined
  onInspect: (node: number) => void
}) {
  const { locale, name } = useI18n()
  const copy = policyCandidateMessages[locale]
  const [settings, setSettings] = useState(
    view.rareOutcomePolicy ?? defaultRareOutcomePolicy,
  )
  const [busy, setBusy] = useState(false)
  const actionName = (a: AutomaticAction) =>
    [
      a.currency ? localizedAction(a.currency as WorkbenchAction) : '✦',
      ...a.omens.map((o) => name(o, o.replaceAll('_', ' '))),
    ].join(' + ')
  const ending = view.methods.some((m) =>
    m.points.some((p) => p.outcomes.some((o) => o.kind === 'REBASE')),
  )
  return (
    <section className="policy-candidates" aria-label={copy.title}>
      <h3>{copy.title}</h3>
      <p>{copy.scope}</p>
      <div className="policy-candidate-grid">
        {(view.candidates ?? []).map((candidate, index) => (
          <article key={candidate.id}>
            <h4>{index + 1}</h4>
            <dl>
              {(['success', 'rebase', 'failure', 'unresolved'] as const).map(
                (key) => (
                  <div key={key}>
                    <dt>{copy[key]}</dt>
                    <dd>
                      {probabilityText(candidate[key].lower, candidate[key])}
                    </dd>
                  </div>
                ),
              )}
            </dl>
            {candidate.rankStatus === 'OVERLAPPING' && <p>{copy.overlap}</p>}
            <details>
              <summary>{copy.uses}</summary>
              {candidate.usageStatus === 'LOWER_BOUND_ONLY' && (
                <p>{copy.lower}</p>
              )}
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
            <details>
              <summary>{copy.decisions}</summary>
              {candidate.decisions.map((decision) => {
                const method = view.methods.find(
                  (m) => m.id === decision.method,
                )!
                const point = method.points.find(
                  (p) => p.cycles === candidate.cycles,
                )
                return (
                  <div key={decision.node}>
                    <button onClick={() => onInspect(decision.node)}>
                      {copy.state} {decision.node}
                    </button>
                    :{' '}
                    {[...method.initial, ...method.cycle]
                      .map(actionName)
                      .join(' → ')}
                    <ul>
                      {point?.outcomes.map((outcome, i) => (
                        <li key={i}>
                          {probabilityText(
                            outcome.probability.lower,
                            outcome.probability,
                          )}{' '}
                          →{' '}
                          {outcome.kind === 'REBASE'
                            ? copy.rebase
                            : outcome.kind === 'GOAL'
                              ? copy.success
                              : ['DEAD', 'LOSS', 'RETRY'].includes(outcome.kind)
                                ? copy.failure
                                : outcome.target === null
                                  ? copy.unresolved
                                  : `${copy.state} ${outcome.target}`}
                        </li>
                      ))}
                    </ul>
                  </div>
                )
              })}
            </details>
          </article>
        ))}
      </div>
      {ending && (
        <div role="status">
          <p>{copy.end}</p>
          <p>{copy.separate}</p>
          {onRestart && (
            <button
              disabled={busy}
              onClick={() => {
                setBusy(true)
                onRestart()
              }}
            >
              {copy.restart}
            </button>
          )}
        </div>
      )}
      {onPolicy && (
        <details>
          <summary>{copy.settings}</summary>
          <label>
            <input
              type="checkbox"
              checked={settings.enabled}
              onChange={(e) =>
                setSettings({ ...settings, enabled: e.target.checked })
              }
            />
            {copy.settings}
          </label>
          {(
            ['groupPartsPerMillion', 'maximumMethodPartsPerMillion'] as const
          ).map((key, i) => (
            <label key={key}>
              {i === 0 ? copy.group : copy.total}
              <input
                type="number"
                min="0"
                max="100"
                step="0.01"
                value={settings[key] / 10000}
                onChange={(e) => {
                  const value = e.target.valueAsNumber
                  if (Number.isFinite(value) && value >= 0 && value <= 100)
                    setSettings({
                      ...settings,
                      [key]: Math.round(value * 10000),
                    })
                }}
              />
            </label>
          ))}
          <button
            disabled={busy}
            onClick={() => {
              setBusy(true)
              onPolicy(settings)
            }}
          >
            {copy.apply}
          </button>
        </details>
      )}
    </section>
  )
}
