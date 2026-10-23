import { act, fireEvent, render, screen } from '@testing-library/react'
import { expect, it, vi } from 'vitest'
import { useRef, useState } from 'react'
import { useGraphWheelZoom } from './useGraphWheelZoom'

function Fixture({ choose = () => {} }: { choose?: () => void }) {
  const host = useRef<HTMLDivElement>(null),
    [zoom, setZoom] = useState(1)
  useGraphWheelZoom(host, zoom, setZoom)
  return (
    <>
      <button onClick={() => setZoom(1)}>Reset</button>
      <button onClick={() => setZoom(0.5)}>Half size</button>
      <output data-testid="zoom">{zoom}</output>
      <div
        data-testid="viewport"
        ref={(element) => {
          host.current = element
          if (element && !element.dataset.ready) {
            element.dataset.ready = 'true'
            element.scrollLeft = 100
            element.scrollTop = 80
            element.scrollBy = (options?: ScrollToOptions | number) => {
              if (typeof options === 'object') {
                element.scrollLeft += options.left ?? 0
                element.scrollTop += options.top ?? 0
              }
            }
            Object.defineProperty(element, 'clientHeight', { value: 400 })
          }
        }}
      >
        <svg
          width={1000 * zoom}
          height={800 * zoom}
          ref={(svg) => {
            if (svg)
              svg.getBoundingClientRect = () =>
                new DOMRect(
                  20 - host.current!.scrollLeft,
                  40 - host.current!.scrollTop,
                  1000 * zoom,
                  800 * zoom,
                )
          }}
        >
          <foreignObject>
            <button onClick={choose}>Select item</button>
          </foreignObject>
        </svg>
      </div>
      <div data-testid="outside">Outside graph</div>
    </>
  )
}
const zoom = () => Number(screen.getByTestId('zoom').textContent)
const event = (options: WheelEventInit = {}) =>
  new WheelEvent('wheel', {
    bubbles: true,
    cancelable: true,
    ctrlKey: true,
    deltaY: -100,
    clientX: 200,
    clientY: 100,
    ...options,
  })

it('cancels browser Ctrl+wheel and keeps the graph point under the pointer', () => {
  render(<Fixture />)
  const viewport = screen.getByTestId('viewport'),
    wheel = event(),
    x = (200 - 20 + viewport.scrollLeft) / zoom(),
    y = (100 - 40 + viewport.scrollTop) / zoom()
  fireEvent(viewport, wheel)
  expect(wheel.defaultPrevented).toBe(true)
  expect(zoom()).toBeCloseTo(Math.exp(0.2))
  expect((200 - 20 + viewport.scrollLeft) / zoom()).toBeCloseTo(x)
  expect((100 - 40 + viewport.scrollTop) / zoom()).toBeCloseTo(y)
  fireEvent(viewport, event({ deltaY: 100 }))
  expect(zoom()).toBeCloseTo(1)
  expect(viewport.scrollLeft).toBeCloseTo(100)
  expect(viewport.scrollTop).toBeCloseTo(80)
})

it('cancels at both zoom limits without allowing page zoom', () => {
  render(<Fixture />)
  const viewport = screen.getByTestId('viewport')
  for (const deltaY of [-10000, -10000, 10000, 10000]) {
    const wheel = event({ deltaY })
    fireEvent(viewport, wheel)
    expect(zoom()).toBe(deltaY < 0 ? 2 : 0.1)
    expect(wheel.defaultPrevented).toBe(true)
  }
})

it('leaves normal wheels and Ctrl+wheel outside the viewport uncancelled', () => {
  render(<Fixture />)
  for (const [target, wheel] of [
    [screen.getByTestId('viewport'), event({ ctrlKey: false })],
    [screen.getByTestId('outside'), event()],
    [screen.getByRole('button', { name: 'Reset' }), event()],
  ] as const) {
    fireEvent(target, wheel)
    expect(wheel.defaultPrevented).toBe(false)
    expect(zoom()).toBe(1)
  }
})

it.each([
  { deltaY: -100, deltaMode: 0 },
  { deltaY: -6.25, deltaMode: 1 },
  { deltaY: -0.25, deltaMode: 2 },
])('normalizes pixel, line and page wheel units: %j', (units) => {
  render(<Fixture />)
  fireEvent(screen.getByTestId('viewport'), event(units))
  expect(zoom()).toBeCloseTo(Math.exp(0.2))
})

it('accumulates rapid wheels before React commits and follows subsequent button zoom', () => {
  render(<Fixture />)
  const viewport = screen.getByTestId('viewport')
  act(() => {
    viewport.dispatchEvent(event({ deltaY: -50 }))
    viewport.dispatchEvent(event({ deltaY: -50 }))
  })
  expect(zoom()).toBeCloseTo(Math.exp(0.2))
  expect((200 - 20 + viewport.scrollLeft) / zoom()).toBeCloseTo(280)
  fireEvent.click(screen.getByRole('button', { name: 'Reset' }))
  fireEvent(viewport, event({ deltaY: 50 }))
  expect(zoom()).toBeCloseTo(Math.exp(-0.1))
})

it('zooms over a graph control while leaving its normal click intact', () => {
  const choose = vi.fn()
  render(<Fixture choose={choose} />)
  const button = screen.getByRole('button', { name: 'Select item' }),
    wheel = event()
  fireEvent(button, wheel)
  expect(wheel.defaultPrevented).toBe(true)
  expect(choose).not.toHaveBeenCalled()
  fireEvent.click(button)
  expect(choose).toHaveBeenCalledTimes(1)
})

it('discards the pointer anchor when rapid opposite wheels cancel before commit', () => {
  render(<Fixture />)
  const viewport = screen.getByTestId('viewport')
  act(() => {
    viewport.dispatchEvent(event({ deltaY: -100 }))
    viewport.dispatchEvent(event({ deltaY: 100 }))
  })
  expect(zoom()).toBe(1)
  fireEvent.click(screen.getByRole('button', { name: 'Half size' }))
  expect(zoom()).toBe(0.5)
  expect(viewport.scrollLeft).toBe(100)
  expect(viewport.scrollTop).toBe(80)
})

it('removes the non-passive listener on unmount', () => {
  const mounted = render(<Fixture />),
    viewport = screen.getByTestId('viewport'),
    wheel = event()
  mounted.unmount()
  fireEvent(viewport, wheel)
  expect(wheel.defaultPrevented).toBe(false)
})
