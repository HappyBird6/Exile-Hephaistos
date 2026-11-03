import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'
import { currencies } from '../currencies'
import { compactCurrencyPositions, compactCurrencyRows } from './coordinates'

const css = readFileSync(
  'src/features/crafting/compact-viewport/compactViewport.css',
  'utf8',
)
const stashCss = readFileSync(
  'src/features/crafting/stash/workbenchStash.css',
  'utf8',
)
type Rect = { x: number; y: number; width: number; height: number }
const overlaps = (a: Rect, b: Rect) =>
  a.x < b.x + b.width &&
  a.x + a.width > b.x &&
  a.y < b.y + b.height &&
  a.y + a.height > b.y

describe('compact viewport contract', () => {
  it('retains every fixed currency once and groups each three-tier family together', () => {
    const expected = currencies
      .filter(
        (currency) =>
          !/Rib|Jawbone|Collarbone|Cranium|Artificers/.test(currency.id),
      )
      .map((currency) => currency.id)
      .sort()
    expect(Object.keys(compactCurrencyPositions).sort()).toEqual(expected)
    expect(new Set(compactCurrencyRows.flat()).size).toBe(21)
    for (const family of [
      'Orb_of_Transmutation',
      'Orb_of_Augmentation',
      'Regal_Orb',
      'Exalted_Orb',
      'Chaos_Orb',
    ]) {
      const normal = compactCurrencyPositions[family]!
      const greater = compactCurrencyPositions[`Greater_${family}`]!
      const perfect = compactCurrencyPositions[`Perfect_${family}`]!
      expect([greater.y, perfect.y]).toEqual([normal.y, normal.y])
      expect([greater.x, perfect.x]).toEqual([normal.x + 90, normal.x + 180])
    }
  })

  it('fits 21 currencies, ten equally sized favorites, the item and stash without overlaps', () => {
    const currencyRects = Object.values(compactCurrencyPositions).map(
      (position) => ({
        ...position,
        width: 74,
        height: 74,
      }),
    )
    const favorites = Array.from({ length: 10 }, (_, index) => ({
      x: 29 + (index % 5) * 90,
      y: 340 + Math.floor(index / 5) * 100,
      width: 74,
      height: 74,
    }))
    const rectangles = [
      ...currencyRects,
      ...favorites,
      { x: 489, y: 340, width: 154, height: 174 },
      { x: 652, y: 12, width: 271, height: 524 },
    ]
    for (const [index, rectangle] of rectangles.entries()) {
      expect(rectangle.x).toBeGreaterThanOrEqual(0)
      expect(rectangle.y).toBeGreaterThanOrEqual(0)
      expect(rectangle.x + rectangle.width).toBeLessThanOrEqual(935)
      expect(rectangle.y + rectangle.height).toBeLessThanOrEqual(550)
      for (const other of rectangles.slice(index + 1))
        expect(overlaps(rectangle, other)).toBe(false)
    }
    // These values tie the geometry check to the actual rendered CSS contract.
    expect(stashCss).toContain('width: calc(434 / 935 * 100%)')
    expect(stashCss).toContain('height: calc(174 / 550 * 100%)')
    expect(stashCss).toContain('column-gap: calc(16 / 434 * 100%)')
    expect(stashCss).toContain('row-gap: calc(26 / 174 * 100%)')
    expect(css).toContain('left: calc(489 / 935 * 100%)')
    expect(css).toContain('top: calc(340 / 550 * 100%)')
    expect(css).toContain('width: calc(154 / 935 * 100%)')
    expect(css).toContain('height: calc(174 / 550 * 100%)')
  })

  it('uses real desktop width and container reflow without visual scaling or a forced scroll plane', () => {
    expect(css).toContain('width: min(100%, max(1000px, 75vw))')
    expect(css).toContain('max-width: 1350px')
    expect(css).toContain('grid-template-columns: minmax(0, 1fr) 374px')
    expect(css).toContain('@container workbench (max-width: 1114px)')
    expect(css).toContain('@container stash (max-width: 739px)')
    expect(css).toContain('grid-template-columns: repeat(5, minmax(0, 1fr))')
    expect(css).toContain('min-width: 0')
    expect(css).toContain('overflow-x: clip')
    expect(css).not.toMatch(/(?:transform|zoom)\s*:/)
    expect(css).not.toMatch(/(?:^|\n)\s*order\s*:/)
    expect(css).not.toMatch(/min-width:\s*(?:600|935)px/)
  })
})
