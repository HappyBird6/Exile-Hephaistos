import { orderedFilms } from '../workbenchHistory'
import { freshCraftMessages } from './freshCraftMessages'
import { CraftingCostLine } from '../currency-prices/CurrencyPrices'
import { useEffect, useRef, useState } from 'react'
import { useI18n } from '../../../shared/i18n/i18n'
import { HistoryCard } from '../session-history/HistoryCard'
import { historyMessages } from '../session-history/messages'
import { localHistoryUrl } from '../session-history/transfer'
import type { SessionDocument } from '../session-history/transfer'
import { sessionTitle } from './sessionTitle'
import { SessionTitleEditor } from './SessionTitleEditor'
import { SessionImport } from './SessionImport'
import { SessionLibrary } from './SessionLibrary'
import type { SessionLibraryProps } from './SessionLibrary'
import { sessionLibraryMessages } from './sessionLibraryMessages'

export type SessionHistoryWorkspaceProps = Omit<
  SessionLibraryProps,
  'onSelect' | 'selectedId' | 'favoritesOnly'
> & {
  onRename: (id: string, title: string) => boolean | Promise<boolean>
  undo?: { title: string; expiresAt: number; onUndo: () => void }
  onContinue: (id: string) => void
  onNewCraft?: (id: string) => void
  onImport: (
    document: SessionDocument,
  ) => string | null | Promise<string | null>
  error?: string
  active?: boolean
}

/** Preview selection is independent of the active simulation and its stored cursor. */
export function SessionHistoryWorkspace({
  onContinue,
  onNewCraft,
  onImport,
  onRename,
  undo,
  error,
  active = true,
  ...library
}: SessionHistoryWorkspaceProps) {
  const { locale } = useI18n()
  const text = sessionLibraryMessages[locale]
  const [filter, setFilter] = useState<'library' | 'favorites'>(() =>
    new URLSearchParams(location.search).get('section') === 'favorites'
      ? 'favorites'
      : 'library',
  )
  const [selectedId, setSelectedId] = useState<string | null>(() =>
    new URLSearchParams(location.search).get('session'),
  )
  const preview = useRef<HTMLElement>(null)
  const importPreview = useRef<HTMLDivElement>(null)
  const visible = orderedFilms(
    library.history.films,
    filter === 'favorites',
  ).filter((film) => filter === 'library' || film.favorite === true)
  const selected =
    visible.find((film) => film.id === selectedId) ??
    visible.find((film) => film.id === library.history.active) ??
    visible[0]
  const finalFrame = selected?.frames.at(-1)
  const title = selected ? sessionTitle(selected, locale) : text.empty
  return (
    <div className="wb-history-workspace">
      <nav className="wb-history-nav" aria-label={text.navigation}>
        {(['library', 'favorites'] as const).map((view) => (
          <button
            key={view}
            type="button"
            aria-current={filter === view ? 'page' : undefined}
            onClick={() => setFilter(view)}
          >
            <span
              className={
                view === 'favorites' ? 'session-favorite-icon' : undefined
              }
              aria-hidden="true"
            >
              {view === 'library' ? '🕘' : '⭐'}
            </span>
            <span>{text[view]}</span>
            <span className="wb-history-nav__count" aria-hidden="true">
              {view === 'library'
                ? library.history.films.length
                : library.history.films.filter((film) => film.favorite).length}
            </span>
          </button>
        ))}
      </nav>
      <div className="wb-history-content">
        <header className="wb-history-heading">
          <h2>{text[filter]}</h2>
          <div className="wb-history-heading__actions">
            <SessionImport
              disabled={library.disabled ?? false}
              previewTarget={importPreview}
              onImport={async (document) => {
                const id = await onImport(document)
                if (!id) return false
                setFilter('library')
                setSelectedId(id)
                return true
              }}
            />
            {undo && (
              <DeletionUndo
                key={undo.expiresAt}
                undo={undo}
                disabled={library.disabled ?? false}
              />
            )}
          </div>
        </header>
        {error && <p role="alert">{error}</p>}
        <div className="wb-history-import-preview" ref={importPreview} />
        <div
          className={`wb-history-columns${selected ? '' : ' wb-history-columns--empty'}`}
        >
          <div className="wb-history-list wb-scrollbar">
            <SessionLibrary
              key={filter}
              {...library}
              selectedId={selected?.id ?? null}
              favoritesOnly={filter === 'favorites'}
              onSelect={(id) => {
                setSelectedId(id)
                if (window.innerWidth <= 760)
                  preview.current?.scrollIntoView({ block: 'nearest' })
              }}
            />
          </div>
          {selected && (
            <section
              ref={preview}
              className="wb-history-preview"
              aria-label={text.finalState}
            >
              <div className="wb-history-preview__heading">
                <SessionTitleEditor
                  key={selected.id}
                  title={title}
                  disabled={library.disabled ?? false}
                  onSave={(value) => onRename(selected.id, value)}
                />
              </div>
              {active && finalFrame && (
                <>
                  <div className="wb-history-preview__item">
                    <HistoryCard film={selected} frame={finalFrame} />
                  </div>
                  <CraftingCostLine
                    film={selected}
                    cursor={selected.frames.length - 1}
                  />
                  <div className="wb-history-preview__actions">
                    <button
                      type="button"
                      disabled={library.disabled}
                      onClick={() => onContinue(selected.id)}
                    >
                      {text.resume} <span aria-hidden="true">→</span>
                    </button>
                    {onNewCraft && (
                      <button
                        type="button"
                        disabled={library.disabled}
                        onClick={() => onNewCraft(selected.id)}
                      >
                        {freshCraftMessages[locale].fromItem}
                      </button>
                    )}
                    <a href={localHistoryUrl(selected.id, 'history', filter)}>
                      {historyMessages[locale].title}{' '}
                      <span aria-hidden="true">↗</span>
                    </a>
                  </div>
                </>
              )}
            </section>
          )}
        </div>
      </div>
    </div>
  )
}

function DeletionUndo({
  undo,
  disabled,
}: {
  undo: NonNullable<SessionHistoryWorkspaceProps['undo']>
  disabled: boolean
}) {
  const { locale } = useI18n()
  const text = sessionLibraryMessages[locale]
  const [now, setNow] = useState(Date.now)
  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 1000)
    return () => clearInterval(timer)
  }, [])
  const seconds = Math.max(0, Math.ceil((undo.expiresAt - now) / 1000))
  if (!seconds) return null
  return (
    <button
      className="wb-history-undo"
      type="button"
      disabled={disabled}
      title={undo.title}
      aria-label={`${text.undo}: ${undo.title}`}
      onClick={undo.onUndo}
    >
      <span aria-hidden="true">↶</span> {text.undo}
      <span className="wb-history-undo__time" aria-hidden="true">
        {seconds}
        {text.seconds}
      </span>
    </button>
  )
}
