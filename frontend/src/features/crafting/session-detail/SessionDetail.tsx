import type { ReactNode } from 'react'
import type { Film } from '../workbenchHistory'
import { useI18n } from '../../../shared/i18n/i18n'
import { HistoryCard } from '../session-history/HistoryCard'
import { CraftingCostLine } from '../currency-prices/CurrencyPrices'
import { SessionNotation } from '../session-notation/SessionNotation'
import type { WorkbenchResource } from '../stash/stashTypes'

/** Display only: the caller owns persistence, cursor changes and reuse permissions. */
export function SessionDetail({
  film,
  cursor,
  item,
  itemClassName,
  className,
  onNavigate,
  disabled = false,
  resources,
  onNewCraft,
  actions,
  costControls,
  showOverview = true,
}: {
  film?: Film | null | undefined
  cursor: number
  item?: ReactNode
  itemClassName?: string
  className?: string
  onNavigate?: (cursor: number) => void
  disabled?: boolean
  resources?: readonly WorkbenchResource[]
  onNewCraft?: () => void
  actions?: ReactNode
  costControls?: ReactNode
  showOverview?: boolean
}) {
  const { t } = useI18n()
  const index = Math.max(0, Math.min(cursor, (film?.frames.length ?? 1) - 1))
  const frame = film?.frames[index]
  return (
    <div className={className}>
      {onNavigate && (
        <div className="workbench-film-toolbar">
          <div className="workbench-film-controls">
            <button
              type="button"
              aria-label={t('ui.previous_crafting_step')}
              disabled={!film || index === 0 || disabled}
              onClick={() => onNavigate(index - 1)}
            >
              <span
                className="workbench-film-controls__arrow"
                aria-hidden="true"
              >
                ‹
              </span>
            </button>
            <span>
              {film
                ? t('film.step', { step: index, total: film.frames.length - 1 })
                : t('ui.new_craft')}
            </span>
            <button
              type="button"
              aria-label={t('ui.next_crafting_step')}
              disabled={!film || index === film.frames.length - 1 || disabled}
              onClick={() => onNavigate(index + 1)}
            >
              <span
                className="workbench-film-controls__arrow"
                aria-hidden="true"
              >
                ›
              </span>
            </button>
          </div>
        </div>
      )}
      <div className={itemClassName}>
        {item ??
          (film && frame ? <HistoryCard film={film} frame={frame} /> : null)}
      </div>
      <CraftingCostLine film={film ?? undefined} cursor={index}>
        {costControls}
      </CraftingCostLine>
      {resources && (
        <SessionNotation
          film={film}
          cursor={index}
          resources={resources}
          showOverview={showOverview}
          {...(onNavigate ? { onNavigate } : {})}
          disabled={disabled}
          {...(onNewCraft ? { onNewCraft } : {})}
        />
      )}
      {actions}
    </div>
  )
}
