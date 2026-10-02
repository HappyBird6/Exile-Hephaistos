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
import { historyStorageKey } from './workbenchHistory'
import type { Films } from './workbenchHistory'

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
  it('labels user conjectures separately from verified probability and restores saved films', async () => {
    vi.stubGlobal(
      'fetch',
      async (url: RequestInfo | URL, init?: RequestInit) => {
        const response = await fixtureFetch(url, init)
        if (!String(url).endsWith('/workbench/apply')) return response
        const result = await response.json()
        result.assumptions.push({
          id: 'user-coupled-ratio-half-up-v1',
          candidateUnit: 'assumed ratio ticks, not rounded outcomes',
          n: 10001,
          candidates: [],
          min: 0,
          max: 10000,
          sourceUrl: 'https://poe2db.tw/us/Gloves_str',
          reason: 'Unverified user conjecture; HALF_UP; ISSUES.md WB-001.',
        })
        return jsonResponse(result)
      },
    )
    const mounted = show()
    await selectAndApply()
    await waitFor(() =>
      expect(screen.getByRole('article')).toHaveClass('item-card--magic'),
    )
    fireEvent.click(screen.getByText('Last craft and roll assumptions'))
    expect(screen.getByText(/Unverified coupled roll model/)).toHaveTextContent(
      'not rounded outcomes',
    )
    expect(screen.getByText(/Unverified coupled roll model/)).toHaveTextContent(
      'WB-001',
    )
    const stored = window.localStorage.getItem(historyStorageKey)
    expect(stored).not.toBeNull()
    mounted.unmount()
    show()
    await waitFor(() =>
      expect(screen.getByRole('article')).toHaveClass('item-card--magic'),
    )
    expect(window.localStorage.getItem(historyStorageKey)).toBe(stored)
  })
  it('preserves the original future when crafting from a prior step and restores films after remount', async () => {
    vi.stubGlobal('fetch', fixtureFetch)
    const mounted = show()
    await selectAndApply()
    await waitFor(() =>
      expect(screen.getByRole('article')).toHaveClass('item-card--magic'),
    )
    const original = JSON.parse(
      window.localStorage.getItem(historyStorageKey)!,
    ) as Films
    expect(original.films).toHaveLength(1)
    fireEvent.click(
      screen.getByRole('button', { name: 'Previous crafting step' }),
    )
    expect(screen.getByRole('article')).toHaveClass('item-card--normal')
    expect(
      (JSON.parse(window.localStorage.getItem(historyStorageKey)!) as Films)
        .films,
    ).toHaveLength(1)
    await selectAndApply()
    await waitFor(() =>
      expect(screen.getByRole('article')).toHaveClass('item-card--magic'),
    )
    const fork = JSON.parse(
      window.localStorage.getItem(historyStorageKey)!,
    ) as Films
    expect(fork.films).toHaveLength(2)
    expect(fork.films[0]).toEqual(original.films[0])
    mounted.unmount()
    client.clear()
    act(() => useItemDraft.getState().setBase())
    show()
    await waitFor(() =>
      expect(screen.getByRole('article')).toHaveClass('item-card--magic'),
    )
    fireEvent.change(
      screen.getByRole('combobox', { name: 'Crafting session' }),
      { target: { value: fork.films[0]!.id } },
    )
    fireEvent.click(
      screen.getByRole('button', { name: 'Previous crafting step' }),
    )
    expect(screen.getByRole('article')).toHaveClass('item-card--normal')
    fireEvent.click(screen.getByRole('button', { name: 'Next crafting step' }))
    expect(screen.getByRole('article')).toHaveClass('item-card--magic')
  })

  it('keeps a successful craft usable when localStorage refuses writes', async () => {
    vi.stubGlobal('fetch', fixtureFetch)
    const storage = vi
      .spyOn(Storage.prototype, 'setItem')
      .mockImplementation(() => {
        throw new DOMException('Quota exceeded', 'QuotaExceededError')
      })
    try {
      show()
      await selectAndApply()
      await waitFor(() =>
        expect(screen.getByRole('article')).toHaveClass('item-card--magic'),
      )
      expect(screen.getByRole('alert')).toHaveTextContent(
        'History could not be saved',
      )
      fireEvent.click(
        screen.getByRole('button', { name: 'Previous crafting step' }),
      )
      expect(screen.getByRole('article')).toHaveClass('item-card--normal')
    } finally {
      storage.mockRestore()
    }
  })
  it('keeps the currency for a Shift craft and clears it on an empty stash click', async () => {
    vi.stubGlobal('fetch', fixtureFetch)
    show()
    await waitFor(() =>
      expect(client.getQueryData(['crafting', 'initial', 82])).toBeDefined(),
    )
    const currency = screen.getByRole('button', {
      name: 'Orb of Transmutation',
    })
    fireEvent.contextMenu(currency)
    fireEvent.click(
      screen.getByRole('button', {
        name: 'Use selected currency on the central item',
      }),
      { shiftKey: true },
    )
    await waitFor(() =>
      expect(screen.getByRole('article')).toHaveClass('item-card--magic'),
    )
    expect(currency).toHaveAttribute('aria-pressed', 'true')
    fireEvent.click(screen.getByLabelText('Currency stash'))
    expect(currency).toHaveAttribute('aria-pressed', 'false')
    act(() => useItemDraft.getState().setBase())
    fireEvent.click(currency)
    fireEvent.click(
      screen.getByRole('button', {
        name: 'Use selected currency on the central item',
      }),
    )
    await waitFor(() =>
      expect(currency).toHaveAttribute('aria-pressed', 'false'),
    )
  })

  it('shows all verified modifier ranges while Alt is held and restores rolls on keyup or blur', async () => {
    vi.stubGlobal('fetch', fixtureFetch)
    show()
    await selectAndApply()
    await waitFor(() =>
      expect(screen.getByRole('article')).toHaveClass('item-card--magic'),
    )
    const card = screen.getByRole('article')
    expect(card).toHaveTextContent('+17 to maximum Life')
    fireEvent.keyDown(window, { key: 'Alt' })
    expect(card).toHaveTextContent(initialFixture.modifiers.p!.text)
    expect(card).not.toHaveTextContent('Source range:')
    fireEvent.keyUp(window, { key: 'Alt' })
    expect(card).toHaveTextContent('+17 to maximum Life')
    fireEvent.keyDown(window, { key: 'Alt' })
    fireEvent.blur(window)
    expect(card).toHaveTextContent('+17 to maximum Life')
  })
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
