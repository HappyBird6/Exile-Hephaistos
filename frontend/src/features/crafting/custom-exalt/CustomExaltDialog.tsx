import { useEffect, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import type { Initial } from '../craftingApi'
import { useI18n } from '../../../shared/i18n/i18n'
import { localizedModifierText } from '../localizedModifiers'
import { modifierPoolMessages } from '../modifier-pool/modifierPoolMessages'
import { customExaltChoices } from './api'
import type { CustomExaltContext } from './api'
import { customExaltMessages, chanceText, currencyRank } from './messages'
import '../custom-exalt/custom-exalt.css'
export function CustomExaltDialog({
  context,
  initial,
  busy,
  error,
  onApply,
  onClose,
}: {
  context: CustomExaltContext
  initial: Initial
  busy: boolean
  error: string
  onApply: (modifierId: string) => void
  onClose: () => void
}) {
  const { locale, t } = useI18n(),
    text = customExaltMessages[locale]
  const dialog = useRef<HTMLDialogElement>(null),
    [selected, setSelected] = useState('')
  const choices = useQuery({
    queryKey: ['crafting', 'custom-exalt', initial.rulesetIdentity, context],
    queryFn: ({ signal }) => customExaltChoices(context, initial, signal),
    retry: false,
  })
  useEffect(() => {
    const d = dialog.current!
    d.showModal()
    return () => d.close()
  }, [])
  const choice = choices.data?.find((c) => c.modifierId === selected)
  return (
    <dialog
      ref={dialog}
      className="custom-exalt-dialog"
      aria-label={text.title}
      onCancel={(e) => {
        e.preventDefault()
        if (!busy) onClose()
      }}
    >
      <form
        onSubmit={(e) => {
          e.preventDefault()
          if (choice && !busy) onApply(choice.modifierId)
        }}
      >
        <h2>
          <span className="wb-session-notation__rank">
            {currencyRank(context.action)}
          </span>
          <span className="custom-currency-art">
            <img src="/assets/currency/CurrencyAddModToRare.webp" alt="" />
            <b aria-hidden="true">C</b>
          </span>
          {text.title}
        </h2>
        <label>
          {text.choose}
          <select
            autoFocus
            aria-label={text.choose}
            disabled={busy || !choices.data?.length}
            value={selected}
            onChange={(e) => setSelected(e.target.value)}
          >
            <option value="">—</option>
            {(['PREFIX', 'SUFFIX'] as const).map((side) => (
              <optgroup
                key={side}
                label={
                  modifierPoolMessages[locale][
                    side === 'PREFIX' ? 'prefix' : 'suffix'
                  ]
                }
              >
                {choices.data
                  ?.filter(
                    (c) => initial.modifiers[c.modifierId]?.affixType === side,
                  )
                  .map((c) => {
                    const d = initial.modifiers[c.modifierId]!
                    return (
                      <option key={c.modifierId} value={c.modifierId}>
                        {localizedModifierText(d, undefined, locale)}
                        {d.tier > 0 ? ` · T${d.tier}` : ''} ·{' '}
                        {chanceText(c.probability, locale)}
                      </option>
                    )
                  })}
              </optgroup>
            ))}
          </select>
        </label>
        {choice && (
          <p className="custom-exalt-dialog__chance">
            {text.chance}{' '}
            <strong>{chanceText(choice.probability, locale)}</strong>
          </p>
        )}
        {choices.isPending && (
          <p role="status">{t('ui.loading_modifier_data')}</p>
        )}
        {choices.data?.length === 0 && <p>{text.empty}</p>}
        {(error || choices.isError) && (
          <p role="alert">{error || text.error}</p>
        )}
        <div className="custom-exalt-dialog__actions">
          <button type="button" onClick={onClose} disabled={busy}>
            {text.cancel}
          </button>
          <button type="submit" disabled={!choice || busy}>
            {text.apply}
          </button>
        </div>
      </form>
    </dialog>
  )
}
