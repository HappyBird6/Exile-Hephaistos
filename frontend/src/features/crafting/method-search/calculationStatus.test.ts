import { expect, it } from 'vitest'
import { calculationState } from './calculationStatus'
import type { AutomaticView } from './automaticApi'
it('distinguishes pending controls, restored READY, budget pauses and unresolved completion', () => {
  const view = { status: 'READY', complete: false } as AutomaticView
  expect(calculationState(view, true, true, 'STOP', false, false)).toBe(
    'stopping',
  )
  expect(calculationState(view, true, false, 'RESUME', false, false)).toBe(
    'resuming',
  )
  expect(calculationState(view, false, false, null, false, false)).toBe(
    'restored',
  )
  expect(calculationState(view, true, false, null, false, false)).toBe('queued')
  expect(calculationState(view, true, true, null, false, false)).toBe(
    'calculating',
  )
  expect(
    calculationState(
      { ...view, status: 'PAUSED' },
      true,
      false,
      null,
      false,
      false,
    ),
  ).toBe('paused')
  expect(
    calculationState(
      { ...view, status: 'COMPLETE' },
      true,
      false,
      null,
      false,
      false,
    ),
  ).toBe('partial')
  expect(
    calculationState(
      { ...view, status: 'COMPLETE', complete: true },
      true,
      false,
      null,
      false,
      false,
    ),
  ).toBe('complete')
  expect(
    calculationState(
      { ...view, status: 'CANCELLED' },
      false,
      false,
      null,
      false,
      false,
    ),
  ).toBe('stopped')
  expect(calculationState(view, true, false, null, true, false)).toBe('error')
})
