import { act, fireEvent, render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'
import { LocaleSelector } from './LocaleSelector'
import { localeStorageKey, setLocale } from './i18n'

describe('language popover', () => {
  beforeEach(() => setLocale('en'))

  it('supports arrows, Home, End, Escape and focus return', () => {
    render(<LocaleSelector />)
    const trigger = screen.getByRole('button', { name: 'Language: English' })
    trigger.focus()
    fireEvent.keyDown(trigger, { key: 'ArrowDown' })
    const options = screen.getAllByRole('menuitemradio')
    expect(options[0]).toHaveFocus()
    expect(options[0]).toHaveAttribute('aria-checked', 'true')
    fireEvent.keyDown(options[0]!, { key: 'ArrowUp' })
    expect(options[5]).toHaveFocus()
    fireEvent.keyDown(options[5]!, { key: 'Home' })
    expect(options[0]).toHaveFocus()
    fireEvent.keyDown(options[0]!, { key: 'End' })
    expect(options[5]).toHaveFocus()
    fireEvent.keyDown(options[5]!, { key: 'Escape' })
    expect(screen.queryByRole('menu')).not.toBeInTheDocument()
    expect(trigger).toHaveFocus()
    expect(trigger).toHaveAttribute('aria-expanded', 'false')
  })

  it('persists a selection and dismisses for outside pointer or focus', () => {
    render(
      <>
        <LocaleSelector />
        <button>Outside</button>
      </>,
    )
    const trigger = screen.getByRole('button', { name: 'Language: English' })
    fireEvent.click(trigger)
    fireEvent.click(screen.getByRole('menuitemradio', { name: 'Español' }))
    expect(window.localStorage.getItem(localeStorageKey)).toBe('es')
    expect(trigger).toHaveFocus()
    expect(trigger).toHaveAccessibleName('Idioma: Español')
    fireEvent.click(trigger)
    expect(screen.getByRole('menuitemradio', { name: 'Español' })).toHaveFocus()
    const outside = screen.getByRole('button', { name: 'Outside' })
    fireEvent.pointerDown(outside)
    expect(screen.queryByRole('menu')).not.toBeInTheDocument()
    fireEvent.click(trigger)
    act(() => outside.focus())
    expect(screen.queryByRole('menu')).not.toBeInTheDocument()
    expect(outside).toHaveFocus()
  })
})
