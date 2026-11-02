import { describe, expect, it } from 'vitest'
import { formatAttributeNumber, formatAttributeText } from './attributeNumbers'

describe('display-only attribute precision', () => {
  it.each([
    ['+12.345% increased', '+12.3% increased'],
    ['(-1.25—2.35) seconds', '(-1.3—2.4) seconds'],
    ['0.0499 to -0.0499', '0 to 0'],
    ['0.05 to -0.05', '0.1 to -0.1'],
    ['999.999%', '1,000%'],
    ['+12 / -9 / 1,234', '+12 / -9 / 1,234'],
    ['1.2 ms and 1.000%', '1.2 ms and 1%'],
    ['9007199254740993.55%', '9,007,199,254,740,993.6%'],
    ['stat_id_1.234', 'stat_id_1.234'],
  ])(
    'rounds %s without altering units or integer spans',
    (source, expected) => {
      expect(formatAttributeText(source, 'en')).toBe(expected)
    },
  )

  it('supports decimal comma and grouping without rounding integer thousands', () => {
    expect(formatAttributeText('1.234,567% / 1.234', 'es')).toBe(
      '1234,6% / 1.234',
    )
    expect(formatAttributeText('999.999%', 'en', 'es')).toBe('1000%')
    expect(formatAttributeNumber(1.25, 'es')).toBe('1,3')
    expect(formatAttributeNumber(-1.25, 'ko')).toBe('-1.3')
    expect(formatAttributeNumber(-0.00001, 'en')).toBe('0')
  })
})
