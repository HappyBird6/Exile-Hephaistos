import { useEffect, useId, useLayoutEffect, useRef, useState } from 'react'
import { locales, localeNames, useI18n } from './i18n'
import './i18n.css'

export function LocaleSelector() {
  const { locale, setLocale, t } = useI18n()
  const [open, setOpen] = useState(false)
  const [active, setActive] = useState(0)
  const root = useRef<HTMLDivElement>(null)
  const trigger = useRef<HTMLButtonElement>(null)
  const menuId = useId()
  const show = () => {
    setActive(locales.indexOf(locale))
    setOpen(true)
  }
  const dismiss = () => {
    setOpen(false)
    trigger.current?.focus()
  }
  useEffect(() => {
    document.documentElement.lang = locale
  }, [locale])
  useLayoutEffect(() => {
    if (!open) return
    const options = root.current?.querySelectorAll<HTMLButtonElement>(
      '[role="menuitemradio"]',
    )
    options?.[active]?.focus()
  }, [open, active])
  useEffect(() => {
    if (!open) return
    const outside = (event: Event) => {
      if (event.target instanceof Node && !root.current?.contains(event.target))
        setOpen(false)
    }
    document.addEventListener('pointerdown', outside)
    document.addEventListener('focusin', outside)
    return () => {
      document.removeEventListener('pointerdown', outside)
      document.removeEventListener('focusin', outside)
    }
  }, [open])
  return (
    <div className="locale-selector" ref={root}>
      <button
        ref={trigger}
        className="locale-trigger"
        type="button"
        aria-label={`${t('language')}: ${localeNames[locale]}`}
        aria-haspopup="menu"
        aria-expanded={open}
        aria-controls={open ? menuId : undefined}
        onClick={() => (open ? dismiss() : show())}
        onKeyDown={(event) => {
          if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
            event.preventDefault()
            show()
          }
        }}
      >
        <span className="locale-icon" aria-hidden="true">
          ◎
        </span>
        <span lang={locale}>{localeNames[locale]}</span>
        <span className="locale-chevron" aria-hidden="true">
          ⌄
        </span>
      </button>
      {open && (
        <div
          id={menuId}
          role="menu"
          aria-label={t('language')}
          className="locale-menu"
          onKeyDown={(event) => {
            if (event.key === 'Escape') {
              event.preventDefault()
              event.stopPropagation()
              dismiss()
            } else if (event.key === 'Tab') {
              dismiss()
            } else if (
              ['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key)
            ) {
              event.preventDefault()
              setActive(
                event.key === 'Home'
                  ? 0
                  : event.key === 'End'
                    ? locales.length - 1
                    : (active +
                        (event.key === 'ArrowDown' ? 1 : -1) +
                        locales.length) %
                      locales.length,
              )
            }
          }}
        >
          <span className="locale-menu-label">{t('language')}</span>
          {locales.map((value, index) => (
            <button
              key={value}
              type="button"
              role="menuitemradio"
              aria-checked={locale === value}
              tabIndex={active === index ? 0 : -1}
              lang={value}
              onFocus={() => setActive(index)}
              onClick={() => {
                setLocale(value)
                dismiss()
              }}
            >
              {localeNames[value]}
              <span aria-hidden="true">{locale === value ? '✓' : ''}</span>
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
