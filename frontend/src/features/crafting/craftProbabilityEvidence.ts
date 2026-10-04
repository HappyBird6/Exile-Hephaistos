import type { AppliedItem } from './workbenchApi'

export const modifierWeightSources = {
  solar: 'https://poe2db.tw/us/Amulets',
  ring: 'https://poe2db.tw/us/Rings',
  helmet: 'https://poe2db.tw/us/Helmets_str',
  belt: 'https://poe2db.tw/us/Belts',
  sceptre: 'https://poe2db.tw/us/Sceptres',
  body: 'https://poe2db.tw/us/Body_Armours_str',
  stocky: 'https://poe2db.tw/us/Gloves_str',
  bow: 'https://poe2db.tw/us/Bows',
  wand: 'https://poe2db.tw/us/Wands',
} as const

export function craftProbabilityEvidence(
  evidence: Pick<AppliedItem, 'action' | 'events'> &
    Partial<Pick<AppliedItem, 'snapshotId'>>,
) {
  if (evidence.events.some((event) => event.kind === 'ADD')) {
    if (
      evidence.snapshotId?.startsWith('poe2db-sapphire-') ||
      evidence.action.includes('LIQUID_')
    ) {
      return {
        weighted: false,
        text: 'Explicit Sapphire simulator model: uniform eligible candidates and legal Liquid removals. Actual game weights and failed-removal behavior are unverified; assumptions are listed below.',
      }
    }
    if (
      evidence.action.includes('ESSENCE') ||
      evidence.action.endsWith('_ALLOY')
    ) {
      return {
        weighted: false,
        text: 'Material result selected from verified eligible targets. Any uniform choices are listed below.',
      }
    }
    return {
      weighted: true,
      text: 'Model probability uses published PoE2DB table weights, not verified game odds.',
    }
  }
  if (
    evidence.events.some((event) =>
      ['REROLL_EXPLICIT', 'REROLL_IMPLICIT'].includes(event.kind),
    )
  ) {
    return {
      weighted: false,
      text: 'Numeric values rerolled within their current source ranges.',
    }
  }
  return {
    weighted: false,
    text: evidence.events.some((event) => event.kind === 'REMOVE')
      ? 'Modifier removal uses the uniform assumption listed below.'
      : 'Operation conditions and any roll assumptions are listed below.',
  }
}
