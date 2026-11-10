import type { RefObject } from 'react'
import { useI18n } from '../../../shared/i18n/i18n'
import { serviceText } from '../serviceMessages'
import type { ItemTextProblem } from './validateItemText'
import './item-editor.css'

export function ItemTextEditorContent({
  value,
  pending,
  disabled,
  problems,
  closeRef,
  onChange,
  onApply,
  onClose,
  onCompositionChange,
}: {
  value: string
  pending: boolean
  disabled: boolean
  problems: ItemTextProblem[]
  closeRef: RefObject<HTMLButtonElement | null>
  onChange: (text: string) => void
  onApply: () => void
  onClose: () => void
  onCompositionChange: (composing: boolean) => void
}) {
  const { t, locale } = useI18n()
  return (
    <div className="item-editor">
      <header className="item-editor__header">
        <h3>{t('ui.edit_item')}</h3>
        <button
          ref={closeRef}
          type="button"
          className="input-close"
          aria-label={t('ui.close_item_input')}
          disabled={disabled}
          onClick={onClose}
        >
          ×
        </button>
      </header>
      <div className="item-editor__layout">
        <aside className="item-editor__sidebar">
          <nav aria-label={t('ui.edit_item')}>
            <button
              type="button"
              aria-current="page"
              onClick={() => document.getElementById('item-text')?.focus()}
            >
              <span aria-hidden="true">≡</span>
              {t('ui.item_text')}
            </button>
          </nav>
          <div
            className="item-editor__feedback wb-scrollbar"
            id="item-editor-feedback"
            aria-live="polite"
            aria-busy={pending}
          >
            {problems.length > 0 && (
              <section role="alert">
                <h4>{t('input.review')}</h4>
                <ul>
                  {problems.map((problem, index) => (
                    <li key={index}>
                      {problem.lineNumber !== undefined && (
                        <span className="item-editor__line">
                          {t('ui.line')} {problem.lineNumber}
                        </span>
                      )}
                      {problem.text && <code lang="en">{problem.text}</code>}
                      <p>
                        {problem.code
                          ? t(problem.code)
                          : serviceText(problem.message ?? '', locale)}
                      </p>
                    </li>
                  ))}
                </ul>
              </section>
            )}
          </div>
          <button
            type="button"
            className="primary-action item-editor__apply"
            disabled={disabled || pending}
            onClick={onApply}
          >
            {pending ? t('input.applying') : t('input.apply')}
          </button>
        </aside>
        <div className="item-editor__input">
          <label htmlFor="item-text">
            {t('ui.item_text_copied_from_the_game')}
          </label>
          <textarea
            id="item-text"
            value={value}
            disabled={disabled}
            className="wb-scrollbar"
            spellCheck={false}
            onCompositionStart={() => onCompositionChange(true)}
            onCompositionEnd={() => onCompositionChange(false)}
            onChange={(event) => onChange(event.target.value)}
            placeholder={t('input.placeholder')}
            aria-invalid={problems.length > 0}
            aria-describedby={
              problems.length ? 'item-editor-feedback' : undefined
            }
          />
        </div>
      </div>
    </div>
  )
}
