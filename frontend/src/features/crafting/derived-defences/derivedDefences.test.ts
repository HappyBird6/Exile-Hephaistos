import { describe, expect, it } from 'vitest'
import bases from '../topBases.json'
import type { Definition } from '../craftingApi'
import type { ConcreteItem } from '../workbenchApi'
import { derivedDefences, defenceObservation } from './derivedDefences'

const base = Object.values(bases).find(
  (b) => b.armour > 0 && 'energyShield' in b && b.energyShield > 0,
)!
const state: ConcreteItem = {
  snapshotId: 'test',
  baseItemId: base.id,
  itemLevel: 82,
  rarity: 'RARE',
  implicits: [],
  explicits: [],
  conditions: [],
  equipmentQuality: 20,
}
function definition(id: string, stat: string, tags: string[]): Definition {
  return {
    id,
    name: id,
    text: id,
    tier: 1,
    affixType: 'PREFIX',
    layer: 'EXPLICIT',
    familyIds: [id],
    tags,
    stats: [{ id: stat, min: 0, max: 1000 }],
  }
}
describe('local defence projection', () => {
  it('uses real base armour/ES with flat then local increased then quality; global and observed totals do not enter', () => {
    const defs = {
      flat: definition('flat', 'local_base_physical_damage_reduction_rating', [
        'defences',
        'armour',
      ]),
      inc: definition('inc', 'local_armour_and_energy_shield_+%', []),
      global: definition('global', 'global_armour_evasion_energy_shield_+%', [
        'defences',
        'armour',
      ]),
    }
    const item = {
      ...state,
      explicits: [
        {
          modifierId: 'flat',
          values: { local_base_physical_damage_reduction_rating: 10 },
        },
        {
          modifierId: 'inc',
          values: { 'local_armour_and_energy_shield_+%': 25 },
        },
        {
          modifierId: 'global',
          values: { 'global_armour_evasion_energy_shield_+%': 100 },
        },
      ],
    }
    const result = derivedDefences(item, defs)
    expect(result.armour?.value).toBeCloseTo((base.armour + 10) * 1.25 * 1.2)
    expect(result.energyShield?.value).toBeCloseTo(
      ('energyShield' in base ? base.energyShield! : 0) * 1.25 * 1.2,
    )
    expect(defenceObservation(result.armour!.value, 9999)).toMatchObject({
      observed: 9999,
      status: 'CONFLICT',
    })
    expect(
      item.explicits[0]?.values.local_base_physical_damage_reduction_rating,
    ).toBe(10)
  })
  it('keeps unknown quality unknown and applies sanctification once rounding the sanctified modifier before applying quality', () => {
    const d = definition(
      'flat',
      'local_base_physical_damage_reduction_rating',
      ['armour'],
    )
    const item: ConcreteItem = {
      ...state,
      conditions: ['SANCTIFIED'],
      explicits: [
        {
          modifierId: 'flat',
          values: { local_base_physical_damage_reduction_rating: 11 },
        },
      ],
      sanctification: {
        policy: 'independent-stat-percent-v1',
        implicits: {},
        explicits: {
          flat: { local_base_physical_damage_reduction_rating: 79 },
        },
      },
    }
    expect(derivedDefences(item, { flat: d }).armour?.value).toBeCloseTo(
      (base.armour + 9) * 1.2,
    )
    expect(
      derivedDefences({ ...item, equipmentQuality: null }, { flat: d }).armour
        ?.value,
    ).toBeNull()
    expect(defenceObservation(null, 20).status).toBe('UNKNOWN_QUALITY')
  })
})
