import type { Definition } from './craftingApi'
import type { AppliedItem, ConcreteItem } from './workbenchApi'
import { verifiedCatalystQuality } from './catalystQuality'
export const qualityCapChangeVersion = 'user-confirmed-quality-cap-preserve-v2'
// Stored quality survives cap loss; operations do not refill it on cap growth.
export function qualityCapChangeMatches(
  before: ConcreteItem,
  result: Pick<AppliedItem, 'state' | 'applied' | 'events' | 'assumptions'>,
  definitions: Record<string, Definition>,
): boolean {
  const old = before.catalystQuality
  const next = result.state.catalystQuality
  return (
    verifiedCatalystQuality(result.state, definitions) &&
    (old == null
      ? next == null
      : next?.type === old.type && next.amount === old.amount) &&
    !result.assumptions.some(
      (a) =>
        a.id === qualityCapChangeVersion ||
        a.id === 'unverified-quality-cap-clamp-v1',
    )
  )
}
