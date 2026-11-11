import { expect, it } from 'vitest'
import rules from './augmentCrafting.json'
import { modifierMagnitudeValues } from './modifierMagnitudes'
import { derivedWeapons } from './derived-defences/derivedWeapons'
import type { Definition } from './craftingApi'
import type { ConcreteItem } from './workbenchApi'
const base: ConcreteItem = {
  snapshotId: 'test',
  baseItemId: 'Metadata/Items/Weapons/TwoHandWeapons/Bows/FourBow1',
  itemLevel: 100,
  rarity: 'RARE',
  conditions: [],
  implicits: [],
  explicits: [],
  equipmentQuality: 0,
}
const definition = (tags: string[], stat = 'value'): Definition => ({
  id: stat,
  name: stat,
  text: stat,
  affixType: 'PREFIX',
  layer: 'EXPLICIT',
  tier: 1,
  tags,
  familyIds: [stat],
  stats: [{ id: stat, min: 0, max: 1000 }],
})
it('applies all nine tag rules without changing original rolls or feeding amplified providers back', () => {
  for (const rule of rules.magnitudeRules) {
    const item = {
      ...base,
      explicits: [{ modifierId: 'provider', values: { [rule.stat]: 20 } }],
    }
    expect(
      modifierMagnitudeValues(item, definition(rule.tags), { value: 100 }),
    ).toEqual({ value: 120 })
    expect(
      modifierMagnitudeValues(
        item,
        definition(rule.tags),
        { value: 100 },
        'implicits',
      ),
    ).toEqual({ value: 100 })
  }
  const stat = 'heist_enchantment_physical_mod_effect_+%',
    item = {
      ...base,
      explicits: [{ modifierId: stat, values: { [stat]: 15 } }],
    }
  expect(
    modifierMagnitudeValues(
      item,
      definition(['physical'], stat),
      item.explicits[0]!.values,
    )[stat],
  ).toBe(17.25)
  expect(
    modifierMagnitudeValues(item, definition(['physical']), { value: 100 })
      .value,
  ).toBe(115)
  expect(item.explicits[0]!.values[stat]).toBe(15)
})
it('adds overlapping increases and limits elemental damage amplification to damage modifiers', () => {
  const item = {
    ...base,
    explicits: [
      {
        modifierId: 'fire',
        values: { 'heist_enchantment_fire_mod_effect_+%': 20 },
      },
      {
        modifierId: 'elemental',
        values: { 'local_explicit_elemental_damage_mod_effect_+%': 15 },
      },
    ],
  }
  expect(
    modifierMagnitudeValues(item, definition(['fire', 'elemental', 'damage']), {
      value: 100,
    }).value,
  ).toBe(135)
  expect(
    modifierMagnitudeValues(
      item,
      definition(['fire', 'elemental', 'resistance']),
      { value: 100 },
    ).value,
  ).toBe(120)
  expect(
    modifierMagnitudeValues(
      { ...item, conditions: ['SANCTIFIED'] },
      definition(['fire', 'elemental', 'damage']),
      { value: 135 },
    ).value,
  ).toBe(135)
})
it('recalculates final weapon damage from amplified explicit rolls', () => {
  const item = {
    ...base,
    explicits: [
      {
        modifierId: 'physical',
        values: { 'heist_enchantment_physical_mod_effect_+%': 15 },
      },
      {
        modifierId: 'flat',
        values: {
          local_minimum_added_physical_damage: 20,
          local_maximum_added_physical_damage: 40,
        },
      },
      { modifierId: 'inc', values: { 'local_physical_damage_+%': 100 } },
    ],
  }
  const definitions = {
    physical: definition(
      ['physical'],
      'heist_enchantment_physical_mod_effect_+%',
    ),
    flat: definition(
      ['physical', 'damage'],
      'local_minimum_added_physical_damage',
    ),
    inc: definition(['physical', 'damage'], 'local_physical_damage_+%'),
  }
  expect(derivedWeapons(item, false, definitions)!.damage.physical).toEqual([
    (6 + 23) * 2.15,
    (9 + 46) * 2.15,
  ])
})

it('never magnifies a fractured provider through a historical Sanctification factor', () => {
  const stat = 'heist_enchantment_physical_mod_effect_+%'
  for (const fractured of [false, true]) {
    const item: ConcreteItem = {
      ...base,
      conditions: ['SANCTIFIED'],
      explicits: [
        { modifierId: 'provider', values: { [stat]: 20 }, fractured },
      ],
      sanctification: {
        policy: 'explicit-stat-percent-v2',
        implicits: {},
        explicits: { provider: { [stat]: 122 } },
      },
    }
    expect(
      modifierMagnitudeValues(item, definition(['physical']), { value: 100 }),
    ).toEqual({ value: fractured ? 120 : 124 })
  }
})
