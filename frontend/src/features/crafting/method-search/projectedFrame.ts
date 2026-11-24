import type { Frame, PlannedCurrency } from '../workbenchHistory'
import type { ConcreteItem } from '../workbenchApi'
import type { AutomaticArrow, AutomaticNode } from './automaticApi'
import { currencyId } from '../session-notation/extractSessionNotation'
import { planOmenId } from './usePlanAction'
import { compare } from '../path-tree/validation'
import { isRepeatable, methodPoint } from './methodObservation'
import { stepProbabilities } from './stepProbabilities'

// A compound method supplies one observed outcome, even when it uses several currencies.
export function projectedFrames(
  previous: ConcreteItem,
  current: AutomaticArrow,
  outcome: AutomaticArrow['points'][number]['outcomes'][number],
  result: AutomaticNode,
  cycles: number,
  identity: string,
): Frame[] | null {
  const cases = outcome.conditional?.filter(
    (c) =>
      BigInt(c.probability.upper.numerator) > 0n &&
      c.retainedFromSource.every((id) =>
        previous.explicits.some((m) => m.modifierId === id),
      ),
  )
  const member =
    cases?.find((c) =>
      [...c.retainedFromSource, ...c.local.map((m) => m.modifierId)].every(
        (id) => result.item.explicits.some((m) => m.modifierId === id),
      ),
    ) ?? cases?.[0]
  if (outcome.conditional?.length && !member) {
    return null
  }
  const explicits = member
    ? [
        ...previous.explicits.filter((m) =>
          member.retainedFromSource.includes(m.modifierId),
        ),
        ...member.local,
      ]
    : result.item.explicits
  const finalState = { ...result.item, explicits } as ConcreteItem
  const intermediate = member?.intermediate ?? []
  const actions = [...current.initial, ...current.cycle]
  const used = member?.preparationOnly
    ? current.initial.slice(0, intermediate.length + 1)
    : actions
  // Historical jobs can lack intermediate witnesses. Keep their original grouped record.
  const witnessed = intermediate.length + 1 === used.length
  const frames: Frame[] = []
  for (const [index, craft] of used.entries()) {
    let projectedWork: Frame['projectedWork']
    const projectedCurrencies: PlannedCurrency[] = []
    if (craft.operation === 'DESECRATE')
      projectedWork =
        craft.side === 'PREFIX' ? 'DESECRATE_PREFIX' : 'DESECRATE_SUFFIX'
    if (craft.purpose === 'QUALITY_BASE_CHECKPOINT')
      projectedWork = 'QUALITY_20'
    if (craft.purpose === 'QUALITY_CHECKPOINT') projectedWork = 'QUALITY_40'
    const id = craft.currency && currencyId(craft.currency)
    if (id && !craft.purpose.startsWith('QUALITY_'))
      projectedCurrencies.push({
        currencyId: id,
        omenIds: craft.omens.map(planOmenId),
        ...(member?.expectedChaos && craft.currency === 'CHAOS'
          ? {
              expectedUses:
                Number(member.expectedChaos.numerator) /
                Number(member.expectedChaos.denominator),
            }
          : isRepeatable(current) && current.cycle.includes(craft)
            ? { maxUses: cycles }
            : {}),
      })
    const witness = witnessed ? intermediate[index] : undefined
    const state = witness
      ? ({
          ...witness,
          explicits: witness.explicits.map(
            (m) =>
              previous.explicits.find((p) => p.modifierId === m.modifierId) ??
              m,
          ),
        } as ConcreteItem)
      : finalState
    if (!witnessed && frames.length) {
      frames[0]!.projectedCurrencies!.push(...projectedCurrencies)
      if (projectedWork) frames[0]!.projectedWork = projectedWork
    } else
      frames.push({
        state,
        action: 'SESSION_PLAN',
        rulesetIdentity: identity,
        projectedCurrencies,
        ...(projectedWork ? { projectedWork } : {}),
      })
  }
  const point = methodPoint(current, cycles)
  if (
    point?.complete &&
    !isRepeatable(current) &&
    used.length === frames.length
  ) {
    const odds = stepProbabilities(
      current,
      point,
      outcome,
      frames,
      0,
      used.map((action) => action.purpose),
    )
    for (const step of odds) {
      const craft = used[step.first]
      if (
        step.first !== step.last ||
        !craft?.currency ||
        ![
          'ORDINARY',
          'GOAL_ACQUISITION',
          'REFILL_AFTER_ANNULMENT',
          'LAST_RESORT_EXALTED',
        ].includes(craft.purpose)
      )
        continue
      const { lower, upper } = step.probability
      if (compare(lower, upper) !== 0) continue
      const p = Number(lower.numerator) / Number(lower.denominator)
      if (
        Number.isFinite(p) &&
        p >= 0 &&
        p <= 1 &&
        (p > 0 || BigInt(lower.numerator) === 0n)
      )
        frames[step.first]!.projectedChance = { probability: p }
    }
  }
  return frames
}
