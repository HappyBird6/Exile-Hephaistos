import { useLayoutEffect, useRef } from 'react'
import type { Dispatch, RefObject, SetStateAction } from 'react'

export function useGraphWheelZoom(
  host: RefObject<HTMLDivElement | null>,
  zoom: number,
  setZoom: Dispatch<SetStateAction<number>>,
) {
  const current = useRef(zoom)
  const anchor = useRef<{
    x: number
    y: number
    clientX: number
    clientY: number
  } | null>(null)

  useLayoutEffect(() => {
    const element = host.current
    if (!element) return
    current.current = zoom
    const svg = element.querySelector(':scope > svg')
    if (anchor.current && svg) {
      const point = anchor.current,
        rect = svg.getBoundingClientRect()
      // Correct after the SVG has resized, including native scroll clamping.
      element.scrollBy({
        left: rect.left + point.x * zoom - point.clientX,
        top: rect.top + point.y * zoom - point.clientY,
        behavior: 'instant',
      })
    }
    anchor.current = null
    const wheel = (event: WheelEvent) => {
      if (!event.ctrlKey) return
      // React's delegated wheel listener can be passive. Register locally so
      // browser zoom is cancelled even when the graph reaches a zoom limit.
      event.preventDefault()
      if (!svg || !event.deltaY) return
      const pixels =
          event.deltaY *
          (event.deltaMode === 1
            ? 16
            : event.deltaMode === 2
              ? element.clientHeight
              : 1),
        next = Math.max(
          0.1,
          Math.min(2, current.current * Math.exp(-pixels * 0.002)),
        )
      if (next === current.current) return
      const rect = svg.getBoundingClientRect()
      anchor.current =
        next === zoom
          ? null
          : {
              x: (event.clientX - rect.left) / zoom,
              y: (event.clientY - rect.top) / zoom,
              clientX: event.clientX,
              clientY: event.clientY,
            }
      current.current = next
      setZoom(next)
    }
    element.addEventListener('wheel', wheel, { passive: false })
    return () => element.removeEventListener('wheel', wheel)
  }, [host, zoom, setZoom])
}
