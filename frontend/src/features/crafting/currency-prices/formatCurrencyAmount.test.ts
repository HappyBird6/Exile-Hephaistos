import { expect, it } from 'vitest'
import { costAccumulator } from './decimalAmount'
import { formatCurrencyAmount } from './formatCurrencyAmount'
import { materialQuantityText } from '../method-search/probabilityPresentation'

it('ceilings whole Exalted and hundredths of Divine including exact and tiny values', () => {
  expect(formatCurrencyAmount(3339.56, 'Exalted_Orb', 'ko')).toBe('3,340')
  expect(formatCurrencyAmount(6.8621, 'Divine_Orb', 'ko')).toBe('6.87')
  expect(formatCurrencyAmount(6.87, 'Divine_Orb', 'es')).toBe('6,87')
  expect(formatCurrencyAmount(1e-20, 'Divine_Orb', 'en')).toBe('0.01')
  const tiny = { numerator: '90071992547409930001', denominator: '10000' }
  expect(materialQuantityText('EXALTED', 'en', tiny)).toBe(
    '9,007,199,254,740,994',
  )
  expect(
    materialQuantityText('DIVINE', 'en', { numerator: '1', denominator: '3' }),
  ).toBe('0.34')
})
it('does not introduce a cent by adding prices on a decimal boundary', () => {
  const costs = costAccumulator()
  costs.add(0.1, 1)
  costs.add(0.1, 2)
  expect(formatCurrencyAmount(costs.total(), 'Divine_Orb', 'en')).toBe('0.3')
})
