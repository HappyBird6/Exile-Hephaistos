import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { expect, it, vi } from 'vitest'
import { CraftingPage } from './CraftingPage'
import { useItemDraft } from './draft'
import { fixtureFetch, jsonResponse } from '../../shared/test/craftingFixtures'

function show() {
  useItemDraft.getState().setBase(82, 'solar')
  vi.stubGlobal(
    'fetch',
    vi.fn((url: RequestInfo | URL, init?: RequestInit) =>
      String(url).endsWith('/workbench/registry')
        ? Promise.resolve(
            jsonResponse({ workbenchBases: { solar: {} }, entries: [] }),
          )
        : fixtureFetch(url, init),
    ),
  )
  render(
    <QueryClientProvider
      client={
        new QueryClient({ defaultOptions: { queries: { retry: false } } })
      }
    >
      <CraftingPage />
    </QueryClientProvider>,
  )
}
it('shows the starting workflow without developer policy, recovery or family comparison panels', async () => {
  show()
  fireEvent.click(screen.getByRole('tab', { name: 'Craft Support' }))
  await waitFor(() =>
    expect(
      screen.getByRole('button', { name: 'Start Crafting' }),
    ).toBeEnabled(),
  )
  expect(screen.getByRole('button', { name: 'Import item text' })).toBeVisible()
  expect(
    screen.queryByText('Advanced family / tier comparison'),
  ).not.toBeInTheDocument()
  expect(
    screen.queryByRole('button', { name: 'Compare currency sequences' }),
  ).not.toBeInTheDocument()
  expect(document.querySelector('.basic-paths')).toBeNull()
  fireEvent.click(screen.getByRole('button', { name: 'Start Crafting' }))
  expect(screen.getByRole('heading', { name: 'Crafting paths' })).toBeVisible()
  expect(
    vi
      .mocked(fetch)
      .mock.calls.some(
        ([url]) =>
          String(url).includes('basic-paths/first-hit') ||
          String(url).includes('support/recommend'),
      ),
  ).toBe(false)
})
it('uses keyboard navigation across all three services', () => {
  show()
  const bench = screen.getByRole('tab', { name: 'Crafting Workbench' })
  bench.focus()
  fireEvent.keyDown(bench, { key: 'ArrowRight' })
  expect(screen.getByRole('tab', { name: 'Craft Support' })).toHaveFocus()
  fireEvent.keyDown(screen.getByRole('tab', { name: 'Craft Support' }), {
    key: 'ArrowRight',
  })
  expect(screen.getByRole('tab', { name: 'State explorer' })).toHaveFocus()
})
