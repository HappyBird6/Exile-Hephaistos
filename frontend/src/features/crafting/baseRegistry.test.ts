import { describe, expect, it } from 'vitest'
import baseline from './baseRegistry.baseline.json'
import data from './topBases.json'
import { baseTexts, baseSlugs, baseClass, basePolicy } from './baseRegistry'
import { reviewedArmourKeys } from './topBases'
describe('central base registration', () => {
  it('preserves every initial text and source slug across 125 bases', () => {
    for (const [key, text] of Object.entries(baseline.textMap)) {
      expect(baseTexts[key as keyof typeof baseTexts]).toEqual(text)
      expect(baseSlugs[key as keyof typeof baseSlugs]).toEqual(
        baseline.slugs[key as keyof typeof baseline.slugs],
      )
    }
    expect(Object.keys(baseline.textMap)).toHaveLength(125)
    expect(Object.keys(baseTexts)).toHaveLength(134)
    expect(reviewedArmourKeys).toEqual(
      Object.keys(data).filter((key) => !['soldier', 'imperial'].includes(key)),
    )
  })
  it('resolves source class and explicit capabilities without a base-key union or class allowlist', () => {
    for (const [key, base] of Object.entries(data).filter(
      ([key]) => key in baseline.textMap,
    )) {
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
  it('registers six Staves and three Talismans through class policies', () => {
    for (const [family, count, itemClass] of [
      ['staves', 6, 'Staves'],
      ['talismans', 3, 'Talismans'],
    ] as const) {
      const roster = Object.entries(data).filter(
        ([, base]) => base.family === family,
      )
      expect(roster).toHaveLength(count)
      for (const [key, base] of roster) {
        expect(baseClass(key)).toBe(itemClass)
        expect(baseTexts[key as keyof typeof baseTexts]).toBe(
          `Item Class: ${itemClass}\nRarity: Normal\n${base.name}`,
        )
        expect(baseSlugs[key as keyof typeof baseSlugs]).toBe(base.slug)
        expect(basePolicy(base.id)?.socketExecutionMaximum).toBeNull()
        expect(basePolicy(base.id)?.ordinaryCatalyst).toBe(false)
      }
    }
  })
})
