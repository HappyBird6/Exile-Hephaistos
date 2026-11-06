import { useRef, useState } from 'react'
import { useIsFetching, useQuery } from '@tanstack/react-query'
import { useI18n } from '../../../shared/i18n/i18n'
import { LocalFilmRepository } from '../workbenchHistory'
import type { Film } from '../workbenchHistory'
import { SessionFlow } from './SessionFlow'
import { historyResources } from './resources'
import { historyMessages } from './messages'
import {
  decodeDocument,
  documentFor,
  saveBlob,
  transferLimit,
} from './transfer'
import { snapshotPages } from './snapshot'
import './history.css'

export function SessionHistoryPage({
  id,
  shared = false,
}: {
  id: string
  shared?: boolean
}) {
  const { locale, t } = useI18n()
  const text = historyMessages[locale]
  const catalogsLoading = useIsFetching({ queryKey: ['history-catalog'] })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [sharing, setSharing] = useState(false)
  const [sharedLink, setSharedLink] = useState<{
    film: Film
    url: string
  } | null>(null)
  const [copiedLink, setCopiedLink] = useState('')
  const shareTrigger = useRef<HTMLButtonElement>(null)
  const root = useRef<HTMLDivElement>(null)
  const data = useQuery({
    queryKey: ['session-history', id, shared],
    retry: false,
    queryFn: async ({ signal }): Promise<Film | null> => {
      if (shared) {
        if (!/^[a-f0-9]{48}$/.test(id)) throw Error('id')
        const r = await fetch(`/api/v1/session-snapshots/${id}`, {
          signal,
          cache: 'no-store',
        })
        if (!r.ok) throw Error('read')
        const reader = r.body?.getReader()
        if (!reader) throw Error('read')
        const chunks: Uint8Array[] = []
        let size = 0
        try {
          while (true) {
            const chunk = await reader.read()
            if (chunk.done) break
            size += chunk.value.length
            if (size > transferLimit) throw Error('size')
            chunks.push(chunk.value)
          }
        } finally {
          await reader.cancel()
        }
        return decodeDocument(await new Blob(chunks as BlobPart[]).text()).film
      }
      return (
        new LocalFilmRepository(localStorage)
          .load()
          .films.find((f) => f.id === id) ?? null
      )
    },
  })
  const film = data.data
  const link = sharedLink?.film === film ? sharedLink?.url : undefined
  async function action(run: () => Promise<void> | void) {
    setBusy(true)
    setError('')
    try {
      await run()
    } catch {
      setError(text.error)
    } finally {
      setBusy(false)
    }
  }
  async function publish() {
    if (!film) return
    let url = link
    if (!url) {
      const body = JSON.stringify(documentFor(film))
      decodeDocument(body)
      const response = await fetch('/api/v1/session-snapshots', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body,
      })
      if (!response.ok) throw Error('share')
      const result: unknown = await response.json()
      const shareId = (result as { id?: string }).id
      if (!shareId || !/^[a-f0-9]{48}$/.test(shareId)) throw Error('id')
      url = new URL(`/history/shared/${shareId}`, location.origin).href
      // Retain the created link if clipboard access fails so a retry never republishes the film.
      setSharedLink({ film, url })
    }
    await navigator.clipboard.writeText(url)
    setCopiedLink(url)
  }
  return (
    <main className="craft-page history-page">
      <header className="craft-header">
        <a
          className="craft-brand"
          href="/"
          aria-label={t('ui.exile_hephaistos_home')}
        >
          <span className="brand-mark" aria-hidden="true">
            H
          </span>
          <span>
            EXILE <b>HEPHAISTOS</b>
            <small>{t('ui.path_of_exile_2_crafting_workbench')}</small>
          </span>
        </a>
        <h1 className="history-title" title={film?.title || text.title}>
          {film?.title || text.title}
        </h1>
        <div className="history-actions">
          <button
            ref={shareTrigger}
            disabled={busy || !film}
            aria-live="polite"
            onClick={() => {
              if (link) void action(publish)
              else setSharing(true)
            }}
          >
            {link && copiedLink === link ? text.copied : text.share}
          </button>
          <button
            disabled={busy || !film || data.isFetching || catalogsLoading > 0}
            onClick={() =>
              void action(async () => {
                if (!root.current) return
                const blobs = await snapshotPages(root.current)
                blobs.forEach((blob, i) =>
                  saveBlob(
                    blob,
                    blobs.length === 1 ? 'session.png' : `session-${i + 1}.png`,
                  ),
                )
              })
            }
          >
            {text.snapshot}
          </button>
        </div>
      </header>
      {sharing && (
        <section
          className="history-share-confirm"
          aria-label={text.share}
          onKeyDown={(event) => {
            if (event.key === 'Escape' && !busy) {
              setSharing(false)
              shareTrigger.current?.focus()
            }
          }}
        >
          <p>{text.privacy}</p>
          <button
            disabled={busy}
            onClick={() =>
              void action(async () => {
                await publish()
                setSharing(false)
                shareTrigger.current?.focus()
              })
            }
          >
            {text.share}
          </button>
          <button
            disabled={busy}
            onClick={() => {
              setSharing(false)
              shareTrigger.current?.focus()
            }}
          >
            {t('ui.cancel_preview')}
          </button>
        </section>
      )}
      {error && <p role="alert">{error}</p>}
      {data.isError && <p role="alert">{text.error}</p>}
      {!film && !data.isFetching && <p>{text.missing}</p>}
      {film && (
        <div className="session-flow" ref={root}>
          <SessionFlow film={film} resources={historyResources} />
        </div>
      )}
    </main>
  )
}
