import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { CraftingPage } from './CraftingPage'
import { useItemDraft } from './draft'
import {
  fixtureFetch,
  initialFixture,
  jsonResponse,
} from '../../shared/test/craftingFixtures'
import {
  currencyButton,
  outerMaterialTab,
} from '../../shared/test/workbenchQueries'
import { concreteInitial } from './workbenchApi'
import type { ConcreteItem } from './workbenchApi'
import { sanctificationTargets } from './sanctification/sanctification'
import {
  startFilm,
  emptyFilms,
  LocalFilmRepository,
  currentFrame,
} from './workbenchHistory'

const initial = {
  ...initialFixture,
  state: {
    ...initialFixture.state,
    baseItemId: 'Metadata/Items/Amulets/FourAmulet9',
  },
}
let client: QueryClient
function show() {
  client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  client.setQueryData(['crafting', 'initial', 'solar', 82, 'manual'], initial)
  return render(
    <QueryClientProvider client={client}>
      <CraftingPage />
    </QueryClientProvider>,
  )
}
afterEach(() => {
  client?.clear()
  vi.unstubAllGlobals()
  localStorage.clear()
})
it('activates sanctification, displays its factor once and restores the original rolls and factors', async () => {
  localStorage.clear()
  useItemDraft.getState().setBase()
  const state: ConcreteItem = {
    ...concreteInitial(initial),
    rarity: 'RARE',
    explicits: [{ modifierId: 'p', values: { life: 17 } }],
  }
  const sanctification = {
    policy: 'explicit-stat-percent-v2' as const,
    implicits: {},
    explicits: {},
  } as NonNullable<ConcreteItem['sanctification']>
  for (const layer of ['explicits'] as const)
    for (const modifier of state[layer]) {
      const targets = sanctificationTargets(
        initialFixture.modifiers[modifier.modifierId]!,
        modifier.values,
      )
      if (targets.length)
        sanctification[layer][modifier.modifierId] = Object.fromEntries(
          targets.map((id) => [id, 79]),
        )
    }
  const next: ConcreteItem = {
    ...state,
    conditions: ['SANCTIFIED'],
    sanctification,
  }
  new LocalFilmRepository(localStorage).save(
    startFilm(
      emptyFilms(),
      state,
      'sanctification-wire',
      initialFixture.rulesetIdentity,
    ),
  )
  vi.stubGlobal('fetch', (input: RequestInfo | URL, init?: RequestInit) => {
    if (String(input).includes('/initial?'))
      return Promise.resolve(jsonResponse(initial))
    if (String(input).endsWith('/workbench/apply')) {
      const request = JSON.parse(String(init?.body))
      expect(request.activeOmens).toEqual(['Omen_of_Sanctification'])
      expect(request.state.explicits).toEqual(state.explicits)
      return Promise.resolve(
        jsonResponse({
          rulesetIdentity: initialFixture.rulesetIdentity,
          ruleVersion: 'sanctification-test',
          ledgerVersion: 'test',
          snapshotId: state.snapshotId,
          action: 'DIVINE',
          applied: true,
          reason: '',
          state: next,
          consumedOmens: ['Omen_of_Sanctification'],
          remainingOmens: [],
          events: [],
          assumptions: [
            {
              id: 'sanctification-explicit-rounded-v2',
              candidateUnit: 'numeric modifier stat',
              n: 45,
              candidates: [],
              min: 78,
              max: 122,
              sourceUrl: 'https://poe2db.tw/us/Sanctified_Items',
              reason: 'Simulator policy',
            },
          ],
        }),
      )
    }
    return fixtureFetch(input, init)
  })
  const mounted = show()
  await screen.findByText(/17.*maximum Life/)
  fireEvent.click(outerMaterialTab('Omen'))
  fireEvent.contextMenu(currencyButton('Omen of Sanctification'))
  fireEvent.click(outerMaterialTab('Currency'))
  fireEvent.click(currencyButton('Divine Orb'))
  fireEvent.click(
    screen.getByRole('button', {
      name: 'Use selected currency on the central item',
    }),
  )
  await waitFor(() => expect(screen.getByText('Sanctified')).toBeVisible())
  expect(screen.getByText(/13.*maximum Life/)).toBeVisible()
  const recorded = currentFrame(
    new LocalFilmRepository(localStorage).load(),
  )!.state
  expect(recorded.explicits).toEqual(state.explicits)
  expect(recorded.sanctification).toEqual(sanctification)
  expect(useItemDraft.getState().activeOmens).toEqual([])
  mounted.unmount()
  client.clear()
  show()
  expect(screen.getByText('Sanctified')).toBeVisible()
  expect(screen.getByText(/13.*maximum Life/)).toBeVisible()
})
