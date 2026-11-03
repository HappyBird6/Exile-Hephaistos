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
describe('exact catalyst / jewel-effect / Sanctification display composition', () => {
  it('multiplies raw11 by1.20 and0.79 without first rounding13.2 to13', () => {
    const output = preciseModifierProjection(state, d, modifier)
    expect(output.values.base_maximum_life).toBeCloseTo(10.428, 12)
    expect(output.ratios.base_maximum_life).toEqual({
      numerator: 10428000n,
      denominator: 1000000n,
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
    expect(output.values.base_maximum_life).toBeCloseTo(14.5992, 12)
    expect(output.ratios.base_maximum_life).toEqual({
      numerator: 14599200n,
      denominator: 1000000n,
    })
    expect(output.qualityStatus).toBe('JEWEL_EFFECT_AND_QUALITY_PROVISIONAL')
  })
  it('keeps catalyst eligibility restrictions while still applying an eligible Sanctification factor', () => {
    expect(
      preciseModifierProjection(state, { ...d, tags: ['mana'] }, modifier)
        .values.base_maximum_life,
    ).toBeCloseTo(8.69, 12)
    expect(
      preciseModifierProjection(
        { ...state, sanctification: null },
        { ...d, tags: ['unscalable'] },
        modifier,
      ).values.base_maximum_life,
    ).toBe(11)
  })
})
