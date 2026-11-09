import { useEffect, useLayoutEffect, useRef } from 'react'
import type { ReactNode, RefObject } from 'react'
import { createPortal } from 'react-dom'

export function RevealPopover({
  anchor,
  label,
  closeLabel,
  onClose,
  children,
}: {
  anchor: RefObject<HTMLButtonElement | null>
  label: string
  closeLabel: string
  onClose: () => void
  children: ReactNode
}) {
  const panel = useRef<HTMLDivElement>(null)
  const close = useRef(onClose)
  useLayoutEffect(() => {
    close.current = onClose
  }, [onClose])
  useLayoutEffect(() => {
    function position() {
      if (!anchor.current || !panel.current) return
      const r = anchor.current.getBoundingClientRect(),
        width = Math.min(480, innerWidth - 16)
      const below = Math.max(0, innerHeight - Math.max(8, r.bottom) - 12),
        above = Math.max(0, Math.min(r.top - 12, innerHeight - 16)),
        up = below < 240 && above > below
      Object.assign(panel.current.style, {
        position: 'fixed',
        width: `${width}px`,
        left: `${Math.max(8, Math.min(r.right - width, innerWidth - width - 8))}px`,
        top: up
          ? 'auto'
          : `${Math.max(8, Math.min(r.bottom + 6, innerHeight - 8))}px`,
        bottom: up ? `${Math.max(8, innerHeight - r.top + 6)}px` : 'auto',
        maxHeight: `${Math.max(0, Math.min(420, up ? above : below))}px`,
      })
    }
    position()
    window.addEventListener('resize', position)
    window.addEventListener('scroll', position, true)
    return () => {
      window.removeEventListener('resize', position)
      window.removeEventListener('scroll', position, true)
    }
  }, [anchor])
  useEffect(() => {
    const trigger = anchor.current,
      element = panel.current
    element?.querySelector<HTMLSelectElement>('select')?.focus()
    function outside(event: PointerEvent) {
      if (
        event.target instanceof Node &&
        !element?.contains(event.target) &&
        !trigger?.contains(event.target)
      )
        close.current()
    }
    function escape(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        event.preventDefault()
        event.stopPropagation()
        close.current()
      }
    }
    document.addEventListener('pointerdown', outside)
    document.addEventListener('keydown', escape)
    return () => {
      document.removeEventListener('pointerdown', outside)
      document.removeEventListener('keydown', escape)
      if (
        element?.contains(document.activeElement) ||
        document.activeElement === document.body
      )
        trigger?.focus()
    }
  }, [anchor])
  return createPortal(
    <div
      ref={panel}
      role="dialog"
      aria-label={label}
      className="abyss-reveal-picker wb-scrollbar"
    >
      <header>
        <strong>{label}</strong>
        <button type="button" aria-label={closeLabel} onClick={onClose}>
          ×
        </button>
      </header>
      {children}
    </div>,
    document.body,
  )
}
