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
import { CraftingExplorer } from './CraftingExplorer'
import { CraftingPage } from './CraftingPage'
import { useItemDraft } from './draft'
import {
  fixtureFetch,
  initialFixture,
  firstOutcomes,
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
function show(level = 82) {
  return render(
    <QueryClientProvider client={client}>
      <CraftingExplorer
        level={level}
        revision={1}
        requestCount={0}
        requestedAction={null}
      />
    </QueryClientProvider>,
  )
}

describe('Crafting probability explorer', () => {
  it('expands grouped tiers and follows the exact tier probability, then returns to a chosen step', async () => {
    const p = {
      ...initialFixture.modifiers.p!,
      layer: 'EXPLICIT',
      tags: ['life'],
      stats: [{ id: 'life', min: 10, max: 20 }],
    }
    const p2 = {
      ...p,
      id: 'p2',
      tier: 2,
      text: '+(1?9) to maximum Life',
      stats: [{ id: 'life', min: 1, max: 9 }],
    }
    vi.stubGlobal('fetch', (url: RequestInfo | URL, init?: RequestInit) => {
      if (String(url).includes('/initial'))
        return Promise.resolve(
          jsonResponse({
            ...initialFixture,
            modifiers: { ...initialFixture.modifiers, p, p2 },
          }),
        )
      if (String(url).endsWith('/transitions'))
        return Promise.resolve(
          jsonResponse({
            fromId: 'root',
            action: 'TRANSMUTATION',
            available: true,
            reason: '',
            outcomes: [
              firstOutcomes[0],
              {
                ...firstOutcomes[0],
                id: 'tier-2',
                probability: 0.4,
                state: { ...firstOutcomes[0]!.state, modifierIds: ['p2'] },
              },
            ],
          }),
        )
      return fixtureFetch(url, init)
    })
    show()
    fireEvent.click(
      await screen.findByRole('button', {
        name: 'Preview Orb of Transmutation',
      }),
    )
    const summary = await screen.findByText(/2 tier combinations/)
    expect(summary.closest('details')).not.toHaveAttribute('open')
    fireEvent.click(summary.closest('summary')!)
    expect(summary.closest('details')).toHaveAttribute('open')
    const buttons = screen.getAllByRole('button', {
      name: 'Explore this state',
    })
    expect(buttons).toHaveLength(2)
    fireEvent.click(buttons[1]!)
    expect(screen.getByRole('article')).toHaveTextContent(
      '+(1?9) to maximum Life',
    )
    expect(screen.getByText(/Selected path probability: 40%/)).toBeVisible()
    expect(
      screen.getByRole('list', { name: 'Selected crafting path' }),
    ).toHaveTextContent('Orb of Transmutation')
    fireEvent.click(screen.getByRole('button', { name: 'Return to step 1' }))
    expect(screen.getByRole('article')).toHaveClass('item-card--normal')
    expect(
      screen.queryByRole('button', { name: 'Return to step 1' }),
    ).not.toBeInTheDocument()
  })
  it('loads the initial state and source, previews outcomes, follows a branch and returns', async () => {
    show()
    expect(
      await screen.findByRole('button', {
        name: 'Preview Orb of Transmutation',
      }),
    ).toBeEnabled()
    expect(
      screen.getByRole('button', { name: 'Preview Exalted Orb' }),
    ).toBeDisabled()
    expect(screen.getByRole('article')).toHaveTextContent('82')
    expect(screen.getByRole('article')).toHaveTextContent('+15 to Spirit')
    expect(
      screen.getByRole('link', { name: 'PoE2DB modifier weights' }),
    ).toHaveAttribute('href', 'https://poe2db.tw/us/Amulets#ModifiersCalc')
    fireEvent.click(
      screen.getByRole('button', { name: 'Preview Orb of Transmutation' }),
    )
    const choices = await screen.findAllByRole('button', {
      name: 'Explore this state',
    })
    expect(screen.getByText('60%')).toBeVisible()
    fireEvent.click(choices[0]!)
    await waitFor(() =>
      expect(screen.getByRole('article')).toHaveClass('item-card--magic'),
    )
    expect(screen.getByRole('article')).toHaveTextContent(
      '+(10—20) to maximum Life',
    )
    expect(screen.getByText(/Selected path probability: 60%/)).toBeVisible()
    fireEvent.click(
      await screen.findByRole('button', {
        name: 'Preview Orb of Augmentation',
      }),
    )
    fireEvent.click(
      (
        await screen.findAllByRole('button', { name: 'Explore this state' })
      )[0]!,
    )
    expect(screen.getByRole('article')).toHaveTextContent('+(5—8) to Strength')
    fireEvent.click(screen.getByRole('button', { name: 'Previous state' }))
    expect(screen.getByRole('article')).not.toHaveTextContent(
      '+(5—8) to Strength',
    )
    fireEvent.click(screen.getByRole('button', { name: 'Previous state' }))
    expect(screen.getByRole('article')).toHaveClass('item-card--normal')
  })

  it('reports partial sequence mass without relabeling it as success', async () => {
    show()
    await screen.findByRole('button', { name: 'Preview Orb of Transmutation' })
    fireEvent.click(screen.getByText('Explore a currency sequence'))
    fireEvent.click(screen.getByRole('button', { name: 'Explore sequence' }))
    expect(await screen.findByText(/Exploration limited/)).toBeVisible()
    const summary = document.querySelector('.exploration-summary')!
    expect(within(summary as HTMLElement).getByText('Unexplored')).toBeVisible()
    expect(summary.textContent).toContain('Unexplored100%')
    expect(summary.textContent).toContain('Reached the end0%')
  })

  it('aborts a pending preview and ignores a response arriving after cancellation', async () => {
    let signal: AbortSignal | undefined
    let finish: (r: Response) => void = () => {
      throw new Error('Request not started')
    }
    vi.stubGlobal('fetch', (url: RequestInfo | URL, init?: RequestInit) => {
      if (String(url).endsWith('/transitions')) {
        signal = init?.signal as AbortSignal
        return new Promise<Response>((resolve) => {
          finish = resolve
        })
      }
      return fixtureFetch(url, init)
    })
    show()
    fireEvent.click(
      await screen.findByRole('button', {
        name: 'Preview Orb of Transmutation',
      }),
    )
    fireEvent.click(
      await screen.findByRole('button', { name: 'Cancel preview' }),
    )
    await waitFor(() => expect(signal?.aborted).toBe(true))
    await act(async () =>
      finish(
        jsonResponse({
          fromId: 'root',
          action: 'TRANSMUTATION',
          available: true,
          reason: '',
          outcomes: firstOutcomes,
        }),
      ),
    )
    expect(
      screen.queryByRole('button', { name: 'Explore this state' }),
    ).not.toBeInTheDocument()
    expect(screen.getByRole('article')).toHaveClass('item-card--normal')
  })

  it('keeps the item and shows a retryable error after an invalid result', async () => {
    vi.stubGlobal('fetch', (url: RequestInfo | URL, init?: RequestInit) =>
      String(url).endsWith('/transitions')
        ? Promise.resolve(
            jsonResponse({
              fromId: 'root',
              action: 'TRANSMUTATION',
              available: true,
              reason: '',
              outcomes: [{ ...firstOutcomes[0], probability: 0.2 }],
            }),
          )
        : fixtureFetch(url, init),
    )
    show()
    fireEvent.click(
      await screen.findByRole('button', {
        name: 'Preview Orb of Transmutation',
      }),
    )
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Could not verify',
    )
    expect(screen.getByRole('article')).toHaveClass('item-card--normal')
    expect(
      screen.queryByRole('button', { name: 'Explore this state' }),
    ).not.toBeInTheDocument()
  })

  it('applies to the chosen base in Workbench and preserves the independent explorer path', async () => {
    render(
      <QueryClientProvider client={client}>
        <CraftingPage />
      </QueryClientProvider>,
    )
    fireEvent.click(screen.getByRole('button', { name: 'Edit item' }))
    fireEvent.change(screen.getByRole('spinbutton', { name: 'Item level' }), {
      target: { value: '70' },
    })
    fireEvent.click(screen.getByRole('button', { name: /Place base/ }))
    await waitFor(() =>
      expect(screen.getByRole('article')).toHaveTextContent('70'),
    )
    fireEvent.click(screen.getByRole('tab', { name: 'State explorer' }))
    fireEvent.click(
      await screen.findByRole('button', {
        name: 'Preview Orb of Transmutation',
      }),
    )
    fireEvent.click(
      (
        await screen.findAllByRole('button', { name: 'Explore this state' })
      )[0]!,
    )
    expect(
      screen.getByRole('button', { name: 'Return to step 1' }),
    ).toBeVisible()
    fireEvent.click(screen.getByRole('tab', { name: 'Crafting Workbench' }))
    fireEvent.click(
      screen.getByRole('button', { name: 'Orb of Transmutation' }),
    )
    fireEvent.click(
      screen.getByRole('button', {
        name: 'Use selected currency on the central item',
      }),
    )
    await waitFor(() =>
      expect(screen.getByRole('article')).toHaveClass('item-card--magic'),
    )
    expect(screen.getByRole('article')).toHaveTextContent('+17 to maximum Life')
    expect(
      screen.getByRole('tab', { name: 'Crafting Workbench' }),
    ).toHaveAttribute('aria-selected', 'true')
    fireEvent.click(screen.getByText('Last craft and roll assumptions'))
    expect(screen.getByText(/Uniform assumption: life, N = 11/)).toBeVisible()
    fireEvent.click(
      screen.getByRole('button', { name: 'Orb of Transmutation' }),
    )
    fireEvent.click(
      screen.getByRole('button', {
        name: 'Use selected currency on the central item',
      }),
    )
    await waitFor(() =>
      expect(screen.getByRole('status')).toHaveTextContent(
        'cannot be used on this rarity',
      ),
    )
    expect(screen.getByRole('article')).toHaveTextContent('+17 to maximum Life')
    fireEvent.click(screen.getByRole('tab', { name: 'State explorer' }))
    expect(
      screen.getByRole('button', { name: 'Return to step 1' }),
    ).toBeVisible()
    fireEvent.click(screen.getByRole('tab', { name: 'Crafting Workbench' }))
    expect(screen.getByRole('article')).toHaveClass('item-card--magic')
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
      ),
    ).rejects.toThrow('verify')
  })

  it('shows a tiny positive probability without rounding it to zero', async () => {
    vi.stubGlobal('fetch', (url: RequestInfo | URL, init?: RequestInit) =>
      String(url).endsWith('/transitions')
        ? Promise.resolve(
            jsonResponse({
              fromId: 'root',
              action: 'TRANSMUTATION',
              available: true,
              reason: '',
              outcomes: [
                { ...firstOutcomes[0], probability: 1e-12 },
                { ...firstOutcomes[1], probability: 1 - 1e-12 },
              ],
            }),
          )
        : fixtureFetch(url, init),
    )
    show()
    fireEvent.click(
      await screen.findByRole('button', {
        name: 'Preview Orb of Transmutation',
      }),
    )
    expect(await screen.findByText('1.000e-10%')).toBeVisible()
    fireEvent.click(
      (
        await screen.findAllByRole('button', { name: 'Explore this state' })
      )[0]!,
    )
    expect(
      screen.getByText(/Selected path probability: 1.000e-10%/),
    ).toBeVisible()
  })
})
