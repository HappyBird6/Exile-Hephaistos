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
import { concreteInitial, mapSolarText } from './workbenchApi'
import type { AppliedItem, ConcreteItem, WorkbenchAction } from './workbenchApi'
import type { Item } from './itemModels'
import { emptyFilms, startFilm, LocalFilmRepository } from './workbenchHistory'

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
const useCurrency = (name: string) => {
  fireEvent.click(screen.getByRole('button', { name }))
  fireEvent.click(
    screen.getByRole('button', {
      name: 'Use selected currency on the central item',
    }),
  )
}
function favoriteOmen(name: string, slot: number) {
  fireEvent.click(screen.getByRole('tab', { name: 'Omen' }))
  fireEvent.click(screen.getByRole('button', { name }))
  fireEvent.click(
    screen.getByRole('button', { name: `Favorite slot ${slot}: empty` }),
  )
  const button = screen.getByRole('button', {
    name: `Favorite slot ${slot}: ${name}`,
  })
  fireEvent.contextMenu(button)
  fireEvent.click(screen.getByRole('tab', { name: 'Currency' }))
  return button
}
describe('Workbench extensions', () => {
  it('activates the reviewed Whittling pair and updates all local candidates without hover requests', async () => {
    const definitions = {
      ...initialFixture.modifiers,
      p: { ...initialFixture.modifiers.p!, requiredItemLevel: 20 },
      p2: {
        ...initialFixture.modifiers.p!,
        id: 'p2',
        familyIds: ['p2'],
        requiredItemLevel: 20,
      },
      s: { ...initialFixture.modifiers.s!, requiredItemLevel: 1 },
    }
    const state: ConcreteItem = {
      ...concreteInitial(initialFixture),
      rarity: 'RARE',
      explicits: [
        { modifierId: 'p', values: { life: 10 } },
        { modifierId: 'p2', values: { life: 10 } },
        { modifierId: 's', values: { strength: 5 } },
      ],
    }
    new LocalFilmRepository(window.localStorage).save(
      startFilm(emptyFilms(), state, 'preview', 'fixture-ruleset'),
    )
    client.setQueryData(['crafting', 'initial', 'solar', 82], {
      ...initialFixture,
      modifiers: definitions,
    })
    const fetch = vi.fn(fixtureFetch)
    vi.stubGlobal('fetch', fetch)
    show()
    const whittling = favoriteOmen('Omen of Whittling', 1)
    expect(
      screen.getAllByTitle('Eligible Whittling removal candidate'),
    ).toHaveLength(1)
    const sinistral = favoriteOmen('Omen of Sinistral Erasure', 2)
    expect(whittling).toHaveClass('is-active-omen')
    expect(sinistral).toHaveClass('is-active-omen')
    expect(
      screen.getAllByTitle('Eligible Whittling removal candidate'),
    ).toHaveLength(2)
    const requests = fetch.mock.calls.length
    fireEvent.pointerMove(screen.getByRole('article'), {
      clientX: 30,
      clientY: 40,
    })
    fireEvent.pointerMove(screen.getByRole('article'), {
      clientX: 50,
      clientY: 60,
    })
    expect(fetch.mock.calls).toHaveLength(requests)
    fireEvent.contextMenu(sinistral)
    expect(
      screen.getAllByTitle('Eligible Whittling removal candidate'),
    ).toHaveLength(1)
    fireEvent.contextMenu(whittling)
    expect(
      screen.queryByTitle('Eligible Whittling removal candidate'),
    ).not.toBeInTheDocument()
  })
  it('prevents unverified matching omen combinations and removes activation when the last favorite is replaced', async () => {
    vi.stubGlobal('fetch', fixtureFetch)
    show()
    await waitFor(() =>
      expect(client.getQueryData(['crafting', 'initial', 82])).toBeDefined(),
    )
    const sinistral = favoriteOmen('Omen of Sinistral Exaltation', 1)
    const dextral = favoriteOmen('Omen of Dextral Exaltation', 2)
    expect(sinistral).toHaveClass('is-active-omen')
    expect(dextral).not.toHaveClass('is-active-omen')
    expect(screen.getByRole('status')).toHaveTextContent(
      'combination has not been verified',
    )
    fireEvent.contextMenu(sinistral)
    fireEvent.contextMenu(dextral)
    expect(dextral).toHaveClass('is-active-omen')
    fireEvent.click(screen.getByRole('tab', { name: 'Omen' }))
    fireEvent.click(screen.getByRole('button', { name: 'Omen of the Blessed' }))
    fireEvent.click(dextral)
    expect(useItemDraft.getState().activeOmens).toEqual([])
    expect(
      screen.queryByRole('checkbox', { name: /^Omen of / }),
    ).not.toBeInTheDocument()
    expect(
      screen.getByRole('checkbox', { name: 'Show legacy Omens' }),
    ).not.toBeChecked()
  })
  it('consumes the matching affix omen and preserves the unrelated Blessed omen until Divine', async () => {
    const bodies: {
      state: ConcreteItem
      action: WorkbenchAction
      activeOmens: string[]
    }[] = []
    vi.stubGlobal('fetch', (url: RequestInfo | URL, init?: RequestInit) => {
      if (!String(url).endsWith('/workbench/apply'))
        return fixtureFetch(url, init)
      const b = JSON.parse(String(init?.body)) as (typeof bodies)[number]
      bodies.push(b)
      const divine = b.action === 'DIVINE'
      const consumed = divine
        ? 'Omen_of_the_Blessed'
        : 'Omen_of_Sinistral_Exaltation'
      return Promise.resolve(
        jsonResponse({
          rulesetIdentity: 'fixture-ruleset',
          ruleVersion: 'solar-workbench-affix-v2',
          ledgerVersion: 'solar-uniform-assumptions-v1',
          snapshotId: b.state.snapshotId,
          action: b.action,
          applied: true,
          reason: '',
          assumptions: [],
          consumedOmens: [consumed],
          remainingOmens: b.activeOmens.filter((id) => id !== consumed),
          state: {
            ...b.state,
            explicits: divine
              ? b.state.explicits
              : [{ modifierId: 'p', values: { life: 17 } }],
            implicits: divine
              ? [
                  {
                    modifierId: 'implicit',
                    values: { base_spirit_from_equipment: 10 },
                  },
                ]
              : b.state.implicits,
          },
          events: divine
            ? [
                {
                  kind: 'REROLL_IMPLICIT',
                  modifierId: 'implicit',
                  values: { base_spirit_from_equipment: 10 },
                  selectionProbability: 1,
                },
              ]
            : [
                {
                  kind: 'ADD',
                  modifierId: 'p',
                  values: { life: 17 },
                  selectionProbability: 0.6,
                },
              ],
        }),
      )
    })
    show()
    await waitFor(() =>
      expect(client.getQueryData(['crafting', 'initial', 82])).toBeDefined(),
    )
    act(() =>
      client.setQueryData(
        ['crafting', 'workbench', useItemDraft.getState().baseRevision],
        {
          rulesetIdentity: 'fixture-ruleset',
          ruleVersion: 'solar-workbench-affix-v2',
          ledgerVersion: 'solar-uniform-assumptions-v1',
          snapshotId: 'fixture-v1',
          state: { ...concreteInitial(initialFixture), rarity: 'RARE' },
          action: 'REGAL',
          applied: true,
          reason: '',
          events: [],
          assumptions: [],
          consumedOmens: [],
          remainingOmens: [],
        } satisfies AppliedItem,
      ),
    )
    const exaltation = favoriteOmen('Omen of Sinistral Exaltation', 1)
    const blessed = favoriteOmen('Omen of the Blessed', 2)
    useCurrency('Exalted Orb')
    await waitFor(() =>
      expect(exaltation).toHaveAttribute('aria-pressed', 'false'),
    )
    expect(blessed).toHaveClass('is-active-omen')
    expect(screen.getByRole('article')).toHaveTextContent('+17 to maximum Life')
    useCurrency('Divine Orb')
    await waitFor(() =>
      expect(blessed).toHaveAttribute('aria-pressed', 'false'),
    )
    expect(screen.getByRole('article')).toHaveTextContent('+10 to Spirit')
    expect(screen.getByRole('article')).toHaveTextContent('+17 to maximum Life')
    expect(bodies[0]!.activeOmens).toHaveLength(2)
    expect(bodies[1]!.activeOmens).toEqual(['Omen_of_the_Blessed'])
    expect(
      screen.getByRole('tab', { name: 'Crafting Workbench' }),
    ).toHaveAttribute('aria-selected', 'true')
  })

  it('enables a server-validated Solar clipboard item without replacing the original text', async () => {
    const original = 'original Solar clipboard document'
    const parsed: Item = {
      text: { originalText: original, lines: [] },
      locale: 'en',
      itemClass: 'Amulets',
      rarity: 'RARE',
      rarityText: 'Rare',
      nameLines: [],
      displayName: 'Imported Solar',
      displayBase: 'Solar Amulet',
      itemLevel: 82,
      properties: [],
      requirements: [],
      markedModifiers: [],
      modifiers: [],
      flags: [],
      unparsedLines: [],
      warnings: [],
    }
    client.setQueryData(['workbench', 'current-item'], parsed)
    useItemDraft.getState().acceptText(original)
    vi.stubGlobal('fetch', (url: RequestInfo | URL, init?: RequestInit) =>
      String(url).endsWith('/map-text')
        ? Promise.resolve(
            jsonResponse({
              mapped: true,
              state: {
                ...concreteInitial(initialFixture),
                rarity: 'RARE',
                explicits: [{ modifierId: 'p', values: { life: 17 } }],
              },
              issues: [],
            }),
          )
        : fixtureFetch(url, init),
    )
    show()
    await waitFor(() =>
      expect(
        screen.getByRole('button', { name: 'Enable Solar crafting' }),
      ).toBeEnabled(),
    )
    fireEvent.click(
      screen.getByRole('button', { name: 'Enable Solar crafting' }),
    )
    await waitFor(() =>
      expect(screen.getByRole('article')).toHaveTextContent(
        '+17 to maximum Life',
      ),
    )
    expect(useItemDraft.getState().currentText.text).toBe(original)
    expect(useItemDraft.getState().source).toBe('text')
    const whittling = favoriteOmen('Omen of Whittling', 1)
    expect(whittling).toHaveClass('is-active-omen')
  })

  it('rejects an unknown mapped modifier instead of rendering it as a validated item', async () => {
    vi.stubGlobal('fetch', () =>
      Promise.resolve(
        jsonResponse({
          mapped: true,
          state: {
            ...concreteInitial(initialFixture),
            explicits: [{ modifierId: 'unknown', values: { life: 17 } }],
          },
          issues: [],
        }),
      ),
    )
    await expect(
      mapSolarText(
        'text',
        new AbortController().signal,
        initialFixture.modifiers,
        'fixture-ruleset',
      ),
    ).rejects.toThrow('verify')
  })
})
