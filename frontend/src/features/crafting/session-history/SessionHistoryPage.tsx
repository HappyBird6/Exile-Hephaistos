import { useEffect, useRef, useState } from 'react'
import { useIsFetching, useQuery } from '@tanstack/react-query'
import { useI18n } from '../../../shared/i18n/i18n'
import { LocaleSelector } from '../../../shared/i18n/LocaleSelector'
import { LocalFilmRepository } from '../workbenchHistory'
import type { Film } from '../workbenchHistory'
import { currencies } from '../currencies'
import { currencyNames } from '../currencyNames'
import { materials, additionalStashCurrencies } from '../materials'
import {
  SessionNotation,
  NotationResources,
} from '../session-notation/SessionNotation'
import { extractSessionNotation } from '../session-notation/extractSessionNotation'
import { historyMessages } from './messages'
import { HistoryCard } from './HistoryCard'
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
  const { locale, name, t } = useI18n()
  const text = historyMessages[locale]
  const catalogsLoading = useIsFetching({ queryKey: ['history-catalog'] })
  const [preview, setPreview] = useState<SessionDocument | null>(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [link, setLink] = useState('')
  const [pngs, setPngs] = useState<string[]>([])
  const pngRefs = useRef<string[]>([])
  useEffect(
    () => () => {
      pngRefs.current.forEach(URL.revokeObjectURL)
    },
    [],
  )
  const fileInput = useRef<HTMLInputElement>(null)
  const root = useRef<HTMLOListElement>(null)
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
  const notation = (() => {
    try {
      return {
        ...extractSessionNotation(shown, shown ? shown.frames.length - 1 : 0),
        failed: false,
      }
    } catch {
      return {
        failed: true,
        steps: [],
        omittedFrames: shown?.frames.length ?? 0,
        incompleteOmens: true,
      }
    }
  })()
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
    <main className="history-page">
      <header>
        <a href="/">{text.home}</a>
        <h1>{text.title}</h1>
        <LocaleSelector />
      </header>
      <p>{text.scope}</p>
      <p>{text.privacy}</p>
      <div className="history-actions">
        <label>
          {text.import}
          <input
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
            <button disabled={busy} onClick={() => void action(publish)}>
              {text.share}
            </button>
            <button
              disabled={busy}
              onClick={() =>
                void action(() =>
                  setLink(
                    new URL(
                      shared ? `/history/shared/${id}` : localHistoryUrl(id),
                      location.origin,
                    ).href,
                  ),
                )
              }
            >
              {shared ? text.shared : text.local}
            </button>
          </>
        )}
        {shown && (
          <button
            disabled={busy || data.isFetching || catalogsLoading > 0}
            onClick={() =>
              void action(async () => {
                if (!root.current) return
                const blobs = await snapshotPages(
                  root.current,
                  shown.title ?? text.title,
                )
                pngRefs.current.forEach(URL.revokeObjectURL)
                pngRefs.current = blobs.map((b) => URL.createObjectURL(b))
                setPngs(pngRefs.current)
              })
            }
          >
            {text.snapshot}
          </button>
        )}
      </div>
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
        <>
          <h2>{shown.title ?? shown.id}</h2>
          <p>
            {shown.frames.length} {text.count} · {text.rules}:{' '}
            {shown.rulesetIdentity ?? '?'}
          </p>
          {!notation.failed && (
            <SessionNotation
              film={shown}
              cursor={shown.frames.length - 1}
              resources={resources}
            />
          )}
          <ol className="history-grid" ref={root}>
            {shown.frames.map((frame, i) => {
              const step = notation.steps.find((s) => s.frameIndex === i)
              const resource =
                step && resources.find((r) => r.id === step.currencyId)
              return (
                <li
                  key={i}
                  data-snapshot-card
                  data-index={i}
                  className="history-cell"
                  style={
                    {
                      '--column4':
                        Math.floor(i / 4) % 2 ? 4 - (i % 4) : (i % 4) + 1,
                      '--row4': Math.floor(i / 4) + 1,
                      '--column2':
                        Math.floor(i / 2) % 2 ? 2 - (i % 2) : (i % 2) + 1,
                      '--row2': Math.floor(i / 2) + 1,
                    } as React.CSSProperties
                  }
                >
                  <div className="history-edge">
                    <span aria-hidden="true" className="history-arrow">
                      ↓
                    </span>
                    {i === 0 ? (
                      text.initial
                    ) : step ? (
                      <>
                        <NotationResources step={step} resources={resources} />
                        <span>
                          {name(
                            step.currencyId,
                            resource?.name ?? step.currencyId,
                          )}
                          {step.repeated ? ` × ${step.uses}` : ''}
                        </span>
                      </>
                    ) : (
                      <span>
                        {text.observation}: {frame.action ?? '?'}
                      </span>
                    )}
                  </div>
                  <span className="history-step">#{i}</span>
                  <HistoryCard film={shown} frame={frame} />
                </li>
              )
            })}
          </ol>
          {shown.sourceDocument !== undefined && (
            <details>
              <summary>{text.original}</summary>
              <pre>{shown.sourceDocument}</pre>
            </details>
          )}
        </>
      )}
      <div className="history-pngs">
        {pngs.map((url, i) => (
          <a key={url} href={url} download={`session-${i + 1}.png`}>
            {text.download} {i + 1}
            <img src={url} alt={`${text.snapshot} ${i + 1}`} />
          </a>
        ))}
      </div>
    </main>
  )
}
