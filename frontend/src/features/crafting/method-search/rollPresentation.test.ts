import { describe, expect, it } from 'vitest'
import { presentRollCell, rangeText } from './rollPresentation'
import { requestFixture } from '../path-tree/fixtures.test-support'
import type { ItemPresentation } from '../path-tree/presentation'

describe('roll cell presentation', () => {
  it('shows disjoint tier ranges without inventing values in their gap', () => {
    const item = {
      ...requestFixture().start.item,
      explicits: [
        { modifierId: 'first', values: { life: 1 }, fractured: false },
      ],
    }
    const present: ItemPresentation = (state) => ({
      name: 'Amulet',
      base: 'Amulet',
      rarity: 'RARE',
      itemClass: 'Amulet',
      itemLevel: 82,
      properties: [],
      requirements: [],
      flags: [],
      modifiers: state.explicits.map((m) => ({
        id: m.modifierId,
        text: `Life ${m.values.life}`,
      })),
    })
    const result = presentRollCell(item, {}, 'en', present, {
      first: [
        { modifierId: 'first', stat: 'life', min: 1, max: 2 },
        { modifierId: 'second', stat: 'life', min: 7, max: 9 },
      ],
    })
    expect(result.modifiers[0]!.text).toBe('Life 1–2 / Life 7–9')
  })
  it('keeps complete numbers and localized surrounding text', () => {
    for (const label of [
      'Spirit',
      '정신력',
      '精魂',
      'スピリット',
      'Geist',
      'Дух',
    ])
      expect(rangeText(`${label} 47`, `${label} 49`)).toBe(`${label} 47–49`)
    expect(rangeText('12% increased damage', '19% increased damage')).toBe(
      '12–19% increased damage',
    )
    expect(rangeText('Tier 3: +47', 'Tier 3: +50')).toBe('Tier 3: +47–+50')
    expect(rangeText('Spirit 47', 'Spirit 47')).toBe('Spirit 47')
  })
})
