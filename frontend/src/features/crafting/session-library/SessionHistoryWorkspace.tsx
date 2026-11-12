import { HistoryWorkspaceLayout } from './HistoryWorkspaceLayout'
import { ClearSessions } from '../../account/ClearSessions'
import { SessionShareLink } from '../../community/SessionShareLink'
import { saveBaseFavorite } from '../base-favorites/repository'
import { baseFavoriteMessages } from '../base-favorites/messages'
import { orderedFilms } from '../workbenchHistory'
import { freshCraftMessages } from './freshCraftMessages'
import { SessionDetail } from '../session-detail/SessionDetail'
import { useEffect, useRef, useState } from 'react'
import { useI18n } from '../../../shared/i18n/i18n'
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
  const pageLabels = {
    en: ['Previous page', 'Next page'],
    ko: ['이전 페이지', '다음 페이지'],
    'zh-CN': ['上一页', '下一页'],
    'zh-TW': ['上一頁', '下一頁'],
    ja: ['前のページ', '次のページ'],
    es: ['Página anterior', 'Página siguiente'],
    ru: ['Предыдущая страница', 'Следующая страница'],
    th: ['หน้าก่อนหน้า', 'หน้าถัดไป'],
  }[locale]
  const [filter, setFilter] = useState<'library' | 'favorites'>(() =>
    new URLSearchParams(location.search).get('section') === 'favorites'
      ? 'favorites'
      : 'library',
  )
  const [selectedId, setSelectedId] = useState<string | null>(() =>
    new URLSearchParams(location.search).get('session'),
  )
  const [page, setPage] = useState(1)
  const [favoriteStatus, setFavoriteStatus] = useState('')
  const preview = useRef<HTMLElement>(null)
  const importPreview = useRef<HTMLDivElement>(null)
  const visible = orderedFilms(
    library.history.films,
    filter === 'favorites',
  ).filter((film) => filter === 'library' || film.favorite === true)
  const pages = Math.max(1, Math.ceil(visible.length / 10))
  const currentPage = Math.min(page, pages)
  const pageFilms = visible.slice((currentPage - 1) * 10, currentPage * 10)
  const selected =
    pageFilms.find((film) => film.id === selectedId) ??
    pageFilms.find((film) => film.id === library.history.active) ??
    pageFilms[0]
  const finalFrame = selected?.frames.at(-1)
  const title = selected ? sessionTitle(selected, locale) : text.empty
  return (
    <HistoryWorkspaceLayout
      navigation={
        <nav className="wb-history-nav" aria-label={text.navigation}>
          {(['library', 'favorites'] as const).map((view) => (
            <button
              key={view}
              type="button"
              aria-current={filter === view ? 'page' : undefined}
              onClick={() => {
                setFilter(view)
                setPage(1)
                setFavoriteStatus('')
              }}
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
                  : library.history.films.filter((film) => film.favorite)
                      .length}
              </span>
            </button>
          ))}
        </nav>
      }
      heading={
        <>
          <h2>{text[filter]}</h2>
          <div className="wb-history-heading__actions">
            <SessionImport
              disabled={library.disabled ?? false}
              previewTarget={importPreview}
              onImport={async (document) => {
                const id = await onImport(document)
                if (!id) return false
                setFilter('library')
                setPage(1)
                setSelectedId(id)
                return true
              }}
            />
            <ClearSessions disabled={library.disabled ?? false} />
            {undo && (
              <DeletionUndo
                key={undo.expiresAt}
                undo={undo}
                disabled={library.disabled ?? false}
              />
            )}
          </div>
        </>
      }
      beforeColumns={
        <>
          {error && <p role="alert">{error}</p>}
          <div className="wb-history-import-preview" ref={importPreview} />
        </>
      }
      list={
        <>
          <nav
            className="wb-history-pagination"
            aria-label={pageLabels.join(' / ')}
          >
            <button
              type="button"
              disabled={currentPage === 1}
              aria-label={pageLabels[0]}
              onClick={() => setPage(currentPage - 1)}
            >
              ‹
            </button>
            {Array.from({ length: pages }, (_, i) => i + 1)
              .filter(
                (n) => n === 1 || n === pages || Math.abs(n - currentPage) <= 2,
              )
              .map((n, i, shown) => (
                <span key={n}>
                  {i > 0 && n - shown[i - 1]! > 1 && (
                    <span aria-hidden="true">…</span>
                  )}
                  <button
                    type="button"
                    aria-current={n === currentPage ? 'page' : undefined}
                    onClick={() => setPage(n)}
                  >
                    {n}
                  </button>
                </span>
              ))}
            <button
              type="button"
              disabled={currentPage === pages}
              aria-label={pageLabels[1]}
              onClick={() => setPage(currentPage + 1)}
            >
              ›
            </button>
          </nav>
          <div className="wb-history-list wb-scrollbar">
            <SessionLibrary
              key={filter}
              {...library}
              page={currentPage}
              selectedId={selected?.id ?? null}
              favoritesOnly={filter === 'favorites'}
              onSelect={(id) => {
                setSelectedId(id)
                if (window.innerWidth <= 760)
                  preview.current?.scrollIntoView({ block: 'nearest' })
              }}
            />
          </div>
        </>
      }
      preview={
        selected ? (
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
              <SessionDetail
                film={selected}
                cursor={selected.frames.length - 1}
                itemClassName="wb-history-preview__item"
                actions={
                  <div className="wb-history-preview__actions">
                    <SessionShareLink id={selected.id} />
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
                    <button
                      type="button"
                      disabled={library.disabled}
                      onClick={() => {
                        try {
                          saveBaseFavorite(selected)
                          setFavoriteStatus(baseFavoriteMessages[locale].saved)
                        } catch {
                          setFavoriteStatus(baseFavoriteMessages[locale].error)
                        }
                      }}
                    >
                      {baseFavoriteMessages[locale].add}
                    </button>
                    {favoriteStatus && (
                      <span role="status">{favoriteStatus}</span>
                    )}
                    <a href={localHistoryUrl(selected.id, 'history', filter)}>
                      {historyMessages[locale].title}{' '}
                      <span aria-hidden="true">↗</span>
                    </a>
                  </div>
                }
              />
            )}
          </section>
        ) : null
      }
    />
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
