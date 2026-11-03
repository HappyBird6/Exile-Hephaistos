import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'
import { currencies } from '../currencies'

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

describe('original composition at 75 percent', () => {
  it('preserves the original tier columns and five family rows', () => {
    for (const [family, y] of [
      ['Orb_of_Transmutation', 40],
      ['Orb_of_Augmentation', 140],
      ['Regal_Orb', 240],
      ['Exalted_Orb', 340],
      ['Chaos_Orb', 440],
    ] as const) {
      for (const [index, prefix] of ['', 'Greater_', 'Perfect_'].entries()) {
        expect(
          currencies.find((c) => c.id === `${prefix}${family}`),
        ).toMatchObject({ x: 29 + 90 * index, y })
      }
    }
  })
  it('keeps the original upper item and shelf free of the in-canvas favorites', () => {
    const rects: Rect[] = currencies
      .filter((c) => !/Rib|Jawbone|Collarbone|Cranium|Artificers/.test(c.id))
      .map((c) => ({ ...c, width: 74, height: 74 }))
    rects.push(
      { x: 371, y: 240, width: 193, height: 274 },
      { x: 652, y: 12, width: 271, height: 524 },
    )
    for (let i = 0; i < 10; i++)
      rects.push({
        x: 29 + (i % 5) * 90,
        y: 570 + Math.floor(i / 5) * 100,
        width: 74,
        height: 74,
      })
    for (const [index, r] of rects.entries()) {
      expect(r.x + r.width).toBeLessThanOrEqual(935)
      expect(r.y + r.height).toBeLessThanOrEqual(764)
      for (const other of rects.slice(index + 1))
        expect(overlaps(r, other)).toBe(false)
    }
    expect(stashCss).toContain('top: calc(570 / 550 * 100%)')
    expect(stashCss).toContain('width: calc(434 / 935 * 100%)')
    expect(stashCss).toContain('height: calc(174 / 550 * 100%)')
    expect(css).toContain('padding-bottom: calc(214 / 935 * 100%)')
    expect(css).not.toContain('.item-placement')
  })
  it('scales the existing composition and preserves the original mobile breakpoint', () => {
    expect(css).toContain('zoom: 0.75')
    expect(css).toContain('width: min(100vw, 1800px)')
    expect(css).toContain('width: calc(100vw / 0.75)')
    expect(css).toContain('minmax(280px, 374px)')
    expect(374 / 340).toBeCloseTo(1.1)
    expect(css).toContain('@media (max-width: 1000px)')
    expect(css).not.toContain('@container')
    expect(css).not.toMatch(/(?:background|border-radius|color)\s*:/)
  })
})
