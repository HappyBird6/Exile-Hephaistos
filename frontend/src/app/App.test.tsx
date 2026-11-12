import { currencyButton } from '../shared/test/workbenchQueries'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { App } from './App'
import { useItemDraft } from '../features/crafting/draft'
import { locales, setLocale } from '../shared/i18n/i18n'

const client = new QueryClient({
  defaultOptions: { mutations: { retry: false } },
})
async function show() {
  const result = render(
    <QueryClientProvider client={client}>
      <App />
    </QueryClientProvider>,
  )
  await screen.findByRole('tab', { name: 'Crafting Workbench' })
  return result
}
afterEach(() => {
  client.clear()
  vi.unstubAllGlobals()
})

describe('Admin page', async () => {
  it.each(['/admin', '/admin/crawling'])(
    'opens %s without credentials or a server request',
    async (path) => {
      const fetch = vi.fn()
      vi.stubGlobal('fetch', fetch)
      window.history.replaceState({}, '', path)
      render(
        <QueryClientProvider client={client}>
          <App />
        </QueryClientProvider>,
      )
      expect(
        await screen.findByRole('heading', { name: 'Dashboard' }),
      ).toBeVisible()
      expect(
        screen.getByRole('link', { name: 'EXILE HEPHAISTOS' }),
      ).toHaveAttribute('href', '/')
      expect(screen.getByLabelText('Password')).toHaveAttribute(
        'type',
        'password',
      )
      expect(window.location.pathname).toBe('/admin')
      expect(fetch).not.toHaveBeenCalled()
    },
  )
})

