import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { CraftingPage } from './CraftingPage'
import { useItemDraft } from './draft'
import {
  initialFixture,
  fixtureFetch,
  jsonResponse,
} from '../../shared/test/craftingFixtures'
import { concreteInitial } from './workbenchApi'
import { applyBone, boneNames } from './desecrationCurrency'
import {
  LocalFilmRepository,
  emptyFilms,
  startFilm,
  currentFrame,
  currentFilm,
  recordAbyss,
  storedAbyssItem,
  filmRulesetStatus,
  viewFrame,
  historyStorageKey,
} from './workbenchHistory'
import type { AbyssState } from './AbyssPanel'

const root = { ...concreteInitial(initialFixture), rarity: 'RARE' as const }
const abyss: AbyssState = {
  item: { ...root, explicits: [] },
  slots: [
    { id: 'bone-slot', side: 'PREFIX', modifier: null, desecrated: true },
  ],
  offer: null,
}
afterEach(() => {
  vi.unstubAllGlobals()
  localStorage.clear()
})

it('persists unknown slots, ancient limits and observed offers without charging a second currency; branches retain provenance', () => {
  const state: AbyssState = {
    ...abyss,
    slots: [{ ...abyss.slots[0]!, minimumModifierLevel: 40 }],
  }
  const before = startFilm(
    emptyFilms(),
    root,
    'root',
    initialFixture.rulesetIdentity,
    'original clipboard',
  )
  const applied = recordAbyss(
    before,
    root,
    state,
    'Ancient_Collarbone',
    'unused',
    initialFixture.rulesetIdentity,
  )
  const modifier = { modifierId: 'p', values: { maximum_life: 10 } }
  const observed = {
    ...state,
    offer: { slotId: 'bone-slot', candidates: [modifier, modifier, modifier] },
  }
  const next = recordAbyss(
    applied,
    storedAbyssItem(state),
    observed,
    'ABYSS_OBSERVE',
    'unused',
    initialFixture.rulesetIdentity,
  )
  const repo = new LocalFilmRepository(localStorage)
  repo.save(next)
  expect(repo.load()).toEqual(next)
  expect(currentFrame(repo.load())?.abyss?.offer?.candidates).toHaveLength(3)
  expect(currentFrame(next)?.state.desecrated?.[0]?.minimumModifierLevel).toBe(
    40,
  )
  expect(
    filmRulesetStatus(currentFilm(next)!, initialFixture.rulesetIdentity),
  ).toBe('current')
  const fork = recordAbyss(
    viewFrame(next, 1),
    storedAbyssItem(state),
    observed,
    'ABYSS_OBSERVE',
    'fork',
    initialFixture.rulesetIdentity,
  )
  expect(fork.films).toHaveLength(2)
  expect(fork.films[0]).toEqual(next.films[0])
  expect(currentFilm(fork)?.sourceDocument).toBe('original clipboard')
  expect(currentFilm(fork)?.frames[0]?.abyss).toEqual(state)
  const fourth = recordAbyss(
    next,
    storedAbyssItem(observed),
    state,
    'ABYSS_CHOOSE',
    'unused',
    initialFixture.rulesetIdentity,
  )
  const offerBranch = recordAbyss(
    viewFrame(fourth, 2),
    storedAbyssItem(observed),
    state,
    'ABYSS_CHOOSE',
    'offer-branch',
    initialFixture.rulesetIdentity,
  )
  expect(currentFilm(offerBranch)?.frames[0]?.abyss?.offer).toEqual(
    observed.offer,
  )
  state.slots[0]!.side = 'SUFFIX'
  expect(currentFrame(applied)?.state.desecrated?.[0]?.side).toBe('PREFIX')
})

it('rejects failed or lossy responses, preserving input and passing cancellation to fetch', async () => {
  const before = structuredClone(root)
  const controller = new AbortController()
  const fetch = vi.fn<
    (input: RequestInfo | URL, init?: RequestInit) => Promise<Response>
  >(() => Promise.resolve(jsonResponse({ ...abyss, slots: [] })))
  vi.stubGlobal('fetch', fetch)
  await expect(
    applyBone(
      root,
      'Preserved_Collarbone',
      'PREFIX',
      '',
      [],
      initialFixture,
      controller.signal,
      'unchanged',
    ),
  ).rejects.toThrow('unchanged')
  expect(root).toEqual(before)
  expect(fetch.mock.calls[0]?.[1]).toMatchObject({ signal: controller.signal })
  vi.stubGlobal('fetch', () =>
    Promise.resolve(
      new Response('{}', {
        status: 422,
        headers: { 'X-Crafting-Ruleset': initialFixture.rulesetIdentity },
      }),
    ),
  )
  await expect(
    applyBone(
      root,
      'Preserved_Collarbone',
      'PREFIX',
      '',
      [],
      initialFixture,
      controller.signal,
      'unchanged',
    ),
  ).rejects.toThrow('unchanged')
})

