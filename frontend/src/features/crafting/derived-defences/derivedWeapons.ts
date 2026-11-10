import type { Definition } from '../craftingApi'
import bases from '../topBases.json'
import { itemStatValues } from '../augmentEffects'
import { augmentPropertyBonuses } from './augmentProperties'
import type { ConcreteItem } from '../workbenchApi'

export type Damage = 'physical' | 'fire' | 'cold' | 'lightning' | 'chaos'
export function derivedWeapons(
  state: ConcreteItem,
  bonded = false,
  definitions: Record<string, Definition> = {},
) {
  const base = Object.values(bases).find((b) => b.id === state.baseItemId)
  const lines =
    base && 'sourceProperties' in base ? base.sourceProperties.en : []
  // Normal Crude Bow source properties: https://poe2db.tw/us/Crude_Bow (also in the Bows source page).
  const legacy =
    state.baseItemId === 'Metadata/Items/Weapons/TwoHandWeapons/Bows/FourBow1'
      ? [
          'Physical Damage: 6-9',
          'Attacks per Second: 1.2',
          'Critical Hit Chance: 5%',
        ]
      : []
  const properties = lines.length ? lines : legacy
  if (
    !state.baseItemId.startsWith('Metadata/Items/Weapons/') ||
    !properties.some((s) => /^Physical Damage:/.test(s))
  )
    return undefined
  const stats = itemStatValues(state, definitions)
  const runes = augmentPropertyBonuses(state, bonded, definitions)
  const quality = state.equipmentQuality
  const damage: Partial<Record<Damage, [number, number] | null>> = {}
  for (const key of [
    'physical',
    'fire',
    'cold',
    'lightning',
    'chaos',
  ] as const) {
    const source = properties.find((line) =>
      line.startsWith(key[0]!.toUpperCase() + key.slice(1) + ' Damage:'),
    )
    const baseRoll = source
      ?.split(':')[1]
      ?.match(/\d+(?:\.\d+)?/g)
      ?.map(Number) ?? [0, 0]
    const flat = [
      stats[`local_minimum_added_${key}_damage`] ?? 0,
      stats[`local_maximum_added_${key}_damage`] ?? 0,
    ]
    const increased =
      key === 'physical' ? (stats['local_physical_damage_+%'] ?? 0) : 0
    const values = baseRoll.map(
      (v, i) =>
        (v + flat[i]!) *
          (1 + increased / 100) *
          (1 + (quality ?? 0) / 100) *
          (1 + (key === 'physical' ? runes.physicalPercent : 0) / 100) +
        runes[key][i]!,
    ) as [number, number]
    if (values.some((v) => v > 0)) damage[key] = quality == null ? null : values
  }
  const read = (label: string) =>
    Number(
      properties
        .find((line) => line.startsWith(label + ':'))
        ?.split(':')[1]
        ?.match(/\d+(?:\.\d+)?/)?.[0],
    )
  const attacks =
    read('Attacks per Second') *
    (1 + (stats['local_attack_speed_+%'] ?? 0) / 100)
  const critical =
    read('Critical Hit Chance') *
    (1 + (stats['local_critical_strike_chance_+%'] ?? 0) / 100)
  return {
    damage,
    attacks: Number.isFinite(attacks) ? attacks : null,
    critical: Number.isFinite(critical) ? critical : null,
  }
}
