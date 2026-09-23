import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { LocaleProvider } from '../shared/i18n/LocaleProvider'
import { fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { App } from './App'
import { useItemDraft } from '../features/crafting/draft'

const client = new QueryClient({
  defaultOptions: { mutations: { retry: false } },
})
function show() {
  return render(
    <QueryClientProvider client={client}>
      <LocaleProvider initialLanguage="en">
        <App />
      </LocaleProvider>
    </QueryClientProvider>,
  )
}
afterEach(() => {
  client.clear()
  vi.unstubAllGlobals()
})

describe('Crafting workbench', () => {
  beforeEach(() => {
    useItemDraft.getState().setBase()
    window.history.replaceState({}, '', '/')
  })
  it('shows the reduced Currency tab', () => {
    show()
    expect(
      screen.getByRole('heading', { level: 1, name: 'Crafting workbench' }),
    ).toBeVisible()
    expect(screen.getByText('21 items')).toBeVisible()
    expect(
      screen.queryByRole('button', { name: 'Scroll of Wisdom' }),
    ).not.toBeInTheDocument()
    expect(screen.getAllByRole('tab')).toHaveLength(6)
  })
  it('preserves the item after a use request and clears selection with Escape', () => {
    show()
    const currency = screen.getByRole('button', {
      name: 'Orb of Transmutation',
    })
    fireEvent.contextMenu(currency, { clientX: 30, clientY: 40 })
    expect(currency).toHaveAttribute('aria-pressed', 'true')
    fireEvent.click(
      screen.getByRole('button', {
        name: 'Use selected currency on the central item',
      }),
    )
    expect(screen.getByRole('status')).toHaveTextContent(
      'The item has not changed',
    )
    expect(useItemDraft.getState().source).toBe('base')
    fireEvent.keyDown(window, { key: 'Escape' })
    expect(currency).toHaveAttribute('aria-pressed', 'false')
    fireEvent.click(
      screen.getByRole('button', {
        name: 'Use selected currency on the central item',
      }),
    )
    expect(screen.getByRole('status')).toHaveTextContent(
      'Select a currency from the stash first',
    )
  })
  it('renders currency images and toggles selection', () => {
    show()
    const currency = screen.getByRole('button', {
      name: "Hinekora's Lock",
    })
    fireEvent.click(currency)
    expect(currency).toHaveAttribute('aria-pressed', 'true')
    expect(currency.querySelector('img')).toHaveAttribute(
      'src',
      '/assets/currency/HinekorasLock.webp',
    )
    expect(
      screen
        .getByRole('button', { name: "Hinekora's Lock" })
        .querySelector('img'),
    ).toHaveAttribute('src', '/assets/currency/HinekorasLock.webp')
    fireEvent.keyDown(window, { key: 'Escape' })
    expect(currency).toHaveAttribute('aria-pressed', 'false')
  })
  it('shows fallback for failed slot and cursor images', () => {
    show()
    const currency = screen.getByRole('button', { name: "Hinekora's Lock" })
    fireEvent.contextMenu(currency, { clientX: 30, clientY: 40 })
    const slotImage = currency.querySelector('img')
    const cursorImage = document.querySelector('.currency-cursor img')
    expect(slotImage).not.toBeNull()
    expect(cursorImage).not.toBeNull()
    if (!slotImage || !cursorImage)
      throw new Error('Expected slot and cursor images')
    fireEvent.error(slotImage)
    fireEvent.error(cursorImage)
    expect(currency.querySelector('img')).toBeNull()
    expect(currency).toHaveTextContent('Image unavailable')
    expect(document.querySelector('.currency-cursor')).toHaveTextContent(
      'Image unavailable',
    )
  })
})
