import { webcrypto } from 'node:crypto'
import { IDBFactory } from 'fake-indexeddb'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, within } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import {
  fixtureFetch,
  initialFixture,
} from '../../../shared/test/craftingFixtures'
import { setLocale } from '../../../shared/i18n/i18n'
import { revealAction } from '../abyss-workbench/revealAction'
import { requestFixture } from '../path-tree/fixtures.test-support'
import { rulesetHeader } from '../rulesetIdentity'
import { SessionOverviewPage } from '../session-history/SessionOverviewPage'
import { IndexedFilmRepository } from '../session-library/IndexedFilmRepository'
import { extractSessionNotation } from '../session-notation/extractSessionNotation'
import { concreteInitial } from '../workbenchApi'
import {
  loadAcquisitionRoutes,
  type AcquisitionResult,
} from './acquisitionRouteApi'
import { acquisitionRouteFilm } from './acquisitionRouteFilm'
import { transferAcquisitionRoute } from './transferAcquisitionRoute'
import { fixedSteps } from './fixedRouteGrades'

afterEach(() => {
  vi.unstubAllGlobals()
  localStorage.clear()
})

function desecrationRoute() {
  const catalog = {
    ...initialFixture,
    state: {
      ...initialFixture.state,
      baseItemId: 'Metadata/Items/Amulets/FourAmulet9',
    },
  }
  const request = requestFixture()
  const base = { ...concreteInitial(catalog), rarity: 'RARE' as const }
  request.start.item = {
    ...request.start.item,
    baseItemId: base.baseItemId,
    snapshotId: base.snapshotId,
    rarity: base.rarity,
    explicits: [],
    implicits: base.implicits.map((mod) => ({ ...mod, fractured: false })),
  }
  request.start.provenance.rulesetIdentity = catalog.rulesetIdentity
  const slot = {
    id: 'recipe-desecrated',
    side: 'PREFIX' as const,
    modifierId: null,
  }
  const hidden = { ...base, desecrated: [slot] }
  const beforeReveal = {
    ...hidden,
    explicits: [{ modifierId: 's', values: { strength: 5 } }],
  }
  const revealed = {
    ...beforeReveal,
    explicits: [
      ...beforeReveal.explicits,
      { modifierId: 'p', values: { life: 10 } },
    ],
    desecrated: [{ ...slot, modifierId: 'p' }],
  }
  const result: AcquisitionResult = {
    complete: true,
    cards: [
      {
        options: [
          { number: 1, modifierIds: ['p'] },
          { number: 2, modifierIds: ['s'] },
        ],
      },
    ],
    states: [beforeReveal, revealed, hidden],
    routes: [
      {
        cardIndex: 0,
        order: [2, 1],
        probability: 0.1,
        stateIndices: [2, 0, 1],
        steps: [
          {
            action: 'Preserved_Collarbone',
            omens: [],
            acquired: [],
            lost: [],
            chance: { probability: 0.5 },
          },
          {
            action: 'EXALTED',
            omens: ['Omen_of_Dextral_Exaltation'],
            acquired: [2],
            lost: [],
            chance: { probability: 0.2 },
          },
          {
            action: 'REVEAL',
            omens: [],
            acquired: [1],
            lost: [],
            chance: { probability: 1 },
          },
        ],
      },
    ],
  }
  return { catalog, request, result, hidden, beforeReveal, revealed }
}

