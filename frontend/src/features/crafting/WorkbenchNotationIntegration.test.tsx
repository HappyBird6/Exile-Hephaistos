import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from '@testing-library/react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { CraftingPage } from './CraftingPage'
import { useItemDraft } from './draft'
import {
  fixtureFetch,
  initialFixture,
} from '../../shared/test/craftingFixtures'
import { currencyButton } from '../../shared/test/workbenchQueries'
import { historyStorageKey } from './workbenchHistory'

let client: QueryClient
beforeEach(() => {
  localStorage.clear()
  useItemDraft.getState().setBase()
  vi.stubGlobal('fetch', fixtureFetch)
})
afterEach(() => {
  client?.clear()
  localStorage.clear()
  vi.unstubAllGlobals()
})
function show() {
  client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  client.setQueryData(
    ['crafting', 'initial', 'solar', 82, 'manual'],
    initialFixture,
  )
  return render(
    <QueryClientProvider client={client}>
      <CraftingPage />
    </QueryClientProvider>,
  )
}
const notation = () =>
  document.querySelector<HTMLElement>('.wb-session-notation')!
function useCurrency(name: string) {
  fireEvent.click(currencyButton(name))
  fireEvent.click(
    screen.getByRole('button', {
      name: 'Use selected currency on the central item',
    }),
  )
}
it('places actual successful notation after lower feedback and retains its tooltip with a selected currency', async () => {
  show()
  useCurrency('Orb of Transmutation')
  const icon = await within(notation()).findByRole('img', {
    name: 'Orb of Transmutation',
  })
  expect(within(notation()).getAllByRole('listitem')).toHaveLength(1)
  const before = localStorage.getItem(historyStorageKey)
  vi.stubGlobal('fetch', (input: RequestInfo | URL, init?: RequestInit) =>
    String(input).endsWith('/workbench/apply')
      ? Promise.reject(new Error('offline'))
      : fixtureFetch(input, init),
  )
  useCurrency('Exalted Orb')
  await waitFor(() =>
    expect(document.querySelector('.workbench-feedback-panel')).toHaveAttribute(
      'role',
      'alert',
    ),
  )
  const panel = document.querySelector('.workbench-feedback-panel')!
  expect(panel.parentElement).toHaveClass('workbench-feedback-region')
  expect(panel.parentElement?.nextElementSibling).toBe(notation())
  expect(localStorage.getItem(historyStorageKey)).toBe(before)
  expect(within(notation()).getAllByRole('listitem')).toHaveLength(1)
  fireEvent.mouseOver(icon.querySelector('img') ?? icon)
  expect(await screen.findByRole('tooltip')).toHaveTextContent(
    'Orb of Transmutation',
  )
  fireEvent.keyDown(document, { key: 'Escape' })
  expect(screen.queryByRole('tooltip')).toBeNull()
  const toggle = screen.getByRole('checkbox', { name: 'Show tooltips' })
  expect(document.querySelector('.workbench-stash')).not.toContainElement(
    toggle,
  )
  fireEvent.click(toggle)
  fireEvent.mouseOver(icon)
  expect(screen.queryByRole('tooltip')).toBeNull()
  fireEvent.click(toggle)
  fireEvent.focusIn(icon)
  expect(await screen.findByRole('tooltip')).toHaveTextContent(
    'Orb of Transmutation',
  )
})
it('uses the active cursor and branch on undo and restores notation without editing the original film', async () => {
  const mounted = show()
  useCurrency('Orb of Transmutation')
  await within(notation()).findByRole('img', { name: 'Orb of Transmutation' })
  const first = JSON.parse(localStorage.getItem(historyStorageKey)!)
  fireEvent.click(
    screen.getByRole('button', { name: 'Previous crafting step' }),
  )
  expect(within(notation()).queryByRole('listitem')).toBeNull()
  fireEvent.click(screen.getByRole('button', { name: 'Next crafting step' }))
  expect(within(notation()).getAllByRole('listitem')).toHaveLength(1)
  fireEvent.click(
    screen.getByRole('button', { name: 'Previous crafting step' }),
  )
  useCurrency('Orb of Transmutation')
  await waitFor(() =>
    expect(
      JSON.parse(localStorage.getItem(historyStorageKey)!).films,
    ).toHaveLength(2),
  )
  const branched = JSON.parse(localStorage.getItem(historyStorageKey)!)
  expect(branched.films[0]).toEqual(first.films[0])
  expect(within(notation()).getAllByRole('listitem')).toHaveLength(1)
  mounted.unmount()
  client.clear()
  show()
  await within(notation()).findByRole('img', { name: 'Orb of Transmutation' })
  expect(JSON.parse(localStorage.getItem(historyStorageKey)!)).toEqual(branched)
})
