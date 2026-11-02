import { useEffect, useLayoutEffect, useRef } from 'react'
import type { ReactNode, RefObject } from 'react'
import { createPortal } from 'react-dom'
import { overlayPlacement } from './overlayPlacement'
import './left-shelf-overlay.css'

export type ShelfOverlayProps = {
  id: string
  anchor: RefObject<HTMLElement | null>
  shelf?: RefObject<HTMLElement | null>
  onClose: () => void
}

/** Nonmodal: currency shelf remains visible and usable outside the covered area. */
export function LeftShelfOverlay({
  id,
  anchor,
  shelf,
  onClose,
  title,
  closeLabel,
  children,
}: ShelfOverlayProps & {
  title: string
  closeLabel: string
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
      const placement = overlayPlacement(
        anchor.current.getBoundingClientRect(),
        {
          width: window.innerWidth,
          height: window.innerHeight,
        },
        520,
        shelf?.current?.getBoundingClientRect(),
      )
      Object.assign(
        panel.current.style,
        Object.fromEntries(
          Object.entries(placement).map(([key, value]) => [key, `${value}px`]),
        ),
      )
    }
    position()
    window.addEventListener('resize', position)
    window.addEventListener('scroll', position, true)
    return () => {
      window.removeEventListener('resize', position)
      window.removeEventListener('scroll', position, true)
    }
  }, [anchor, shelf])
  useEffect(() => {
    const trigger = anchor.current
    const element = panel.current
    element?.querySelector<HTMLButtonElement>('button')?.focus()
    function dismiss(event: PointerEvent) {
      const target = event.target
      if (
        target instanceof Node &&
        !element?.contains(target) &&
        !trigger?.contains(target)
      )
        close.current()
    }
    document.addEventListener('pointerdown', dismiss)
    return () => {
      document.removeEventListener('pointerdown', dismiss)
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
      id={id}
      className="wb-shelf-overlay"
      role="dialog"
      aria-labelledby={`${id}-title`}
      onKeyDown={(event) => {
        if (event.key !== 'Escape') return
        event.preventDefault()
        event.stopPropagation()
        onClose()
        anchor.current?.focus()
      }}
    >
      <header className="wb-shelf-overlay__header">
        <h3 id={`${id}-title`}>{title}</h3>
        <button type="button" aria-label={closeLabel} onClick={onClose}>
          ×
        </button>
      </header>
      <div className="wb-shelf-overlay__body">{children}</div>
    </div>,
    document.body,
  )
}
