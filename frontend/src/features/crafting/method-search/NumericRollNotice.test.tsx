import { render, screen } from '@testing-library/react'
import { expect, it } from 'vitest'
import { NumericRollNotice } from './NumericRollNotice'
import { setLocale } from '../../../shared/i18n/i18n'

it('declares the actual coupled model before displaying modeled probabilities', () => {
  render(<NumericRollNotice models={['user-coupled-ratio-half-up-v1']} />)
  expect(screen.getByRole('note')).toHaveTextContent('not verified in the game')
  expect(screen.getByRole('note').textContent).toContain('10,001')
  expect(screen.getByRole('note').textContent).toContain(
    'not rolled independently',
  )
})
it('leaves deterministic evaluation and old results without this model unlabelled', () => {
  render(<NumericRollNotice models={[]} />)
  expect(screen.queryByRole('note')).toBeNull()
})
it('explains the assumed model in Korean', () => {
  setLocale('ko')
  render(<NumericRollNotice models={['user-coupled-ratio-half-up-v1']} />)
  expect(screen.getByRole('note')).toHaveTextContent(
    '게임에서 검증되지 않았습니다',
  )
})
