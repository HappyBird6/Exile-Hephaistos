import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { expect, it, vi } from 'vitest'
import {
  fixtureFetch,
  jsonResponse,
} from '../../../shared/test/craftingFixtures'
import type { Context, GoalFilter, Stat } from '../goal-filter/types'
import { GoalPreviewWorkbench } from './GoalPreviewWorkbench'

vi.mock('./GoalPreviewResults', () => ({
  GoalPreviewResults: ({
    context,
    goal,
  }: {
    context: Context
    goal: GoalFilter
  }) => (
    <output data-testid="validated-item">
      {[
        context.baseItemId,
        context.itemLevel,
        goal.groups[0]?.entries[0]?.range.min,
      ].join(':')}
    </output>
  ),
}))

it('previews a reviewed special goal at the chosen level without writing the production goal', async () => {
  const stat: Stat = {
    statId: 'hephaistos:v1:explicit:special',
    kind: 'EXPLICIT',
    label: 'Special effect',
    unit: 'flat',
    eligible: false,
    eligibilityReason: 'NO_ELIGIBLE_GENERATION_MODIFIER',
    support: {
      evaluation: 'SUPPORTED',
      probability: 'UNSUPPORTED',
      reasonCode: null,
    },
    sourceStatIds: ['special'],
    contributions: [],
    sourceUrls: [],
  }
  vi.stubGlobal(
    'fetch',
    vi.fn(async (url: RequestInfo | URL, init?: RequestInit) => {
      if (String(url).endsWith('/workbench/registry'))
        return jsonResponse({
          workbenchBases: { solar: {}, stocky: {} },
          entries: [],
        })
      if (String(url).includes('/workbench/initial?base=stocky'))
        return jsonResponse({}, 503)
      const response = await fixtureFetch(url, init)
      if (String(url).includes('/goal-filters/catalog?'))
        return jsonResponse({ ...(await response.json()), stats: [stat] })
      return response
    }),
  )
  const productionGoal = 'hephaistos.support.v1:goal'
  localStorage.setItem(productionGoal, 'untouched')
  const catalogContract = 'hephaistos.goal-catalog-contract.v1'
  localStorage.setItem(catalogContract, 'untouched')
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  const renderPreview = (active: boolean) => (
    <QueryClientProvider client={client}>
      <GoalPreviewWorkbench active={active} />
    </QueryClientProvider>
  )
  const { rerender } = render(renderPreview(true))
  expect(await screen.findByText(/Choose a goal to see matching/)).toBeVisible()
  expect(screen.queryByTestId('validated-item')).toBeNull()
  fireEvent.focus(screen.getByRole('combobox', { name: 'Search stats' }))
  fireEvent.click(await screen.findByRole('option', { name: /Special effect/ }))
  fireEvent.change(screen.getByRole('textbox', { name: 'Min' }), {
    target: { value: '90' },
  })
  await waitFor(() =>
    expect(screen.getByTestId('validated-item')).toHaveTextContent(
      'solar:82:90',
    ),
  )
  const level = screen.getByRole('spinbutton', { name: 'Item level' })
  fireEvent.change(level, { target: { value: '51' } })
  await waitFor(() =>
    expect(screen.getByTestId('validated-item')).toHaveTextContent(
      'solar:51:90',
    ),
  )
  fireEvent.change(level, { target: { value: '101' } })
  expect(screen.queryByTestId('validated-item')).toBeNull()
  fireEvent.change(level, { target: { value: '51' } })
  rerender(renderPreview(false))
  expect(screen.queryByTestId('validated-item')).toBeNull()
  rerender(renderPreview(true))
  expect(await screen.findByTestId('validated-item')).toHaveTextContent(
    'solar:51:90',
  )
  fireEvent.change(screen.getByRole('combobox', { name: 'Base' }), {
    target: { value: 'stocky' },
  })
  expect(screen.queryByTestId('validated-item')).toBeNull()
  expect(await screen.findByRole('button', { name: 'Retry' })).toBeVisible()
  expect(localStorage.getItem(productionGoal)).toBe('untouched')
  expect(localStorage.getItem(catalogContract)).toBe('untouched')
})
