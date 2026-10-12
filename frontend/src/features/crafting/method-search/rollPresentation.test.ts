import { describe, expect, it } from 'vitest'
import { rangeText } from './rollPresentation'

describe('roll cell presentation', () => {
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
