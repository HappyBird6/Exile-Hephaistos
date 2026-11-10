import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor, act } from '@testing-library/react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { CraftingPage } from './CraftingPage'
import { useItemDraft } from './draft'
import {
  fixtureFetch,
  initialFixture,
  jsonResponse,
} from '../../shared/test/craftingFixtures'
import { concreteInitial, mapSolarText } from './workbenchApi'
import { currentItemText } from './currentItemText'
import { editorFixtureFetch } from './item-editor/fixtures.test-support'
import { startFilm, emptyFilms, historyStorageKey } from './workbenchHistory'
let client: QueryClient
const root = concreteInitial(initialFixture)
beforeEach(() => {
  localStorage.clear()
  useItemDraft.getState().resetText('')
  useItemDraft.getState().setBase()
  client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  localStorage.setItem(
    historyStorageKey,
    JSON.stringify(
      startFilm(
        emptyFilms(),
        root,
        'original',
        initialFixture.rulesetIdentity,
        'exact original source',
      ),
    ),
  )
  vi.stubGlobal('fetch', editorFixtureFetch)
})
afterEach(() => {
  client.clear()
  vi.unstubAllGlobals()
})
async function open() {
  render(
    <QueryClientProvider client={client}>
      <CraftingPage />
    </QueryClientProvider>,
  )
  await waitFor(() =>
    expect(useItemDraft.getState().text).toContain('Item Level: 82'),
  )
  fireEvent.click(screen.getByRole('button', { name: 'Edit item' }))
  return screen.getByRole('textbox', { name: 'Item text copied from the game' })
}
it('serializes raw rolls with English metadata and leaves state untouched', () => {
  const before = JSON.stringify(root)
  const text = currentItemText(
    root,
    initialFixture.modifiers,
    'Solar Amulet',
    'Amulets',
  )
  expect(text).toContain('Item Level: 82')
  expect(text).toContain('(implicit)')
  expect(JSON.stringify(root)).toBe(before)
})
it('applies a validated edit atomically and keeps the previous original document', async () => {
  const input = await open()
  vi.stubGlobal('fetch', (url: RequestInfo | URL, init?: RequestInit) =>
    String(url).endsWith('/edit-text')
      ? Promise.resolve(
          jsonResponse({
            mapped: true,
            state: { ...root, itemLevel: 81 },
            issues: [],
          }),
        )
      : editorFixtureFetch(url, init),
  )
  fireEvent.change(input, {
    target: {
      value: useItemDraft
        .getState()
        .text.replace('Item Level: 82', 'Item Level: 81'),
    },
  })
  fireEvent.click(screen.getByRole('button', { name: 'Apply' }))
  await waitFor(() =>
    expect(
      JSON.parse(localStorage.getItem(historyStorageKey)!).films,
    ).toHaveLength(2),
  )
  const saved = JSON.parse(localStorage.getItem(historyStorageKey)!)
  expect(saved.films[0].sourceDocument).toBe('exact original source')
  expect(saved.films[1].frames[0].state.itemLevel).toBe(81)
})
it('keeps dirty input and discards a cancelled late edit response', async () => {
  const input = await open()
  let finish!: (value: Response) => void
  vi.stubGlobal('fetch', (url: RequestInfo | URL, init?: RequestInit) =>
    String(url).endsWith('/edit-text')
      ? new Promise<Response>((resolve) => {
          finish = resolve
        })
      : editorFixtureFetch(url, init),
  )
  const text = useItemDraft
    .getState()
    .text.replace('Item Level: 82', 'Item Level: 81')
  fireEvent.change(input, { target: { value: text } })
  fireEvent.click(screen.getByRole('button', { name: 'Apply' }))
  await waitFor(() => expect(finish).toBeDefined())
  fireEvent.change(input, { target: { value: text + '\nnew draft' } })
  await act(async () => {
    finish(
      jsonResponse({
        mapped: true,
        state: { ...root, itemLevel: 81 },
        issues: [],
      }),
    )
  })
  expect(useItemDraft.getState().text).toBe(text + '\nnew draft')
  expect(
    JSON.parse(localStorage.getItem(historyStorageKey)!).films,
  ).toHaveLength(1)
})
it('preserves dirty text when the selected base changes', () => {
  useItemDraft.getState().setText('unfinished input')
  useItemDraft.getState().setBase(70, 'stocky')
  useItemDraft.getState().syncText('automatic replacement')
  expect(useItemDraft.getState().text).toBe('unfinished input')
})

