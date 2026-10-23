import { expect, it } from 'vitest'
import { evaluationNumber } from './evaluationNumber'

it('shows exact repeating conversions without suggesting decimal rounding decides the goal', () => {
  expect(
    evaluationNumber(91 / 60, { numerator: '91', denominator: '60' }),
  ).toBe(`91/60 ≈ ${91 / 60}`)
  expect(evaluationNumber(6.01)).toBe(6.01)
  expect(evaluationNumber(null)).toBe('—')
})
