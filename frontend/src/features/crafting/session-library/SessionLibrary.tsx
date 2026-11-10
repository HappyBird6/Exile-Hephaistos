import { orderedFilms } from '../workbenchHistory'
import { useState } from 'react'
import { useI18n, gameName } from '../../../shared/i18n/i18n'
import { BaseImage } from '../base-picker/BaseImage'
import { historyMessages } from '../session-history/messages'
import {
  decodeDocument,
  documentFor,
  saveBlob,
  localHistoryUrl,
} from '../session-history/transfer'
import type { Films } from '../workbenchHistory'
import { sessionLibraryMessages } from './sessionLibraryMessages'
import { sessionTitle } from './sessionTitle'
import './session-library.css'

export type SessionLibraryProps = {
  history: Films
  selectedId?: string | null
  onSelect: (id: string) => void
  onFavoriteChange: (id: string, favorite: boolean) => void
  onDelete: (id: string) => void
  disabled?: boolean
  imageForBase?: (baseItemId: string) => string | undefined
  favoritesOnly?: boolean
}

export function SessionLibrary({
  history,
  selectedId = history.active,
  onSelect,
  onFavoriteChange,
  onDelete,
  disabled = false,
  imageForBase,
  favoritesOnly = false,
}: SessionLibraryProps) {
  const { locale, t } = useI18n()
  const text = sessionLibraryMessages[locale]
  const transfer = historyMessages[locale]
  const [error, setError] = useState(false)
  return (
    <div className="wb-session-library">
      {error && <p role="alert">{transfer.error}</p>}
      {!history.films.some((film) => !favoritesOnly || film.favorite) && (
        <p>{favoritesOnly ? text.emptyFavorites : text.empty}</p>
      )}
      {orderedFilms(history.films, favoritesOnly)
        .filter((film) => !favoritesOnly || film.favorite)
        .map((film) => {
          const title = sessionTitle(film, locale)
          const baseId = film.frames.at(-1)?.state.baseItemId ?? ''
          const baseName = gameName(baseId, t('ui.equipment_base'), locale)
          return (
            <article
              key={film.id}
              className="wb-session-card"
              data-active={selectedId === film.id}
              aria-label={title}
              onClick={(event) => {
                if (
                  event.target instanceof Element &&
                  !event.target.closest('button, a, input')
                )
                  onSelect(film.id)
              }}
            >
              <div className="wb-session-card__main">
                <button
                  type="button"
                  className="wb-session-card__select"
                  aria-label={`${text.select}: ${title}`}
                  aria-pressed={selectedId === film.id}
                  onClick={() => onSelect(film.id)}
                >
                  <BaseImage
                    baseItemId={baseId}
                    label={baseName}
                    image={imageForBase?.(baseId)}
                  />
                  <span className="wb-session-card__text">
                    <strong>{title}</strong>
                    <small>{baseName}</small>
                  </span>
                </button>
                <button
                  type="button"
                  className="wb-session-card__star"
                  aria-label={`${film.favorite ? text.unfavorite : text.favorite}: ${title}`}
                  aria-pressed={film.favorite === true}
                  disabled={disabled}
                  onClick={() => onFavoriteChange(film.id, !film.favorite)}
                >
                  <span className="session-favorite-icon" aria-hidden="true">
                    ⭐
                  </span>
                </button>
              </div>
              <div className="wb-session-card__actions">
                <a
                  className="wb-session-card__overview"
                  href={localHistoryUrl(
                    film.id,
                    'history',
                    favoritesOnly ? 'favorites' : 'library',
                  )}
                >
                  <svg viewBox="0 0 20 20" aria-hidden="true">
                    <rect x="2.5" y="3" width="15" height="14" rx="2" />
                    <path d="M6 7h8M6 10h8M6 13h5" />
                  </svg>
                  {transfer.title}
                </a>
                <button
                  type="button"
                  aria-label={`${transfer.export}: ${title}`}
                  onClick={() => {
                    setError(false)
                    try {
                      const raw = JSON.stringify(documentFor(film), null, 2)
                      decodeDocument(raw)
                      saveBlob(
                        new Blob([raw], { type: 'application/json' }),
                        'session.json',
                      )
                    } catch {
                      setError(true)
                    }
                  }}
                >
                  {transfer.export}
                </button>
                <button
                  type="button"
                  className="wb-session-card__delete"
                  disabled={disabled}
                  aria-label={`${text.remove}: ${title}`}
                  onClick={() => onDelete(film.id)}
                >
                  {text.remove}
                </button>
              </div>
            </article>
          )
        })}
    </div>
  )
}