it('does not submit an unfinished IME composition', async () => {
  const input = await open()
  const fetcher = vi.fn(fixtureFetch)
  vi.stubGlobal('fetch', fetcher)
  fireEvent.compositionStart(input)
  fireEvent.click(screen.getByRole('button', { name: 'Apply' }))
  expect(
    fetcher.mock.calls.some(([url]) => String(url).endsWith('/edit-text')),
  ).toBe(false)
  expect(
    JSON.parse(localStorage.getItem(historyStorageKey)!).films,
  ).toHaveLength(1)
  fireEvent.compositionEnd(input)
})
it('updates socket and installed Rune text from state without altering its identity', () => {
  const state = {
    ...root,
    augmentSockets: 2,
    installedAugments: [{ slotIndex: 1, augmentId: 'Perfect_Desert_Rune' }],
  }
  const text = currentItemText(
    state,
    initialFixture.modifiers,
    'Stocky Mitts',
    'Gloves',
  )
  expect(text).toContain('Sockets: S S')
  expect(text).toContain('+22% to Fire Resistance (rune)')
  expect(text).not.toContain('Rune: Slot')
  expect(state.installedAugments[0]!.augmentId).toBe('Perfect_Desert_Rune')
})

it('exports game desecration markers without adding private reveal constraints', () => {
  const state = {
    ...root,
    rarity: 'RARE' as const,
    conditions: ['CORRUPTED'] as ['CORRUPTED'],
    explicits: [
      { modifierId: 'p', values: { life: 17 } },
      { modifierId: 's', values: { strength: 6 } },
    ],
    desecrated: [
      {
        id: 'revealed',
        side: 'PREFIX' as const,
        modifierId: 'p',
        minimumModifierLevel: 40 as const,
        revealPool: 'ALTERED',
      },
      {
        id: 'hidden',
        side: 'SUFFIX' as const,
        modifierId: null,
        minimumModifierLevel: 40 as const,
        revealPool: 'ULAMAN',
      },
    ],
  }
  const before = JSON.stringify(state)
  const text = currentItemText(
    state,
    initialFixture.modifiers,
    'Solar Amulet',
    'Amulets',
  )
  expect(text).toContain('{ Desecrated Prefix Modifier "Healthy" (Tier: 1) }')
  expect(text).toContain('+17(10-20) to maximum Life (desecrated)')
  expect(text).toContain('+6(5-8) to Strength')
  expect(text).not.toContain('+6(5-8) to Strength (desecrated)')
  expect(text).toContain('Desecrated Suffix')
  expect(text).not.toMatch(/Minimum Modifier Level|Reveal Pool/)
  expect(text).toMatch(/Corrupted$/)
  expect(JSON.stringify(state)).toBe(before)
})

it('accepts supported corrupted desecration imports while blocking unrelated special conditions', async () => {
  const state = {
    ...root,
    rarity: 'RARE' as const,
    conditions: ['CORRUPTED'],
    desecrated: [{ id: 'hidden', side: 'PREFIX' as const, modifierId: null }],
  }
  vi.stubGlobal('fetch', async () =>
    jsonResponse({ mapped: true, state, issues: [] }),
  )
  const mapped = await mapSolarText(
    'source',
    new AbortController().signal,
    initialFixture.modifiers,
    initialFixture.rulesetIdentity,
  )
  expect(mapped.state?.desecrated).toEqual(state.desecrated)
  for (const invalid of [
    { ...state, desecrated: [] },
    { ...state, conditions: ['MIRRORED'] },
  ]) {
    vi.stubGlobal('fetch', async () =>
      jsonResponse({ mapped: true, state: invalid, issues: [] }),
    )
    await expect(
      mapSolarText(
        'source',
        new AbortController().signal,
        initialFixture.modifiers,
        initialFixture.rulesetIdentity,
      ),
    ).rejects.toThrow()
  }
})

it('copies Sanctified effect values and the game flag without inventing an original-roll payload', () => {
  const state = {
    ...root,
    explicits: [{ modifierId: 'p', values: { life: 17 } }],
    conditions: ['SANCTIFIED'] as ['SANCTIFIED'],
    sanctification: {
      policy: 'independent-stat-percent-v1' as const,
      implicits: {},
      explicits: { p: { life: 120 } },
    },
  }
  const text = currentItemText(
    state,
    initialFixture.modifiers,
    'Solar Amulet',
    'Amulets',
  )
  expect(text).toContain('+20.4 to maximum Life')
  expect(text).toMatch(/Sanctified$/)
  expect(text).not.toContain('120%')
  expect(state.explicits[0]!.values.life).toBe(17)
})
