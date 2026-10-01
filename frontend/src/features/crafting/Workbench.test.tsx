import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { CraftingPage } from './CraftingPage'
import { useItemDraft } from './draft'
import {
  fixtureFetch,
  initialFixture,
  jsonResponse,
} from '../../shared/test/craftingFixtures'
import { applyCurrency, concreteInitial, rolledText } from './workbenchApi'

let client: QueryClient
beforeEach(() => {
  client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  useItemDraft.getState().setBase()
})
afterEach(() => client.clear())
function show() {
  return render(
    <QueryClientProvider client={client}>
      <CraftingPage />
    </QueryClientProvider>,
  )
}
async function selectAndApply() {
  await waitFor(() =>
    expect(client.getQueryData(['crafting', 'initial', 82])).toBeDefined(),
  )
  fireEvent.click(screen.getByRole('button', { name: 'Orb of Transmutation' }))
  fireEvent.click(
    screen.getByRole('button', {
      name: 'Use selected currency on the central item',
    }),
  )
}
describe('Workbench actual application', () => {
  it('preserves the current item and tab on a network failure, then retries the same selection', async () => {
    let fail = true
    vi.stubGlobal('fetch', (url: RequestInfo | URL, init?: RequestInit) =>
      String(url).endsWith('/workbench/apply') && fail
        ? Promise.resolve(jsonResponse({}, 500))
        : fixtureFetch(url, init),
    )
    show()
    await selectAndApply()
    await waitFor(() =>
      expect(screen.getByRole('status')).toHaveTextContent(
        'Your item is unchanged',
      ),
    )
    expect(screen.getByRole('article')).toHaveClass('item-card--normal')
    expect(
      screen.getByRole('tab', { name: 'Crafting Workbench' }),
    ).toHaveAttribute('aria-selected', 'true')
    fail = false
    fireEvent.click(
      screen.getByRole('button', {
        name: 'Use selected currency on the central item',
      }),
    )
    await waitFor(() =>
      expect(screen.getByRole('article')).toHaveClass('item-card--magic'),
    )
  })

  it('blocks duplicate applies and ignores a late result after replacing the base', async () => {
    let signal: AbortSignal | undefined
    let finish: (r: Response) => void = () => {
      throw Error('not started')
    }
    let calls = 0
    vi.stubGlobal('fetch', (url: RequestInfo | URL, init?: RequestInit) => {
      if (String(url).endsWith('/workbench/apply')) {
        calls++
        signal = init?.signal as AbortSignal
        return new Promise<Response>((resolve) => {
          finish = resolve
        })
      }
      return fixtureFetch(url, init)
    })
    show()
    await selectAndApply()
    const button = screen.getByRole('button', {
      name: 'Use selected currency on the central item',
    })
    expect(button).toBeDisabled()
    fireEvent.click(button)
    expect(calls).toBe(1)
    act(() => useItemDraft.getState().setBase(70))
    await waitFor(() => expect(signal?.aborted).toBe(true))
    const result = await fixtureFetch('/api/v1/crafting/workbench/apply', {
      body: JSON.stringify({
        state: concreteInitial(initialFixture),
        action: 'TRANSMUTATION',
      }),
    })
    await act(async () => finish(result))
    expect(screen.getByRole('article')).toHaveClass('item-card--normal')
    expect(screen.getByRole('article')).toHaveTextContent('70')
  })

  it('rejects an out-of-range numeric roll instead of updating the item', async () => {
    const result = await (
      await fixtureFetch('/api/v1/crafting/workbench/apply', {
        body: JSON.stringify({
          state: concreteInitial(initialFixture),
          action: 'TRANSMUTATION',
        }),
      })
    ).json()
    result.state.explicits[0].values.life = 9000
    vi.stubGlobal('fetch', () => Promise.resolve(jsonResponse(result)))
    await expect(
      applyCurrency(
        concreteInitial(initialFixture),
        'TRANSMUTATION',
        initialFixture.modifiers,
        new AbortController().signal,
      ),
    ).rejects.toThrow('verify')
  })

  it('renders source units honestly when the source range differs from the display range', () => {
    expect(
      rolledText(
        {
          ...initialFixture.modifiers.p!,
          name: 'Regeneration',
          text: 'Regenerate (1–2) Life per second',
          stats: [{ id: 'life_regeneration_per_minute', min: 60, max: 120 }],
        },
        { life_regeneration_per_minute: 90 },
      ),
    ).toContain('90 (source units)')
    expect(rolledText(initialFixture.modifiers.p!, { life: 17 })).toBe(
      '+17 to maximum Life',
    )
  })
})
