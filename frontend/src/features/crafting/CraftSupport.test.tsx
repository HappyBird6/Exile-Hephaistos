import { emptyGoal, addStat } from './goal-filter/editor'
import { saveSupport } from './method-search/supportStorage'
import { mockGoalFilterAdapter, fixtureContext } from './goal-filter/mock'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { expect, it, vi } from 'vitest'
import { CraftingPage } from './CraftingPage'
import { useItemDraft } from './draft'
import { fixtureFetch, jsonResponse } from '../../shared/test/craftingFixtures'

async function show() {
  useItemDraft.getState().setBase(82, 'solar')
  const goalFixture = await mockGoalFilterAdapter.catalog(
    fixtureContext,
    new AbortController().signal,
  )
  const goal = emptyGoal({ baseItemId: 'solar' }, 'test-catalog')
  addStat(goal.groups[0]!, goalFixture.stats[0]!)
  saveSupport('goal', goal)

  vi.stubGlobal(
    'fetch',
    vi.fn((url: RequestInfo | URL, init?: RequestInit) =>
      String(url).endsWith('/workbench/registry')
        ? Promise.resolve(
            jsonResponse({ workbenchBases: { solar: {} }, entries: [] }),
          )
        : String(url).includes('/goal-filters/catalog?')
          ? fixtureFetch(url, init).then(async (r) =>
              jsonResponse({
                ...(await r.json()),
                stats: goalFixture.stats,
                groupTypes: [
                  {
                    type: 'AND',
                    evaluation: 'SUPPORTED',
                    probability: 'SUPPORTED',
                    reasonCode: null,
                  },
                ],
              }),
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
  await show()
  fireEvent.click(screen.getByRole('tab', { name: 'Craft Support' }))
  await waitFor(
    () =>
      expect(
        screen.getByRole('button', { name: 'Start Crafting' }),
      ).toBeEnabled(),
    { timeout: 10000 },
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
  expect(
    screen.queryByRole('heading', { name: 'Crafting paths' }),
  ).not.toBeInTheDocument()
  expect(screen.getByRole('region', { name: 'Crafting paths' })).toBeVisible()
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
it('uses keyboard navigation across the workbench, support, test workbench and history', async () => {
  await show()
  const bench = screen.getByRole('tab', { name: 'Crafting Workbench' })
  bench.focus()
  fireEvent.keyDown(bench, { key: 'ArrowRight' })
  expect(screen.getByRole('tab', { name: 'Craft Support' })).toHaveFocus()
  fireEvent.keyDown(screen.getByRole('tab', { name: 'Craft Support' }), {
    key: 'ArrowRight',
  })
  const preview = screen.getByRole('tab', { name: 'Test Workbench' })
  expect(preview).toHaveFocus()
  expect(window.location.search).toBe('?workbench=preview')
  fireEvent.keyDown(preview, { key: 'ArrowRight' })
  expect(screen.getByRole('tab', { name: 'History' })).toHaveFocus()
  fireEvent.keyDown(screen.getByRole('tab', { name: 'History' }), {
    key: 'Home',
  })
  expect(bench).toHaveFocus()
})
