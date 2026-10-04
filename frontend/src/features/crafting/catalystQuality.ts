import type { Definition } from './craftingApi'
import type { ConcreteItem } from './workbenchApi'
import { maximumQuality } from './qualityLimit'
import { roundQualityRatio } from './qualityRoundingPolicy'

export const catalystTypes = {
  FLESH: { name: 'Flesh', tags: ['life'] },
  NEURAL: { name: 'Neural', tags: ['mana'] },
  CARAPACE: {
    name: 'Carapace',
    tags: ['defences', 'armour', 'evasion', 'energyshield'],
  },
  UUL_NETOL: { name: "Uul-Netol's", tags: ['physical'] },
  XOPH: { name: "Xoph's", tags: ['fire'] },
  TUL: { name: "Tul's", tags: ['cold'] },
  ESH: { name: "Esh's", tags: ['lightning'] },
  CHAYULA: { name: "Chayula's", tags: ['chaos'] },
  REAVER: { name: 'Reaver', tags: ['attack'] },
  SIBILANT: { name: 'Sibilant', tags: ['caster'] },
  SKITTERING: { name: 'Skittering', tags: ['speed'] },
  ADAPTIVE: { name: 'Adaptive', tags: ['attribute'] },
  NECROTIC: { name: 'Necrotic', tags: ['minion'] },
} as const
export type CatalystQuality = {
  type: keyof typeof catalystTypes
  amount: number
}
export const catalystQualityVersion = 'catalyst-quality-display-round-v2'
export const catalystBase = (base: string) =>
  [
    'Metadata/Items/Amulets/FourAmulet9',
    'Metadata/Items/Rings/FourRing1',
    'Metadata/Items/Jewels/JewelInt',
  ].includes(base)

export function qualityShape(
  value: unknown,
): value is CatalystQuality | null | undefined {
  if (value == null) return true
  if (typeof value !== 'object' || Array.isArray(value)) return false
  const q = value as Record<string, unknown>
  return (
    Object.keys(q).length === 2 &&
    typeof q.type === 'string' &&
    Object.hasOwn(catalystTypes, q.type) &&
    Number.isInteger(q.amount) &&
    Number(q.amount) >= 0 &&
    Number(q.amount) <= 100
  )
}
export function verifiedCatalystQuality(
  state: ConcreteItem,
  definitions: Record<string, Definition>,
): boolean {
  if (!qualityShape(state.catalystQuality)) return false
  if (state.catalystQuality == null) return true
  const cap = maximumQuality(state, definitions)
  return (
    catalystBase(state.baseItemId) &&
    cap !== null &&
    state.catalystQuality.amount <= cap
  )
}
const integerStats = new Set([
  'base_maximum_life',
  'base_maximum_mana',
  'base_maximum_energy_shield',
  'base_evasion_rating',
  'additional_strength',
  'additional_dexterity',
  'additional_intelligence',
  'additional_all_attributes',
  'base_fire_damage_resistance_%',
  'base_cold_damage_resistance_%',
  'base_lightning_damage_resistance_%',
  'base_chaos_damage_resistance_%',
  'base_resist_all_elements_%',
  'spell_damage_+%',
  'fire_damage_+%',
  'cold_damage_+%',
  'lightning_damage_+%',
  'chaos_damage_+%',
  'physical_damage_reduction_rating_+%',
  'maximum_energy_shield_+%',
  'evasion_rating_+%',
  'base_cast_speed_+%',
])
export function catalystProjection(
  d: Definition,
  values: Record<string, number>,
  quality?: CatalystQuality | null,
) {
  if (!quality) return { values, status: 'NO_TYPED_QUALITY' }
  if (
    Object.hasOwn(values, 'local_maximum_quality_+') ||
    d.tags?.includes('unscalable')
  )
    return { values, status: 'UNSCALABLE' }
  if (!catalystTypes[quality.type].tags.some((tag) => d.tags?.includes(tag)))
    return { values, status: 'NO_MATCH' }
  const ironImplicit =
    d.id === 'iron-ring:implicit:added-physical-damage-to-attacks' &&
    Object.keys(values).length === 2 &&
    Object.hasOwn(values, 'attack_minimum_added_physical_damage') &&
    Object.hasOwn(values, 'attack_maximum_added_physical_damage')
  const reviewed =
    ironImplicit ||
    (d.id === 'sapphire:suffix:of-enchanting' &&
      d.stats?.length === 1 &&
      d.stats[0]?.id === 'display_cast_speed_percent') ||
    (d.weight !== undefined &&
      d.weight > 0 &&
      d.stats?.length === 1 &&
      integerStats.has(d.stats[0]!.id))
  if (
    !reviewed ||
    !d.stats ||
    d.stats.some((s) => s.min < 0) ||
    Object.values(values).some((v) => !Number.isSafeInteger(v) || v < 0)
  )
    return { values, status: 'UNREVIEWED_NUMERIC_SEMANTICS' }
  const displayed = Object.fromEntries(
    Object.entries(values).map(([id, value]) => [
      id,
      roundQualityRatio(BigInt(value) * BigInt(100 + quality.amount), 100n),
    ]),
  )
  if (!Object.values(displayed).every(Number.isSafeInteger))
    return { values, status: 'UNREVIEWED_NUMERIC_SEMANTICS' }
  return { values: displayed, status: 'SCALED_INTEGER' }
}
