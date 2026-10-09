import { describe, expect, it } from 'vitest'
import { topBase, topBaseKey } from './topBases'
import { useItemDraft } from './draft'
import { maximumQuality } from './qualityLimit'
import { supportsConcreteStateShape } from './workbenchStateShape'

describe('ordinary Quarterstaff and Spear representatives', () => {
  it.each([
    ['aegis-quarterstaff', 'Quarterstaves', 79, 0, 127, 50],
    ['bolting-quarterstaff', 'Quarterstaves', 78, 0, 127, 50],
    ['dreaming-quarterstaff', 'Quarterstaves', 78, 0, 127, 50],
    ['grand-spear', 'Spears', 79, 68, 109, 0],
    ['flying-spear', 'Spears', 78, 50, 127, 0],
    ['akoyan-spear', 'Spears', 78, 50, 127, 90],
  ] as const)(
    '%s retains class and actual popup requirements',
    (key, itemClass, level, str, dex, int) => {
      const base = topBase(key)!
      expect(topBaseKey(base.id)).toBe(key)
      expect(base.id).toContain(
        itemClass === 'Spears' ? '/OneHandSpears/' : '/Staves/',
      )
      expect([
        base.requiredLevel,
        base.strength,
        base.dexterity,
        base.intelligence,
      ]).toEqual([level, str, dex, int])
      expect(base.maximumQuality).toBe(20)
      expect(base.implicitStats?.length === 0).toBe(
        key === 'dreaming-quarterstaff',
      )
      for (const lines of Object.values(base.sourceProperties!))
        expect(lines).toHaveLength(5)
      if (itemClass === 'Spears')
        for (const lines of Object.values(base.skillLines!))
          expect(lines).toHaveLength(1)
      const state = {
        snapshotId: 'test',
        baseItemId: base.id,
        itemLevel: 1,
        rarity: 'NORMAL' as const,
        implicits: [],
        explicits: [],
        conditions: [],
      }
      expect(maximumQuality(state, {})).toBe(20)
      expect(supportsConcreteStateShape(state)).toBe(true)
      expect(supportsConcreteStateShape({ ...state, augmentSockets: 1 })).toBe(
        false,
      )
      expect(
        supportsConcreteStateShape({
          ...state,
          catalystQuality: { type: 'FLESH', amount: 20 },
        }),
      ).toBe(false)
      useItemDraft.getState().setBase(1, key)
      expect(useItemDraft.getState().text).toContain('Item Class: ' + itemClass)
      expect(useItemDraft.getState().text).toContain(base.name)
      expect(useItemDraft.getState().baseItemLevel).toBe(1)
    },
  )
})
