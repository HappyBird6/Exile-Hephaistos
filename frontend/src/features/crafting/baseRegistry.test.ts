import { describe, expect, it } from 'vitest'
import baseline from './baseRegistry.baseline.json'
import data from './topBases.json'
import { baseTexts, baseSlugs, baseClass, basePolicy } from './baseRegistry'
import { reviewedArmourKeys } from './topBases'
describe('central base registration', () => {
  it('preserves every initial text and source slug across 125 bases', () => {
    expect(baseTexts).toEqual(baseline.textMap)
    expect(baseSlugs).toEqual(baseline.slugs)
    expect(Object.keys(baseTexts)).toHaveLength(125)
    expect(reviewedArmourKeys).toEqual(
      Object.keys(data).filter((key) => !['soldier', 'imperial'].includes(key)),
    )
  })
  it('resolves source class and explicit capabilities without a base-key union or class allowlist', () => {
    for (const [key, base] of Object.entries(data)) {
      expect(baseClass(key)).toEqual(
        baseline.textMap[key as keyof typeof baseline.textMap]
          .split('\n')[0]!
          .replace('Item Class: ', ''),
      )
      expect(basePolicy(base.id)).toBeDefined()
    }
    expect(basePolicy(data['grand-spear'].id)?.ordinaryCatalyst).toBe(false)
    expect(
      basePolicy(data['grand-spear'].id)?.socketExecutionMaximum,
    ).toBeNull()
    expect(basePolicy(data.kinetic.id)?.ordinaryCatalyst).toBe(true)
    expect(basePolicy(data['linen-belt'].id)?.qualityLimit).toBe(false)
    expect(basePolicy('Metadata/Items/Unknown')).toBeUndefined()
  })
})
