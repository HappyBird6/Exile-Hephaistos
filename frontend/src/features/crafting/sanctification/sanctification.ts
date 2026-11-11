import { roundQualityRatio } from '../qualityRoundingPolicy'
import type { Definition } from '../craftingApi'
import type {
  ConcreteItem,
  AppliedItem,
  WorkbenchAction,
} from '../workbenchApi'
import { equivalent, sameRolls } from './equivalence'

export type Sanctification = {
  policy: 'independent-stat-percent-v1' | 'explicit-stat-percent-v2'
  implicits: Record<string, Record<string, number>>
  explicits: Record<string, Record<string, number>>
}
const structural = new Set([
  'local_maximum_quality_+',
  'local_maximum_prefixes_allowed_+',
  'local_maximum_suffixes_allowed_+',
  'local_attribute_requirements_+%',
  'local_charm_slots',
  'local_item_additional_skill_slots',
  'local_item_benefit_socketable_as_if_helmet',
  'local_display_grants_spear_throw_skill',
  'local_always_hit',
  'local_jewel_effect_base_radius',
])
function object(value: unknown): value is Record<string, unknown> {
  return !!value && typeof value === 'object' && !Array.isArray(value)
}
export function sanctificationShape(item: Record<string, unknown>): boolean {
  const value = item.sanctification
  if (value == null) return true
  if (
    !object(value) ||
    !['independent-stat-percent-v1', 'explicit-stat-percent-v2'].includes(
      String(value.policy),
    ) ||
    Object.keys(value).sort().join('|') !== 'explicits|implicits|policy' ||
    item.rarity !== 'RARE' ||
    !Array.isArray(item.conditions) ||
    !item.conditions.includes('SANCTIFIED')
  )
    return false
  return (['implicits', 'explicits'] as const).every((layer) => {
    const factors = value[layer]
    const modifiers = item[layer]
    if (!object(factors) || !Array.isArray(modifiers)) return false
    return Object.entries(factors).every(([id, values]) => {
      const modifier = modifiers.find(
        (m: unknown) => object(m) && m.modifierId === id,
      ) as Record<string, unknown> | undefined
      return (
        modifier &&
        object(modifier.values) &&
        object(values) &&
        Object.keys(values).length > 0 &&
        Object.entries(values).every(
          ([stat, factor]) =>
            Object.hasOwn(modifier.values as object, stat) &&
            !structural.has(stat) &&
            Number.isInteger(factor) &&
            Number(factor) >= 78 &&
            Number(factor) <= 122,
        )
      )
    })
  })
}
export function sanctificationTargets(
  d: Definition,
  values: Record<string, number>,
): string[] {
  return d.tags?.includes('unscalable')
    ? []
    : Object.keys(values)
        .filter((s) => !structural.has(s))
        .sort()
}
export function validSanctification(
  state: ConcreteItem,
  definitions: Record<string, Definition>,
): boolean {
  if (!sanctificationShape(state as unknown as Record<string, unknown>))
    return false
  const value = state.sanctification
  if (!value) return true
  return (['implicits', 'explicits'] as const).every((layer) => {
    if (layer === 'implicits' && Object.keys(value.implicits).length === 0)
      return true
    if (layer === 'implicits' && value.policy === 'explicit-stat-percent-v2')
      return false
    const expected = state[layer].filter(
      (m) =>
        definitions[m.modifierId] &&
        sanctificationTargets(definitions[m.modifierId]!, m.values).length,
    )
    return (
      Object.keys(value[layer]).length === expected.length &&
      expected.every(
        (m) =>
          Object.keys(value[layer][m.modifierId] ?? {})
            .sort()
            .join('|') ===
          sanctificationTargets(definitions[m.modifierId]!, m.values).join('|'),
      )
    )
  })
}
/** Apply to original source-unit rolls exactly once, before display formatting. */
export function sanctifiedValues(
  state: ConcreteItem,
  modifier: ConcreteItem['explicits'][number],
  layer: 'implicits' | 'explicits' = 'explicits',
): Record<string, number> {
  const factors =
    layer === 'implicits'
      ? {}
      : (state.sanctification?.explicits[modifier.modifierId] ?? {})
  return Object.fromEntries(
    Object.entries(modifier.values).map(([id, value]) => [
      id,
      roundQualityRatio(BigInt(value) * BigInt(factors[id] ?? 100), 100n),
    ]),
  )
}
export function sanctificationTransitionMatches(
  state: ConcreteItem,
  result: AppliedItem,
  action: WorkbenchAction,
  activeOmens: readonly string[],
  definitions: Record<string, Definition>,
): boolean {
  if (!validSanctification(result.state, definitions)) return false
  if (
    !result.applied ||
    action !== 'DIVINE' ||
    !activeOmens.includes('Omen_of_Sanctification')
  )
    return (
      equivalent(
        result.state.sanctification ?? null,
        state.sanctification ?? null,
      ) &&
      (!result.applied || !state.conditions.includes('SANCTIFIED'))
    )
  return (
    state.rarity === 'RARE' &&
    state.conditions.length === 0 &&
    state.sanctification == null &&
    result.state.sanctification?.policy === 'explicit-stat-percent-v2' &&
    Object.keys(result.state.sanctification.implicits).length === 0 &&
    sameRolls(result.state.implicits, state.implicits) &&
    sameRolls(result.state.explicits, state.explicits) &&
    result.consumedOmens.length === 1 &&
    result.consumedOmens[0] === 'Omen_of_Sanctification' &&
    !activeOmens.includes('Omen_of_the_Blessed') &&
    result.events.length === 0 &&
    result.assumptions.length === 1 &&
    result.assumptions[0]?.id === 'sanctification-explicit-rounded-v2'
  )
}

/** Frozen game imports contain their final values instead of invented original rolls and factors. */
export const observedSanctified = (state: ConcreteItem): boolean =>
  state.conditions.includes('SANCTIFIED') && state.sanctification == null
