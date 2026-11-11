import { useLayoutEffect, useRef, useState } from 'react'
import { useI18n } from '../../../shared/i18n/i18n'
import { sessionLibraryMessages } from './sessionLibraryMessages'

export function SessionTitleEditor({
  title,
  disabled = false,
  onSave,
}: {
  title: string
  disabled?: boolean
  onSave: (title: string) => boolean | Promise<boolean>
}) {
  const { locale } = useI18n()
  const text = sessionLibraryMessages[locale]
  const heading = useRef<HTMLHeadingElement>(null)
  const editing = useRef(false)
  const original = useRef(title)
  const composing = useRef(false)
  const [editable, setEditable] = useState(false)
  const [failed, setFailed] = useState(false)
  useLayoutEffect(() => {
    if (!editable || !heading.current) return
    heading.current.focus()
    const selection = window.getSelection()
    selection?.selectAllChildren(heading.current)
  }, [editable])
  const committing = useRef(false)
  async function finish(save: boolean) {
    if (committing.current || !editing.current || (save && disabled)) return
    const value = heading.current?.textContent?.trim() ?? ''
    if (save && value && value !== original.current && !disabled) {
      committing.current = true
      let saved: boolean
      try {
        saved = await onSave(value)
      } catch {
        saved = false
      } finally {
        committing.current = false
      }
      if (!saved) {
        setFailed(true)
        return
      }
    } else if (heading.current) heading.current.textContent = title
    editing.current = false
    setEditable(false)
    setFailed(false)
  }
  return (
    <div className="wb-history-title">
      <h3
        ref={heading}
        contentEditable={editable && !disabled ? 'plaintext-only' : false}
        aria-disabled={disabled || undefined}
        suppressContentEditableWarning
        role={editable ? 'textbox' : undefined}
        aria-label={editable ? text.title : undefined}
        aria-invalid={failed || undefined}
        tabIndex={editable ? 0 : undefined}
        onCompositionStart={() => {
          composing.current = true
        }}
        onCompositionEnd={() => {
          composing.current = false
        }}
        onBlur={() => finish(true)}
        onKeyDown={(event) => {
          if (event.key === 'Escape') {
            event.preventDefault()
            event.stopPropagation()
            finish(false)
          }
          if (
            event.key === 'Enter' &&
            !composing.current &&
            !event.nativeEvent.isComposing
          ) {
            event.preventDefault()
            finish(true)
          }
        }}
      >
        {title}
      </h3>
      <button
        type="button"
        className="wb-history-title__edit"
        aria-label={`${text.rename}: ${title}`}
        disabled={disabled || editable}
        onClick={() => {
          original.current = title
          editing.current = true
          setFailed(false)
          setEditable(true)
        }}
      >
        <svg viewBox="0 0 20 20" aria-hidden="true">
          <path d="m12.5 3 4.5 4.5M3 17l4.5-1 10-10a1.6 1.6 0 0 0 0-2.3l-1.2-1.2a1.6 1.6 0 0 0-2.3 0L4 12.5 3 17Z" />
        </svg>
      </button>
      {failed && (
        <p className="wb-history-title__error" role="alert">
          {text.saveError}
        </p>
      )}
    </div>
  )
}
