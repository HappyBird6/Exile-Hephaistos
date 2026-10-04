import { describe, expect, it } from 'vitest'
import { topBase, reviewedGloveKeys } from './topBases'
import terms from '../../shared/i18n/gameTerms.json'
import { supportsConcreteStateShape } from './workbenchStateShape'
import { useItemDraft } from './draft'

describe('reviewed endgame Gloves source and restoration', () => {
  it('offers distinct Armour, ES and Armour/ES bases with exact source facts', () => {
    expect(reviewedGloveKeys).toEqual(['massive', 'sirenscale', 'adherent'])
    expect(topBase('massive')?.armour).toBe(187)
    expect(topBase('sirenscale')?.energyShield).toBe(54)
    expect(topBase('sirenscale')?.strength).toBe(0)
    expect(topBase('sirenscale')?.intelligence).toBe(101)
    expect(topBase('adherent')?.armour).toBe(98)
    expect(topBase('adherent')?.energyShield).toBe(27)
  })
  it.each(['massive', 'sirenscale', 'adherent'] as const)(
    'preserves %s identity and six source names without inventing socket state',
    (key) => {
      const base = topBase(key)!
      for (const locale of [
        'en',
        'ko',
        'ja',
        'zh-CN',
        'zh-TW',
        'es',
      ] as const) {
        const dictionary = terms[locale] as Record<
          string,
          { name: string; itemKey?: string }
        >
        expect(dictionary[base.slug]?.name).toBeTruthy()
        expect(dictionary[base.slug]?.itemKey).toBe(base.id)
      }
      useItemDraft.getState().setBase(1, key)
      expect(useItemDraft.getState().text).toContain('Item Class: Gloves')
      expect(useItemDraft.getState().baseItemLevel).toBe(1)
      const state = {
        snapshotId: 'qa',
        baseItemId: base.id,
        itemLevel: 82,
        rarity: 'RARE',
        implicits: [],
        explicits: [],
        conditions: [],
        augmentSockets: null,
        catalystQuality: null,
      }
      expect(supportsConcreteStateShape(state)).toBe(true)
      expect(supportsConcreteStateShape({ ...state, augmentSockets: 1 })).toBe(
        false,
      )
    },
  )
})
