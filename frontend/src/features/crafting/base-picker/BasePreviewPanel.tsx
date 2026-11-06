import type { FocusEventHandler, MouseEventHandler } from 'react'
import { ItemCard } from '../ItemCard'
import type { ItemCardData } from '../itemCardData'

/** A dedicated preview rail, kept inside the base chooser instead of a floating tooltip. */
export function BasePreviewPanel({
  id,
  label,
  item,
  slug,
  onMouseLeave,
  onBlur,
}: {
  id: string
  label: string
  item: ItemCardData
  slug: string
  onMouseLeave: MouseEventHandler<HTMLElement>
  onBlur: FocusEventHandler<HTMLElement>
}) {
  return (
    <aside
      id={id}
      className="wb-base-picker__preview wb-scrollbar"
      role="region"
      aria-label={label}
      tabIndex={0}
      onMouseLeave={onMouseLeave}
      onBlur={onBlur}
    >
      <ItemCard item={item} baseItemId={slug} />
    </aside>
  )
}
