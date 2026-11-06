import { historyMessages } from '../session-history/messages'
import { useState } from 'react'
import { SessionImport } from './SessionImport'
import {
  decodeDocument,
  documentFor,
  saveBlob,
} from '../session-history/transfer'
import type { SessionDocument } from '../session-history/transfer'
import { useI18n, gameName } from '../../../shared/i18n/i18n'
import { BaseImage } from '../base-picker/BaseImage'
import { LeftShelfOverlay } from '../base-picker/LeftShelfOverlay'
import type { ShelfOverlayProps } from '../base-picker/LeftShelfOverlay'
import { filmSessionNumber } from '../workbenchHistory'
import type { Film, Films } from '../workbenchHistory'
import { sessionLibraryMessages } from './sessionLibraryMessages'
import './session-library.css'

export type SessionLibraryProps = Omit<ShelfOverlayProps, 'onClose'> & {
  open: boolean
  onOpenChange: (open: boolean) => void
  history: Films
  onSelect: (id: string) => void
  onRename: (id: string, title: string) => void
  onFavoriteChange: (id: string, favorite: boolean) => void
  onDelete: (id: string) => void
  disabled?: boolean
  undo?: { title: string; onUndo: () => void }
  imageForBase?: (baseItemId: string) => string | undefined
  favoritesOnly?: boolean
  onImport?: (document: SessionDocument) => boolean
}

/** Session data and durable mutations belong to the parent; ephemeral confirmations stay here. */
export function SessionLibrary(props: SessionLibraryProps) {
  if (!props.open) return null
  return (
    <SessionLibraryContent
      key={String(props.favoritesOnly === true)}
      {...props}
    />
  )
}

