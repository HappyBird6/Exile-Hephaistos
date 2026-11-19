import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, expect, it, vi } from 'vitest'
import {
  fixtureFetch,
  jsonResponse,
} from '../../../shared/test/craftingFixtures'
import type { Context, GoalFilter, Stat } from '../goal-filter/types'
import { CraftStart } from '../goal-filter/CraftStart'
import { GoalPreviewWorkbench } from './GoalPreviewWorkbench'

beforeEach(() => localStorage.clear())

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

function mockCatalog(eligible: boolean) {
  const stat: Stat = {
    statId: 'hephaistos:v1:explicit:special',
    kind: 'EXPLICIT',
    label: 'Special effect',
    unit: 'flat',
    eligible,
    eligibilityReason: eligible ? null : 'NO_ELIGIBLE_GENERATION_MODIFIER',
    support: {
      evaluation: 'SUPPORTED',
      probability: 'UNSUPPORTED',
      reasonCode: null,
    },
    sourceStatIds: ['special'],
    contributions: [],
    sourceUrls: [],
  }
  const endpoints = vi.fn(
    async (url: RequestInfo | URL, init?: RequestInit) => {
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
    },
  )
  vi.stubGlobal('fetch', endpoints)
  return endpoints
}

it('previews a reviewed special goal at the chosen level without writing the production goal', async () => {
  const endpoints = mockCatalog(false)
  const productionGoal = 'hephaistos.support.v1:goal'
  const productionStart = 'hephaistos.support.v1:start'
  const productionDisclosure = 'hephaistos.support.v1:settings-expanded'
  localStorage.setItem(productionGoal, 'untouched')
  localStorage.setItem(productionStart, 'untouched')
  localStorage.setItem(productionDisclosure, 'false')
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
  const disclosure = screen.getByRole('button', { name: /Crafting setup/ })
  expect(disclosure).toHaveAttribute('aria-expanded', 'true')
  expect(
    screen.getByRole('combobox', { name: 'Starting item base' }),
  ).toBeVisible()
  expect(screen.getByRole('button', { name: 'Import item text' })).toBeVisible()
  expect(screen.queryByRole('button', { name: 'Start Crafting' })).toBeNull()
  fireEvent.focus(await screen.findByRole('combobox', { name: 'Search stats' }))
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
  const rarity = screen.getByRole('combobox', { name: 'Starting rarity' })
  fireEvent.change(rarity, { target: { value: 'RARE' } })
  expect(rarity).toHaveValue('RARE')
  await waitFor(() =>
    expect(screen.getByTestId('validated-item')).toHaveTextContent(
      'solar:51:90',
    ),
  )
  fireEvent.change(level, { target: { value: '101' } })
  expect(level).toHaveValue(101)
  expect(level).toHaveAttribute('aria-invalid', 'true')
  expect(screen.queryByTestId('validated-item')).toBeNull()
  fireEvent.change(level, { target: { value: '51' } })
  rerender(renderPreview(false))
  expect(screen.queryByTestId('validated-item')).toBeNull()
  rerender(renderPreview(true))
  expect(await screen.findByTestId('validated-item')).toHaveTextContent(
    'solar:51:90',
  )
  window.dispatchEvent(new Event('pagehide'))
  expect(localStorage.getItem('hephaistos.support.v1:preview-goal')).toContain(
    '90',
  )
  expect(
    localStorage.getItem('hephaistos.support.v1:preview-start'),
  ).toBeTruthy()
  fireEvent.click(disclosure)
  fireEvent.click(disclosure)
  fireEvent.change(
    screen.getByRole('combobox', { name: 'Starting equipment type' }),
    {
      target: { value: 'Gloves' },
    },
  )
  expect(screen.queryByTestId('validated-item')).toBeNull()
  expect(await screen.findByRole('button', { name: 'Retry' })).toBeVisible()
  window.dispatchEvent(new Event('pagehide'))
  expect(localStorage.getItem(productionGoal)).toBe('untouched')
  expect(localStorage.getItem(productionStart)).toBe('untouched')
  expect(localStorage.getItem(productionDisclosure)).toBe('false')
  expect(localStorage.getItem(catalogContract)).toBe('untouched')
  expect(
    endpoints.mock.calls.some(([url]) =>
      String(url).includes('/method-searches'),
    ),
  ).toBe(false)
  localStorage.removeItem(productionDisclosure)
  localStorage.removeItem(catalogContract)
})

it('keeps both editors independent while switching between mounted tabs', async () => {
  mockCatalog(true)
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  const tabs = (previewActive: boolean) => (
    <QueryClientProvider client={client}>
      <div hidden={previewActive}>
        <CraftStart active={!previewActive} />
      </div>
      <div hidden={!previewActive}>
        <GoalPreviewWorkbench active={previewActive} />
      </div>
    </QueryClientProvider>
  )
  const { rerender } = render(tabs(false))
  const level = () => screen.getByRole('spinbutton', { name: 'Item level' })
  await waitFor(() => expect(level()).toBeEnabled())
  fireEvent.change(level(), { target: { value: '72' } })
  const pickStat = async () => {
    fireEvent.focus(
      await screen.findByRole('combobox', { name: 'Search stats' }),
    )
    fireEvent.click(
      await screen.findByRole('option', { name: /Special effect/ }),
    )
  }
  await pickStat()
  const minimum = () => screen.getByRole('textbox', { name: 'Min' })
  fireEvent.change(minimum(), { target: { value: '20' } })
  await waitFor(() =>
    expect(
      screen.getByRole('button', { name: 'Start Crafting' }),
    ).toBeEnabled(),
  )
  rerender(tabs(true))
  await waitFor(() => expect(level()).toHaveValue(82))
  expect(screen.queryByRole('textbox', { name: 'Min' })).toBeNull()
  await pickStat()
  fireEvent.change(minimum(), { target: { value: '40' } })
  expect(await screen.findByTestId('validated-item')).toHaveTextContent(
    'solar:82:40',
  )
  const ids = Array.from(
    document.querySelectorAll('[id]'),
    (element) => element.id,
  )
  expect(new Set(ids).size).toBe(ids.length)
  fireEvent.click(screen.getByRole('button', { name: 'Reset goal' }))
  expect(screen.queryByTestId('validated-item')).toBeNull()
  rerender(tabs(false))
  expect(level()).toHaveValue(72)
  expect(minimum()).toHaveValue('20')
})
