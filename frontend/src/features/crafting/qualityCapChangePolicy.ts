import type { Definition } from './craftingApi'
import type { AppliedItem, ConcreteItem } from './workbenchApi'
import { maximumQuality } from './qualityLimit'

export const qualityCapChangeVersion = 'unverified-quality-cap-clamp-v1'
// Accept only the declared after-operation model; never refill quality on a larger cap.
export function qualityCapChangeMatches(
  before: ConcreteItem,
  result: Pick<AppliedItem, 'state' | 'applied' | 'events' | 'assumptions'>,
  definitions: Record<string, Definition>,
): boolean {
  const after = result.state
  const old = before.catalystQuality
  const next = after.catalystQuality
  const models = result.assumptions.filter(
    (a) => a.id === qualityCapChangeVersion,
  )
  if (old == null) return next == null && models.length === 0
  const cap = maximumQuality(after, definitions)
  const amount =
    result.applied && cap !== null ? Math.min(old.amount, cap) : old.amount
  if (next?.type !== old.type || next.amount !== amount) return false
  if (amount === old.amount) return models.length === 0
  const model = models[0]
  return (
    result.applied &&
    maximumQuality(before, definitions) === 40 &&
    cap === 20 &&
    result.events.some(
      (e) =>
        e.kind === 'REMOVE' &&
        e.modifierId === 'amulet:prefix:essence-maximum-quality',
    ) &&
    models.length === 1 &&
    model?.candidateUnit === 'quality amount' &&
    model.n === 1 &&
    model.candidates.length === 0 &&
    model.min === amount &&
    model.max === old.amount &&
    model.sourceUrl === 'https://poe2db.tw/us/Quality' &&
    model.reason.includes('UNVERIFIED')
  )
}
