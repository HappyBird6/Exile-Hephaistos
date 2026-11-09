import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from '@testing-library/react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { CraftingPage } from '../CraftingPage'
import { useItemDraft } from '../draft'
import {
  initialFixture,
  fixtureFetch,
} from '../../../shared/test/craftingFixtures'
import {
  currencyButton,
  outerMaterialTab,
} from '../../../shared/test/workbenchQueries'
import { historyStorageKey } from '../workbenchHistory'
import { gameName, setLocale } from '../../../shared/i18n/i18n'

let client: QueryClient
beforeEach(() => {
  useItemDraft.getState().setBase()
  client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  client.setQueryData(
    ['crafting', 'initial', 'solar', 82, 'manual'],
    initialFixture,
  )
})
afterEach(() => {
  client.clear()
  vi.useRealTimers()
})
function show() {
  return render(
    <QueryClientProvider client={client}>
      <CraftingPage />
    </QueryClientProvider>,
  )
}
const shared = () =>
  document.querySelector<HTMLElement>('.workbench-feedback-tooltip')
const favorite = () =>
  screen.getByRole('button', { name: 'Favorite slot 1: empty' })

it('retains the last valid description after hover/focus leave and replaces it with another valid target', () => {
  vi.useFakeTimers()
  show()
  const chaos = currencyButton('Chaos Orb')
  fireEvent.pointerOver(chaos)
  fireEvent.pointerOut(chaos, { relatedTarget: document.body })
  fireEvent.mouseOver(document.body)
  fireEvent.focusIn(screen.getByRole('button', { name: 'Edit item' }))
  act(() => vi.advanceTimersByTime(1000))
  expect(shared()).toHaveTextContent('Chaos Orb')
  const link = within(shared()!).getByRole('link', { name: 'PoE2DB' })
  act(() => link.focus())
  fireEvent.scroll(shared()!)
  expect(link).toHaveFocus()
  expect(link.parentElement).toHaveClass('material-description__header')
  fireEvent.pointerOver(currencyButton('Divine Orb'))
  expect(shared()).toHaveTextContent('Divine Orb')
  fireEvent.keyDown(document, { key: 'Escape' })
  expect(shared()).toBeNull()
})

it('clears captured UI text on locale change while keeping the picked material in the new language', () => {
  show()
  const help = document.createElement('button')
  help.dataset.feedbackTooltip = 'Session actions'
  document.querySelector('main')!.append(help)
  fireEvent.focusIn(help)
  expect(shared()).toHaveTextContent('Session actions')
  act(() => setLocale('ko'))
  expect(shared()).toBeNull()
  const chaos = document.querySelector<HTMLElement>(
    '.currency-catalog [data-material-tooltip="Chaos_Orb"]',
  )!
  fireEvent.contextMenu(chaos)
  expect(
    shared()?.querySelector('.material-description__name'),
  ).toHaveTextContent(gameName('Chaos_Orb', 'Chaos Orb', 'ko'))
  act(() => setLocale('ja'))
  expect(
    shared()?.querySelector('.material-description__name'),
  ).toHaveTextContent(gameName('Chaos_Orb', 'Chaos Orb', 'ja'))
  expect(chaos).toHaveAttribute('aria-pressed', 'true')
  expect(chaos).toHaveAttribute('aria-describedby', 'material-description')
})

it('pins the actual left-click held material across other hover and tabs, then releases it on favorite placement', () => {
  show()
  fireEvent.click(outerMaterialTab('Essence'))
  const essence = currencyButton('Greater Essence of the Body')
  fireEvent.click(essence)
  expect(essence).toHaveClass('is-held')
  expect(shared()).toHaveTextContent('Greater Essence of the Body')
  fireEvent.pointerOver(currencyButton('Chaos Orb'))
  expect(shared()).toHaveTextContent('Greater Essence of the Body')
  fireEvent.click(outerMaterialTab('Currency'))
  fireEvent.pointerDown(currencyButton('Divine Orb'), { pointerType: 'touch' })
  fireEvent.focusIn(screen.getByRole('tab', { name: 'History' }))
  expect(shared()).toHaveTextContent('Greater Essence of the Body')
  fireEvent.click(favorite())
  expect(
    screen.getByRole('button', {
      name: 'Favorite slot 1: Greater Essence of the Body',
    }),
  ).toBeVisible()
  expect(shared()).toBeNull()
  fireEvent.pointerOver(currencyButton('Divine Orb'))
  expect(shared()).toHaveTextContent('Divine Orb')
})

it('pins right-click use selection and lets Escape from its source link cancel the real Page state', () => {
  show()
  const chaos = currencyButton('Chaos Orb')
  fireEvent.contextMenu(chaos)
  expect(chaos).toHaveAttribute('aria-pressed', 'true')
  expect(shared()).toHaveTextContent('Chaos Orb')
  fireEvent.pointerOver(currencyButton('Divine Orb'))
  expect(shared()).toHaveTextContent('Chaos Orb')
  const link = within(shared()!).getByRole('link', { name: 'PoE2DB' })
  act(() => link.focus())
  fireEvent.keyDown(link, { key: 'Escape' })
  expect(chaos).toHaveAttribute('aria-pressed', 'false')
  expect(document.querySelector('.currency-cursor')).toBeNull()
  expect(shared()).toBeNull()
  fireEvent.pointerOver(currencyButton('Divine Orb'))
  expect(shared()).toHaveTextContent('Divine Orb')
})

