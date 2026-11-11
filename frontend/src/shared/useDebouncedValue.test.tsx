import { act, renderHook } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { useDebouncedValue } from './useDebouncedValue'
afterEach(() => vi.useRealTimers())
it('emits only the settled input and cleans up pending work on unmount', () => {
  vi.useFakeTimers()
  const { result, rerender, unmount } = renderHook(
    ({ value }) => useDebouncedValue(value, 250),
    { initialProps: { value: '82' } },
  )
  rerender({ value: '7' })
  act(() => vi.advanceTimersByTime(80))
  rerender({ value: '73' })
  act(() => vi.advanceTimersByTime(249))
  expect(result.current).toBe('82')
  act(() => vi.advanceTimersByTime(1))
  expect(result.current).toBe('73')
  rerender({ value: '74' })
  unmount()
  expect(vi.getTimerCount()).toBe(0)
})
