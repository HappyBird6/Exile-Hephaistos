import { useEffect, useLayoutEffect, useRef } from 'react'
import { createPortal } from 'react-dom'
import { ItemCard } from '../ItemCard'
import type { ItemCardData } from '../itemCardData'

export function BasePreviewTooltip({
  id,
  label,
  anchor,
  item,
  slug,
  onDismiss,
}: {
  id: string
  label: string
  anchor: HTMLElement
  item: ItemCardData
  slug: string
  onDismiss?: () => void
}) {
  const tooltip = useRef<HTMLDivElement>(null)
  useEffect(() => {
    const previous =
      anchor
        .getAttribute('aria-describedby')
        ?.split(/\s+/)
        .filter((value) => value !== id) ?? []
    anchor.setAttribute('aria-describedby', [...previous, id].join(' '))
    return () => {
      if (previous.length)
        anchor.setAttribute('aria-describedby', previous.join(' '))
      else anchor.removeAttribute('aria-describedby')
    }
  }, [anchor, id])
  useLayoutEffect(() => {
    function position() {
      const element = tooltip.current
      if (!element) return
      const rect = anchor.getBoundingClientRect()
      const width = Math.min(340, window.innerWidth - 16)
      element.style.width = `${width}px`
      const height = element.getBoundingClientRect().height
      const left =
        rect.left - width - 8 >= 8
          ? rect.left - width - 8
          : rect.right + width + 8 <= window.innerWidth - 8
            ? rect.right + 8
            : Math.max(8, Math.min(rect.left, window.innerWidth - width - 8))
      element.style.left = `${left}px`
      element.style.top = `${Math.max(8, Math.min(rect.top, window.innerHeight - height - 8))}px`
      element.style.maxHeight = `${window.innerHeight - 16}px`
    }
    position()
    window.addEventListener('resize', position)
    window.addEventListener('scroll', position, true)
    return () => {
      window.removeEventListener('resize', position)
      window.removeEventListener('scroll', position, true)
    }
  }, [anchor, item])
  return createPortal(
    <div
      id={id}
      ref={tooltip}
      role="tooltip"
      aria-label={label}
      className="wb-base-picker__preview wb-scrollbar"
      tabIndex={0}
      onKeyDown={(event) => {
        if (event.key !== 'Escape') return
        event.stopPropagation()
        anchor.focus()
        onDismiss?.()
      }}
      onMouseLeave={(event) => {
        if (
          !(
            event.relatedTarget instanceof Node &&
            anchor.contains(event.relatedTarget)
          ) &&
          !event.currentTarget.contains(document.activeElement)
        )
          onDismiss?.()
      }}
      onBlur={(event) => {
        if (!(
          event.relatedTarget instanceof Node &&
          (event.currentTarget.contains(event.relatedTarget) ||
            anchor.contains(event.relatedTarget))
        ))
          onDismiss?.()
      }}
    >
      <ItemCard item={item} baseItemId={slug} />
    </div>,
    document.body,
  )
}
