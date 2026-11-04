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
it('places actual successful notation between the item card and feedback and retains its tooltip with a selected currency', async () => {
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
  expect(notation().previousElementSibling).toHaveClass('bench-item-card')
  expect(notation().nextElementSibling).toBe(panel.parentElement)
  expect(panel.parentElement?.nextElementSibling).toBeNull()
  expect(localStorage.getItem(historyStorageKey)).toBe(before)
  expect(within(notation()).getAllByRole('listitem')).toHaveLength(1)
  fireEvent.mouseOver(icon.querySelector('img') ?? icon)
  expect(await screen.findByRole('tooltip')).toHaveTextContent(
    'Orb of Transmutation',
  )
  expect(panel.parentElement).toContainElement(screen.getByRole('tooltip'))
  expect(panel).toHaveAttribute('role', 'alert')
  fireEvent.keyDown(document, { key: 'Escape' })
  expect(screen.queryByRole('tooltip')).toBeNull()
  expect(screen.queryByRole('checkbox', { name: 'Show tooltips' })).toBeNull()
  expect(panel).toHaveAttribute('role', 'alert')
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
  expect(within(notation()).getAllByRole('listitem')).toHaveLength(1)
  expect(notation().querySelector('li[aria-current]')).toBeNull()
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

it('jumps directly backward and forward without dropping future records, including keyboard-style activation', async () => {
  const { twoSteps } = await import('./session-notation/fixtures.test-support')
  const { LocalFilmRepository } = await import('./workbenchHistory')
  const { history } = await twoSteps()
  new LocalFilmRepository(localStorage).save(history)
  const mounted = show()
  const second = await within(notation()).findByRole('button', {
    name: 'Step 2',
  })
  const originalFilms = JSON.parse(
    localStorage.getItem(historyStorageKey)!,
  ).films
  fireEvent.click(within(notation()).getByRole('button', { name: 'Step 1' }))
  expect(JSON.parse(localStorage.getItem(historyStorageKey)!).cursor).toBe(1)
  expect(within(notation()).getAllByRole('listitem')).toHaveLength(2)
  expect(
    within(notation()).getByRole('listitem', { name: 'Step 1' }),
  ).toHaveAttribute('aria-current', 'step')
  second.focus()
  fireEvent.focusIn(second)
  expect(second).toHaveFocus()
  expect(await screen.findByRole('tooltip')).toHaveTextContent(
    'Orb of Augmentation',
  )
  fireEvent.click(second, { detail: 0 })
  expect(JSON.parse(localStorage.getItem(historyStorageKey)!).cursor).toBe(2)
  expect(
    within(notation()).getByRole('listitem', { name: 'Step 2' }),
  ).toHaveAttribute('aria-current', 'step')
  expect(JSON.parse(localStorage.getItem(historyStorageKey)!).films).toEqual(
    originalFilms,
  )
  fireEvent.click(
    screen.getByRole('button', { name: 'Previous crafting step' }),
  )
  expect(within(notation()).getAllByRole('listitem')).toHaveLength(2)
  expect(
    within(notation()).getByRole('listitem', { name: 'Step 1' }),
  ).toHaveAttribute('aria-current', 'step')
  mounted.unmount()
  client.clear()
  show()
  expect(
    await within(notation()).findByRole('listitem', { name: 'Step 1' }),
  ).toHaveAttribute('aria-current', 'step')
  expect(within(notation()).getAllByRole('listitem')).toHaveLength(2)
  fireEvent.click(screen.getByRole('button', { name: 'Next crafting step' }))
  expect(
    within(notation()).getByRole('listitem', { name: 'Step 2' }),
  ).toHaveAttribute('aria-current', 'step')
  expect(JSON.parse(localStorage.getItem(historyStorageKey)!).films).toEqual(
    originalFilms,
  )
})
