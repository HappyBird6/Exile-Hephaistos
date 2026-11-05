import { useEffect, useRef, useState } from 'react'
import { useIsFetching, useQuery } from '@tanstack/react-query'
import { useI18n } from '../../../shared/i18n/i18n'
import { LocaleSelector } from '../../../shared/i18n/LocaleSelector'
import { LocalFilmRepository } from '../workbenchHistory'
import type { Film } from '../workbenchHistory'
import { currencies } from '../currencies'
import { currencyNames } from '../currencyNames'
import { materials, additionalStashCurrencies } from '../materials'
import { SessionFlow } from './SessionFlow'
import { historyMessages } from './messages'
import {
  appendDocument,
  decodeDocument,
  documentFor,
  localHistoryUrl,
  saveBlob,
  transferLimit,
} from './transfer'
import type { SessionDocument } from './transfer'
import { snapshotPages } from './snapshot'
import './history.css'

const resources = [
  ...currencies.map((c) => ({
    ...c,
    name: currencyNames[c.id] ?? c.id,
    category: 'Currency' as const,
  })),
  ...materials,
  ...additionalStashCurrencies,
]
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
  const [preview, setPreview] = useState<SessionDocument | null>(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [link, setLink] = useState('')
  const [sharing, setSharing] = useState(false)
  const shareTrigger = useRef<HTMLButtonElement>(null)
  const [pngs, setPngs] = useState<string[]>([])
  const pngRefs = useRef<string[]>([])
  useEffect(
    () => () => {
      pngRefs.current.forEach(URL.revokeObjectURL)
    },
    [],
  )
  const fileInput = useRef<HTMLInputElement>(null)
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
  const shown = preview?.film ?? film
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
    const body = JSON.stringify(documentFor(film))
    decodeDocument(body)
    const r = await fetch('/api/v1/session-snapshots', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body,
    })
    if (!r.ok) throw Error('share')
    const result: unknown = await r.json()
    const shareId = (result as { id?: string }).id
    if (!shareId || !/^[a-f0-9]{48}$/.test(shareId)) throw Error('id')
    setLink(new URL(`/history/shared/${shareId}`, location.origin).href)
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
        <nav className="workspace-nav" aria-label={t('ui.main_navigation')}>
          <a href="/">{text.home}</a>
          <a href={location.pathname} aria-current="page">
            {text.title}
          </a>
          <a className="workspace-admin" href="/admin">
            {t('ui.admin')}
          </a>
        </nav>
        <LocaleSelector />
      </header>
      <div className="history-heading">
        <h1>{shown?.title || text.title}</h1>
        <div className="history-actions">
          <label>
            {text.import}
            <input
              className="history-file-input"
              ref={fileInput}
              type="file"
              accept=".json,application/json"
              disabled={busy}
              onChange={(e) => {
                const file = e.target.files?.[0]
                e.target.value = ''
                if (file)
                  void action(async () => {
                    if (file.size > transferLimit) throw Error('size')
                    setPreview(decodeDocument(await file.text()))
                  })
              }}
            />
          </label>
          {film && !preview && (
            <>
              <button
                disabled={busy}
                onClick={() =>
                  void action(() => {
                    const raw = JSON.stringify(documentFor(film), null, 2)
                    decodeDocument(raw)
                    saveBlob(
                      new Blob([raw], { type: 'application/json' }),
                      'session.json',
                    )
                  })
                }
              >
                {text.export}
              </button>
              <button
                ref={shareTrigger}
                disabled={busy}
                onClick={() => setSharing(true)}
              >
                {text.share}
              </button>
            </>
          )}
          {shown && (
            <button
              disabled={busy || data.isFetching || catalogsLoading > 0}
              onClick={() =>
                void action(async () => {
                  if (!root.current) return
                  const blobs = await snapshotPages(root.current)
                  pngRefs.current.forEach(URL.revokeObjectURL)
                  pngRefs.current = blobs.map((b) => URL.createObjectURL(b))
                  setPngs(pngRefs.current)
                  if (blobs.length === 1) saveBlob(blobs[0]!, 'session.png')
                })
              }
            >
              {text.snapshot}
            </button>
          )}
        </div>
      </div>
      {sharing && (
        <section
          className="history-share-confirm"
          aria-label={text.share}
          onKeyDown={(e) => {
            if (e.key === 'Escape' && !busy) {
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
      {link && (
        <div className="history-link">
          <input
            aria-label={text.shared}
            value={link}
            readOnly
            onFocus={(e) => e.target.select()}
          />
          <button
            onClick={() =>
              void action(() => navigator.clipboard.writeText(link))
            }
          >
            {text.copy}
          </button>
          <a href={link}>{text.shared}</a>
        </div>
      )}
      <div className="history-pngs" role="status">
        {pngs.map((url, i) => (
          <a key={url} href={url} download={`session-${i + 1}.png`}>
            {text.download} {i + 1}
          </a>
        ))}
      </div>
      {error && <p role="alert">{error}</p>}
      {data.isError && <p role="alert">{text.error}</p>}
      {!shown && !data.isFetching && <p>{text.missing}</p>}
      {preview && (
        <section
          className="history-preview"
          onKeyDown={(e) => {
            if (e.key === 'Escape' && !busy) {
              setPreview(null)
              fileInput.current?.focus()
            }
          }}
        >
          <h2>{text.preview}</h2>
          <p>
            {preview.film.title ?? preview.film.id} ·{' '}
            {preview.film.frames.length} {text.count}
          </p>
          <button
            disabled={busy}
            onClick={() =>
              void action(() => {
                const repository = new LocalFilmRepository(localStorage)
                const added = appendDocument(repository.load(), preview)
                repository.save(added.history)
                location.assign(localHistoryUrl(added.id))
              })
            }
          >
            {text.confirm}
          </button>
          <button
            disabled={busy}
            onClick={() => {
              setPreview(null)
              fileInput.current?.focus()
            }}
          >
            {t('ui.cancel_preview')}
          </button>
        </section>
      )}
      {shown && (
        <div className="session-flow" ref={root}>
          <SessionFlow film={shown} resources={resources} />
        </div>
      )}
    </main>
  )
}
