import { formatNumber, useI18n } from '../../../shared/i18n/i18n'
import { revealAction } from '../abyss-workbench/revealAction'
import { useHistoryInitial } from '../session-history/useHistoryInitial'
import { extractSessionNotation } from '../session-notation/extractSessionNotation'
import {
  filmRulesetStatus,
  validProjectedCurrencies,
  validStepChance,
  verifiedFrameEvidence,
  type Film,
  type Frame,
} from '../workbenchHistory'
import { priceMessages } from './messages'

function Chance({ probability }: { probability: number | null }) {
  const { locale, t } = useI18n()
  return (
    <span className="crafting-cost-line__chance">
      {priceMessages[locale].actionChance}{' '}
      {probability === null
        ? t('ui.unknown')
        : formatNumber(
            probability,
            { style: 'percent', maximumSignificantDigits: 4 },
            locale,
          )}
    </span>
  )
}

function RecordedChance({
  film,
  frame,
  cursor,
}: {
  film: Film
  frame: Frame
  cursor: number
}) {
  const { initial } = useHistoryInitial(frame.state)
  if (
    !initial.data ||
    filmRulesetStatus(film, initial.data.rulesetIdentity) !== 'current'
  )
    return null
  const receipt = verifiedFrameEvidence(frame, initial.data)
  if (!receipt) return null
  if (frame.customExalt) {
    const choice = extractSessionNotation(film, cursor).steps.find(
      (step) => step.frameIndex === cursor,
    )?.customExalt
    return choice ? <Chance probability={choice.probability} /> : null
  }
  if (receipt.events.length !== 1) return null
  const event = receipt.events[0]!
  if (!['ADD', 'REMOVE', 'FRACTURE'].includes(event.kind)) return null
  return <Chance probability={event.selectionProbability} />
}

export function SelectedStepChance({
  film,
  cursor,
}: {
  film: Film | undefined
  cursor: number
}) {
  const frame = film?.frames[cursor]
  if (!film || !frame || cursor === 0 || frame.chaosRepeat) return null
  if (
    validStepChance(frame.projectedChance) &&
    film.rulesetIdentity &&
    frame.rulesetIdentity === film.rulesetIdentity &&
    ((frame.action === 'SESSION_PLAN' &&
      validProjectedCurrencies(frame.projectedCurrencies) &&
      !frame.evidence &&
      !frame.manual) ||
      (frame.action === revealAction &&
        frame.manual?.action === revealAction &&
        frame.manual.rulesetIdentity === film.rulesetIdentity))
  )
    return <Chance probability={frame.projectedChance.probability} />
  return frame.evidence &&
    (frame.customExalt || frame.evidence.events?.length === 1) ? (
    <RecordedChance film={film} frame={frame} cursor={cursor} />
  ) : null
}
