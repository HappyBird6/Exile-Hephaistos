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

// Read the production coordinates so a shortened/clipped panel fails this check.
function coordinate(selector: string, property: string, extent: number) {
  const rule = stashCss.slice(stashCss.indexOf(selector))
  const declaration = rule.slice(0, rule.indexOf('}'))
  const match = declaration.match(
    new RegExp(`${property}: calc\\((\\d+) / (\\d+) \\* 100%\\)`),
  )
  if (!match) throw new Error(`Missing ${selector} ${property} coordinate`)
  return (Number(match[1]) / Number(match[2])) * extent
}
const stashLeft = coordinate('.workbench-stash {', 'left', 935)
const stashRight = coordinate('.workbench-stash {', 'right', 935)
const stashTop = coordinate('.workbench-stash {', 'top', 550)
const stashHeight = coordinate('.workbench-stash {', 'height', 550)

describe('original composition at 75 percent', () => {
  it('matches the mockup margins, aligned top, equal gaps and full-height stash', () => {
    const first = currencies.find((c) => c.id === 'Orb_of_Transmutation')!
    const third = currencies.find(
      (c) => c.id === 'Perfect_Orb_of_Transmutation',
    )!
    const alchemy = currencies.find((c) => c.id === 'Orb_of_Alchemy')!
    const annulment = currencies.find((c) => c.id === 'Orb_of_Annulment')!
    expect(stashRight).toBeCloseTo(first.x)
    expect(stashTop).toBeCloseTo(annulment.y)
    expect(stashLeft - (annulment.x + 74)).toBeCloseTo(
      alchemy.x - (third.x + 74),
    )
    const favoritesBottom =
      coordinate('.workbench-stash__favorites {', 'top', 550) +
      coordinate('.workbench-stash__favorites {', 'height', 550)
    expect(stashTop + stashHeight).toBeCloseTo(favoritesBottom)
  })
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
      {
        x: stashLeft,
        y: stashTop,
        width: 935 - stashLeft - stashRight,
        height: stashHeight,
      },
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
