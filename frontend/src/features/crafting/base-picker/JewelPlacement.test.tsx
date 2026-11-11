import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { CraftingPage } from '../CraftingPage'
import { loadInitial } from '../craftingApi'
import type { Definition, Initial } from '../craftingApi'
import { useItemDraft } from '../draft'
import { concreteInitial } from '../workbenchApi'
import {
  emptyFilms,
  historyStorageKey,
  startFilm,
  verifiedHistoryState,
} from '../workbenchHistory'
import {
  fixtureFetch,
  initialFixture,
  jsonResponse,
} from '../../../shared/test/craftingFixtures'
import catalog from '../../../shared/test/sapphire-catalog.json'
const initial: Initial = {
  ruleVersion: 'fixture',
  rulesetIdentity: 'fixture-ruleset',
  metadata: catalog.metadata,
  id: 'root',
  actions: initialFixture.actions,
  baseRules: catalog.base,
  state: {
    snapshotId: catalog.metadata.snapshotId,
    baseItemId: 'Metadata/Items/Jewels/JewelInt',
    itemLevel: 82,
    rarity: 'MAGIC',
    implicits: [],
    modifierIds: [],
    conditions: [],
  },
  modifiers: Object.fromEntries(
    (catalog.modifiers as Definition[]).map((d) => [d.id, d]),
  ),
}
afterEach(() => vi.unstubAllGlobals())
it.each(['NORMAL', 'MAGIC', 'RARE'] as const)(
  'places a new %s Sapphire with no suffix while preserving an old Magic film',
  async (rarity) => {
    expect(
      verifiedHistoryState(
        { ...concreteInitial(initial), rarity, explicits: [] },
        initial,
      ),
    ).toBe(true)
    useItemDraft.getState().setBase()
    const old = startFilm(
      emptyFilms(),
      concreteInitial(initial),
      'old',
      initial.rulesetIdentity,
      'Original copied text',
    )
    localStorage.setItem(historyStorageKey, JSON.stringify(old))
    vi.stubGlobal('fetch', (input: RequestInfo | URL, init?: RequestInit) =>
      String(input).includes('base=sapphire')
        ? Promise.resolve(jsonResponse(initial))
        : fixtureFetch(input, init),
    )
    expect(
      await loadInitial(82, new AbortController().signal, 'sapphire', true),
    ).toEqual(initial)
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })
    const mounted = render(
      <QueryClientProvider client={client}>
        <CraftingPage />
      </QueryClientProvider>,
    )
    fireEvent.click(screen.getByRole('button', { name: 'Select base' }))
    const dialog = await screen.findByRole('dialog')
    fireEvent.change(within(dialog).getByRole('searchbox'), {
      target: { value: 'Sapphire' },
    })
    fireEvent.click(
      within(dialog).getByRole('button', { name: /^Sapphire\s*Jewels$/ }),
    )
    expect(screen.getByLabelText('Rarity')).toHaveValue('NORMAL')
    fireEvent.change(screen.getByLabelText('Rarity'), {
      target: { value: rarity },
    })
    await waitFor(() =>
      expect(
        within(dialog).getByRole('button', { name: 'Select base' }),
      ).toBeEnabled(),
    )
    fireEvent.click(within(dialog).getByRole('button', { name: 'Select base' }))
    await waitFor(() => {
      expect(
        document.querySelector('.craft-page > .sr-only[role="status"]')
          ?.textContent,
      ).not.toContain('Could not')
      const saved = JSON.parse(localStorage.getItem(historyStorageKey)!)
      expect(saved.films).toHaveLength(2)
      const added = saved.films.find((f: { id: string }) => f.id !== 'old')
      expect(added.frames[0].state).toMatchObject({ rarity, explicits: [] })
      expect(saved.films.find((f: { id: string }) => f.id === 'old')).toEqual(
        old.films[0],
      )
    })
    mounted.unmount()
    client.clear()
  },
)
