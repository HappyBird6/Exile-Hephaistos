import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import {
  fixtureFetch,
  initialFixture,
  jsonResponse,
} from '../../shared/test/craftingFixtures'
import { CraftingPage } from './CraftingPage'
import { useItemDraft } from './draft'
import { applyCurrency, concreteInitial } from './workbenchApi'
import {
  currentFilm,
  emptyFilms,
  filmRulesetStatus,
  historyStorageKey,
  LocalFilmRepository,
  recordCraft,
  startFilm,
  viewFrame,
} from './workbenchHistory'

afterEach(() => vi.unstubAllGlobals())
beforeEach(() => localStorage.clear())
const identity = initialFixture.rulesetIdentity
const root = concreteInitial(initialFixture)

it('permits preserved record selection even when the current catalog service is unavailable', async () => {
  const history = startFilm(
    startFilm(emptyFilms(), root, 'first', identity),
    { ...root, baseItemId: 'historical-unsupported-base' },
    'past',
    identity,
  )
  history.active = 'first'
  delete history.films[1]!.rulesetIdentity
  delete history.films[1]!.frames[0]!.rulesetIdentity
  localStorage.setItem(historyStorageKey, JSON.stringify(history))
  useItemDraft.getState().setBase()
  const fetch = vi.fn().mockRejectedValue(new Error('Offline'))
  vi.stubGlobal('fetch', fetch)
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  const page = render(
    <QueryClientProvider client={client}>
      <CraftingPage />
    </QueryClientProvider>,
  )
  fireEvent.change(screen.getByRole('combobox', { name: 'Crafting session' }), {
    target: { value: 'past' },
  })
  await waitFor(() =>
    expect(JSON.parse(localStorage.getItem(historyStorageKey)!).active).toBe(
      'past',
    ),
  )
  fireEvent.click(screen.getByText('Preserved craft record'))
  expect(
    screen.getByText(/historical-unsupported-base/, { selector: 'pre' }),
  ).toBeVisible()
  expect(JSON.parse(localStorage.getItem(historyStorageKey)!).films).toEqual(
    history.films,
  )
  page.unmount()
  client.clear()
})

it('requires explicit request identity and rejects mismatched transport and result identities', async () => {
  const fetch = vi.fn(fixtureFetch)
  vi.stubGlobal('fetch', fetch)
  await expect(
    applyCurrency(
      root,
      'TRANSMUTATION',
      initialFixture.modifiers,
      new AbortController().signal,
      [],
      undefined as unknown as string,
    ),
  ).rejects.toThrow('identity is missing')
  expect(fetch).not.toHaveBeenCalled()
  const result = await applyCurrency(
    root,
    'TRANSMUTATION',
    initialFixture.modifiers,
    new AbortController().signal,
    [],
    identity,
  )
  expect(
    new Headers(fetch.mock.calls[0]![1]?.headers).get('X-Crafting-Ruleset'),
  ).toBe(identity)
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue(
      new Response(JSON.stringify(result), {
        headers: { 'X-Crafting-Ruleset': 'other' },
      }),
    ),
  )
  await expect(
    applyCurrency(
      root,
      'TRANSMUTATION',
      initialFixture.modifiers,
      new AbortController().signal,
      [],
      identity,
    ),
  ).rejects.toThrow('ruleset changed')
  vi.stubGlobal(
    'fetch',
    vi
      .fn()
      .mockResolvedValue(jsonResponse({ ...result, rulesetIdentity: 'other' })),
  )
  await expect(
    applyCurrency(
      root,
      'TRANSMUTATION',
      initialFixture.modifiers,
      new AbortController().signal,
      [],
      identity,
    ),
  ).rejects.toThrow('ruleset changed')
})