it('keeps the actual favorite trigger for aria and focus when the same material has multiple slots', () => {
  show()
  fireEvent.click(outerMaterialTab('Essence'))
  const essence = currencyButton('Greater Essence of the Body')
  for (const index of [1, 2]) {
    fireEvent.click(essence)
    fireEvent.click(
      screen.getByRole('button', { name: `Favorite slot ${index}: empty` }),
    )
  }
  const first = screen.getByRole('button', {
    name: 'Favorite slot 1: Greater Essence of the Body',
  })
  const second = screen.getByRole('button', {
    name: 'Favorite slot 2: Greater Essence of the Body',
  })
  fireEvent.contextMenu(second)
  expect(second).toHaveAttribute('aria-describedby', 'material-description')
  expect(first).not.toHaveAttribute('aria-describedby')
  fireEvent.pointerOver(first)
  expect(second).toHaveAttribute('aria-describedby', 'material-description')
  expect(first).not.toHaveAttribute('aria-describedby')
  const link = within(shared()!).getByRole('link', { name: 'PoE2DB' })
  act(() => link.focus())
  fireEvent.keyDown(link, { key: 'Escape' })
  expect(second).toHaveFocus()
  expect(second).not.toHaveAttribute('aria-describedby')
  expect(second).toHaveAttribute('aria-pressed', 'false')
})

it('keeps failure alerts and active omens while the selected description overrides other hover', async () => {
  show()
  act(() => useItemDraft.getState().setActiveOmens(['Omen_of_Whittling']))
  fireEvent.contextMenu(currencyButton('Orb of Annulment'))
  const bytes = localStorage.getItem(historyStorageKey)
  fireEvent.click(
    screen.getByRole('button', {
      name: 'Use selected currency on the central item',
    }),
  )
  await waitFor(() =>
    expect(document.querySelector('.workbench-feedback-panel')).toHaveAttribute(
      'role',
      'alert',
    ),
  )
  const alert = document.querySelector<HTMLElement>(
    '.workbench-feedback-panel',
  )!
  fireEvent.pointerOver(currencyButton('Chaos Orb'))
  expect(shared()).toHaveTextContent('Orb of Annulment')
  expect(alert).toBeVisible()
  expect(localStorage.getItem(historyStorageKey)).toBe(bytes)
  expect(useItemDraft.getState().activeOmens).toEqual(['Omen_of_Whittling'])
})

it('keeps a pinned material while the independent base preview opens and leaves', () => {
  show()
  fireEvent.contextMenu(currencyButton('Chaos Orb'))
  fireEvent.click(screen.getByRole('button', { name: 'Select base' }))
  const menu = screen.getByRole('dialog')
  const base = within(menu).getByRole('button', { name: /Massive Mitts/ })
  fireEvent.mouseEnter(base)
  const floating = document.querySelector<HTMLElement>(
    '.wb-base-picker__preview',
  )!
  expect(floating).toBeVisible()
  expect(shared()).toHaveTextContent('Chaos Orb')
  expect(shared()).not.toContainElement(floating)
  fireEvent.mouseLeave(menu.querySelector('.wb-base-picker__options')!, {
    relatedTarget: document.body,
  })
  expect(document.querySelector('.wb-base-picker__preview')).toHaveTextContent(
    'Solar Amulet',
  )
  expect(shared()).toHaveTextContent('Chaos Orb')
})

it('clears stale descriptions on material/workspace context changes and successful history navigation without extra execution requests', async () => {
  const fetch = vi.fn(fixtureFetch)
  vi.stubGlobal('fetch', fetch)
  show()
  fireEvent.pointerOver(currencyButton('Chaos Orb'))
  fireEvent.click(outerMaterialTab('Essence'))
  expect(shared()).toBeNull()
  fireEvent.click(outerMaterialTab('Currency'))
  fireEvent.click(currencyButton('Orb of Transmutation'))
  fireEvent.click(
    screen.getByRole('button', {
      name: 'Use selected currency on the central item',
    }),
  )
  await waitFor(() =>
    expect(screen.getByRole('article')).toHaveClass('item-card--magic'),
  )
  fireEvent.pointerOver(currencyButton('Chaos Orb'))
  const requests = fetch.mock.calls.length
  fireEvent.click(
    screen.getByRole('button', { name: 'Previous crafting step' }),
  )
  expect(shared()).toBeNull()
  expect(
    fetch.mock.calls
      .slice(requests)
      .every(
        ([url]) =>
          String(url).endsWith('/modifier-pools') ||
          String(url).includes('/economy/prices'),
      ),
  ).toBe(true)
  fireEvent.pointerOver(currencyButton('Chaos Orb'))
  fireEvent.click(screen.getByRole('tab', { name: /Craft Support/ }))
  expect(shared()).toBeNull()
  fireEvent.click(screen.getByRole('tab', { name: /Crafting Workbench/ }))
  expect(shared()).toBeNull()
})
