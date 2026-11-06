import { useRef, useState } from 'react'
import { useI18n } from '../../../shared/i18n/i18n'
import { HistoryCard } from '../session-history/HistoryCard'
import { historyMessages } from '../session-history/messages'
import { localHistoryUrl } from '../session-history/transfer'
import type { SessionDocument } from '../session-history/transfer'
import { filmSessionNumber } from '../workbenchHistory'
import { SessionLibrary } from './SessionLibrary'
import type { SessionLibraryProps } from './SessionLibrary'
import { sessionLibraryMessages } from './sessionLibraryMessages'

export type SessionHistoryWorkspaceProps = Omit<
  SessionLibraryProps,
  'onSelect' | 'selectedId' | 'favoritesOnly' | 'onImport'
> & {
  onContinue: (id: string) => void
  onImport: (document: SessionDocument) => string | null
  error?: string
  active?: boolean
}

/** Preview selection is independent of the active simulation and its stored cursor. */
export function SessionHistoryWorkspace({
  onContinue,
  onImport,
  error,
  active = true,
  ...library
}: SessionHistoryWorkspaceProps) {
  const { locale, t } = useI18n()
  const text = sessionLibraryMessages[locale]
  const [filter, setFilter] = useState<'library' | 'favorites'>('library')
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const preview = useRef<HTMLElement>(null)
  const visible = library.history.films.filter(
    (film) => filter === 'library' || film.favorite === true,
  )
  const selected =
    visible.find((film) => film.id === selectedId) ??
    visible.find((film) => film.id === library.history.active) ??
    visible[0]
  const finalFrame = selected?.frames.at(-1)
  const title = selected
    ? selected.title ||
      t('film.session', {
        index: filmSessionNumber(
          selected,
          library.history.films.indexOf(selected),
        ),
      })
    : text.choose
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
            <span aria-hidden="true">{view === 'library' ? '🕘' : '⭐'}</span>
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
          <p>{text.choose}</p>
        </header>
        {error && <p role="alert">{error}</p>}
        <div className="wb-history-columns">
          <div className="wb-history-list wb-scrollbar">
            <SessionLibrary
              key={filter}
              {...library}
              selectedId={selected?.id ?? null}
              favoritesOnly={filter === 'favorites'}
              onImport={(document) => {
                const id = onImport(document)
                if (!id) return false
                setSelectedId(id)
                return true
              }}
              onSelect={(id) => {
                setSelectedId(id)
                if (window.innerWidth <= 760)
                  preview.current?.scrollIntoView({ block: 'nearest' })
              }}
            />
          </div>
          <section
            ref={preview}
            className="wb-history-preview"
            aria-label={text.finalState}
          >
            <div className="wb-history-preview__heading">
              <span>{text.finalState}</span>
              <h3>{title}</h3>
            </div>
            {active && selected && finalFrame && (
              <>
                <div className="wb-history-preview__item">
                  <HistoryCard film={selected} frame={finalFrame} />
                </div>
                <div className="wb-history-preview__actions">
                  <button
                    type="button"
                    disabled={library.disabled}
                    onClick={() => onContinue(selected.id)}
                  >
                    {text.resume} <span aria-hidden="true">→</span>
                  </button>
                  <a href={localHistoryUrl(selected.id)}>
                    {historyMessages[locale].title}{' '}
                    <span aria-hidden="true">↗</span>
                  </a>
                </div>
              </>
            )}
          </section>
        </div>
      </div>
    </div>
  )
}