it('classifies complete films without inventing identity and preserves original bytes on selection, new craft and recovery', async () => {
  const result = await (
    await fixtureFetch('/api/v1/crafting/workbench/apply', {
      body: JSON.stringify({ state: root, action: 'TRANSMUTATION' }),
    })
  ).json()
  const current = recordCraft(
    startFilm(emptyFilms(), root, 'current', identity),
    root,
    result,
    'unused',
  )
  expect(filmRulesetStatus(currentFilm(current)!, identity)).toBe('current')
  const fork = recordCraft(viewFrame(current, 0), root, result, 'fork')
  expect(fork.films[0]).toEqual(current.films[0])
  expect(filmRulesetStatus(currentFilm(fork)!, identity)).toBe('current')
  const legacy = structuredClone(current)
  delete legacy.films[0]!.rulesetIdentity
  for (const frame of legacy.films[0]!.frames) {
    delete frame.rulesetIdentity
    if (frame.evidence)
      delete (frame.evidence as Partial<typeof frame.evidence>).rulesetIdentity
  }
  expect(filmRulesetStatus(legacy.films[0]!, identity)).toBe(
    'legacy-unverified',
  )
  expect(() => recordCraft(legacy, root, result, 'blocked')).toThrow(
    'unverified',
  )
  const mismatch = structuredClone(current)
  mismatch.films[0]!.rulesetIdentity = 'past'
  for (const frame of mismatch.films[0]!.frames) {
    frame.rulesetIdentity = 'past'
    if (frame.evidence) frame.evidence.rulesetIdentity = 'past'
  }
  expect(filmRulesetStatus(mismatch.films[0]!, identity)).toBe('mismatch')
  mismatch.films[0]!.frames[0]!.rulesetIdentity = identity
  expect(filmRulesetStatus(mismatch.films[0]!, identity)).toBe('inconsistent')
  const raw = JSON.stringify(legacy, null, 2)
  localStorage.setItem(historyStorageKey, raw)
  const repository = new LocalFilmRepository(localStorage)
  expect(repository.load()).toEqual(legacy)
  expect(localStorage.getItem(historyStorageKey)).toBe(raw)
  repository.save(startFilm(repository.load(), root, 'fresh', identity))
  expect(localStorage.getItem(historyStorageKey + '.preserved')).toBe(raw)
  expect(repository.load().films[0]).toEqual(legacy.films[0])
  expect(filmRulesetStatus(repository.load().films[1]!, identity)).toBe(
    'current',
  )
})

it.each(['legacy-unverified', 'mismatch', 'inconsistent'] as const)(
  'keeps %s records viewable and never submits execution from them',
  async (status) => {
    const history = startFilm(emptyFilms(), root, 'saved', identity)
    if (status === 'legacy-unverified') {
      delete history.films[0]!.rulesetIdentity
      delete history.films[0]!.frames[0]!.rulesetIdentity
    } else {
      history.films[0]!.rulesetIdentity = 'past'
      history.films[0]!.frames[0]!.rulesetIdentity =
        status === 'mismatch' ? 'past' : identity
    }
    const bytes = JSON.stringify(history, null, 2)
    localStorage.setItem(historyStorageKey, bytes)
    useItemDraft.getState().setBase()
    const fetch = vi.fn(fixtureFetch)
    vi.stubGlobal('fetch', fetch)
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })
    const page = render(
      <QueryClientProvider client={client}>
        <CraftingPage />
      </QueryClientProvider>,
    )
    await screen.findByText(/Execution is disabled/)
    fireEvent.click(screen.getByText('Preserved craft record'))
    expect(
      screen.getByText(/"rulesetIdentity"|"snapshotId"/, { selector: 'pre' }),
    ).toBeVisible()
    fireEvent.click(
      screen.getByRole('button', { name: 'Orb of Transmutation' }),
    )
    fireEvent.click(
      screen.getByRole('button', {
        name: 'Use selected currency on the central item',
      }),
    )
    await waitFor(() =>
      expect(
        fetch.mock.calls.every(([url]) => !String(url).endsWith('/apply')),
      ).toBe(true),
    )
    expect(localStorage.getItem(historyStorageKey)).toBe(bytes)
    page.unmount()
    client.clear()
  },
)
