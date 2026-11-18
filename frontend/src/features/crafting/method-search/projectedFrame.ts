import type { Frame, PlannedCurrency } from '../workbenchHistory'
import type { ConcreteItem } from '../workbenchApi'
import type { AutomaticArrow, AutomaticNode } from './automaticApi'
import { currencyId } from '../session-notation/extractSessionNotation'
import { planOmenId } from './usePlanAction'
import { isRepeatable } from './methodObservation'

// A compound method supplies one observed outcome, even when it uses several currencies.
export function projectedFrame(
  previous: ConcreteItem,
  current: AutomaticArrow,
  outcome: AutomaticArrow['points'][number]['outcomes'][number],
  result: AutomaticNode,
  cycles: number,
  identity: string,
): Frame | null {
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
  let projectedWork: Frame['projectedWork']
  const projectedCurrencies: PlannedCurrency[] = []
  const used = member?.preparationOnly
    ? current.initial
    : [...current.initial, ...current.cycle]
  for (const craft of used) {
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
        ...(isRepeatable(current) && current.cycle.includes(craft)
          ? { maxUses: cycles }
          : {}),
      })
  }
  const explicits = member
    ? [
        ...previous.explicits.filter((m) =>
          member.retainedFromSource.includes(m.modifierId),
        ),
        ...member.local,
      ]
    : result.item.explicits
  return {
    state: { ...result.item, explicits } as ConcreteItem,
    action: 'SESSION_PLAN',
    rulesetIdentity: identity,
    projectedCurrencies,
    ...(projectedWork ? { projectedWork } : {}),
  }
}
