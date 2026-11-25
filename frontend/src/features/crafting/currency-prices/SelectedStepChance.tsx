import { formatNumber, useI18n } from '../../../shared/i18n/i18n'
import { useHistoryInitial } from '../session-history/useHistoryInitial'
import { extractSessionNotation } from '../session-notation/extractSessionNotation'
import {
  filmRulesetStatus,
  validStepChance,
  verifiedFrameEvidence,
  type Film,
  type Frame,
  type ProjectedChance,
  type ProjectedOmenOutcomes,
} from '../workbenchHistory'
import { priceMessages } from './messages'
import { acquisitionRouteMessages } from '../method-search/acquisitionRouteMessages'
import {
  projectedStepProbability,
  supportProbabilityText,
} from '../supportProbability'

function RecordedProbability({ probability }: { probability: number | null }) {
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
    return choice ? (
      <RecordedProbability probability={choice.probability} />
    ) : null
  }
  if (receipt.events.length !== 1) return null
  const event = receipt.events[0]!
  if (!['ADD', 'REMOVE', 'FRACTURE'].includes(event.kind)) return null
  return <RecordedProbability probability={event.selectionProbability} />
}

export function SupportChance({
  chance,
  route = false,
  outcomes,
}: {
  chance: ProjectedChance
  route?: boolean
  outcomes?: ProjectedOmenOutcomes
}) {
  const { locale } = useI18n()
  const copy = priceMessages[locale]
  if (outcomes)
    return (
      <span
        className="crafting-cost-line__outcomes"
        role="group"
        aria-label={copy.actionChance}
      >
        <strong>
          {copy.success} {supportProbabilityText(chance, locale)}
        </strong>
        <span>
          {copy.recoverable}{' '}
          {supportProbabilityText(outcomes.recoverable, locale)}
        </span>
        <span>
          {copy.failure} {supportProbabilityText(outcomes.failure, locale)}
        </span>
      </span>
    )
  return (
    <span className="crafting-cost-line__chance crafting-cost-line__chance--projected">
      {route
        ? acquisitionRouteMessages[locale].chance + ' · '
        : copy.actionChance + ' '}
      {supportProbabilityText(chance, locale)}
    </span>
  )
}

export function RouteChanceSummary({
  film,
}: {
  film?: Film | null | undefined
}) {
  return film?.rulesetIdentity && validStepChance(film.projectedRouteChance) ? (
    <SupportChance chance={film.projectedRouteChance} route />
  ) : null
}

export function SelectedStepChance({
  film,
  cursor,
  sessionSummary = false,
}: {
  film: Film | undefined
  cursor: number
  sessionSummary?: boolean
}) {
  const frame = film?.frames[cursor]
  if (!film || !frame) return null
  if (
    sessionSummary &&
    film.rulesetIdentity &&
    validStepChance(film.projectedRouteChance)
  )
    return <SupportChance chance={film.projectedRouteChance} route />
  const step = cursor > 0 ? projectedStepProbability(film, frame) : undefined
  return (
    <>
      {step ? (
        <SupportChance {...step} />
      ) : cursor > 0 &&
        !frame.chaosRepeat &&
        frame.evidence &&
        (frame.customExalt || frame.evidence.events?.length === 1) ? (
        <RecordedChance film={film} frame={frame} cursor={cursor} />
      ) : null}
    </>
  )
}
