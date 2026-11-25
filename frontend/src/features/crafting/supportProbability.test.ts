import { expect, it } from 'vitest'
import { locales } from '../../shared/i18n/i18n'
import { supportProbabilityText } from './supportProbability'

const range = (lower: number, upper: number) =>
  supportProbabilityText(
    { probability: null, probabilityBounds: { lower, upper } },
    'en',
  )
it('preserves exact, one-sided and two-sided support estimates, even when both ends round to the same nearest percent', () => {
  expect(range(0, 0)).toBe('0%')
  expect(range(1, 1)).toBe('100%')
  expect(range(0, 1)).toBe('0% – 100%')
  expect(range(0.123451, 1)).toBe('≥ 12.34%')
  expect(range(0, 0.123451)).toBe('≤ 12.35%')
  expect(range(0.123451, 0.123452)).toBe('12.34% – 12.35%')
  expect(range(0, 0.99999999999)).toBe('< 100%')
  expect(range(0.99999999999, 0.99999999999)).toBe('> 99.99%')
  expect(range(0.123456, 0.123456)).toBe('≈ 12.35%')
  expect(supportProbabilityText({ probability: 1e-310 }, 'en')).toBe('1E-308%')
})

it.each(locales)(
  'formats the bounds in the selected language (%s)',
  (locale) => {
    const percent = new Intl.NumberFormat(locale, {
      style: 'percent',
      maximumSignificantDigits: 4,
    })
    expect(
      supportProbabilityText(
        {
          probability: null,
          probabilityBounds: { lower: 0.125, upper: 0.875 },
        },
        locale,
      ),
    ).toBe(`${percent.format(0.125)} – ${percent.format(0.875)}`)
  },
)
