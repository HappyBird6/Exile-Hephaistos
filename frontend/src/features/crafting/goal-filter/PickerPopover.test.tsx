import { fireEvent, render, screen } from '@testing-library/react'
import { useRef } from 'react'
import { afterEach, expect, it, vi } from 'vitest'
import { PickerPopover } from './PickerPopover'

afterEach(() => vi.restoreAllMocks())
function Example() {
  const anchor = useRef<HTMLInputElement>(null)
  return (
    <>
      <input ref={anchor} aria-label="Search" />
      <PickerPopover anchor={anchor}>
        <p>Results</p>
      </PickerPopover>
    </>
  )
}
it('positions outside flow, fits a narrow viewport and moves above a low anchor', () => {
  vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockReturnValue({
    left: 300,
    top: 700,
    bottom: 730,
    width: 250,
  } as DOMRect)
  vi.stubGlobal('innerWidth', 390)
  vi.stubGlobal('innerHeight', 844)
  const view = render(<Example />)
  const panel = screen.getByText('Results').parentElement!
  expect(panel.style.position).toBe('fixed')
  expect(panel.style.width).toBe('280px')
  expect(panel.style.left).toBe('102px')
  expect(panel.style.top).toBe('auto')
  expect(panel.style.bottom).toBe('148px')
  expect(panel.style.maxHeight).toBe('340px')
  vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockReturnValue({
    left: 10,
    top: 20,
    bottom: 50,
    width: 250,
  } as DOMRect)
  fireEvent.scroll(window)
  expect(panel.style.top).toBe('54px')
  expect(panel.style.bottom).toBe('auto')
  view.unmount()
  vi.unstubAllGlobals()
})
