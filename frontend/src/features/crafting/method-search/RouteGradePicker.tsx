import { useEffect, useId, useRef, useState } from 'react'
import { useI18n } from '../../../shared/i18n/i18n'
import { currencyRank } from '../custom-exalt/messages'
import { localizedAction } from '../localizedCrafting'
import type { WorkbenchAction } from '../currencyDefinitions'
import { CurrencyRankIcon } from '../session-notation/CurrencyRankIcon'
import { sessionNotationMessages } from '../session-notation/messages'

export function RouteGradePicker({
  choices,
  current,
  step,
  onSelect,
}: {
  choices: string[]
  current: string
  step: number
  onSelect: (grade: number) => void
}) {
  const { locale } = useI18n()
  const [open, setOpen] = useState(false)
  const trigger = useRef<HTMLButtonElement>(null)
  const options = useRef<HTMLSpanElement>(null)
  const id = useId()
  const actionLabel = (action: string) =>
    localizedAction(action as WorkbenchAction)
  const label = `${sessionNotationMessages[locale].step} ${step} · ${actionLabel(current)}`
  useEffect(() => {
    if (open)
      options.current
        ?.querySelector<HTMLButtonElement>('[aria-pressed="true"]')
        ?.focus()
  }, [open])
  function close() {
    setOpen(false)
    trigger.current?.focus()
  }
  return (
    <span
      className="route-grade-picker"
      onBlur={(event) => {
        if (!event.currentTarget.contains(event.relatedTarget)) setOpen(false)
      }}
      onKeyDown={(event) => {
        if (event.key === 'Escape' && open) {
          event.preventDefault()
          close()
        }
        const direction = ['ArrowDown', 'ArrowRight'].includes(event.key)
          ? 1
          : ['ArrowUp', 'ArrowLeft'].includes(event.key)
            ? -1
            : 0
        if (direction) {
          event.preventDefault()
          if (!open) setOpen(true)
          else {
            const buttons = Array.from(
              options.current?.querySelectorAll('button:enabled') ?? [],
            ) as HTMLButtonElement[]
            const index = buttons.indexOf(
              document.activeElement as HTMLButtonElement,
            )
            buttons[
              (index + direction + buttons.length) % buttons.length
            ]?.focus()
          }
        }
      }}
    >
      <button
        type="button"
        ref={trigger}
        title={label}
        aria-label={label}
        aria-expanded={open}
        aria-controls={open ? id : undefined}
        onClick={() => setOpen(!open)}
      >
        <CurrencyRankIcon rank={currencyRank(current)!} />
        <span aria-hidden="true" className="route-grade-picker__arrow" />
      </button>
      {open && (
        <span
          ref={options}
          id={id}
          role="group"
          aria-label={label}
          className="route-grade-picker__options"
        >
          {choices.map((action, index) => {
            const name = actionLabel(action)
            return (
              <button
                type="button"
                key={action}
                title={name}
                aria-label={name}
                aria-pressed={action === current}
                onClick={() => {
                  onSelect(index + 1)
                  close()
                }}
              >
                <CurrencyRankIcon rank={currencyRank(action)!} />
              </button>
            )
          })}
        </span>
      )}
    </span>
  )
}
