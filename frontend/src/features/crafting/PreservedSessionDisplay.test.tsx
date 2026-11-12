import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from '@testing-library/react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { CraftingPage } from './CraftingPage'
import { useItemDraft } from './draft'
import { setLocale } from '../../shared/i18n/i18n'
import {
  fixtureFetch,
  initialFixture,
  jsonResponse,
} from '../../shared/test/craftingFixtures'
import { concreteInitial } from './workbenchApi'
import type { AppliedItem } from './workbenchApi'
import {
  emptyFilms,
  startFilm,
  recordCraft,
  historyStorageKey,
} from './workbenchHistory'

const initial = {
  ...initialFixture,
  state: {
    ...initialFixture.state,
    baseItemId: 'Metadata/Items/Amulets/FourAmulet9',
  },
}
const root = concreteInitial(initial)
let client: QueryClient
beforeEach(() => {
  setLocale('en')
  localStorage.clear()
  useItemDraft.getState().setBase()
  client = new QueryClient({
    defaultOptions: {
      queries: { retry: false, staleTime: Infinity },
      mutations: { retry: false },
    },
  })
  client.setQueryData(['crafting', 'initial', 'solar', 82, 'manual'], initial)
})
afterEach(() => {
  client.clear()
  vi.unstubAllGlobals()
  localStorage.clear()
})
async function show(compatible = true) {
  const response = await fixtureFetch('/api/v1/crafting/workbench/apply', {
    body: JSON.stringify({
      state: root,
      action: 'TRANSMUTATION',
      activeOmens: [],
    }),
  })
  const receipt = (await response.json()) as AppliedItem
  receipt.rulesetIdentity = 'previous-rules'
  const saved = recordCraft(
    startFilm(emptyFilms(), root, 'original', 'previous-rules'),
    root,
    receipt,
    'unused',
  )
  localStorage.setItem(historyStorageKey, JSON.stringify(saved))
  const request = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
    if (String(input).endsWith('/session-compatibility')) {
      const { state } = JSON.parse(String(init?.body))
      return Promise.resolve(
        jsonResponse({
          compatible,
          sameRules: false,
          compatibilityId: compatible ? 'current-compatible' : null,
          state: compatible ? state : null,
          abyss: null,
          reasons: compatible ? [] : ['UNKNOWN_MODIFIER'],
        }),
      )
    }
    return fixtureFetch(input, init)
  })
  vi.stubGlobal('fetch', request)
  render(
    <QueryClientProvider client={client}>
      <CraftingPage />
    </QueryClientProvider>,
  )
  await within(
    document.querySelector<HTMLElement>('.bench-item-card')!,
  ).findByText('+17 to maximum Life')
  return { saved, request }
}
it('shows preserved options and currency history, puts continuation in the image slot and follows the selected step', async () => {
  const { saved, request } = await show()
  const slot = document.querySelector<HTMLElement>('.item-placement')!
  const card = document.querySelector<HTMLElement>('.bench-item-card')!
  const button = await within(slot).findByRole('button', {
    name: 'Continue in a new session',
  })
  expect(card).not.toContainElement(button)
  expect(slot.querySelector('.wb-item-slot-art')).toBeNull()
  expect(document.querySelector('.bench-item-card pre')).toBeNull()
  expect(screen.queryByText('Preserved craft record')).toBeNull()
  expect(
    within(
      document.querySelector<HTMLElement>('.wb-session-notation')!,
    ).getByRole('img', { name: 'Orb of Transmutation' }),
  ).toBeInTheDocument()
  fireEvent.click(
    screen.getByRole('button', { name: 'Previous crafting step' }),
  )
  await waitFor(() =>
    expect(within(card).queryByText('+17 to maximum Life')).toBeNull(),
  )
  expect(within(card).getByText('+15 to Spirit')).toBeInTheDocument()
  fireEvent.click(screen.getByRole('button', { name: 'Next crafting step' }))
  await within(card).findByText('+17 to maximum Life')
  expect(JSON.parse(localStorage.getItem(historyStorageKey)!).films).toEqual(
    saved.films,
  )
  expect(
    request.mock.calls.some(([url]) =>
      String(url).endsWith('/workbench/apply'),
    ),
  ).toBe(false)
})
it('keeps the original session intact when explicitly continuing the displayed checkpoint', async () => {
  const { saved } = await show()
  fireEvent.click(
    await screen.findByRole('button', { name: 'Continue in a new session' }),
  )
  await waitFor(() =>
    expect(
      JSON.parse(localStorage.getItem(historyStorageKey)!).films,
    ).toHaveLength(2),
  )
  const stored = JSON.parse(localStorage.getItem(historyStorageKey)!)
  expect(
    stored.films.find((film: { id: string }) => film.id === 'original'),
  ).toEqual(saved.films[0])
  const next = stored.films.find(
    (film: { id: string }) => film.id === stored.active,
  )
  expect(next.rulesetIdentity).toBe(initial.rulesetIdentity)
  expect(next.frames[0].state).toEqual(saved.films[0]!.frames[1]!.state)
  expect(next.continuationOf).toMatchObject({
    sessionId: 'original',
    frameIndex: 1,
  })
  await waitFor(() =>
    expect(document.querySelector('.item-slot--preserved')).toBeNull(),
  )
  expect(
    within(document.querySelector<HTMLElement>('.bench-item-card')!).getByText(
      '+17 to maximum Life',
    ),
  ).toBeInTheDocument()
})
it('still shows options when the server rejects continuation', async () => {
  const { saved } = await show(false)
  await within(
    document.querySelector<HTMLElement>('.item-placement')!,
  ).findByText('A saved modifier is not present in the current catalog.')
  expect(
    screen.queryByRole('button', { name: 'Continue in a new session' }),
  ).toBeNull()
  expect(
    within(document.querySelector<HTMLElement>('.bench-item-card')!).getByText(
      '+17 to maximum Life',
    ),
  ).toBeInTheDocument()
  expect(JSON.parse(localStorage.getItem(historyStorageKey)!).films).toEqual(
    saved.films,
  )
})
