import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
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
  client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  client.setQueryData(
    ['crafting', 'initial', 'solar', 82, 'manual'],
    initialFixture,
  )
  vi.stubGlobal('fetch', fixtureFetch)
})
afterEach(() => {
  client.clear()
  vi.unstubAllGlobals()
  localStorage.clear()
})
function show() {
  render(
    <QueryClientProvider client={client}>
      <CraftingPage />
    </QueryClientProvider>,
  )
}
const panel = () =>
  document.querySelector<HTMLElement>('.workbench-feedback-panel')
function apply(name: string) {
  fireEvent.click(currencyButton(name))
  fireEvent.click(
    screen.getByRole('button', {
      name: 'Use selected currency on the central item',
    }),
  )
}
it('keeps currency explanations out of selection and successful crafting while saving the result', async () => {
  show()
  expect(panel()).toBeNull()
  fireEvent.click(currencyButton('Orb of Transmutation'))
  expect(panel()).toBeNull()
  fireEvent.click(
    screen.getByRole('button', {
      name: 'Use selected currency on the central item',
    }),
  )
  await waitFor(() =>
    expect(localStorage.getItem(historyStorageKey)).toContain('MAGIC'),
  )
  expect(panel()).toBeNull()
})
it('shows only a failed-use reason, preserves history and clears it on a new selection', async () => {
  show()
  const before = localStorage.getItem(historyStorageKey)
  apply('Orb of Annulment')
  await waitFor(() => expect(panel()).toHaveAttribute('role', 'alert'))
  expect(panel()?.textContent?.trim()).not.toBe('')
  expect(document.querySelector('.bench-lower')).toContainElement(panel())
  expect(
    document.querySelector('.stash-viewport .workbench-feedback-panel'),
  ).toBeNull()
  expect(localStorage.getItem(historyStorageKey)).toBe(before)
  fireEvent.click(currencyButton('Orb of Transmutation'))
  expect(panel()).toBeNull()
})
