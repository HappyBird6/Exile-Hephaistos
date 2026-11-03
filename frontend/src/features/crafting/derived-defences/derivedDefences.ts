import type { ConcreteItem } from '../workbenchApi'
import type { Definition } from '../craftingApi'
import bases from '../topBases.json'
import { sanctifiedValues } from '../sanctification/sanctification'

export type Defence = 'armour' | 'evasion' | 'energyShield'
export type DefenceResult = {
  base: number
  flat: number
  increased: number
  quality: number | null
  value: number | null
}
const statRules: Record<
  Defence,
  { flat: readonly string[]; increased: readonly string[] }
> = {
  armour: {
    flat: ['local_base_physical_damage_reduction_rating'],
    increased: [
      'local_physical_damage_reduction_rating_+%',
      'local_armour_and_evasion_+%',
      'local_armour_and_energy_shield_+%',
    ],
  },
  evasion: {
    flat: ['local_base_evasion_rating'],
    increased: [
      'local_evasion_rating_+%',
      'local_armour_and_evasion_+%',
      'local_evasion_and_energy_shield_+%',
    ],
  },
  energyShield: {
    flat: ['local_energy_shield'],
    increased: [
      'local_energy_shield_+%',
      'local_armour_and_energy_shield_+%',
      'local_evasion_and_energy_shield_+%',
    ],
  },
}
// Legacy Body/Helmet observations match CraftingPage; Stocky is in the captured base evidence.
const legacy: Record<string, Partial<Record<Defence, number>>> = {
  'Metadata/Items/Armours/Gloves/FourGlovesStr1': { armour: 15 },
  'Metadata/Items/Armours/Helmets/FourHelmetStr1': { armour: 29 },
  'Metadata/Items/Armours/BodyArmours/FourBodyStr1': { armour: 45 },
}

/** (base + local flat) * (1 + local increased / 100) * (1 + quality / 100).
 * Global defences, observed item property totals, and catalogue display text never enter the sum.
 */
export function derivedDefences(
  state: ConcreteItem,
  definitions: Record<string, Definition>,
): Partial<Record<Defence, DefenceResult>> {
  const base =
    (Object.values(bases).find((b) => b.id === state.baseItemId) as
      Partial<Record<Defence, number>> | undefined) ?? legacy[state.baseItemId]
  if (!base || !state.baseItemId.startsWith('Metadata/Items/Armours/'))
    return {}
  const result: Partial<Record<Defence, DefenceResult>> = {}
  for (const key of ['armour', 'evasion', 'energyShield'] as const) {
    const amount = base[key] ?? 0
    if (amount <= 0) continue
    let flat = 0,
      increased = 0
    const rule = statRules[key]
    for (const layer of ['implicits', 'explicits'] as const) {
      for (const modifier of state[layer]) {
        const definition = definitions[modifier.modifierId]
        if (!definition) continue
        const values = sanctifiedValues(state, modifier, layer)
        for (const stat of definition.stats ?? []) {
          const value = values[stat.id]
          if (value === undefined) continue
          if (rule.flat.includes(stat.id)) flat += value
          if (rule.increased.includes(stat.id)) increased += value
        }
      }
    }
    const quality = state.equipmentQuality ?? null
    result[key] = {
      base: amount,
      flat,
      increased,
      quality,
      value:
        quality === null
          ? null
          : (amount + flat) * (1 + increased / 100) * (1 + quality / 100),
    }
  }
  return result
}

/** Observations are evidence, not an input to recomputation; neither side is overwritten. */
export function defenceObservation(computed: number | null, observed?: number) {
  return {
    computed,
    observed: observed ?? null,
    status:
      observed === undefined
        ? ('UNOBSERVED' as const)
        : computed === null
          ? ('UNKNOWN_QUALITY' as const)
          : Math.abs(computed - observed) < 0.05
            ? ('AGREES_AT_DISPLAY_PRECISION' as const)
            : ('CONFLICT' as const),
  }
}