function SessionLibraryContent({
  id,
  anchor,
  placementRegion,
  shelf,
  onOpenChange,
  history,
  onSelect,
  onRename,
  onFavoriteChange,
  onDelete,
  disabled = false,
  undo,
  imageForBase,
  favoritesOnly = false,
  onImport,
}: SessionLibraryProps) {
  const { locale, t } = useI18n()
  const messages = sessionLibraryMessages[locale]
  const [editing, setEditing] = useState<string | null>(null)
  const [title, setTitle] = useState('')
  const [confirming, setConfirming] = useState<string | null>(null)
  const [transferError, setTransferError] = useState('')
  const transferText = historyMessages[locale]
  function label(film: Film, index: number) {
    return typeof film.title === 'string' && film.title.trim()
      ? film.title
      : t('film.session', { index: filmSessionNumber(film, index) })
  }
  return (
    <LeftShelfOverlay
      id={id}
      anchor={anchor}
      {...(placementRegion ? { placementRegion } : {})}
      {...(shelf ? { shelf } : {})}
      onClose={() => onOpenChange(false)}
      title={favoritesOnly ? messages.favorites : messages.history}
      closeLabel={favoritesOnly ? messages.closeFavorites : messages.close}
    >
      <div className="wb-session-library">
        {!favoritesOnly && onImport && (
          <SessionImport disabled={disabled} onImport={onImport} />
        )}
        {transferError && <p role="alert">{transferError}</p>}
        {undo && (
          <div className="wb-session-library__undo" role="status">
            <span>
              {messages.deleted}: {undo.title}
            </span>
            <button type="button" disabled={disabled} onClick={undo.onUndo}>
              {messages.undo}
            </button>
          </div>
        )}
        {!(favoritesOnly
          ? history.films.some((film) => film.favorite === true)
          : history.films.length > 0) && (
          <p>{favoritesOnly ? messages.emptyFavorites : messages.empty}</p>
        )}
        {history.films.map((film, index) => {
          // Keep the original index for legacy records without a sessionNumber.
          if (favoritesOnly && film.favorite !== true) return null
          const state = film.frames.at(-1)?.state
          const number = filmSessionNumber(film, index)
          const displayTitle = label(film, index)
          const baseId = state?.baseItemId ?? ''
          const baseName = gameName(baseId, t('ui.equipment_base'), locale)
          return (
            <article
              key={film.id}
              className="wb-session-card"
              data-active={history.active === film.id}
              aria-label={displayTitle}
            >
              <div className="wb-session-card__main">
                <button
                  type="button"
                  className="wb-session-card__select"
                  aria-label={`${messages.select}: ${displayTitle}`}
                  aria-pressed={history.active === film.id}
                  disabled={disabled}
                  onClick={() => onSelect(film.id)}
                >
                  <BaseImage
                    baseItemId={baseId}
                    label={baseName}
                    image={imageForBase?.(baseId)}
                  />
                  <span className="wb-session-card__text">
                    <strong>
                      <span className="wb-session-card__number">{number}</span>{' '}
                      {displayTitle}
                    </strong>
                    <small>{baseName}</small>
                  </span>
                </button>
                <button
                  type="button"
                  className="wb-session-card__star"
                  aria-label={`${film.favorite === true ? messages.unfavorite : messages.favorite}: ${displayTitle}`}
                  aria-pressed={film.favorite === true}
                  disabled={disabled}
                  onClick={() =>
                    onFavoriteChange(film.id, film.favorite !== true)
                  }
                >
                  ★
                </button>
              </div>
              <a href={`/history/${encodeURIComponent(film.id)}`}>
                {historyMessages[locale].title}
              </a>
              {editing === film.id ? (
                <form
                  className="wb-session-card__edit"
                  onSubmit={(event) => {
                    event.preventDefault()
                    if (disabled) return
                    onRename(film.id, title)
                    setEditing(null)
                  }}
                >
                  <label>
                    {messages.title}
                    <input
                      aria-label={`${messages.title}: ${displayTitle}`}
                      value={title}
                      disabled={disabled}
                      onChange={(event) => setTitle(event.target.value)}
                    />
                  </label>
                  <button type="submit" disabled={disabled}>
                    {messages.save}
                  </button>
                  <button
                    type="button"
                    disabled={disabled}
                    onClick={() => setEditing(null)}
                  >
                    {messages.cancel}
                  </button>
                </form>
              ) : (
                <div className="wb-session-card__actions">
                  {!favoritesOnly && (
                    <button
                      type="button"
                      aria-label={`${transferText.export}: ${displayTitle}`}
                      onClick={() => {
                        setTransferError('')
                        try {
                          const raw = JSON.stringify(documentFor(film), null, 2)
                          decodeDocument(raw)
                          saveBlob(
                            new Blob([raw], { type: 'application/json' }),
                            'session.json',
                          )
                        } catch {
                          setTransferError(transferText.error)
                        }
                      }}
                    >
                      {transferText.export}
                    </button>
                  )}
                  <button
                    type="button"
                    disabled={disabled}
                    aria-label={`${messages.rename}: ${displayTitle}`}
                    onClick={() => {
                      setConfirming(null)
                      setEditing(film.id)
                      setTitle(typeof film.title === 'string' ? film.title : '')
                    }}
                  >
                    {messages.rename}
                  </button>
                  <button
                    type="button"
                    disabled={disabled}
                    aria-label={`${messages.remove}: ${displayTitle}`}
                    onClick={() => {
                      setEditing(null)
                      setConfirming(film.id)
                    }}
                  >
                    {messages.remove}
                  </button>
                </div>
              )}
              {confirming === film.id && (
                <div
                  className="wb-session-card__confirm"
                  role="group"
                  aria-label={`${messages.confirm}: ${displayTitle}`}
                >
                  <p>
                    {displayTitle} · {messages.confirmBody}
                  </p>
                  <button
                    type="button"
                    disabled={disabled}
                    onClick={() => {
                      onDelete(film.id)
                      setConfirming(null)
                    }}
                  >
                    {messages.confirm}
                  </button>
                  <button
                    type="button"
                    disabled={disabled}
                    onClick={() => setConfirming(null)}
                  >
                    {messages.cancel}
                  </button>
                </div>
              )}
            </article>
          )
        })}
      </div>
    </LeftShelfOverlay>
  )
}
