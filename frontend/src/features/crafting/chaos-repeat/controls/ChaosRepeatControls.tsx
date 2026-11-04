import { useId } from 'react'
import { useI18n } from '../../../../shared/i18n/i18n'
import { currencies } from '../../currencies'
import { currencyNames } from '../../currencyNames'
import { CurrencyImage } from '../../CurrencyImage'
import { chaosRepeatControlMessages } from './messages'
import type { ChaosRepeatControlsProps } from './types'
import './chaos-repeat-controls.css'

const chaosImage = currencies.find(
  (currency) => currency.id === 'Chaos_Orb',
)!.image

/** Mode editor only: the caller owns execution, validation and durable state. */
export function ChaosRepeatControls({
  enabled,
  pattern,
  maximumUses,
  running,
  disabled = false,
  currencyId = 'Chaos_Orb',
  errors,
  serverMaximumUses,
  onEnabledChange,
  onPatternChange,
  onMaximumUsesChange,
  onHintInteraction,
}: ChaosRepeatControlsProps) {
  const { locale, name } = useI18n()
  const messages = chaosRepeatControlMessages[locale]
  const id = useId()
  const locked = disabled || running
  const hintId = `${id}-hint`
  const patternErrorId = `${id}-pattern-error`
  const maximumErrorId = `${id}-maximum-error`
  const limitId = `${id}-limit`
  const hasLimit = serverMaximumUses !== null && serverMaximumUses !== undefined
  const explain = () => onHintInteraction?.(currencyId)

  return (
    <section
      className="chaos-repeat-controls"
      aria-label={messages.mode}
      aria-busy={running}
      data-chaos-repeat-currency={currencyId}
      data-feedback-tooltip={messages.hint}
      onPointerEnter={explain}
      onPointerDown={explain}
      onFocus={explain}
      onClick={explain}
    >
      <span id={hintId} className="chaos-repeat-controls__sr-only">
        {messages.hint}
      </span>
      <div className="chaos-repeat-controls__main">
        <button
          className="chaos-repeat-controls__toggle"
          type="button"
          aria-pressed={enabled}
          aria-label={`${enabled ? 'ON' : 'OFF'} · ${name(currencyId, currencyNames[currencyId])} · ${messages.continueUsing}`}
          aria-describedby={hintId}
          disabled={locked}
          onClick={() => {
            if (!locked) onEnabledChange(!enabled)
          }}
        >
          <span>{enabled ? 'ON' : 'OFF'}</span>
          <span className="chaos-repeat-controls__image" aria-hidden="true">
            <CurrencyImage
              id={currencyId}
              image={chaosImage}
              name={currencyNames[currencyId]}
            />
          </span>
          <span>{messages.continueUsing}</span>
        </button>
        <label
          className="chaos-repeat-controls__pattern"
          htmlFor={`${id}-pattern`}
        >
          {messages.pattern}
          <input
            id={`${id}-pattern`}
            type="text"
            value={pattern}
            disabled={locked}
            autoComplete="off"
            spellCheck={false}
            aria-invalid={Boolean(errors?.pattern)}
            aria-describedby={[hintId, errors?.pattern ? patternErrorId : '']
              .filter(Boolean)
              .join(' ')}
            onChange={(event) => {
              if (!locked) onPatternChange(event.target.value)
            }}
          />
        </label>
      </div>
      {errors?.pattern && (
        <p id={patternErrorId} role="alert">
          {errors.pattern}
        </p>
      )}
      <div className="chaos-repeat-controls__limit-row">
        <label htmlFor={`${id}-maximum`}>
          {messages.maximumUses}
          <input
            id={`${id}-maximum`}
            type="text"
            inputMode="numeric"
            value={maximumUses}
            disabled={locked}
            autoComplete="off"
            aria-invalid={Boolean(errors?.maximumUses)}
            aria-describedby={
              [
                hasLimit ? limitId : '',
                errors?.maximumUses ? maximumErrorId : '',
              ]
                .filter(Boolean)
                .join(' ') || undefined
            }
            onChange={(event) => {
              if (!locked) onMaximumUsesChange(event.target.value)
            }}
          />
        </label>
        {hasLimit && (
          <span id={limitId}>
            {messages.serverLimit.replace(
              '{maximum}',
              String(serverMaximumUses),
            )}
          </span>
        )}
      </div>
      {errors?.maximumUses && (
        <p id={maximumErrorId} role="alert">
          {errors.maximumUses}
        </p>
      )}
      {errors?.request && <p role="alert">{errors.request}</p>}
      <p
        className="chaos-repeat-controls__status"
        role="status"
        aria-live="polite"
        aria-atomic="true"
      >
        {running && (
          <>
            <span
              className="chaos-repeat-controls__spinner"
              aria-hidden="true"
            />
            {messages.running}
          </>
        )}
      </p>
    </section>
  )
}
