import { useRef, useState } from 'react'
import type { RefObject } from 'react'
import { createPortal } from 'react-dom'
import { useI18n } from '../../../shared/i18n/i18n'
import { historyMessages } from '../session-history/messages'
import { decodeDocument, transferLimit } from '../session-history/transfer'
import type { SessionDocument } from '../session-history/transfer'
import { SessionFlow } from '../session-history/SessionFlow'
import { historyResources } from '../session-history/resources'
import '../session-history/history.css'

export function SessionImport({
  disabled,
  onImport,
  previewTarget,
}: {
  disabled: boolean
  previewTarget?: RefObject<HTMLDivElement | null>
  onImport: (document: SessionDocument) => boolean
}) {
  const { locale, t } = useI18n()
  const text = historyMessages[locale]
  const input = useRef<HTMLInputElement>(null)
  const [preview, setPreview] = useState<SessionDocument | null>(null)
  const [reading, setReading] = useState(false)
  const [error, setError] = useState(false)

  function cancel() {
    setPreview(null)
    input.current?.focus()
  }

  async function read(file: File) {
    setReading(true)
    setError(false)
    setPreview(null)
    try {
      if (file.size > transferLimit) throw Error('size')
      setPreview(decodeDocument(await file.text()))
    } catch {
      setError(true)
    } finally {
      setReading(false)
    }
  }

  const content = preview ? (
    <section
      className="wb-session-import__preview"
      aria-label={text.preview}
      onKeyDown={(event) => {
        if (event.key === 'Escape' && !reading) {
          event.stopPropagation()
          cancel()
        }
      }}
    >
      <h4>{text.preview}</h4>
      <p>
        {preview.film.title ?? preview.film.id} · {preview.film.frames.length}{' '}
        {text.count}
      </p>
      <div className="wb-session-import__actions">
        <button
          type="button"
          disabled={disabled || reading}
          onClick={() => {
            setError(false)
            try {
              if (!onImport(preview)) throw Error('save')
              setPreview(null)
            } catch {
              setError(true)
            }
          }}
        >
          {text.confirm}
        </button>
        <button type="button" disabled={reading} onClick={cancel}>
          {t('ui.cancel_preview')}
        </button>
      </div>
      <div
        className="wb-session-import__flow"
        tabIndex={0}
        aria-label={text.preview}
      >
        <SessionFlow
          film={preview.film}
          resources={historyResources}
          columnCount={1}
        />
      </div>
    </section>
  ) : null

  return (
    <div className="wb-session-import">
      <label className="wb-session-import__file">
        {text.import}
        <input
          ref={input}
          type="file"
          accept=".json,application/json"
          disabled={disabled || reading}
          onChange={(event) => {
            const file = event.target.files?.[0]
            event.target.value = ''
            if (file) void read(file)
          }}
        />
      </label>
      {error && <p role="alert">{text.error}</p>}
      {content && previewTarget?.current
        ? createPortal(content, previewTarget.current)
        : content}
    </div>
  )
}
