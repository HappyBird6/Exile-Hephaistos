import { describe, expect, it } from 'vitest'
import { supportsBaseMaterial } from './baseMaterialPolicy'
import { visibilityPolicy } from './visibilityPolicy'
import { materials } from '../materials'
import { basePolicy } from '../baseRegistry'
import { augments } from '../socketAugments'
import bases from '../topBases.json'

const solar = 'Metadata/Items/Amulets/FourAmulet9'
const gloves = 'Metadata/Items/Armours/Gloves/FourGlovesStr1'
const resource = (id: string) => materials.find((m) => m.id === id)!

describe('base material policy', () => {
  it('keeps all globally selectable materials until a base is actually selected', () => {
    for (const entry of materials)
      expect(supportsBaseMaterial(entry)).toBe(true)
    expect(
      materials.filter((m) => m.category === 'Augments' && visibilityPolicy(m)),
    ).toHaveLength(34)
  })
  it('uses the server base eligibility and never filters Omen or ordinary Currency', () => {
    expect(
      supportsBaseMaterial(resource('Greater_Essence_of_the_Body'), solar, [
        'GREATER_ESSENCE_BODY',
      ]),
    ).toBe(true)
    expect(
      supportsBaseMaterial(resource('Perfect_Essence_of_the_Body'), solar, [
        'GREATER_ESSENCE_BODY',
      ]),
    ).toBe(false)
    expect(supportsBaseMaterial(resource('Runic_Alloy'), solar, [])).toBe(false)
    expect(
      supportsBaseMaterial(resource('Flesh_Catalyst'), solar, [
        'CATALYST_FLESH',
      ]),
    ).toBe(true)
    expect(
      supportsBaseMaterial(resource('Refined_Flesh_Catalyst'), solar, [
        'CATALYST_FLESH',
      ]),
    ).toBe(false)
    expect(supportsBaseMaterial(resource('Omen_of_Whittling'), solar, [])).toBe(
      true,
    )
    expect(
      supportsBaseMaterial(
        { id: 'Artificers_Orb', category: 'Currency' },
        solar,
        [],
      ),
    ).toBe(true)
    expect(supportsBaseMaterial(resource('Runic_Alloy'), solar)).toBe(true)
  })
  it('uses registered socket support and the exact effect class, even before sockets exist', () => {
    expect(
      supportsBaseMaterial(resource('Perfect_Desert_Rune'), solar, []),
    ).toBe(false)
    expect(
      supportsBaseMaterial(resource('Perfect_Desert_Rune'), gloves, []),
    ).toBe(true)
    for (const base of Object.values(bases))
      for (const augment of augments) {
        const policy = basePolicy(base.id)
        const expected =
          (policy?.socketExecutionMaximum ?? 0) > 0 &&
          !!augment.effects[policy?.itemClass ?? '']
        expect(
          supportsBaseMaterial(
            { id: augment.id, category: 'Augments' },
            base.id,
            [],
          ),
        ).toBe(expected)
      }
  })
})
