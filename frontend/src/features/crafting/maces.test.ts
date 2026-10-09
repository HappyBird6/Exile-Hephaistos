import { describe, expect, it } from 'vitest'
import { topBase, topBaseKey } from './topBases'
import { useItemDraft } from './draft'
import { maximumQuality } from './qualityLimit'
import { supportsConcreteStateShape } from './workbenchStateShape'

describe('independent ordinary Mace representatives', () => {
  it.each([
    ['fortified-hammer', 'One Hand Maces', 79],
    ['strife-pick', 'One Hand Maces', 78],
    ['akoyan-club', 'One Hand Maces', 78],
    ['ruination-maul', 'Two Hand Maces', 79],
    ['fanatic-greathammer', 'Two Hand Maces', 78],
    ['tawhoan-greatclub', 'Two Hand Maces', 78],
  ] as const)(
    '%s retains class, source requirements and restricted state',
    (key, itemClass, level) => {
      const base = topBase(key)!
      expect(topBaseKey(base.id)).toBe(key)
      expect(base.id).toContain(
        itemClass === 'One Hand Maces' ? '/OneHandMaces/' : '/TwoHandMaces/',
      )
      expect(base.requiredLevel).toBe(level)
      expect(base.strength).toBe(163)
      expect(base.maximumQuality).toBe(20)
      expect(base.implicitStats).not.toHaveLength(0)
      for (const lines of Object.values(base.sourceProperties!))
        expect(lines).toHaveLength(5)
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
