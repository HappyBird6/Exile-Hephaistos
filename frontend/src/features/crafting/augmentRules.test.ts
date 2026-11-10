import { expect, it } from 'vitest'
import {
  installedAugmentEffects,
  observedAugmentEffects,
} from './augmentEffects'
import { basePolicy, baseAffixCapacity } from './baseRegistry'
import { canInstallAugment } from './socketAugments'
import { equipmentQualityRefusal } from './qualityCurrency'
import { effectiveDefinitions } from './augmentRules'
import bases from './topBases.json'
import rules from './augmentCrafting.json'
import type { ConcreteItem } from './workbenchApi'
import type { Definition } from './craftingApi'
function item(itemClass: string, ids: string[] = []): ConcreteItem {
  return {
    snapshotId: 'test',
    baseItemId: Object.values(bases).find(
      (b) => basePolicy(b.id)?.itemClass === itemClass,
    )!.id,
    itemLevel: 100,
    rarity: 'RARE',
    implicits: [],
    explicits: [],
    conditions: [],
    equipmentQuality: 0,
    augmentSockets: 2,
    installedAugments: ids.map((augmentId, slotIndex) => ({
      augmentId,
      slotIndex,
    })),
  }
}
it('boosts other socketed Runes, preserves provider strength, and anchors unidentified effects', () => {
  const state = item('Wands', [
    'Legacy_of_Runeseekers_Call',
    'Perfect_Desert_Rune',
  ])
  expect(installedAugmentEffects(state, 'en').map((e) => e.english)).toEqual([
    '75% increased effect of Socketed Runes',
    'Gain 21% of Damage as Extra Fire Damage',
  ])
  const observed = {
    ...state,
    observations: {
      socketCount: null,
      augmentEffects: ['Gain 21% of Damage as Extra Fire Damage'],
      grantedSkills: [],
      augmentEffectScale: 1.75,
    },
  }
  expect(observedAugmentEffects(observed)).toEqual(
    observed.observations.augmentEffects,
  )
  expect(
    observedAugmentEffects({ ...observed, installedAugments: [] }),
  ).toEqual(['Gain 12% of Damage as Extra Fire Damage'])
})
it('activates Idol Bonded effects through Fox without the global toggle', () => {
  const state = item('Body Armours', ['Fox_Idol'])
  const effects = installedAugmentEffects(state, 'en', false)
  expect(
    effects.some(
      (e) => e.bonded && e.english === '+5% to Quality of all Skills',
    ),
  ).toBe(true)
  expect(effects.some((e) => e.english.includes('display additional'))).toBe(
    false,
  )
  expect(
    installedAugmentEffects({ ...state, installedAugments: [] }, 'en', false),
  ).toEqual([])
})
it('binds Serle and grants one extra suffix, while Grit enables quality up to 40', () => {
  const state = item('One Hand Maces', ['Serles_Triumph'])
  expect(canInstallAugment(state, 'Astrids_Creativity', 0)).toBe(false)
  expect(canInstallAugment(state, 'Astrids_Creativity', 1)).toBe(true)
  expect(
    baseAffixCapacity(state.baseItemId, 'RARE', state.installedAugments),
  ).toEqual({ prefixes: 3, suffixes: 4 })
  const grit = item('One Hand Maces', ['Legacy_of_Serles_Grit'])
  expect(
    equipmentQualityRefusal(
      { ...grit, equipmentQuality: 20 },
      'BLACKSMITHS_WHETSTONE',
    ),
  ).toBeNull()
  expect(
    equipmentQualityRefusal(
      { ...grit, equipmentQuality: 40 },
      'BLACKSMITHS_WHETSTONE',
    ),
  ).toBe('maximum')
})
it('activates source weights through equipped Runes and retains static weights', () => {
  const state = item('Boots', ['Uhtreds_Sidereus'])
  const definitions = Object.fromEntries(
    rules.modifiers.map((d) => [d.id, d as Definition]),
  )
  const active = effectiveDefinitions(state, definitions)
  const inactive = effectiveDefinitions(
    { ...state, installedAugments: [] },
    definitions,
  )
  const all = effectiveDefinitions(
    { ...state, installedAugments: [] },
    definitions,
    true,
  )
  const applicable = Object.keys(active).filter(
    (id) => active[id]!.weight === 1,
  )
  expect(applicable.length).toBeGreaterThan(0)
  expect(
    applicable.every((id) => active[id]!.tags!.includes('chronomancy_pool')),
  ).toBe(true)
  expect(
    applicable.every(
      (id) => inactive[id]!.weight === 0 && all[id]!.weight === 1,
    ),
  ).toBe(true)
})
