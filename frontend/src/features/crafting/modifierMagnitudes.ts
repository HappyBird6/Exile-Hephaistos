import { sanctificationFactor } from './sanctification/sanctification'
import { roundQualityRatio } from './qualityRoundingPolicy'
import rules from './augmentCrafting.json'
import type { ConcreteItem } from './workbenchApi'
import type { Definition } from './craftingApi'

/** Original provider rolls are frozen inputs. Projected provider values never feed back. */
export function modifierMagnitudeRatio(
  state: ConcreteItem,
  definition: Definition,
  layer: 'implicits' | 'explicits' = definition.layer === 'IMPLICIT' ||
  state.implicits.some((m) => m.modifierId === definition.id)
    ? 'implicits'
    : 'explicits',
) {
  if (
    layer !== 'explicits' ||
    (state.conditions.includes('SANCTIFIED') && state.sanctification == null) ||
    definition.tags?.includes('unscalable') ||
    /(^|\n)Grants Skill:/.test(definition.text)
  )
    return { numerator: 1n, denominator: 1n }
  let increased = 0n
  for (const rule of rules.magnitudeRules) {
    if (!rule.tags.every((tag) => definition.tags?.includes(tag))) continue
    for (const provider of state.explicits) {
      const original = provider.values[rule.stat]
      if (original == null) continue
      const factor = sanctificationFactor(state, provider, rule.stat)
      increased +=
        BigInt(roundQualityRatio(BigInt(original) * BigInt(factor), 100n)) *
        100n
    }
  }
  return increased === 0n
    ? { numerator: 1n, denominator: 1n }
    : { numerator: 10000n + increased, denominator: 10000n }
}
export function modifierMagnitudeValues(
  state: ConcreteItem,
  definition: Definition,
  values: Record<string, number>,
  layer?: 'implicits' | 'explicits',
): Record<string, number> {
  const ratio = modifierMagnitudeRatio(state, definition, layer)
  if (ratio.numerator === ratio.denominator) return values
  return Object.fromEntries(
    Object.entries(values).map(([id, value]) => [
      id,
      (value * Number(ratio.numerator)) / Number(ratio.denominator),
    ]),
  )
}
export function modifierMagnitudePercent(
  state: ConcreteItem,
  definition: Definition,
  layer?: 'implicits' | 'explicits',
) {
  const ratio = modifierMagnitudeRatio(state, definition, layer)
  return (Number(ratio.numerator) / Number(ratio.denominator) - 1) * 100
}
