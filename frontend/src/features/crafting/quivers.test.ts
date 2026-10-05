import { describe, expect, it } from 'vitest'
import { topBase, topBaseKey } from './topBases'
import { useItemDraft } from './draft'
import { maximumQuality } from './qualityLimit'
import { supportsConcreteStateShape } from './workbenchStateShape'

describe('independent ordinary Quiver representatives', () => {
  it.each([
    ['visceral-quiver', 65, 'FourQuiver11'],
    ['volant-quiver', 61, 'FourQuiver10'],
    ['penetrating-quiver', 55, 'FourQuiver9'],
    ['primed-quiver', 51, 'FourQuiver8'],
    ['serrated-quiver', 44, 'FourQuiver7'],
    ['toxic-quiver', 39, 'FourQuiver6'],
    ['blunt-quiver', 33, 'FourQuiver5'],
    ['two-point-quiver', 24, 'FourQuiver4'],
    ['sacral-quiver', 16, 'FourQuiver3'],
    ['fire-quiver', 8, 'FourQuiver2'],
    ['broadhead-quiver', 0, 'FourQuiver1'],
  ] as const)(
    '%s preserves class, requirement and restricted state',
    (key, level, id) => {
      const base = topBase(key)!
      expect(base.id).toBe(`Metadata/Items/Quivers/${id}`)
      expect(topBaseKey(base.id)).toBe(key)
      expect(base.family).toBe('quivers')
      expect(base.requiredLevel).toBe(level)
      expect(base.maximumQuality).toBeNull()
      const state = {
        snapshotId: 'test',
        baseItemId: base.id,
        itemLevel: 1,
        rarity: 'NORMAL' as const,
        implicits: [],
        explicits: [],
        conditions: [],
      }
      expect(maximumQuality(state, {})).toBeNull()
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
      expect(useItemDraft.getState().text).toContain('Item Class: Quivers')
      expect(useItemDraft.getState().text).toContain(base.name)
      expect(useItemDraft.getState().baseItemLevel).toBe(1)
      expect(
        Object.values(base.sourceProperties!).every(
          (lines) => lines.length === 1,
        ),
      ).toBe(true)
    },
  )
})
