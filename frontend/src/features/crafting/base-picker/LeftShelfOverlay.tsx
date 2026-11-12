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
  footer,
  children,
}: ShelfOverlayProps & {
  title: string
  closeLabel: string
  bodyClassName?: string
  footer?: ReactNode
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
      const visual = window.visualViewport
      const placement =
        window.innerWidth <= 1200
          ? {
              left: (visual?.offsetLeft ?? 0) + 8,
              top: (visual?.offsetTop ?? 0) + 8,
              width: (visual?.width ?? viewport.width) - 16,
              height: (visual?.height ?? viewport.height) - 16,
            }
          : overlayPlacement(
              bounds ?? {
                left: 0,
                top: 0,
                right: viewport.width,
                bottom: viewport.height,
              },
              bounds
                ? opener
                : {
                    left: 0,
                    top: 0,
                    right: viewport.width,
                    bottom: viewport.height,
                  },
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
    window.visualViewport?.addEventListener('resize', position)
    window.visualViewport?.addEventListener('scroll', position)
    window.addEventListener('resize', position)
    window.addEventListener('scroll', position, true)
    return () => {
      observer?.disconnect()
      window.visualViewport?.removeEventListener('resize', position)
      window.visualViewport?.removeEventListener('scroll', position)
      window.removeEventListener('resize', position)
      window.removeEventListener('scroll', position, true)
    }
  }, [anchor, canvas])
  useEffect(() => {
    const originalOverflow = document.body.style.overflow
    const smallScreen = window.innerWidth <= 1200
    if (smallScreen) document.body.style.overflow = 'hidden'
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
      if (smallScreen) document.body.style.overflow = originalOverflow
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
        if (event.key === 'Tab' && window.innerWidth <= 1200) {
          const targets = [
            ...(panel.current?.querySelectorAll<HTMLElement>(
              'button:not(:disabled), input:not(:disabled), select:not(:disabled), a[href], [tabindex="0"]',
            ) ?? []),
          ].filter((element) => element.getClientRects().length > 0)
          const first = targets[0],
            last = targets.at(-1)
          if (event.shiftKey && document.activeElement === first) {
            event.preventDefault()
            last?.focus()
          } else if (!event.shiftKey && document.activeElement === last) {
            event.preventDefault()
            first?.focus()
          }
        }
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
      {footer && <footer className="wb-shelf-overlay__footer">{footer}</footer>}
    </div>,
    document.body,
  )
}
