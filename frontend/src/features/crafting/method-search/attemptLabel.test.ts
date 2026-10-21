import { expect, it } from 'vitest'
import { attemptLabel } from './attemptLabel'
import { methodPoint } from './methodObservation'
import type { AutomaticArrow } from './automaticApi'

it('removes single-use text while retaining the 100-attempt Chaos label', () => {
  expect(attemptLabel('ko', 1)).toBe('')
  expect(attemptLabel('ko', 100)).toBe('100회 이내')
  expect(attemptLabel('en', 1)).toBe('')
  expect(attemptLabel('en', 100)).toBe('Within 100 attempts')
})
it('uses the same first-hit observation for Chaos graph and route probability', () => {
  const method = {
    executionMode: 'REPEATABLE',
    cycle: [{ currency: 'CHAOS' }],
    points: [{ cycles: 1 }, { cycles: 100 }],
  } as AutomaticArrow
  expect(methodPoint(method, 100)?.cycles).toBe(100)
  const exalted = {
    ...method,
    executionMode: 'SINGLE_PASS',
    cycle: [{ currency: 'EXALTED' }],
  } as AutomaticArrow
  expect(methodPoint(exalted, 100)?.cycles).toBe(1)
})