describe('Crafting workbench', async () => {
  beforeEach(() => {
    useItemDraft.getState().setBase()
    window.history.replaceState({}, '', '/')
  })
  it('falls back from a saved explorer workspace without mounting or requesting it', async () => {
    const fetch = vi.fn(globalThis.fetch)
    vi.stubGlobal('fetch', fetch)
    localStorage.setItem(
      'hephaistos.support.v1:workspace',
      JSON.stringify({
        version: 1,
        savedAt: Date.now(),
        data: 'explorer',
      }),
    )
    await show()
    expect(
      screen.getByRole('tab', { name: 'Crafting Workbench' }),
    ).toHaveAttribute('aria-selected', 'true')
    expect(screen.queryByRole('tab', { name: 'State explorer' })).toBeNull()
    expect(document.getElementById('panel-explorer')).toBeNull()
    expect(
      vi
        .mocked(fetch)
        .mock.calls.some(([url]) =>
          /\/(explore|transitions)$/.test(String(url)),
        ),
    ).toBe(false)
  })
  it('shows the reduced Currency tab', async () => {
    await show()
    expect(document.querySelector('.craft-header .workspace-nav')).toBe(
      document.querySelector('.workspace-nav'),
    )
    expect(document.querySelector('.craft-page > .workspace-nav')).toBeNull()
    expect(document.querySelector('.craft-title')).toBeNull()
    expect(document.querySelector('.workbench-tabs')).toBeNull()
    expect(
      screen.queryByRole('checkbox', { name: 'Show tooltips' }),
    ).not.toBeInTheDocument()
    fireEvent.pointerOver(currencyButton('Chaos Orb'))
    expect(
      document.querySelector('.workbench-feedback-region'),
    ).toContainElement(screen.getByRole('tooltip'))
    expect(screen.getByRole('tooltip')).toHaveTextContent('Chaos Orb')
    expect(
      screen.queryByRole('button', { name: 'Scroll of Wisdom' }),
    ).not.toBeInTheDocument()
    expect(screen.getAllByRole('tab')).toHaveLength(11)
    expect(
      screen.getByRole('tab', { name: 'Crafting Workbench' }),
    ).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByRole('tab', { name: 'History' })).toHaveAttribute(
      'aria-selected',
      'false',
    )
    expect(
      screen.getByRole('tab', { name: 'Crafting Workbench' }),
    ).toHaveAttribute('aria-selected', 'true')
    expect(
      screen.queryByRole('heading', { name: 'Current state' }),
    ).not.toBeInTheDocument()
  })
  it('navigates three top-level workspaces and closes the picker while preserving simulation input', async () => {
    await show()
    const nav = document.querySelector('.workspace-nav')!
    const tabs = [...nav.querySelectorAll<HTMLButtonElement>('[role="tab"]')]
    expect(tabs.map((tab) => tab.textContent)).toEqual([
      'Crafting Workbench',
      'Craft Support',
      'History',
      'Session board',
    ])
    const base = screen.getByRole('button', { name: 'Select base' })
    await act(async () => {
      fireEvent.click(base)
    })
    await act(async () => {
      fireEvent.click(tabs[2]!)
    })
    tabs[2]!.focus()
    expect(tabs[2]).toHaveFocus()
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(document.getElementById('workbench-panel-history')).toBeVisible()
    fireEvent.keyDown(tabs[2]!, { key: 'Home' })
    expect(tabs[0]).toHaveFocus()
    expect(base).toBeVisible()
    fireEvent.keyDown(tabs[0]!, { key: 'ArrowRight' })
    expect(tabs[1]).toHaveFocus()
    fireEvent.keyDown(tabs[1]!, { key: 'Home' })
    expect(tabs[0]).toHaveFocus()
    expect(useItemDraft.getState().baseItemLevel).toBe(82)
  })
  it('preserves the item after a use request and clears selection with Escape', async () => {
    await show()
    const currency = currencyButton('Orb of Transmutation')
    fireEvent.contextMenu(currency, { clientX: 30, clientY: 40 })
    expect(currency).toHaveAttribute('aria-pressed', 'true')
    await act(async () => {
      fireEvent.click(
        screen.getByRole('button', {
          name: 'Use selected currency on the central item',
        }),
      )
    })
    await waitFor(() =>
      expect(screen.getByRole('article')).toHaveClass('item-card--magic'),
    )

    expect(useItemDraft.getState().source).toBe('base')
    fireEvent.keyDown(window, { key: 'Escape' })
    expect(currency).toHaveAttribute('aria-pressed', 'false')
    await act(async () => {
      fireEvent.click(screen.getByRole('tab', { name: 'Crafting Workbench' }))
    })
    await act(async () => {
      fireEvent.click(
        screen.getByRole('button', {
          name: 'Use selected currency on the central item',
        }),
      )
    })
    expect(
      document.querySelector('.craft-page > .sr-only[role="status"]'),
    ).toHaveTextContent('Select a currency from the stash first')
  })
  it('renders currency images and toggles selection', async () => {
    await show()
    const currency = currencyButton("Hinekora's Lock")
    await act(async () => {
      fireEvent.click(currency)
    })
    expect(currency).toHaveAttribute('aria-pressed', 'true')
    expect(currency.querySelector('img')).toHaveAttribute(
      'src',
      '/assets/currency/HinekorasLock.webp',
    )
    expect(currency.querySelector('img')).toHaveAttribute(
      'src',
      '/assets/currency/HinekorasLock.webp',
    )
    fireEvent.keyDown(window, { key: 'Escape' })
    expect(currency).toHaveAttribute('aria-pressed', 'false')
  })
  it('shows fallback for failed slot and cursor images', async () => {
    await show()
    const currency = currencyButton("Hinekora's Lock")
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

it.each(locales)(
  'sets the document language on a direct overview load in %s',
  (locale) => {
    const previous = location.pathname + location.search
    setLocale(locale)
    document.documentElement.lang = 'ko'
    window.history.replaceState({}, '', '/history/missing?from=simulation')
    const view = render(
      <QueryClientProvider client={client}>
        <App />
      </QueryClientProvider>,
    )
    expect(document.documentElement.lang).toBe(locale)
    expect(
      screen.queryByRole('button', { name: /English|한국어|Español/ }),
    ).toBeNull()
    view.unmount()
    window.history.replaceState({}, '', previous)
  },
)
