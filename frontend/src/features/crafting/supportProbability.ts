import type { Locale } from '../../shared/i18n/i18n'
import {
  validProjectedCurrencies,
  projectedWhittlingOutcomes,
  validStepChance,
  type Film,
  type Frame,
  type ProjectedChance,
} from './workbenchHistory'
import { revealAction } from './abyss-workbench/revealAction'
import { priceMessages } from './currency-prices/messages'

type Rounding = 'floor' | 'ceil' | 'halfExpand'
const formats = new Map<string, Intl.NumberFormat>()

function percent(value: number, locale: Locale, roundingMode: Rounding) {
  const notation = value > 0 && value < 1e-7 ? 'scientific' : 'standard'
  const key = `${locale}/${notation}/${roundingMode}`
  let format = formats.get(key)
  if (!format) {
    const options = {
      style: 'percent',
      maximumSignificantDigits: 4,
      notation,
      roundingMode,
    } as const
    format = new Intl.NumberFormat(locale, options)
    formats.set(key, format)
  }
  return format.format(value)
}

/** Outward rounding keeps narrow intervals distinct from a point probability. */
function estimateText(chance: ProjectedChance, locale: Locale) {
  const checked = validStepChance(chance) ? chance : undefined
  const lower = checked?.probabilityBounds?.lower ?? checked?.probability ?? 0
  const upper = checked?.probabilityBounds?.upper ?? checked?.probability ?? 1
  const low = percent(lower, locale, 'floor')
  const high = percent(upper, locale, 'ceil')
  const hundred = percent(1, locale, 'ceil')
  if (lower === upper) {
    if (low === high) return low
    const point = percent(lower, locale, 'halfExpand')
    return point === hundred ? `> ${low}` : `≈ ${point}`
  }
  if (lower > 0 && upper === 1) return `≥ ${low}`
  if (lower === 0 && upper < 1)
    return high === hundred ? `< ${hundred}` : `≤ ${high}`
  return `${low} – ${upper < 1 && high === hundred ? '< ' : ''}${high}`
}

export function supportProbabilityText(
  chance: ProjectedChance,
  locale: Locale,
) {
  const text = estimateText(chance, locale)
  return chance.assumesEligibleReveal
    ? `${text} (${priceMessages[locale].assumedReveal})`
    : text
}

export function projectedStepProbability(film: Film, frame: Frame) {
  if (
    frame.chaosRepeat ||
    !film.rulesetIdentity ||
    frame.rulesetIdentity !== film.rulesetIdentity
  )
    return undefined
  const outcomes = projectedWhittlingOutcomes(frame)
  if (outcomes) return { chance: outcomes.success, outcomes }
  if (!validStepChance(frame.projectedChance)) return undefined
  return (frame.action === 'SESSION_PLAN' &&
    validProjectedCurrencies(frame.projectedCurrencies) &&
    !frame.evidence &&
    !frame.manual) ||
    (frame.action === revealAction &&
      frame.manual?.action === revealAction &&
      frame.manual.rulesetIdentity === film.rulesetIdentity)
    ? { chance: frame.projectedChance }
    : undefined
}
