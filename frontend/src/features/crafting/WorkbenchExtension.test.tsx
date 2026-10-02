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
describe('Workbench extensions', () => {
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
    fireEvent.click(
      screen.getByRole('checkbox', { name: /Omen of Sinistral Exaltation/ }),
    )
    fireEvent.click(
      screen.getByRole('checkbox', { name: /Omen of the Blessed/ }),
    )
    useCurrency('Exalted Orb')
    await waitFor(() =>
      expect(
        screen.getByRole('checkbox', { name: /Omen of Sinistral Exaltation/ }),
      ).not.toBeChecked(),
    )
    expect(
      screen.getByRole('checkbox', { name: /Omen of the Blessed/ }),
    ).toBeChecked()
    expect(screen.getByRole('article')).toHaveTextContent('+17 to maximum Life')
    useCurrency('Divine Orb')
    await waitFor(() =>
      expect(
        screen.getByRole('checkbox', { name: /Omen of the Blessed/ }),
      ).not.toBeChecked(),
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
    expect(
      screen.getByRole('checkbox', { name: /Omen of Whittling/ }),
    ).toBeEnabled()
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
      ),
    ).rejects.toThrow('verify')
  })
})
