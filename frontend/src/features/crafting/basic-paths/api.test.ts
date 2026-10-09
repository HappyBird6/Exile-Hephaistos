import { describe, expect, it } from 'vitest'
import { percent } from './api'

describe('exact probability display', () => {
  it('handles thousands of digits without converting either integer to Number', () => {
    const denominator = 10n ** 5000n
    expect(
      percent({
        numerator: String((denominator * 3n) / 4n),
        denominator: String(denominator),
      }),
    ).toBe('≈75%')
  })
  it('never rounds below-one values to 100%', () => {
    const denominator = 10n ** 5000n
    expect(
      percent({
        numerator: String(denominator - 1n),
        denominator: String(denominator),
      }),
    ).toBe('≈99.99999%')
    expect(percent({ numerator: '1', denominator: '1' })).toBe('100%')
  })
  it('distinguishes tiny positive values from zero', () => {
    expect(
      percent({ numerator: '1', denominator: '100000000000000000000000' }),
    ).toBe('<0.00001%')
    expect(percent({ numerator: '0', denominator: '1' })).toBe('0%')
  })
  it('truncates ratios and rejects invalid fractions', () => {
    expect(percent({ numerator: '1', denominator: '3' })).toBe('≈33.33333%')
    for (const value of [
      { numerator: '1', denominator: '0' },
      { numerator: '2', denominator: '1' },
      { numerator: 'NaN', denominator: '1' },
    ])
      expect(() => percent(value)).toThrow()
  })
})