it('offers all nine bones in the first tab and applies into the main film; restore retains the unknown slot', async () => {
  useItemDraft.getState().setBase()
  new LocalFilmRepository(localStorage).save(
    startFilm(emptyFilms(), root, 'seed', initialFixture.rulesetIdentity),
  )
  const fetch = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input)
    if (url.endsWith('/abyss/desecrate') || url.endsWith('/abyss/validate'))
      return Promise.resolve(jsonResponse(abyss))
    if (url.endsWith('/abyss/plan'))
      return Promise.resolve(jsonResponse({ available: false, branches: [] }))
    return fixtureFetch(input, init)
  })
  vi.stubGlobal('fetch', fetch)
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  const show = () =>
    render(
      <QueryClientProvider client={client}>
        <CraftingPage />
      </QueryClientProvider>,
    )
  const mounted = show()
  await waitFor(() =>
    expect(screen.getByRole('article')).toHaveClass('item-card--rare'),
  )
  for (const name of Object.values(boneNames))
    expect(screen.getByRole('button', { name })).toBeVisible()
  fireEvent.click(screen.getByRole('button', { name: 'Preserved Collarbone' }))
  fireEvent.change(screen.getByLabelText('Observed affix side'), {
    target: { value: 'PREFIX' },
  })
  fireEvent.click(
    screen.getByRole('button', {
      name: 'Use selected currency on the central item',
    }),
  )
  await waitFor(() =>
    expect(
      currentFrame(new LocalFilmRepository(localStorage).load())?.action,
    ).toBe('Preserved_Collarbone'),
  )
  expect(
    currentFrame(new LocalFilmRepository(localStorage).load())?.state
      .desecrated,
  ).toEqual([{ id: 'bone-slot', side: 'PREFIX', modifierId: null }])
  const saved = localStorage.getItem(historyStorageKey)
  mounted.unmount()
  client.clear()
  const restored = show()
  await waitFor(() =>
    expect(screen.getByRole('article')).toHaveClass('item-card--rare'),
  )
  expect(localStorage.getItem(historyStorageKey)).toBe(saved)
  restored.unmount()
  client.clear()
})

it('removes the selected modifier when a restored full item has noncanonical observation order', async () => {
  const ids = ['z', 'a', 'b', 'c', 'd', 'e']
  const initial = structuredClone(initialFixture)
  const explicits = ids.map((id, index) => {
    const template = initialFixture.modifiers[index < 3 ? 'p' : 's']!
    initial.modifiers[id] = { ...template, id, familyIds: [id] }
    return {
      modifierId: id,
      values: Object.fromEntries(
        template.stats!.map((stat) => [stat.id, stat.min]),
      ),
    }
  })
  const source = { ...root, explicits }
  const result: AbyssState = {
    item: { ...root, explicits: [] },
    offer: null,
    slots: [
      ...explicits.slice(1).map((modifier, index) => ({
        id: `explicit-${index}`,
        side: initial.modifiers[modifier.modifierId]!.affixType as
          'PREFIX' | 'SUFFIX',
        modifier,
        desecrated: false,
      })),
      { id: 'new', side: 'PREFIX', modifier: null, desecrated: true },
    ],
  }
  const fetch = vi.fn<
    (input: RequestInfo | URL, init?: RequestInit) => Promise<Response>
  >(() => Promise.resolve(jsonResponse(result)))
  vi.stubGlobal('fetch', fetch)
  const next = await applyBone(
    source,
    'Preserved_Collarbone',
    'PREFIX',
    'explicit-0',
    [],
    initial,
    new AbortController().signal,
    'unchanged',
  )
  expect(
    JSON.parse(fetch.mock.calls[0]![1]!.body as string).removedSlotId,
  ).toBe('explicit-5')
  expect(
    next.slots.flatMap((slot) =>
      slot.modifier ? [slot.modifier.modifierId] : [],
    ),
  ).toEqual(['a', 'b', 'c', 'd', 'e'])
  expect(source.explicits.map((modifier) => modifier.modifierId)).toEqual(ids)
})
