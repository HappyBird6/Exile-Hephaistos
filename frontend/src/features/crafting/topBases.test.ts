import { describe, expect, it } from 'vitest'
import { topBase, topBaseKey } from './topBases'
import { useItemDraft } from './draft'
import { maximumQuality } from './qualityLimit'

describe('reviewed endgame base identities', () => {
  it.each(['soldier', 'imperial'] as const)(
    'preserves source facts and six languages for %s',
    (key) => {
      const base = topBase(key)!
      expect(topBaseKey(base.id)).toBe(key)
      expect(Object.keys(base.requirements).sort()).toEqual([
        'en',
        'es',
        'ja',
        'ko',
        'zh-CN',
        'zh-TW',
      ])
      expect(
        Object.values(base.requirements).every(
          (text) =>
            text.includes(String(base.requiredLevel)) &&
            text.includes(String(base.strength)),
        ),
      ).toBe(true)
      useItemDraft.getState().setBase(82, key)
      expect(useItemDraft.getState().text).toContain(base.name)
      expect(useItemDraft.getState().baseItemLevel).toBe(82)
      expect(
        maximumQuality(
          {
            snapshotId: '',
            baseItemId: base.id,
            itemLevel: 82,
            rarity: 'NORMAL',
            implicits: [],
            explicits: [],
            conditions: [],
          },
          {},
        ),
      ).toBe(20)
    },
  )
  it('does not reinterpret legacy films as new bases', () => {
    expect(
      topBaseKey('Metadata/Items/Armours/BodyArmours/FourBodyStr1'),
    ).toBeUndefined()
    expect(
      topBaseKey('Metadata/Items/Armours/Helmets/FourHelmetStr1'),
    ).toBeUndefined()
    expect(topBase('body')).toBeUndefined()
    expect(topBase('unknown')).toBeUndefined()
  })
})
