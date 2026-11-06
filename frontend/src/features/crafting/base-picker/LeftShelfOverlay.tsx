import { useEffect, useLayoutEffect, useRef } from 'react'
import type { ReactNode, RefObject } from 'react'
import { createPortal } from 'react-dom'
import { overlayPlacement } from './overlayPlacement'
import '../workbench-scrollbar.css'
import './left-shelf-overlay.css'

export type ShelfOverlayProps = {
  id: string
  anchor: RefObject<HTMLElement | null>
  /** Cover this canvas down to the opener's bottom edge, with a shared inset. */
  canvas?: RefObject<HTMLElement | null>
  onClose: () => void
}

/** The base chooser covers the canvas; the current item remains outside it. */
export function LeftShelfOverlay({
  id,
  anchor,
  canvas,
  onClose,
  title,
  closeLabel,
  bodyClassName = '',
  children,
}: ShelfOverlayProps & {
  title: string
  closeLabel: string
  bodyClassName?: string
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
      const viewport = { width: window.innerWidth, height: window.innerHeight }
      const bounds = canvas?.current?.getBoundingClientRect()
      const opener = anchor.current.getBoundingClientRect()
      const placement = overlayPlacement(
        bounds ?? {
          left: 0,
          top: 0,
          right: viewport.width,
          bottom: viewport.height,
        },
        bounds
          ? opener
          : { left: 0, top: 0, right: viewport.width, bottom: viewport.height },
        viewport,
      )
      Object.assign(
        panel.current.style,
        Object.fromEntries(
          Object.entries(placement).map(([key, value]) => [key, `${value}px`]),
        ),
      )
    }
    position()
    const observer =
      typeof ResizeObserver === 'undefined'
        ? null
        : new ResizeObserver(position)
    if (canvas?.current) observer?.observe(canvas.current)
    if (anchor.current) observer?.observe(anchor.current)
    window.addEventListener('resize', position)
    window.addEventListener('scroll', position, true)
    return () => {
      observer?.disconnect()
      window.removeEventListener('resize', position)
      window.removeEventListener('scroll', position, true)
    }
  }, [anchor, canvas])
  useEffect(() => {
    const trigger = anchor.current
    const element = panel.current
    element?.querySelector<HTMLButtonElement>('button')?.focus()
    function dismiss(event: PointerEvent) {
      const target = event.target
      if (
        target instanceof Node &&
        !(
          target instanceof Element &&
          target.closest('.workbench-feedback-region, .wb-base-picker__preview')
        ) &&
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
      <div className={`wb-shelf-overlay__body wb-scrollbar ${bodyClassName}`}>
        {children}
      </div>
    </div>,
    document.body,
  )
}
