import { act, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { useGraphPan } from './useGraphPan'

function Fixture({ select = () => {} }: { select?: () => void }) {
  const { panning, ...handlers } = useGraphPan()
  return (
    <div
      data-testid="viewport"
      data-panning={panning}
      tabIndex={0}
      {...handlers}
    >
      <svg data-testid="background">
        <path data-testid="edge" role="button" onClick={select} />
        <foreignObject>
          <div>
            <span>Selectable item text</span>
            <button onClick={select}>Select item</button>
            <input aria-label="Goal" />
            <a href="#item">Item link</a>
            <details>
              <summary>Candidate tiers</summary>
              T1: 10–20
            </details>
            <svg data-testid="icon" />
          </div>
        </foreignObject>
      </svg>
    </div>
  )
}

function setup() {
  const select = vi.fn(),
    mounted = render(<Fixture select={select} />),
    viewport = screen.getByTestId('viewport')
  Object.defineProperties(viewport, {
    clientWidth: { value: 500 },
    clientHeight: { value: 400 },
  })
  viewport.scrollLeft = 100
  viewport.scrollTop = 120
  return { ...mounted, viewport, select }
}

function pointer(
  target: Element | Window,
  type: string,
  init: MouseEventInit & { pointerId?: number; pointerType?: string } = {},
) {
  const { pointerId = 1, pointerType = 'mouse', ...mouse } = init
  return fireEvent(
    target,
    Object.assign(
      new MouseEvent(type, {
        bubbles: true,
        cancelable: true,
        button: 0,
        buttons: 1,
        clientX: 200,
        clientY: 200,
        ...mouse,
      }),
      { pointerId, pointerType },
    ),
  )
}

afterEach(() => vi.useRealTimers())

it('pans both axes from the background, without selecting or changing item data', () => {
  const { viewport, select } = setup(),
    text = viewport.textContent
  pointer(screen.getByTestId('background'), 'pointerdown')
  expect(viewport).toHaveAttribute('data-panning', 'true')
  expect(viewport).toHaveFocus()
  pointer(window, 'pointermove', { clientX: 150, clientY: 170 })
  expect([viewport.scrollLeft, viewport.scrollTop]).toEqual([150, 150])
  pointer(window, 'pointerup', { buttons: 0 })
  expect(viewport).toHaveAttribute('data-panning', 'false')
  pointer(window, 'pointermove', { clientX: 20, clientY: 20 })
  expect([viewport.scrollLeft, viewport.scrollTop]).toEqual([150, 150])
  expect(viewport.textContent).toBe(text)
  expect(select).not.toHaveBeenCalled()
})

it('keeps card, text, edge, form, link, candidate and nested icon interactions', () => {
  const { viewport, select } = setup()
  for (const target of [
    screen.getByText('Selectable item text'),
    screen.getByRole('button', { name: 'Select item' }),
    screen.getByTestId('edge'),
    screen.getByRole('textbox'),
    screen.getByRole('link'),
    screen.getByText('Candidate tiers'),
    screen.getByTestId('icon'),
  ]) {
    pointer(target, 'pointerdown')
    pointer(window, 'pointermove', { clientX: 150 })
    expect(viewport).toHaveAttribute('data-panning', 'false')
    expect([viewport.scrollLeft, viewport.scrollTop]).toEqual([100, 120])
  }
  fireEvent.click(screen.getByRole('button', { name: 'Select item' }))
  fireEvent.click(screen.getByTestId('edge'))
  fireEvent.click(screen.getByText('Candidate tiers'))
  expect(select).toHaveBeenCalledTimes(2)
  expect(viewport.querySelector('details')?.open).toBe(true)
  fireEvent.change(screen.getByRole('textbox'), { target: { value: 'T1' } })
  expect(screen.getByRole('textbox')).toHaveValue('T1')
})

it('leaves other mouse buttons, touch, pen, modifiers and native scrollbars alone', () => {
  const { viewport } = setup()
  for (const init of [
    { button: 1 },
    { button: 2 },
    { pointerType: 'touch' },
    { pointerType: 'pen' },
    { ctrlKey: true },
    { altKey: true },
    { metaKey: true },
    { shiftKey: true },
    { clientX: 500 },
    { clientY: 400 },
    { clientX: -1 },
  ]) {
    expect(pointer(viewport, 'pointerdown', init)).toBe(true)
    pointer(window, 'pointermove', { clientX: 150 })
    expect(viewport).toHaveAttribute('data-panning', 'false')
    expect([viewport.scrollLeft, viewport.scrollTop]).toEqual([100, 120])
  }
  expect(fireEvent.wheel(viewport, { deltaY: 100 })).toBe(true)
})

it.each(['pointercancel', 'lostpointercapture', 'blur', 'missing-release'])(
  'ends dragging on %s and permits a new drag',
  (ending) => {
    const { viewport } = setup()
    pointer(viewport, 'pointerdown')
    pointer(window, 'pointermove', { clientX: 150 })
    if (ending === 'blur') fireEvent(window, new Event('blur'))
    else if (ending === 'missing-release')
      pointer(window, 'pointermove', { buttons: 0, clientX: 130 })
    else pointer(ending === 'lostpointercapture' ? viewport : window, ending)
    expect(viewport).toHaveAttribute('data-panning', 'false')
    pointer(window, 'pointermove', { clientX: 10 })
    expect(viewport.scrollLeft).toBe(150)
    pointer(viewport, 'pointerdown')
    pointer(window, 'pointermove', { clientX: 180 })
    expect(viewport.scrollLeft).toBe(170)
    pointer(window, 'pointerup')
  },
)

it('ignores another pointer and treats a small movement as a click', () => {
  const { viewport } = setup()
  pointer(viewport, 'pointerdown')
  pointer(window, 'pointermove', { pointerId: 2, clientX: 100 })
  pointer(window, 'pointerup', { pointerId: 2 })
  expect(viewport).toHaveAttribute('data-panning', 'true')
  pointer(window, 'pointermove', { clientX: 198, clientY: 199 })
  expect([viewport.scrollLeft, viewport.scrollTop]).toEqual([100, 120])
  pointer(window, 'pointerup')
  expect(fireEvent.click(viewport, { detail: 1 })).toBe(true)
})

it('suppresses only the drag release click, leaving subsequent and keyboard clicks intact', () => {
  vi.useFakeTimers()
  const { viewport, select } = setup(),
    button = screen.getByRole('button', { name: 'Select item' })
  pointer(viewport, 'pointerdown')
  pointer(window, 'pointermove', { clientX: 100 })
  pointer(window, 'pointerup')
  fireEvent.click(button, { detail: 1 })
  expect(select).not.toHaveBeenCalled()
  act(() => vi.runAllTimers())
  fireEvent.click(button, { detail: 1 })
  fireEvent.click(button, { detail: 0 })
  expect(select).toHaveBeenCalledTimes(2)
})

it('releases capture and removes window handlers on unmount', () => {
  const { viewport, unmount } = setup(),
    release = vi.fn()
  Object.assign(viewport, {
    setPointerCapture: vi.fn(),
    hasPointerCapture: () => true,
    releasePointerCapture: release,
  })
  pointer(viewport, 'pointerdown')
  unmount()
  expect(release).toHaveBeenCalledWith(1)
  pointer(window, 'pointermove', { clientX: 100 })
  expect(viewport.scrollLeft).toBe(100)
})
