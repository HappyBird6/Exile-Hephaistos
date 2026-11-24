import { emptyGoal, addStat } from './goal-filter/editor'
import { saveSupport } from './method-search/supportStorage'
import { mockGoalFilterAdapter, fixtureContext } from './goal-filter/mock'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { expect, it, vi } from 'vitest'
import { CraftingPage } from './CraftingPage'
import { App } from '../../app/App'
import { useItemDraft } from './draft'
import { fixtureFetch, jsonResponse } from '../../shared/test/craftingFixtures'

async function show(fromTestTab = false) {
  useItemDraft.getState().setBase(82, 'solar')
  const goalFixture = await mockGoalFilterAdapter.catalog(
    fixtureContext,
    new AbortController().signal,
  )
  const goal = emptyGoal({ baseItemId: 'solar' }, 'test-catalog')
  addStat(goal.groups[0]!, goalFixture.stats[0]!)
  saveSupport(fromTestTab ? 'support-test-goal' : 'goal', goal)

  vi.stubGlobal(
    'fetch',
    vi.fn(async (url: RequestInfo | URL, init?: RequestInit) => {
      if (String(url).endsWith('/workbench/registry'))
        return jsonResponse({ workbenchBases: { solar: {} }, entries: [] })
      if (String(url).endsWith('/goal-filters/test-routes'))
        return jsonResponse({ cards: [], routes: [], complete: true })
      const response = await fixtureFetch(url, init)
      if (!String(url).includes('/goal-filters/catalog?')) return response
      return jsonResponse({
        ...(await response.json()),
        stats: goalFixture.stats,
        groupTypes: [
          {
            type: 'AND',
            evaluation: 'SUPPORTED',
            probability: 'SUPPORTED',
            reasonCode: null,
          },
        ],
      })
    }),
  )
  render(
    <QueryClientProvider
      client={
        new QueryClient({ defaultOptions: { queries: { retry: false } } })
      }
    >
      {fromTestTab ? <App /> : <CraftingPage />}
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
it('uses keyboard navigation across workbench, support and history', async () => {
  await show()
  const bench = screen.getByRole('tab', { name: 'Crafting Workbench' })
  bench.focus()
  fireEvent.keyDown(bench, { key: 'ArrowRight' })
  expect(screen.getByRole('tab', { name: 'Craft Support' })).toHaveFocus()
  fireEvent.keyDown(screen.getByRole('tab', { name: 'Craft Support' }), {
    key: 'ArrowRight',
  })
  expect(screen.getByRole('tab', { name: 'History' })).toHaveFocus()
  fireEvent.keyDown(screen.getByRole('tab', { name: 'History' }), {
    key: 'Home',
  })
  expect(bench).toHaveFocus()
})

it('opens the existing test tab and submits its selected base and goal to the route list', async () => {
  await show(true)
  fireEvent.click(
    await screen.findByRole('tab', { name: 'Craft Support Test' }),
  )
  expect(location.pathname).toBe('/support-test')
  await waitFor(() =>
    expect(
      screen.getByRole('button', { name: 'Start Crafting' }),
    ).toBeEnabled(),
  )
  expect(
    screen.getByRole('tab', { name: 'Craft Support Test' }),
  ).toHaveAttribute('aria-selected', 'true')
  expect(screen.getByLabelText('Starting item base')).toHaveValue('solar')
  expect(screen.getByRole('button', { name: 'Import item text' })).toBeVisible()
  expect(screen.queryByRole('button', { name: 'Send' })).toBeNull()
  expect(screen.queryByRole('tab', { name: /Request JSON/ })).toBeNull()
  fireEvent.change(screen.getByLabelText('Starting rarity'), {
    target: { value: 'RARE' },
  })
  fireEvent.click(screen.getByRole('button', { name: 'Start Crafting' }))
  expect(
    await screen.findByText('No matching crafting route found.'),
  ).toBeVisible()
  const [, init] = vi
    .mocked(fetch)
    .mock.calls.find(([url]) =>
      String(url).endsWith('/goal-filters/test-routes'),
    )!
  expect(init?.method).toBe('POST')
  const sent = JSON.parse(String(init?.body))
  expect(sent.item).toMatchObject({ baseItemId: 'solar', rarity: 'RARE' })
  expect(sent.goal.general.baseItemId).toBe('solar')
  expect(sent.goal.groups[0].entries[0]).toHaveProperty('statId')
  expect(
    vi
      .mocked(fetch)
      .mock.calls.some(([url]) =>
        /support-test\/(cards|send)|method-searches\/automatic/.test(
          String(url),
        ),
      ),
  ).toBe(false)
  fireEvent.click(screen.getByRole('tab', { name: 'Craft Support' }))
  await waitFor(() =>
    expect(screen.getByLabelText('Starting rarity')).toHaveValue('NORMAL'),
  )
})
