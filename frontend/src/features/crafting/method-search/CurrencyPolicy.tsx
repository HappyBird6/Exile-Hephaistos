import { guideMessages } from './guideMessages'
import { workspaceMessages } from './workspaceMessages'
import { planningNotes } from './planningNotes'
import { useId, useRef } from 'react'
import policy from '../../../../../backend/src/main/resources/crafting/automatic-currency-policy-v1.json'
import { useI18n } from '../../../shared/i18n/i18n'
import { policyMessages } from './policyMessages'
import { acquisitionHints } from './planningMessages'
import './currency-policy.css'

const parts: Record<string, number[]> = {
  HYSTERIA_PARTS: [8, 9, 10],
  PRISMATIC_PARTS: [8],
  SWIFT_PARTS: [8, 11, 12, 13, 14],
  SOVEREIGN_PARTS: [11, 15, 16],
  EXPANSIVE_PARTS: [9],
  CYCLONIC_PARTS: [9, 8, 17],
  MYSTIC_PARTS: [17, 8, 9, 10],
}
const decisions: Record<string, number> = {
  USE: 2,
  CONDITIONAL: 3,
  EXCLUDED: 4,
  DEFERRED: 5,
}

export function CurrencyPolicy() {
  const { locale, name } = useI18n()
  const copy = policyMessages[locale]
  const dialog = useRef<HTMLDialogElement>(null)
  const button = useRef<HTMLButtonElement>(null)
  const title = useId()
  return (
    <>
      <button
        id="crafting-policy"
        ref={button}
        onClick={() => dialog.current?.showModal()}
      >
        {copy[0]}
      </button>
      <dialog
        className="currency-policy"
        ref={dialog}
        aria-labelledby={title}
        onClose={() => button.current?.focus()}
      >
        <h2 id={title}>{copy[0]}</h2>
        <form method="dialog">
          <button>{copy[1]}</button>
        </form>
        <h3>{guideMessages[locale].scope}</h3>
        <p>
          {planningNotes[locale][0]} {planningNotes[locale][1]}
        </p>
        <h3>{guideMessages[locale].calculations}</h3>
        <p>{planningNotes[locale][2]}</p>
        <h3>{guideMessages[locale].records}</h3>
        <p>{workspaceMessages[locale].hint}</p>
        <p>{workspaceMessages[locale].unknown}</p>
        <h3>{guideMessages[locale].supported}</h3>
        <p>{copy[6]}</p>
        <p>{copy[21]}</p>
        <ol>
          {policy.entries.map((entry) => {
            const slug = entry.sourceUrl.split('/').at(-1)!
            const family = parts[entry.condition]
            return (
              <li key={entry.id} value={entry.number}>
                <strong>{name(slug, slug.replaceAll('_', ' '))}</strong>
                {' — '}
                {copy[decisions[entry.decision]!]}
                {family && <p>{family.map((i) => copy[i]).join(' · ')}</p>}
                {(entry.condition.startsWith('TEMPORARY_') ||
                  entry.condition === 'GOAL_ACQUISITION') && (
                  <p>{acquisitionHints[locale]}</p>
                )}
                {entry.condition === 'BREACH_QUALITY' && <p>{copy[18]}</p>}
                {entry.number === 100 && <p>{copy[19]}</p>}
                {entry.decision === 'DEFERRED' && <p>{copy[20]}</p>}
              </li>
            )
          })}
        </ol>
      </dialog>
    </>
  )
}
