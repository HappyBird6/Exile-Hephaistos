import { expect, it, vi, afterEach } from 'vitest'
import { isTouchActivation } from './touchInput'
afterEach(() => vi.unstubAllGlobals())
it('distinguishes touch, mouse and keyboard on hybrid devices', () => {
  vi.stubGlobal('matchMedia', () => ({ matches: true }))
  const event = (pointerType?: string) =>
    Object.assign(new Event('click'), pointerType ? { pointerType } : {})
  expect(isTouchActivation({ detail: 1, nativeEvent: event('touch') })).toBe(
    true,
  )
  expect(isTouchActivation({ detail: 1, nativeEvent: event('mouse') })).toBe(
    false,
  )
  expect(isTouchActivation({ detail: 0, nativeEvent: event() })).toBe(false)
  expect(isTouchActivation({ detail: 1, nativeEvent: event() })).toBe(true)
})
