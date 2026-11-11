import type { Initial } from '../craftingApi'
import { concreteInitial } from '../workbenchApi'
import type { ConcreteItem } from '../workbenchApi'
import type { ModifierPoolMode } from './modifierPool'

/** Rolls change display magnitude, not candidate eligibility. Keep all other state (including
 * hidden slots, augments and future fields) in the key so new mechanics cannot share stale pools. */
export function modifierPoolKey(
  catalog: Initial | undefined,
  state: ConcreteItem | undefined,
  mode: ModifierPoolMode,
) {
  const reference =
    mode === 'static' && catalog ? concreteInitial(catalog) : state
  return [
    'crafting',
    'modifier-pools',
    catalog?.rulesetIdentity,
    reference && {
      ...reference,
      explicits: reference.explicits.map((modifier) => {
        const result = { ...modifier }
        delete (result as Partial<typeof modifier>).values
        return result
      }),
    },
    mode,
  ] as const
}
