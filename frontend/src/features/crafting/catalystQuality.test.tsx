import { afterEach, expect, it, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import {
  catalystProjection,
  catalystTypes,
  verifiedCatalystQuality,
} from './catalystQuality'
import {
  initialFixture,
  fixtureFetch,
} from '../../shared/test/craftingFixtures'
import { applyCurrency, concreteInitial } from './workbenchApi'
import {
  LocalFilmRepository,
  startFilm,
  emptyFilms,
  verifiedHistoryState,
  currentFrame,
  historyStorageKey,
} from './workbenchHistory'
import { CraftingPage } from './CraftingPage'
import { useItemDraft } from './draft'
import ringCapture from '../../shared/test/ring-essence-responses.json'
import type { Definition, Initial } from './craftingApi'

const reviewedInitial: Initial = {
  ...initialFixture,
  state: {
    ...initialFixture.state,
    baseItemId: 'Metadata/Items/Amulets/FourAmulet9',
  },
}
async function reviewedFetch(input: RequestInfo | URL, init?: RequestInit) {
  if (String(input).includes('/initial'))
    return new Response(JSON.stringify(reviewedInitial), { status: 200 })
  return fixtureFetch(input, init)
}

it('selects a typed Ring film from an active Solar film without losing either root', async () => {
  localStorage.clear()
  useItemDraft.getState().setBase()
  const ringInitial = {
    ...ringCapture.initial,
    qualityLimit: { ruleVersion: 'quality-limit-v1', maximumQuality: 20 },
  }
  const ring = {
    ...concreteInitial(ringInitial as unknown as Initial),
    catalystQuality: { type: 'REAVER' as const, amount: 20 },
  }
  const repo = new LocalFilmRepository(localStorage)
  repo.save(
    startFilm(startFilm(emptyFilms(), ring, 'typed-ring'), root, 'solar'),
  )
  vi.stubGlobal('fetch', (input: RequestInfo | URL, init?: RequestInit) =>
    String(input).includes('base=ring')
      ? Promise.resolve(
          new Response(JSON.stringify(ringInitial), { status: 200 }),
        )
      : reviewedFetch(input, init),
  )
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  const view = render(
    <QueryClientProvider client={client}>
      <CraftingPage />
    </QueryClientProvider>,
  )
  fireEvent.change(await screen.findByLabelText('Crafting session'), {
    target: { value: 'typed-ring' },
  })
  await screen.findByText('Quality (Reaver): +20%')
  expect(repo.load().active).toBe('typed-ring')
  expect(repo.load().films[0]!.frames[0]!.state).toEqual(ring)
  expect(repo.load().films[1]!.frames[0]!.state).toEqual(root)
  view.unmount()
  client.clear()
})

afterEach(() => vi.unstubAllGlobals())
const root = concreteInitial(reviewedInitial)
const d: Definition = {
  id: 'life',
  name: 'Healthy',
  text: '+(20–29) to maximum Life',
  tier: 8,
  affixType: 'PREFIX',
  familyIds: ['Life'],
  stats: [{ id: 'base_maximum_life', min: 20, max: 29 }],
  tags: ['life'],
  weight: 1000,
}
it('derives once with truncation and retains the source roll and bounds', () => {
  const values = { base_maximum_life: 29 }
  expect(catalystProjection(d, values, { type: 'FLESH', amount: 20 })).toEqual({
    values: { base_maximum_life: 34 },
    status: 'SCALED_INTEGER',
  })
  expect(values).toEqual({ base_maximum_life: 29 })
  expect(d.stats![0]!.max).toBe(29)
  expect(
    catalystProjection(d, values, { type: 'NEURAL', amount: 20 }).status,
  ).toBe('NO_MATCH')
  expect(Object.keys(catalystTypes)).toHaveLength(13)
  const defence = {
    ...d,
    tags: ['armour', 'defences', 'energyshield'],
    stats: [{ id: 'base_maximum_energy_shield', min: 20, max: 29 }],
  }
  expect(
    catalystProjection(
      defence,
      { base_maximum_energy_shield: 29 },
      { type: 'CARAPACE', amount: 20 },
    ).values,
  ).toEqual({ base_maximum_energy_shield: 34 })
})
it.each([
  { ...d, tags: ['life', 'unscalable'] },
  { ...d, tags: [] },
  {
    ...d,
    stats: [{ id: 'base_life_regeneration_rate_per_minute', min: 20, max: 29 }],
  },
  { ...d, stats: [{ id: 'base_maximum_life', min: -1, max: 29 }] },
  {
    ...d,
    stats: [
      { id: 'base_maximum_life', min: 20, max: 29 },
      { id: 'other', min: 0, max: 1 },
    ],
  },
])(
  'keeps unscalable, missing-tag and unreviewed numeric cases unchanged',
  (definition) => {
    const values = { base_maximum_life: 29 }
    expect(
      catalystProjection(definition, values, { type: 'FLESH', amount: 20 })
        .values,
    ).toBe(values)
  },
)
it('preserves typed and legacy frames without rewriting storage and blocks unverified currency use before fetch', async () => {
  const state = {
    ...root,
    catalystQuality: { type: 'FLESH' as const, amount: 20 },
  }
  const repo = new LocalFilmRepository(localStorage)
  const legacy = startFilm(emptyFilms(), root, 'legacy')
  repo.save(startFilm(legacy, state, 'typed'))
  const bytes = localStorage.getItem(historyStorageKey)
  const restored = repo.load()
  expect(currentFrame(restored)!.state).toEqual(state)
  expect(verifiedHistoryState(state, reviewedInitial)).toBe(true)
  expect(
    verifiedHistoryState(restored.films[0]!.frames[0]!.state, reviewedInitial),
  ).toBe(true)
  expect(localStorage.getItem(historyStorageKey)).toBe(bytes)
  const fetch = vi.fn()
  vi.stubGlobal('fetch', fetch)
  await expect(
    applyCurrency(
      state,
      'TRANSMUTATION',
      reviewedInitial.modifiers,
      new AbortController().signal,
    ),
  ).rejects.toThrow('interactions')
  expect(fetch).not.toHaveBeenCalled()
  expect(
    verifiedCatalystQuality(
      { ...state, catalystQuality: { type: 'FLESH', amount: 21 } },
      reviewedInitial.modifiers,
    ),
  ).toBe(false)
  expect(
    verifiedHistoryState(
      { ...state, catalystQuality: { type: 'FLESH', amount: 1.5 } },
      reviewedInitial,
    ),
  ).toBe(false)
})
it('places already present quality through the existing base form and restores it on reload', async () => {
  localStorage.clear()
  useItemDraft.getState().setBase()
  vi.stubGlobal('fetch', reviewedFetch)
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  const show = () =>
    render(
      <QueryClientProvider client={client}>
        <CraftingPage />
      </QueryClientProvider>,
    )
  const view = show()
  fireEvent.click(screen.getByRole('button', { name: 'Edit item' }))
  fireEvent.change(screen.getByLabelText('Existing catalyst quality'), {
    target: { value: 'FLESH' },
  })
  fireEvent.change(screen.getByLabelText('Current quality (%)'), {
    target: { value: '21' },
  })
  expect(screen.getByRole('button', { name: /Place base/ })).toBeDisabled()
  fireEvent.change(screen.getByLabelText('Current quality (%)'), {
    target: { value: '20' },
  })
  fireEvent.click(screen.getByRole('button', { name: /Place base/ }))
  await screen.findByText('Quality (Flesh): +20%')
  await waitFor(() =>
    expect(localStorage.getItem(historyStorageKey)).toContain(
      'catalystQuality',
    ),
  )
  const bytes = localStorage.getItem(historyStorageKey)
  view.unmount()
  const reload = show()
  await screen.findByText('Quality (Flesh): +20%')
  expect(localStorage.getItem(historyStorageKey)).toBe(bytes)
  reload.unmount()
  client.clear()
})
