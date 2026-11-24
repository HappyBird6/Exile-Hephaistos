import type { Locale } from '../../../shared/i18n/i18n'
import type { Definition, Initial } from '../craftingApi'
import { localizedModifierText } from '../localizedModifiers'

type Tier = Definition & { stats: NonNullable<Definition['stats']> }
type Bounds = { hasLowerBound?: boolean; hasUpperBound?: boolean }

/** Format the edge of the eligible tiers. Keep both real tiers if hybrid stats have incomparable limits. */
function tierEdge(tiers: Tier[], edge: 'min' | 'max', locale: Locale) {
  const values = tiers.map((d) =>
    Object.fromEntries(d.stats.map((s) => [s.id, s[edge]])),
  )
  const labels = new Set<string>()
  tiers.forEach((tier, index) => {
    const value = values[index]!
    const dominated = values.some((other) => {
      const comparisons = tier.stats.map(
        (s) => (other[s.id]! - value[s.id]!) * (edge === 'min' ? 1 : -1),
      )
      return comparisons.every((n) => n <= 0) && comparisons.some((n) => n < 0)
    })
    if (!dominated) labels.add(localizedModifierText(tier, value, locale))
  })
  return [...labels].join(' / ')
}

/** The backend has already selected the tiers compatible with the input on this card. */
export function acquisitionTierText(
  ids: readonly string[],
  definitions: Initial['modifiers'],
  locale: Locale,
  bounds: Bounds,
) {
  const tiers = ids.map((id) => definitions[id])
  if (!tiers.every((d): d is Tier => !!d?.stats?.length)) {
    return ids
      .map((id) =>
        definitions[id]
          ? localizedModifierText(definitions[id], undefined, locale)
          : id,
      )
      .join(' / ')
  }
  if (!bounds.hasUpperBound) return tierEdge(tiers, 'min', locale)
  const high = tierEdge(tiers, 'max', locale)
  if (!bounds.hasLowerBound) return high
  const low = tierEdge(tiers, 'min', locale)
  return low === high ? low : `${low} – ${high}`
}