it('stores and renders a paid bone, an intervening craft and the free reveal as three distinct checkpoints', async () => {
  const { catalog, request, result, hidden, beforeReveal, revealed } =
    desecrationRoute()
  await setLocale('en')
  vi.stubGlobal('indexedDB', new IDBFactory())
  vi.stubGlobal('crypto', webcrypto)
  vi.stubGlobal('fetch', (url: RequestInfo | URL, init?: RequestInit) =>
    String(url).endsWith('/test-routes')
      ? Promise.resolve(
          new Response(JSON.stringify(result), {
            headers: { [rulesetHeader]: catalog.rulesetIdentity },
          }),
        )
      : fixtureFetch(url, init),
  )
  const signal = new AbortController().signal
  const response = await loadAcquisitionRoutes(request, signal)
  const film = acquisitionRouteFilm(request, response, 0, 'en')!
  const fixed = fixedSteps(response.routes[0]!, [1, 2, 1])
  expect(fixed.map((step) => [step.action, step.omens, step.acquired])).toEqual(
    [
      ['Preserved_Collarbone', [], []],
      ['GREATER_EXALTED', ['Omen_of_Dextral_Exaltation'], [2]],
      ['REVEAL', [], [1]],
    ],
  )
  await transferAcquisitionRoute(film, 2, 'simulation', catalog, null, signal)
  const repo = new IndexedFilmRepository()
  const stored = await repo.load()
  await repo.close()
  const branch = stored.films.find((entry) => entry.id === stored.active)!
  const saved = stored.films.find(
    (entry) => entry.id === branch.continuationOf?.sessionId,
  )!
  expect(branch.frames[stored.cursor]!.state).toEqual(beforeReveal)
  expect(saved.frames.slice(1).map((frame) => frame.state)).toEqual([
    hidden,
    beforeReveal,
    revealed,
  ])
  expect(
    extractSessionNotation(saved, 3).steps.map((step) => [
      step.currencyId,
      step.consumedOmenIds,
    ]),
  ).toEqual([
    ['Preserved_Collarbone', []],
    ['Exalted_Orb', ['Omen_of_Dextral_Exaltation']],
    [revealAction, []],
  ])
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false, staleTime: Infinity } },
  })
  client.setQueryData(['crafting', 'initial', 'solar', 82, 'manual'], catalog)
  const price = (name: string, exalted: number) => ({
    name,
    exalted,
    divine: exalted / 100,
  })
  client.setQueryData(['economy', 'prices'], {
    snapshot: {
      league: 'fixture',
      updatedAt: '2026-11-10T00:00:00Z',
      exaltedPerDivine: 100,
      prices: {
        'Preserved Collarbone': price('Preserved Collarbone', 8),
        'Omen of Dextral Exaltation': price('Omen of Dextral Exaltation', 4),
        'Exalted Orb': price('Exalted Orb', 1),
      },
    },
    unavailable: false,
    stale: false,
    nextRefreshAllowedAt: '2026-11-10T00:05:00Z',
  })
  const page = render(
    <QueryClientProvider client={client}>
      <SessionOverviewPage id={saved.id} />
    </QueryClientProvider>,
  )
  const cards = await screen.findAllByRole('article')
  expect(cards).toHaveLength(4)
  for (const card of cards.slice(1, 3)) {
    expect(
      within(card).getByRole('img', { name: 'Unrevealed desecrated modifier' }),
    ).toBeVisible()
    expect(card).not.toHaveTextContent('+10 to maximum Life')
  }
  expect(cards[2]).toHaveTextContent('+5 to Strength')
  expect(cards[3]).toHaveTextContent('+10 to maximum Life')
  expect(
    within(cards[3]!).queryByRole('img', {
      name: 'Unrevealed desecrated modifier',
    }),
  ).toBeNull()
  const edges = page.container.querySelectorAll('[data-flow-edge]')
  expect(edges).toHaveLength(3)
  expect(edges[0]).toHaveAccessibleName('Preserved Collarbone')
  expect(
    within(edges[1] as HTMLElement).getByRole('img', {
      name: 'Omen of Dextral Exaltation',
    }),
  ).toBeVisible()
  expect(edges[1]).toHaveAccessibleName('Exalted Orb')
  expect(edges[2]).toHaveAccessibleName('Desecrated Reveal')
  expect(cards[1]?.parentElement).toHaveTextContent('Step chance 50%')
  expect(cards[1]?.parentElement).not.toHaveTextContent(
    'assumes an eligible desecration target is revealed',
  )
  expect(cards[3]?.parentElement).toHaveTextContent(
    'Step chance 100% (assumes an eligible desecration target is revealed)',
  )
  expect(
    page.container.querySelector(
      '.history-session-summary [aria-label="13 Exalted Orb"]',
    ),
  ).toBeInTheDocument()
  page.unmount()
  client.clear()
})

it('requires a matching hidden slot in the preceding checkpoint before showing a verified reveal', () => {
  const { request, result } = desecrationRoute()
  for (const invalidSlot of [
    { id: 'other-slot' },
    { side: 'SUFFIX' as const },
  ]) {
    const unrelated = structuredClone(result)
    Object.assign(unrelated.states[1]!.desecrated![0]!, invalidSlot)
    expect(acquisitionRouteFilm(request, unrelated, 0)).toBeNull()
  }
})
