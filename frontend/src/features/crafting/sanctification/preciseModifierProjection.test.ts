import { describe, expect, it } from 'vitest'
import type { Definition } from '../craftingApi'
import type { ConcreteItem } from '../workbenchApi'
import { preciseModifierProjection } from './preciseModifierProjection'

const d: Definition = {
  id: 'amulet:prefix:life',
  name: 'Healthy',
  text: '+11 to Life',
  tier: 1,
  layer: 'EXPLICIT',
  affixType: 'PREFIX',
  familyIds: ['Life'],
  weight: 1,
  tags: ['life'],
  stats: [{ id: 'base_maximum_life', min: 1, max: 100 }],
}
const modifier = { modifierId: d.id, values: { base_maximum_life: 11 } }
const state: ConcreteItem = {
  snapshotId: 'test',
  baseItemId: 'Metadata/Items/Amulets/FourAmulet9',
  itemLevel: 82,
  rarity: 'RARE',
  conditions: ['SANCTIFIED'],
  implicits: [],
  explicits: [modifier],
  catalystQuality: { type: 'FLESH', amount: 20 },
  sanctification: {
    policy: 'independent-stat-percent-v1',
    implicits: {},
    explicits: { [d.id]: { base_maximum_life: 79 } },
  },
}
describe('staged Sanctification then catalyst display composition', () => {
  it('rounds raw11 by0.79 to9, then quality1.20 to11', () => {
    const output = preciseModifierProjection(state, d, modifier)
    expect(output.values.base_maximum_life).toBe(11)
    expect(output.ratios.base_maximum_life).toEqual({
      numerator: 11n,
      denominator: 1n,
    })
    expect(modifier.values.base_maximum_life).toBe(11)
    expect(() =>
      preciseModifierProjection(state, d, {
        ...modifier,
        values: { base_maximum_life: 13.2 },
      }),
    ).toThrow()
  })
  it('combines source Jewel effect with matching refined quality and Sanctification once', () => {
    const jewel = { ...d, id: 'sapphire:prefix:life' }
    const item: ConcreteItem = {
      ...state,
      baseItemId: 'Metadata/Items/Jewels/JewelInt',
      explicits: [
        { modifierId: jewel.id, values: { base_maximum_life: 11 } },
        {
          modifierId: 'sapphire:crafted:CraftedJewelPrefixEffect',
          values: { display_source_value: 40 },
        },
      ],
      sanctification: {
        policy: 'independent-stat-percent-v1',
        implicits: {},
        explicits: { [jewel.id]: { base_maximum_life: 79 } },
      },
    }
    const output = preciseModifierProjection(item, jewel, item.explicits[0]!)
    expect(output.values.base_maximum_life).toBe(15)
    expect(output.ratios.base_maximum_life).toEqual({
      numerator: 15n,
      denominator: 1n,
    })
    expect(output.qualityStatus).toBe('JEWEL_EFFECT_AND_QUALITY_PROVISIONAL')
  })
  it('keeps catalyst eligibility restrictions while still applying an eligible Sanctification factor', () => {
    expect(
      preciseModifierProjection(state, { ...d, tags: ['mana'] }, modifier)
        .values.base_maximum_life,
    ).toBe(9)
    expect(
      preciseModifierProjection(
        { ...state, sanctification: null },
        { ...d, tags: ['unscalable'] },
        modifier,
      ).values.base_maximum_life,
    ).toBe(11)
  })
})

it('rounds sanctification before quality: minion3 with122% and50% quality is6', () => {
  const skill: Definition = {
    ...d,
    id: 'amulet:suffix:of-the-overseer',
    name: 'of the Overseer',
    text: '+3 to Level of all Minion Skills',
    tags: ['minion', 'gem'],
    stats: [{ id: 'minion_skill_gem_level_+', min: 3, max: 3 }],
  }
  const mod = {
    modifierId: skill.id,
    values: { 'minion_skill_gem_level_+': 3 },
  }
  const item: ConcreteItem = {
    ...state,
    explicits: [mod],
    catalystQuality: { type: 'NECROTIC', amount: 50 },
    qualityInfused: true,
    sanctification: {
      policy: 'independent-stat-percent-v1',
      implicits: {},
      explicits: { [skill.id]: { 'minion_skill_gem_level_+': 122 } },
    },
  }
  expect(
    preciseModifierProjection(item, skill, mod).values[
      'minion_skill_gem_level_+'
    ],
  ).toBe(6)
  expect(mod.values['minion_skill_gem_level_+']).toBe(3)
})

it('never changes implicit values, including legacy saved implicit factors', () => {
  const mod = {
    modifierId: 'implicit',
    values: { base_spirit_from_equipment: 15 },
  }
  const implicit: Definition = {
    ...d,
    id: 'implicit',
    layer: 'IMPLICIT',
    affixType: 'NONE',
    tags: [],
    stats: [{ id: 'base_spirit_from_equipment', min: 10, max: 15 }],
  }
  const item: ConcreteItem = {
    ...state,
    implicits: [mod],
    sanctification: {
      ...state.sanctification!,
      implicits: { implicit: { base_spirit_from_equipment: 122 } },
    },
  }
  expect(
    preciseModifierProjection(item, implicit, mod, 'implicits').values
      .base_spirit_from_equipment,
  ).toBe(15)
})
