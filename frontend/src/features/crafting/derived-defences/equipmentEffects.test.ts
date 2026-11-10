import { afterEach, describe, expect, it } from 'vitest'
import bases from '../topBases.json'
import { derivedWeapons } from './derivedWeapons'
import { derivedDefences } from './derivedDefences'
import { installedAugmentEffects, renderAugmentEffect } from '../augmentEffects'
import { currentItemText } from '../currentItemText'
import { uiText, locales } from '../../../shared/i18n/i18n'
import { augmentFeedbackLines } from '../augmentFeedback'
import { bondedEnabled, setBonded } from '../bonded'
import type { ConcreteItem } from '../workbenchApi'
import type { Definition } from '../craftingApi'

const bow = Object.values(bases).find((b) => b.name === 'Warmonger Bow')!
const state = (id = bow.id): ConcreteItem => ({
  snapshotId: 'test',
  baseItemId: id,
  itemLevel: 100,
  rarity: 'RARE',
  implicits: [],
  explicits: [],
  conditions: [],
  equipmentQuality: 0,
})
function definitions(item: ConcreteItem): Record<string, Definition> {
  return Object.fromEntries(
    item.explicits.map((m) => [
      m.modifierId,
      {
        id: m.modifierId,
        name: m.modifierId,
        text: m.modifierId,
        tier: 1,
        affixType: 'PREFIX',
        familyIds: [m.modifierId],
        layer: 'EXPLICIT',
        stats: Object.keys(m.values).map((id) => ({ id, min: 0, max: 1000 })),
      },
    ]),
  )
}
afterEach(() => {
  localStorage.clear()
})
describe('final equipment properties', () => {
  it('recalculates both weapon damage endpoints after rolls and quality, then applies Rune effects to the final totals', () => {
    const item = {
      ...state(),
      equipmentQuality: 20,
      augmentSockets: 2,
      installedAugments: [
        { slotIndex: 0, augmentId: 'Perfect_Iron_Rune' },
        { slotIndex: 1, augmentId: 'Perfect_Desert_Rune' },
      ],
      explicits: [
        {
          modifierId: 'physical',
          values: {
            local_minimum_added_physical_damage: 10,
            local_maximum_added_physical_damage: 20,
            'local_physical_damage_+%': 50,
          },
        },
      ],
    }
    const result = derivedWeapons(item)!
    expect(result.damage.physical?.[0]).toBeCloseTo((56 + 10) * 1.5 * 1.2 * 1.2)
    expect(result.damage.physical?.[1]).toBeCloseTo((84 + 20) * 1.5 * 1.2 * 1.2)
    expect(result.damage.fire).toEqual([17, 20])
    const rerolled = {
      ...item,
      explicits: [
        {
          modifierId: 'physical',
          values: {
            local_minimum_added_physical_damage: 20,
            local_maximum_added_physical_damage: 30,
            'local_physical_damage_+%': 60,
          },
        },
      ],
    }
    expect(derivedWeapons(rerolled)!.damage.physical?.[0]).toBeCloseTo(
      76 * 1.6 * 1.2 * 1.2,
    )
    expect(
      currentItemText({ ...item, explicits: [] }, {}, 'Warmonger Bow', 'Bows'),
    ).toContain('Physical Damage: 80.6-121')
  })
  it('rounds the generic 60% Augment amplification and applies percentage bonuses to the completed defence value', () => {
    const item = {
      ...state('Metadata/Items/Armours/Gloves/FourGlovesStr1'),
      augmentSockets: 1,
      installedAugments: [{ slotIndex: 0, augmentId: 'Perfect_Iron_Rune' }],
      explicits: [
        {
          modifierId: 'flat',
          values: { local_base_physical_damage_reduction_rating: 285 },
        },
        {
          modifierId: 'horror',
          values: { 'local_socketed_items_effect_+%': 60 },
        },
      ],
    }
    expect(installedAugmentEffects(item, 'en')[0]!.english).toContain(
      '32% increased Armour',
    )
    expect(derivedDefences(item, definitions(item)).armour?.value).toBe(396)
    expect(
      renderAugmentEffect(
        { template: '{v0}% increased effect per 10 Strength', values: ['7'] },
        1.6,
      ),
    ).toBe('11% increased effect per 10 Strength')
    const elemental = {
      ...item,
      baseItemId: bow.id,
      installedAugments: [{ slotIndex: 0, augmentId: 'Perfect_Desert_Rune' }],
    }
    expect(derivedWeapons(elemental)!.damage.fire).toEqual([27, 32])
  })
  it('never creates Runic Ward from a percentage alone, and computes flat Ward with its increases', () => {
    const item = {
      ...state('Metadata/Items/Amulets/Amulet1'),
      explicits: [{ modifierId: 'ward', values: { 'maximum_ward_+%': 20 } }],
    }
    expect(derivedDefences(item, definitions(item)).ward).toBeUndefined()
    item.explicits[0]!.values = {
      ...item.explicits[0]!.values,
      base_maximum_ward: 100,
    } as (typeof item.explicits)[0]['values']
    expect(derivedDefences(item, definitions(item)).ward?.value).toBe(120)
  })
  it('adds Bonded sentences without replacing the ordinary Rune effect or modifying the item state', () => {
    const item = {
      ...state('Metadata/Items/Armours/Gloves/FourGlovesStr1'),
      augmentSockets: 1,
      installedAugments: [{ slotIndex: 0, augmentId: 'Perfect_Iron_Rune' }],
    }
    const before = JSON.stringify(item)
    expect(installedAugmentEffects(item, 'en', false)).toHaveLength(1)
    const active = installedAugmentEffects(item, 'en', true)
    expect(active).toHaveLength(2)
    expect(active[0]!.english).toContain('20% increased Armour')
    expect(active[1]!.english).toContain('+20 to maximum Life')
    setBonded(true)
    expect(bondedEnabled()).toBe(true)
    expect(JSON.stringify(item)).toBe(before)
  })
  it('localizes Amulet and Boots classification across every supported locale, including Emergent Possibility', () => {
    for (const locale of locales.filter((l) => l !== 'en')) {
      expect(uiText('Amulet', locale)).not.toBe('Amulet')
      expect(uiText('Boots', locale)).not.toBe('Boots')
      expect(
        augmentFeedbackLines('Emergent_Possibility', locale)?.join('\n'),
      ).not.toMatch(/\bBoots\b/)
    }
    expect(uiText('Amulet', 'ko')).toBe('목걸이')
    expect(uiText('Boots', 'ko')).toBe('장화')
  })
})
