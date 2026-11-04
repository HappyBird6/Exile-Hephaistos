import { useI18n } from '../../../../shared/i18n/i18n'
import { currencies } from '../../currencies'
import { currencyNames } from '../../currencyNames'
import { CurrencyImage } from '../../CurrencyImage'
import { chaosRepeatControlMessages } from './messages'
import { chaosRepeatMaximumUsesLimit } from './types'
import type { ChaosRepeatControlsProps } from './types'
import './chaos-repeat-controls.css'

export function ChaosRepeatControls({
  enabled,
  pattern,
  running,
  disabled = false,
  currencyId = 'Chaos_Orb',
  errors,
  onEnabledChange,
  onPatternChange,
  onHintInteraction,
}: ChaosRepeatControlsProps) {
  const { locale, name } = useI18n()
  const messages = chaosRepeatControlMessages[locale]
  const locked = disabled || running
  const image = currencies.find((currency) => currency.id === currencyId)!.image
  const explain = () => onHintInteraction?.(currencyId)
  return (
    <div
      className="chaos-repeat-controls"
      role="group"
      aria-label={messages.mode}
      aria-busy={running}
      data-chaos-repeat-currency={currencyId}
      data-feedback-tooltip={messages.hint}
      onPointerEnter={explain}
      onPointerDown={explain}
      onFocus={explain}
    >
      <button
        className="chaos-repeat-controls__use"
        type="button"
        aria-label={`${chaosRepeatMaximumUsesLimit} x ${name(currencyId, currencyNames[currencyId])} ${messages.use}`}
        aria-pressed={enabled}
        disabled={locked}
        onClick={() => {
          if (!locked) {
            explain()
            onEnabledChange(!enabled)
          }
        }}
      >
        <span>{chaosRepeatMaximumUsesLimit} x</span>
        <span className="chaos-repeat-controls__image" aria-hidden="true">
          <CurrencyImage
            id={currencyId}
            image={image}
            name={currencyNames[currencyId]}
          />
        </span>
        {running && (
          <span className="chaos-repeat-controls__spinner" aria-hidden="true" />
        )}
        <span aria-live="polite">
          {running ? messages.running : messages.use}
        </span>
      </button>
      <input
        className="chaos-repeat-controls__pattern"
        type="text"
        value={pattern}
        aria-label={messages.pattern}
        placeholder={messages.patternPlaceholder}
        disabled={locked}
        autoComplete="off"
        spellCheck={false}
        aria-invalid={Boolean(errors?.pattern || errors?.request)}
        onChange={(event) => {
          if (!locked) onPatternChange(event.target.value)
        }}
      />
    </div>
  )
}
