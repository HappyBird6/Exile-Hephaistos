import { useLayoutEffect, useRef } from 'react'
import type { ReactNode, RefObject } from 'react'

/** Fixed placement keeps result counts out of layout; follow the anchor on scroll. */
export function PickerPopover({
  anchor,
  children,
}: {
  anchor: RefObject<HTMLInputElement | null>
  children: ReactNode
}) {
  const panel = useRef<HTMLDivElement>(null)
  useLayoutEffect(() => {
    function position() {
      if (!anchor.current || !panel.current) return
      const rect = anchor.current.getBoundingClientRect()
      const viewport = window.visualViewport
      const top = viewport?.offsetTop ?? 0
      const left = viewport?.offsetLeft ?? 0
      const height = viewport?.height ?? window.innerHeight
      const width = viewport?.width ?? window.innerWidth
      const below = top + height - rect.bottom - 8
      const above = rect.top - top - 8
      const upward = below < 240 && above > below
      Object.assign(panel.current.style, {
        position: 'fixed',
        zIndex: '1000',
        width: `${Math.min(Math.max(rect.width, 280), width - 16)}px`,
        left: `${Math.max(left + 8, Math.min(rect.left, left + width - Math.min(Math.max(rect.width, 280), width - 16) - 8))}px`,
        top: upward ? 'auto' : `${rect.bottom + 4}px`,
        bottom: upward ? `${window.innerHeight - rect.top + 4}px` : 'auto',
        maxHeight: `${Math.max(0, Math.min(340, upward ? above : below))}px`,
      })
    }
    position()
    window.addEventListener('resize', position)
    window.addEventListener('scroll', position, true)
    window.visualViewport?.addEventListener('resize', position)
    return () => {
      window.removeEventListener('resize', position)
      window.removeEventListener('scroll', position, true)
      window.visualViewport?.removeEventListener('resize', position)
    }
  }, [anchor])
  return (
    <div ref={panel} className="picker-popover">
      {children}
    </div>
  )
}
