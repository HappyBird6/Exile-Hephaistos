import { outerMaterialTab } from '../../shared/test/workbenchQueries'
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
import { isExcludedWorkbenchBone } from './abyss-workbench/policy'
import {
  LocalFilmRepository,
  emptyFilms,
  startFilm,
  currentFrame,
  currentFilm,
  recordAbyss,
  storedAbyssItem,
  filmRulesetStatus,
  verifiedHistoryState,
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

it.each(['', 'Omen_of_Sinistral_Necromancy', 'Omen_of_Dextral_Necromancy'])(
  'applies first-tab bones with %s and restores the slot and consumption',
  async (omen) => {
    const side = omen.includes('Dextral') ? 'SUFFIX' : 'PREFIX'
    const outcome: AbyssState = {
      ...abyss,
      slots: [{ ...abyss.slots[0]!, side }],
    }
    useItemDraft.getState().setBase()
    new LocalFilmRepository(localStorage).save(
      startFilm(emptyFilms(), root, 'seed', initialFixture.rulesetIdentity),
    )
    let rejectApplication = !!omen
    const fetch = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input)
      if (url.endsWith('/abyss/desecrate') && rejectApplication)
        return Promise.resolve(
          new Response('{}', {
            status: 422,
            headers: { 'X-Crafting-Ruleset': initialFixture.rulesetIdentity },
          }),
        )
      if (url.endsWith('/abyss/desecrate') || url.endsWith('/abyss/validate'))
        return Promise.resolve(jsonResponse(outcome))
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
    for (const [, name] of Object.entries(boneNames).filter(
      ([id]) => !isExcludedWorkbenchBone(id),
    ))
      expect(screen.getByRole('button', { name })).toBeVisible()
    if (omen) {
      fireEvent.click(outerMaterialTab('Omen'))
      fireEvent.click(
        screen.getByRole('button', { name: omen.replaceAll('_', ' ') }),
      )
      fireEvent.click(
        screen.getByRole('button', { name: 'Favorite slot 1: empty' }),
      )
      fireEvent.contextMenu(
        screen.getByRole('button', {
          name: `Favorite slot 1: ${omen.replaceAll('_', ' ')}`,
        }),
      )
      expect(useItemDraft.getState().activeOmens).toEqual([omen])
      fireEvent.click(outerMaterialTab('Currency'))
    }
    fireEvent.click(
      screen.getByRole('button', { name: 'Preserved Collarbone' }),
    )
    if (omen)
      expect(screen.getByLabelText('Observed affix side')).toBeDisabled()
    else
      fireEvent.change(screen.getByLabelText('Observed affix side'), {
        target: { value: side },
      })
    fireEvent.click(
      screen.getByRole('button', {
        name: 'Use selected currency on the central item',
      }),
    )
    if (omen) {
      await screen.findAllByText(/Only one Necromancy omen can be active/)
      expect(useItemDraft.getState().activeOmens).toEqual([omen])
      expect(
        currentFilm(new LocalFilmRepository(localStorage).load())?.frames,
      ).toHaveLength(1)
      rejectApplication = false
      fireEvent.click(
        screen.getByRole('button', {
          name: 'Use selected currency on the central item',
        }),
      )
    }
    await waitFor(() =>
      expect(
        currentFrame(new LocalFilmRepository(localStorage).load())?.action,
      ).toBe('Preserved_Collarbone'),
    )
    expect(
      currentFrame(new LocalFilmRepository(localStorage).load())?.state
        .desecrated,
    ).toEqual([{ id: 'bone-slot', side, modifierId: null }])
    expect(useItemDraft.getState().activeOmens).toEqual([])
    expect(
      currentFrame(new LocalFilmRepository(localStorage).load())?.manual
        ?.consumedOmens ?? [],
    ).toEqual(omen ? [omen] : [])
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
  },
)

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

it('records Putrefaction corruption and every observed slot, rejects loss, and restores history', async () => {
  const result: AbyssState = {
    ...abyss,
    item: { ...abyss.item, conditions: ['CORRUPTED'] },
    slots: [
      { ...abyss.slots[0]!, revealPool: 'ALTERED' },
      {
        id: 'second',
        side: 'SUFFIX',
        modifier: null,
        desecrated: true,
        revealPool: 'ALTERED',
      },
    ],
  }
  const fetch = vi.fn(() => Promise.resolve(jsonResponse(result)))
  vi.stubGlobal('fetch', fetch)
  const next = await applyBone(
    root,
    'Altered_Collarbone',
    '',
    '',
    ['Omen_of_Putrefaction'],
    initialFixture,
    new AbortController().signal,
    'unchanged',
    ['PREFIX', 'SUFFIX'],
  )
  const history = recordAbyss(
    startFilm(emptyFilms(), root, 'seed', initialFixture.rulesetIdentity),
    root,
    next,
    'Altered_Collarbone',
    'unused',
    initialFixture.rulesetIdentity,
    ['Omen_of_Putrefaction'],
  )
  const repo = new LocalFilmRepository(localStorage)
  repo.save(history)
  expect(repo.load()).toEqual(history)
  expect(currentFrame(repo.load())?.state.conditions).toEqual(['CORRUPTED'])
  expect(currentFrame(repo.load())?.state.desecrated).toHaveLength(2)
  expect(
    currentFrame(repo.load())?.state.desecrated?.every(
      (s) => s.revealPool === 'ALTERED',
    ),
  ).toBe(true)
  expect(currentFrame(repo.load())?.manual?.consumedOmens).toEqual([
    'Omen_of_Putrefaction',
  ])
  vi.stubGlobal('fetch', () =>
    Promise.resolve(
      jsonResponse({ ...result, slots: result.slots.slice(0, 1) }),
    ),
  )
  await expect(
    applyBone(
      root,
      'Altered_Collarbone',
      '',
      '',
      ['Omen_of_Putrefaction'],
      initialFixture,
      new AbortController().signal,
      'unchanged',
      ['PREFIX', 'SUFFIX'],
    ),
  ).rejects.toThrow('unchanged')
  expect(root.conditions).toEqual([])
})

it('rejects a forged Lich pool paired with an ordinary saved modifier', () => {
  const state = {
    ...root,
    explicits: [{ modifierId: 'p', values: { life: 10 } }],
    desecrated: [
      {
        id: 'lich',
        side: 'PREFIX' as const,
        modifierId: 'p',
        revealPool: 'AMANAMU' as const,
      },
    ],
  }
  expect(
    verifiedHistoryState(
      {
        ...state,
        desecrated: state.desecrated.map((s) => ({ ...s, revealPool: null })),
      },
      initialFixture,
    ),
  ).toBe(true)
  expect(verifiedHistoryState(state, initialFixture)).toBe(false)
})
