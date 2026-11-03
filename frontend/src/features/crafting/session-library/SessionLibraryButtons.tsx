import type { RefObject } from 'react'
import { useI18n } from '../../../shared/i18n/i18n'
import { sessionLibraryMessages } from './sessionLibraryMessages'
import './session-library.css'

export type SessionLibraryView = 'history' | 'favorites'

export function SessionLibraryButtons({
  openView,
  onOpenChange,
  historyAnchor,
  favoritesAnchor,
  disabled = false,
  libraryId,
}: {
  openView: SessionLibraryView | null
  onOpenChange: (view: SessionLibraryView | null) => void
  historyAnchor: RefObject<HTMLButtonElement | null>
  favoritesAnchor: RefObject<HTMLButtonElement | null>
  disabled?: boolean
  libraryId: string
}) {
  const { locale } = useI18n()
  const messages = sessionLibraryMessages[locale]
  return (
    <div className="wb-session-library-buttons">
      {(['history', 'favorites'] as const).map((view) => (
        <button
          key={view}
          ref={view === 'history' ? historyAnchor : favoritesAnchor}
          className="wb-session-library-trigger"
          type="button"
          disabled={disabled}
          aria-label={messages[view]}
          title={messages[view]}
          aria-haspopup="dialog"
          aria-expanded={openView === view}
          aria-controls={libraryId}
          onClick={() => onOpenChange(openView === view ? null : view)}
        >
          <span aria-hidden="true">{view === 'history' ? '🕘' : '⭐'}</span>
        </button>
      ))}
    </div>
  )
}
