import { useEffect, useRef, useState } from 'react'
import type { MouseEvent, PointerEvent } from 'react'

// Only the empty viewport or its root SVG starts a pan. Cards, text, controls
// and interactive SVG paths retain their own pointer and selection behavior.
export function useGraphPan() {
  const active = useRef<{
    element: HTMLDivElement
    pointerId: number
    x: number
    y: number
    left: number
    top: number
    moved: boolean
  } | null>(null)
  const suppressClick = useRef(false)
  const resetClick = useRef<number | undefined>(undefined)
  const [panning, setPanning] = useState(false)

  const finish = () => {
    const drag = active.current
    if (!drag) return
    active.current = null
    suppressClick.current = drag.moved
    resetClick.current = window.setTimeout(() => {
      suppressClick.current = false
    }, 0)
    setPanning(false)
    if (drag.element.hasPointerCapture?.(drag.pointerId)) {
      drag.element.releasePointerCapture(drag.pointerId)
    }
  }
  const finishRef = useRef(finish)
  useEffect(() => {
    const move = (event: globalThis.PointerEvent) => {
      const drag = active.current
      if (!drag || event.pointerId !== drag.pointerId) return
      if (!(event.buttons & 1)) {
        finishRef.current()
        return
      }
      const dx = event.clientX - drag.x,
        dy = event.clientY - drag.y
      if (!drag.moved && Math.hypot(dx, dy) < 4) return
      drag.moved = true
      drag.element.scrollLeft = drag.left - dx
      drag.element.scrollTop = drag.top - dy
      event.preventDefault()
    }
    const release = (event: globalThis.PointerEvent) => {
      if (event.pointerId === active.current?.pointerId) finishRef.current()
    }
    const blur = () => finishRef.current()
    window.addEventListener('pointermove', move, { passive: false })
    window.addEventListener('pointerup', release)
    window.addEventListener('pointercancel', release)
    window.addEventListener('blur', blur)
    return () => {
      window.removeEventListener('pointermove', move)
      window.removeEventListener('pointerup', release)
      window.removeEventListener('pointercancel', release)
      window.removeEventListener('blur', blur)
      const drag = active.current
      active.current = null
      if (drag?.element.hasPointerCapture?.(drag.pointerId)) {
        drag.element.releasePointerCapture(drag.pointerId)
      }
      window.clearTimeout(resetClick.current)
    }
  }, [])

  return {
    panning,
    onPointerDown: (event: PointerEvent<HTMLDivElement>) => {
      const element = event.currentTarget,
        target = event.target
      if (
        active.current ||
        event.pointerType !== 'mouse' ||
        event.button !== 0 ||
        event.altKey ||
        event.ctrlKey ||
        event.metaKey ||
        event.shiftKey ||
        (target !== element &&
          !(
            target instanceof SVGSVGElement && target.parentElement === element
          ))
      )
        return
      const rect = element.getBoundingClientRect(),
        x = event.clientX - rect.left - element.clientLeft,
        y = event.clientY - rect.top - element.clientTop
      // Leave native scrollbars and borders draggable as usual.
      if (
        x < 0 ||
        y < 0 ||
        x >= element.clientWidth ||
        y >= element.clientHeight
      )
        return
      window.clearTimeout(resetClick.current)
      suppressClick.current = false
      // Establish keyboard focus before tracking the drag: changing focus can
      // itself emit a window blur event in embedded/browser test contexts.
      element.focus({ preventScroll: true })
      active.current = {
        element,
        pointerId: event.pointerId,
        x: event.clientX,
        y: event.clientY,
        left: element.scrollLeft,
        top: element.scrollTop,
        moved: false,
      }
      setPanning(true)
      try {
        element.setPointerCapture?.(event.pointerId)
      } catch {
        // Window listeners also handle browsers without pointer capture.
      }
      event.preventDefault()
    },
    onLostPointerCapture: (event: PointerEvent<HTMLDivElement>) => {
      if (event.pointerId === active.current?.pointerId) finish()
    },
    onClickCapture: (event: MouseEvent<HTMLDivElement>) => {
      if (suppressClick.current && event.detail !== 0) {
        suppressClick.current = false
        event.preventDefault()
        event.stopPropagation()
      }
    },
  }
}
