import { useEffect, useRef, type ReactNode } from 'react'
import type { Film } from '../workbenchHistory'
import type { NotationStep } from '../session-notation/extractSessionNotation'
import { useI18n } from '../../../shared/i18n/i18n'
import { HistoryCard } from '../session-history/HistoryCard'
import { RouteChanceSummary } from '../currency-prices/SelectedStepChance'
import { CraftingCostLine } from '../currency-prices/CurrencyPrices'
import { SessionNotation } from '../session-notation/SessionNotation'
import type { WorkbenchResource } from '../stash/stashTypes'
import './session-detail.css'

/** Display only: the caller owns persistence, cursor changes and reuse permissions. */
export function SessionDetail({
  film,
  cursor,
  item,
  itemClassName,
  className,
  onNavigate,
  stepControls,
  pendingSteps,
  throughFrame,
  disabled = false,
  resources,
  onNewCraft,
  actions,
  costControls,
  showOverview = true,
  layout = 'stack',
  artwork,
  overviewHref,
}: {
  film?: Film | null | undefined
  cursor: number
  item?: ReactNode
  itemClassName?: string
  className?: string
  onNavigate?: (cursor: number) => void
  stepControls?: (frameIndex: number) => ReactNode
  pendingSteps?: NotationStep[]
  throughFrame?: number
  disabled?: boolean
  resources?: readonly WorkbenchResource[]
  onNewCraft?: () => void
  actions?: ReactNode
  costControls?: ReactNode
  showOverview?: boolean
  layout?: 'stack' | 'columns'
  artwork?: ReactNode
  overviewHref?: string
}) {
  const { t } = useI18n()
  const index = Math.max(0, Math.min(cursor, (film?.frames.length ?? 1) - 1))
  const frame = film?.frames[index]
  const previous = useRef<HTMLButtonElement>(null)
  const next = useRef<HTMLButtonElement>(null)
  const canBrowse = Boolean(
    onNavigate && !disabled && (film?.frames.length ?? 0) > 1,
  )
  useEffect(() => {
    if (!canBrowse) return
    function navigate(event: KeyboardEvent) {
      if (
        event.defaultPrevented ||
        event.isComposing ||
        event.ctrlKey ||
        event.altKey ||
        event.metaKey ||
        (event.target instanceof Element &&
          event.target.closest(
            'input, textarea, select, [contenteditable]:not([contenteditable="false"]), [role="textbox"], [role="combobox"], [role="dialog"], dialog',
          ))
      )
        return
      const key = /^[a-z]$/i.test(event.key)
        ? event.key.toLowerCase()
        : event.code === 'KeyQ'
          ? 'q'
          : event.code === 'KeyE'
            ? 'e'
            : ''
      const button =
        key === 'q' ? previous.current : key === 'e' ? next.current : null
      if (
        !button ||
        button.disabled ||
        button.closest('[hidden], [inert], [aria-hidden="true"]') ||
        document.querySelector(
          'dialog[open], [aria-modal="true"]:not([hidden])',
        )
      )
        return
      event.preventDefault()
      button.click()
    }
    window.addEventListener('keydown', navigate)
    return () => window.removeEventListener('keydown', navigate)
  }, [canBrowse])
  const controls = onNavigate && (film || !pendingSteps) && (
    <div className="workbench-film-toolbar">
      <div className="workbench-film-controls">
        <button
          ref={previous}
          aria-keyshortcuts="Q"
          type="button"
          aria-label={t('ui.previous_crafting_step')}
          disabled={!film || index === 0 || disabled}
          onClick={() => onNavigate(index - 1)}
        >
          <span className="workbench-film-controls__arrow" aria-hidden="true">
            ‹
          </span>
        </button>
        <span>
          {film
            ? t('film.step', { step: index, total: film.frames.length - 1 })
            : t('ui.new_craft')}
        </span>
        <button
          ref={next}
          aria-keyshortcuts="E"
          type="button"
          aria-label={t('ui.next_crafting_step')}
          disabled={!film || index === film.frames.length - 1 || disabled}
          onClick={() => onNavigate(index + 1)}
        >
          <span className="workbench-film-controls__arrow" aria-hidden="true">
            ›
          </span>
        </button>
      </div>
    </div>
  )
  const itemView = (
    <div
      className={['session-detail__card', itemClassName]
        .filter(Boolean)
        .join(' ')}
    >
      {item ??
        (film && frame ? <HistoryCard film={film} frame={frame} /> : null)}
    </div>
  )
  const record = (
    <>
      {throughFrame === undefined && <RouteChanceSummary film={film} />}
      {(film || !pendingSteps) && (
        <CraftingCostLine film={film ?? undefined} cursor={index}>
          {costControls}
        </CraftingCostLine>
      )}
      {resources && (
        <SessionNotation
          film={film}
          cursor={index}
          resources={resources}
          showOverview={showOverview}
          {...(overviewHref ? { overviewHref } : {})}
          {...(onNavigate ? { onNavigate } : {})}
          {...(stepControls ? { stepControls } : {})}
          {...(pendingSteps ? { pendingSteps } : {})}
          {...(throughFrame !== undefined ? { throughFrame } : {})}
          disabled={disabled}
          {...(onNewCraft ? { onNewCraft } : {})}
        />
      )}
    </>
  )
  return (
    <div
      className={[
        'session-detail',
        layout === 'columns' && 'session-detail--columns',
        className,
      ]
        .filter(Boolean)
        .join(' ')}
    >
      {layout === 'columns' ? (
        <>
          {(artwork || actions) && (
            <div className="session-detail__tools">
              {artwork && (
                <div className="session-detail__artwork">{artwork}</div>
              )}
              {actions && (
                <div className="session-detail__actions">{actions}</div>
              )}
            </div>
          )}
          <div className="session-detail__item">
            {controls}
            {itemView}
          </div>
          <div className="session-detail__record">{record}</div>
        </>
      ) : (
        <>
          {controls}
          {itemView}
          {record}
          {actions}
        </>
      )}
    </div>
  )
}
