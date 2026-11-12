import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'
import { currencies } from '../currencies'

const css = readFileSync(
  'src/features/crafting/compact-viewport/compactViewport.css',
  'utf8',
)
const notationCss = readFileSync(
  'src/features/crafting/session-notation/session-notation.css',
  'utf8',
)
const stashCss = readFileSync(
  'src/features/crafting/stash/workbenchStash.css',
  'utf8',
)
function rule(source: string, selector: string, last = false) {
  const start = last
    ? source.lastIndexOf(selector + ' {')
    : source.indexOf(selector + ' {')
  if (start < 0) throw new Error(`Missing ${selector}`)
  return source.slice(start, source.indexOf('}', start))
}
const grid = rule(stashCss, '.stash-canvas__content')
const tracks = [
  ...grid.match(/grid-template-columns:([^;]+)/)![1]!.matchAll(/([0-9]+)fr/g),
].map((match) => Number(match[1]))
const topMargin = rule(stashCss, '.workbench-stash-column').match(
  /margin-top: calc\(([0-9]+) \/ ([0-9]+)/,
)!
const extraGap = Number(
  rule(stashCss, '.stash-canvas__content::after').match(
    /margin-bottom: ([0-9]+)(?:px)?/,
  )![1],
)
const bottomPadding = Number(
  rule(css, '.stash-viewport .stash-canvas').match(
    /padding-bottom: ([0-9]+)px/,
  )![1],
)

describe('aligned composition at 75 percent', () => {
  it('keeps catalog content out of external track sizing without a fixed panel height at any breakpoint', () => {
    const panel = rule(stashCss, '.workbench-stash-column')
    expect(panel).toContain('contain: size')
    expect(panel).toContain('align-self: stretch')
    expect(panel).toContain('grid-row: 1 / 4')
    expect(panel).not.toMatch(/(?:^|\n)\s*(?:height|max-height|block-size):/)
    expect(rule(stashCss, '.workbench-stash__panel')).toContain('min-height: 0')
    const resources = rule(stashCss, '.workbench-stash__resources')
    expect(resources).toContain('min-height: 0')
    expect(resources).toContain('overflow-y: auto')
    // Size containment applies outside media rules; the same elastic canvas
    // and lower track continue to determine the panel area on narrow screens.
    expect(stashCss.indexOf('contain: size')).toBeLessThan(
      stashCss.indexOf('.workbench-stash__panel'),
    )
    expect(rule(css, '.stash-viewport .stash-canvas')).toContain(
      'flex: 1 0 auto',
    )
    expect(grid).toContain('auto minmax(min-content, 1fr) auto')
  })
  it('preserves horizontal slot coordinates and shares one lower track with an elastic vertical gap', () => {
    expect(tracks).toEqual([29, 434, 23, 140, 23, 257, 29])
    expect(tracks.reduce((sum, value) => sum + value, 0)).toBe(935)
    expect(grid).toContain('minmax(min-content, 1fr)')
    expect(rule(stashCss, '.stash-canvas__upper')).toContain(
      'aspect-ratio: 935 / 550',
    )
    expect(rule(stashCss, '.workbench-stash-column')).toContain(
      'grid-row: 1 / 4',
    )
    expect(rule(stashCss, '.workbench-stash__favorites')).toContain(
      'grid-row: 3',
    )
    expect(rule(css, '.workbench-item-controls')).toContain('grid-row: 3')
    expect(
      (Number(topMargin[1]) / Number(topMargin[2])) * tracks[5]!,
    ).toBeCloseTo(40)
    for (const width of [224, 330, 424, 640, 935]) {
      const scale = width / 935
      const upperHeight = 550 * scale
      const favoriteWidth = tracks[1]! * scale
      const slot = (favoriteWidth - 4 * 16 * scale) / 5
      expect(slot).toBeCloseTo(74 * scale)
      const lowerHeight = Math.max(174 * scale, width < 935 ? 208 : 0)
      const minimumHeight =
        upperHeight + 20 * scale + extraGap + lowerHeight + bottomPadding
      for (const rightHeight of [
        minimumHeight,
        minimumHeight + 56,
        minimumHeight + 400,
      ]) {
        const lowerBottom = rightHeight - bottomPadding
        const lowerTop = lowerBottom - lowerHeight
        expect(lowerTop).toBeGreaterThanOrEqual(
          upperHeight + 20 * scale + extraGap - 0.001,
        )
        expect(lowerTop + slot).toBeLessThanOrEqual(lowerBottom - slot)
        // Favorites, controls and the spanning stash have the same bottom;
        // 24px also equals the right column padding.
        expect(lowerBottom + bottomPadding).toBeCloseTo(rightHeight)
        expect(lowerBottom - slot).toBeGreaterThan(514 * scale)
        expect((649 - 626) * scale).toBeGreaterThan(0)
      }
    }
    expect(extraGap).toBe(0)
    expect(bottomPadding).toBe(24)
    expect(
      rule(
        stashCss,
        '.workbench-stash__favorites .workbench-stash__favorite',
        true,
      ),
    ).toContain('height: auto')
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
  it('preserves zoom, card expansion, responsive stacking and the bounded feedback and History surfaces', () => {
    expect(css).toContain('zoom: 0.75')
    expect(css).toContain('width: min(100vw, 1800px)')
    expect(css).toContain('width: auto')
    expect(css).toContain('minmax(280px, 374px)')
    expect(374 / 340).toBeCloseTo(1.1)
    expect(css).toContain('@media (max-width: 1000px)')
    expect(css).toContain('min-height: 208px')
    expect(rule(css, '.workbench-feedback-region')).toContain('flex: 1')
    expect(rule(stashCss, '.workbench-stash__resources')).toContain(
      '4 * var(--stash-row-height) + 15px',
    )
    expect(rule(notationCss, '.wb-session-notation', true)).toContain('flex: 1')
    expect(rule(notationCss, '.wb-session-notation__steps', true)).toContain(
      'max-height: none',
    )
    expect(css).toContain('max-height: 50%')
    expect(
      readFileSync(
        'src/features/crafting/session-notation/session-notation.css',
        'utf8',
      ),
    ).toContain('max-height: 12rem')
  })
})
