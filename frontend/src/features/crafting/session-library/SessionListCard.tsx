import type { ReactNode } from 'react'
import { BaseImage } from '../base-picker/BaseImage'
/** Shared card geometry and selection behavior; callers provide their own metadata/actions. */
export function SessionListCard({
  title,
  baseId,
  baseName,
  image,
  selected,
  selectLabel,
  onSelect,
  metadata,
  trailing,
  actions,
}: {
  title: string
  baseId: string
  baseName: string
  image?: string | undefined
  selected: boolean
  selectLabel: string
  onSelect: () => void
  metadata?: ReactNode
  trailing?: ReactNode
  actions?: ReactNode
}) {
  return (
    <article
      className="wb-session-card"
      data-active={selected}
      aria-label={title}
      onClick={(event) => {
        if (
          event.target instanceof Element &&
          !event.target.closest('button, a, input')
        )
          onSelect()
      }}
    >
      <div className="wb-session-card__main">
        <button
          type="button"
          className="wb-session-card__select"
          aria-label={selectLabel}
          aria-pressed={selected}
          onClick={onSelect}
        >
          <BaseImage baseItemId={baseId} label={baseName} image={image} />
          <span className="wb-session-card__text">
            <strong>{title}</strong>
            {metadata ?? <small>{baseName}</small>}
          </span>
        </button>
        {trailing}
      </div>
      {actions && <div className="wb-session-card__actions">{actions}</div>}
    </article>
  )
}
