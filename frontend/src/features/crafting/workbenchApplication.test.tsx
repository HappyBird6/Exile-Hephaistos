import { currencyButton } from '../../shared/test/workbenchQueries'
import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { CraftingPage } from './CraftingPage'
import { useItemDraft } from './draft'
import {
  initialFixture,
  jsonResponse,
  rootBucket,
} from '../../shared/test/craftingFixtures'
import { loadTransition } from './craftingApi'

let client: QueryClient
beforeEach(() => {
  client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  useItemDraft.getState().setBase()
})
afterEach(() => client.clear())
describe('Workbench application and shared transition validation', () => {
  it('applies to the chosen base in Workbench and preserves it after a rejected craft', async () => {
    render(
      <QueryClientProvider client={client}>
        <CraftingPage />
      </QueryClientProvider>,
    )
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Select base' })).toBeEnabled(),
    )
    await act(async () =>
      fireEvent.click(screen.getByRole('button', { name: 'Select base' })),
    )
    const level = await screen.findByRole('spinbutton', { name: 'Item level' })
    await act(async () => fireEvent.change(level, { target: { value: '70' } }))
    await waitFor(() =>
      expect(
        client.getQueryData(['crafting', 'initial', 'solar', 70, 'manual']),
      ).toBeDefined(),
    )
    const confirm = within(
      screen.getByRole('dialog', { name: 'Select base' }),
    ).getByRole('button', { name: 'Select base' })
    await waitFor(() => expect(confirm).toBeEnabled())
    await act(async () => fireEvent.click(confirm))
    await waitFor(() =>
      expect(
        within(
          document.querySelector('.bench-item-card') as HTMLElement,
        ).getByRole('article'),
      ).toHaveTextContent('70'),
    )
    fireEvent.click(currencyButton('Orb of Transmutation'))
    fireEvent.click(
      screen.getByRole('button', {
        name: 'Use selected currency on the central item',
      }),
    )
    await waitFor(() =>
      expect(
        within(
          document.querySelector('.bench-item-card') as HTMLElement,
        ).getByRole('article'),
      ).toHaveClass('item-card--magic'),
    )
    expect(
      within(
        document.querySelector('.bench-item-card') as HTMLElement,
      ).getByRole('article'),
    ).toHaveTextContent('+17 to maximum Life')
    expect(
      screen.getByRole('tab', { name: 'Crafting Workbench' }),
    ).toHaveAttribute('aria-selected', 'true')
    expect(
      screen.queryByText('Last craft and roll assumptions'),
    ).not.toBeInTheDocument()
    expect(localStorage.getItem('hephaistos.workbench.films.v1')).toContain(
      '"n":11',
    )
    fireEvent.click(currencyButton('Orb of Transmutation'))
    fireEvent.click(
      screen.getByRole('button', {
        name: 'Use selected currency on the central item',
      }),
    )
    await waitFor(() =>
      expect(
        document.querySelector('.craft-page > .sr-only[role="status"]'),
      ).toHaveTextContent('cannot be used on this rarity'),
    )
    expect(
      within(
        document.querySelector('.bench-item-card') as HTMLElement,
      ).getByRole('article'),
    ).toHaveTextContent('+17 to maximum Life')
    expect(
      within(
        document.querySelector('.bench-item-card') as HTMLElement,
      ).getByRole('article'),
    ).toHaveClass('item-card--magic')
  })

  it('rejects a response for another snapshot', async () => {
    vi.stubGlobal('fetch', () =>
      Promise.resolve(
        jsonResponse({
          fromId: 'root',
          action: 'TRANSMUTATION',
          available: true,
          reason: '',
          outcomes: [
            {
              id: 'bad',
              probability: 1,
              state: { ...rootBucket, snapshotId: 'other' },
            },
          ],
        }),
      ),
    )
    await expect(
      loadTransition(
        initialFixture.state,
        'TRANSMUTATION',
        new AbortController().signal,
        'fixture-ruleset',
      ),
    ).rejects.toThrow('verify')
  })
})
