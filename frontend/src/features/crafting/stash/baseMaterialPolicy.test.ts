import { describe, expect, it } from 'vitest'
import { baseMaterialTargets, supportsBaseMaterial } from './baseMaterialPolicy'
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
    ).toHaveLength(232)
  })
  it('preserves an unclassified legacy base without guessing its target groups', () => {
    expect(
      supportsBaseMaterial(
        { id: 'Preserved_Collarbone', category: 'Currency' },
        'legacy-unclassified',
      ),
    ).toBe(true)
    expect(
      supportsBaseMaterial(
        resource('Greater_Essence_of_Enhancement'),
        'legacy-unclassified',
        [],
      ),
    ).toBe(true)
    expect(
      supportsBaseMaterial(
        resource('Perfect_Desert_Rune'),
        'legacy-unclassified',
        [],
      ),
    ).toBe(false)
  })
  it('uses source target classes and keeps Omen and ordinary Currency independent', () => {
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
    expect(supportsBaseMaterial(resource('Runic_Alloy'), solar, [])).toBe(true)
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
    expect(supportsBaseMaterial(resource('Runic_Alloy'), solar)).toBe(
      supportsBaseMaterial(resource('Runic_Alloy'), solar, []),
    )
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
  it('includes Armour-targeting Essence on Body Armour independently of action availability', () => {
    const body = Object.values(bases).find(
      (b) => basePolicy(b.id)?.itemClass === 'Body Armours',
    )!.id
    expect(baseMaterialTargets(body)).toContain('Armour')
    for (const actions of [undefined, [], ['PERFECT_ESSENCE_BODY']]) {
      expect(
        supportsBaseMaterial(
          resource('Greater_Essence_of_Enhancement'),
          body,
          actions,
        ),
      ).toBe(true)
      expect(
        supportsBaseMaterial(
          resource('Perfect_Essence_of_the_Body'),
          body,
          actions,
        ),
      ).toBe(true)
      expect(
        supportsBaseMaterial(
          resource('Greater_Essence_of_Flames'),
          body,
          actions,
        ),
      ).toBe(false)
    }
    expect(
      supportsBaseMaterial(
        resource('Greater_Essence_of_Enhancement'),
        solar,
        [],
      ),
    ).toBe(false)
  })
  it('filters desecration and quality by item class, never by current rarity or quality', () => {
    const currency = (id: string) => ({ id, category: 'Currency' as const })
    const bow = Object.values(bases).find(
      (b) => basePolicy(b.id)?.itemClass === 'Bows',
    )!.id
    const wand = Object.values(bases).find(
      (b) => basePolicy(b.id)?.itemClass === 'Wands',
    )!.id
    for (const actions of [undefined, []]) {
      expect(
        supportsBaseMaterial(currency('Preserved_Rib'), gloves, actions),
      ).toBe(true)
      expect(
        supportsBaseMaterial(currency('Preserved_Jawbone'), gloves, actions),
      ).toBe(false)
      expect(
        supportsBaseMaterial(currency('Ancient_Collarbone'), solar, actions),
      ).toBe(true)
      expect(
        supportsBaseMaterial(currency('Altered_Collarbone'), gloves, actions),
      ).toBe(false)
      expect(
        supportsBaseMaterial(
          currency('Preserved_Cranium'),
          'Metadata/Items/Jewels/JewelStr',
          actions,
        ),
      ).toBe(true)
      expect(
        supportsBaseMaterial(currency('Preserved_Cranium'), solar, actions),
      ).toBe(false)
      expect(
        supportsBaseMaterial(currency('Armourers_Scrap'), gloves, actions),
      ).toBe(true)
      expect(
        supportsBaseMaterial(currency('Armourers_Scrap'), bow, actions),
      ).toBe(false)
      expect(
        supportsBaseMaterial(currency('Blacksmiths_Whetstone'), bow, actions),
      ).toBe(true)
      expect(
        supportsBaseMaterial(currency('Blacksmiths_Whetstone'), wand, actions),
      ).toBe(false)
    }
  })
})
