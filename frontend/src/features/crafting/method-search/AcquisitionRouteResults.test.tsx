import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { initialFixture } from '../../../shared/test/craftingFixtures'
import type { ConcreteItem } from '../workbenchApi'
import { setLocale } from '../../../shared/i18n/i18n'
import type { Initial } from '../craftingApi'
import { CurrencyPriceContext } from '../currency-prices/context'
import { requestFixture } from '../path-tree/fixtures.test-support'
import { rulesetHeader } from '../rulesetIdentity'
import { AcquisitionRouteResults } from './AcquisitionRouteResults'
import {
  loadAcquisitionRoutes,
  type AcquisitionResult,
} from './acquisitionRouteApi'

afterEach(() => vi.unstubAllGlobals())

const baseId = 'Metadata/Items/Amulets/FourAmulet9'
const request = requestFixture()
request.start.item = {
  ...request.start.item,
  baseItemId: baseId,
  snapshotId: initialFixture.state.snapshotId,
  explicits: [],
  implicits: initialFixture.state.implicits.map((m) => ({
    ...m,
    fractured: false,
  })),
}
request.goal.general.baseItemId = baseId
request.start.provenance.rulesetIdentity = initialFixture.rulesetIdentity

const definitions: Initial['modifiers'] = {
  life: {
    id: 'life',
    name: 'Life',
    text: '+(10—20) to maximum Life',
    stats: [{ id: 'life', min: 10, max: 20 }],
    tier: 1,
    affixType: 'PREFIX',
    familyIds: [],
  },
  cold: {
    id: 'cold',
    name: 'Cold',
    text: '+(5—12)% to Cold Resistance',
    stats: [{ id: 'cold', min: 5, max: 12 }],
    tier: 1,
    affixType: 'SUFFIX',
    familyIds: [],
  },
}

const market = {
  data: {
    snapshot: {
      league: 'Test',
      updatedAt: '2026-11-09T00:00:00Z',
      exaltedPerDivine: 500,
      prices: {
        'Exalted Orb': { name: 'Exalted Orb', exalted: 1, divine: 0.002 },
        'Perfect Exalted Orb': {
          name: 'Perfect Exalted Orb',
          exalted: 5,
          divine: 0.01,
        },
      },
    },
    nextRefreshAllowedAt: '2026-11-09T01:00:00Z',
    stale: false,
    unavailable: false,
  },
  pending: false,
  refreshing: false,
  error: false,
  refresh: vi.fn(),
}

function observed(life: number | null, cold: number | null): ConcreteItem {
  return {
    ...request.start.item,
    rarity: 'RARE',
    catalystQuality: null,
    explicits: [
      ...(life == null ? [] : [{ modifierId: 'life', values: { life } }]),
      ...(cold == null ? [] : [{ modifierId: 'cold', values: { cold } }]),
    ],
  }
}

function serve(result: AcquisitionResult) {
  const fetch = vi.fn().mockImplementation(
    async () =>
      new Response(JSON.stringify(result), {
        headers: {
          [rulesetHeader]: request.start.provenance.rulesetIdentity,
        },
      }),
  )
  vi.stubGlobal('fetch', fetch)
  return fetch
}
function show() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  client.setQueryData(['crafting', 'initial', 'solar', 82, 'manual'], {
    ...initialFixture,
    state: { ...initialFixture.state, baseItemId: baseId },
    modifiers: { ...initialFixture.modifiers, ...definitions },
  })
  return render(
    <QueryClientProvider client={client}>
      <CurrencyPriceContext.Provider value={market}>
        <AcquisitionRouteResults
          request={request}
          definitions={definitions}
          baseName="Solar Amulet"
          active
        />
      </CurrencyPriceContext.Provider>
    </QueryClientProvider>,
  )
}

async function showTwoRoutes() {
  await setLocale('en')
  const step = { action: 'EXALTED', omens: [], acquired: [], lost: [] }
  const fetch = serve({
    states: [
      observed(17, null),
      observed(null, 9),
      observed(17, 7),
      observed(16, 9),
      observed(13, null),
    ],
    cards: [
      {
        options: [
          { number: 1, modifierIds: ['life'] },
          { number: 2, modifierIds: ['cold'] },
        ],
      },
    ],
    routes: [
      {
        cardIndex: 0,
        order: [1, 2],
        stateIndices: [0, 2],
        probability: 0.27,
        steps: [
          {
            ...step,
            action: 'PERFECT_ESSENCE_ENHANCEMENT',
            omens: ['Omen_of_Sinistral_Crystallisation'],
            acquired: [1],
          },
          { ...step, action: 'Preserved_Collarbone', acquired: [2] },
        ],
      },
      {
        cardIndex: 0,
        order: [1, 2, 1],
        stateIndices: [4, 1, 3],
        probability: null,
        steps: [
          { ...step, acquired: [1] },
          { ...step, acquired: [2], lost: [1] },
          { ...step, acquired: [1] },
        ],
      },
    ],
    complete: false,
  })
  show()
  const list = await screen.findByRole('list', { name: 'Crafting methods' })
  const first = within(list).getByRole('button', { pressed: true })
  const second = within(list).getByRole('button', { pressed: false })
  return { fetch, first, second }
}

it('requests the current item and goal, and describes the materials, gains and losses', async () => {
  const { fetch, second } = await showTwoRoutes()
  const [path, options] = fetch.mock.calls[0]!
  expect(path).toBe('/api/v1/crafting/support/goal-filters/test-routes')
  expect(options.method).toBe('POST')
  expect(JSON.parse(options.body)).toEqual({
    item: request.start.item,
    goal: request.goal,
  })
  expect(options.signal).toBeInstanceOf(AbortSignal)
  const summary = screen.getByText('Modifier acquisition order', {
    selector: 'summary',
  })
  fireEvent.click(summary)
  const detail = summary.closest('details')!
  expect(within(detail).getAllByRole('listitem')[0]).toHaveTextContent(
    'Perfect Essence of Enhancement + Omen of Sinistral Crystallisation',
  )
  expect(within(detail).getAllByRole('listitem')[1]).toHaveTextContent(
    'Preserved Collarbone',
  )
  fireEvent.click(second)
  expect(detail).toHaveTextContent('maximum Life')
  const steps = within(detail).getAllByRole('listitem')
  expect(steps).toHaveLength(3)
  expect(steps[0]).toHaveTextContent('Exalted Orb')
  expect(steps[0]).toHaveTextContent('Added 1')
  expect(steps[1]).toHaveTextContent('Removed 1 · Added 2')
})

it('previews the selected final item and navigates its returned states and cumulative costs', async () => {
  const { first, second } = await showTwoRoutes()
  expect(first).toHaveAccessibleName(/1 → 2.*27%/)
  const card = () => screen.getByRole('article', { name: 'Item card' })
  const previous = screen.getByRole('button', {
    name: 'Previous crafting step',
  })
  const next = screen.getByRole('button', { name: 'Next crafting step' })
  expect(card()).toHaveTextContent('+17 to maximum Life')
  expect(card()).toHaveTextContent('+7% to Cold Resistance')
  expect(next).toBeDisabled()
  const history = screen.getByRole('region', { name: 'History' })
  expect(
    within(history).getByRole('img', {
      name: /Perfect Essence of Enhancement/,
    }),
  ).toBeInTheDocument()
  expect(
    within(history).getByRole('img', {
      name: /Omen of Sinistral Crystallisation/,
    }),
  ).toBeInTheDocument()
  fireEvent.click(previous)
  expect(card()).toHaveTextContent('+17 to maximum Life')
  expect(card()).not.toHaveTextContent('Cold Resistance')
  fireEvent.click(previous)
  expect(previous).toBeDisabled()
  expect(card()).not.toHaveTextContent('maximum Life')
  fireEvent.click(next)
  expect(card()).toHaveTextContent('+17 to maximum Life')
  expect(
    within(history).getByRole('link', { name: 'Session overview' }),
  ).toHaveAttribute(
    'href',
    '/support-test/overview?request=' +
      encodeURIComponent(request.clientRequestId) +
      '&route=0',
  )
  expect(second).toHaveAccessibleName(/1 → 2 → 1.*3 ex · Not yet determined/)
  fireEvent.click(second)
  expect(screen.getByLabelText('3 Exalted Orb')).toBeVisible()
  expect(card()).toHaveTextContent('+16 to maximum Life')
  expect(card()).toHaveTextContent('+9% to Cold Resistance')
  expect(
    screen.getByRole('button', { name: 'Next crafting step' }),
  ).toBeDisabled()
  const secondHistory = screen.getByRole('region', { name: 'History' })
  expect(
    within(secondHistory).getByRole('link', { name: 'Session overview' }),
  ).toHaveAttribute(
    'href',
    '/support-test/overview?request=' +
      encodeURIComponent(request.clientRequestId) +
      '&route=1',
  )
  fireEvent.click(
    screen.getByRole('button', { name: 'Previous crafting step' }),
  )
  expect(card()).toHaveTextContent('+9% to Cold Resistance')
  expect(card()).not.toHaveTextContent('maximum Life')
  expect(screen.getByLabelText('2 Exalted Orb')).toBeVisible()
  fireEvent.click(within(secondHistory).getByRole('button', { name: 'Step 1' }))
  expect(card()).toHaveTextContent('+13 to maximum Life')
  expect(card()).not.toHaveTextContent('Cold Resistance')
  expect(second).toHaveAttribute('aria-pressed', 'true')
})

it('shows already-complete and incomplete empty results distinctly', async () => {
  await setLocale('en')
  serve({
    cards: [{ options: [] }],
    states: [],
    routes: [
      { cardIndex: 0, steps: [], order: [], probability: 1, stateIndices: [] },
    ],
    complete: true,
  })
  const first = show()
  expect(
    await screen.findByRole('button', { name: /Goal already reached.*100%/ }),
  ).toBeInTheDocument()
  expect(
    screen.getByRole('button', { name: 'Previous crafting step' }),
  ).toBeDisabled()
  expect(
    screen.getByRole('button', { name: 'Next crafting step' }),
  ).toBeDisabled()
  first.unmount()
  serve({ cards: [], routes: [], states: [], complete: false })
  show()
  expect(await screen.findByText('Not fully calculated')).toBeInTheDocument()
  expect(
    screen.queryByText('No matching crafting route found.'),
  ).not.toBeInTheDocument()
})

it('keeps a zero success chance distinct from an unknown chance', async () => {
  await setLocale('en')
  const route = {
    cardIndex: 0,
    order: [1],
    stateIndices: [0],
    steps: [{ action: 'EXALTED', omens: [], acquired: [1], lost: [] }],
  }
  serve({
    cards: [{ options: [{ number: 1, modifierIds: ['life'] }] }],
    routes: [
      { ...route, probability: 0 },
      { ...route, probability: null },
    ],
    states: [observed(15, null)],
    complete: false,
  })
  show()
  const list = await screen.findByRole('list', { name: 'Crafting methods' })
  expect(
    within(list).getByRole('button', { name: /1 ex · 0%/ }),
  ).toBeInTheDocument()
  expect(
    within(list).getByRole('button', { name: /1 ex · Not yet determined/ }),
  ).toBeInTheDocument()
})

it('retains the reported probability when an example state is unavailable', async () => {
  await setLocale('en')
  serve({
    cards: [{ options: [{ number: 1, modifierIds: ['life'] }] }],
    states: [],
    routes: [
      {
        cardIndex: 0,
        order: [1],
        probability: 0.5,
        stateIndices: [0],
        steps: [{ action: 'EXALTED', omens: [], acquired: [1], lost: [] }],
      },
    ],
    complete: true,
  })
  show()
  const list = await screen.findByRole('list', { name: 'Crafting methods' })
  expect(within(list).getByRole('button', { name: /1 ex · 50%/ })).toBeVisible()
  expect(
    screen.queryByRole('article', { name: 'Item card' }),
  ).not.toBeInTheDocument()
  expect(screen.getByRole('status')).toHaveTextContent('Not fully calculated')
})

it('reevaluates only the selected grades on the original method and keeps the choice in its overview URL', async () => {
  await setLocale('en')
  const step = {
    action: 'EXALTED',
    omens: [],
    acquired: [1],
    lost: [],
    chance: { probability: 0.4 },
  }
  const last = { ...step, acquired: [2] }
  const route = {
    cardIndex: 0,
    order: [1, 2],
    steps: [step, last],
    stateIndices: [0, 1],
    probability: 0.2,
  }
  const base = {
    cards: [
      {
        options: [
          { number: 1, modifierIds: ['life'] },
          { number: 2, modifierIds: ['cold'] },
        ],
      },
    ],
    states: [observed(10, null), observed(10, 5)],
    routes: [route],
    complete: false,
  }
  const upgraded = {
    ...base,
    states: [observed(19, null), observed(19, 11)],
    routes: [
      {
        ...route,
        steps: [
          { ...step, action: 'PERFECT_EXALTED', expectedUses: 2 },
          { ...last },
        ],
        probability: 0.6,
      },
    ],
  }
  const response = (body: AcquisitionResult) =>
    new Response(JSON.stringify(body), {
      headers: { [rulesetHeader]: request.start.provenance.rulesetIdentity },
    })
  let finish: ((result: Response) => void) | undefined
  const post = vi.fn(async (input: RequestInfo | URL) =>
    String(input).endsWith('/grade')
      ? new Promise<Response>((resolve) => {
          finish = resolve
        })
      : response(base),
  )
  vi.stubGlobal('fetch', post)
  window.history.replaceState(null, '', '/support-test')
  try {
    show()
    const list = await screen.findByRole('list', { name: 'Crafting methods' })
    expect(within(list).getAllByRole('listitem')).toHaveLength(1)
    const method = within(list).getByRole('button', { pressed: true })
    expect(method).toHaveAccessibleName(/2 ex · 20%/)
    const history = screen.getByRole('region', { name: 'History' })
    fireEvent.click(within(history).getByRole('button', { name: 'Step 1' }))
    const trigger = within(history).getByRole('button', {
      name: 'Step 1 · Exalted Orb',
    })
    expect(trigger.parentElement?.closest('button')).toBeNull()
    fireEvent.keyDown(trigger, { key: 'ArrowDown' })
    expect(
      within(history).getByRole('button', { name: 'Exalted Orb' }),
    ).toHaveFocus()
    fireEvent.keyDown(trigger, { key: 'Escape' })
    expect(trigger).toHaveFocus()
    fireEvent.click(trigger)
    const perfect = within(history).getByRole('button', {
      name: 'Perfect Exalted Orb',
    })
    expect(perfect.querySelector('svg')).toBeInTheDocument()
    fireEvent.click(perfect)
    await waitFor(() => expect(post).toHaveBeenCalledTimes(2))
    const [path, init] = (
      post.mock.calls as unknown as [string, RequestInit][]
    )[1]!
    expect(path).toBe('/api/v1/crafting/support/goal-filters/test-routes/grade')
    expect(JSON.parse(String(init.body))).toEqual({
      item: request.start.item,
      goal: request.goal,
      cardIndex: 0,
      steps: [
        { action: 'PERFECT_EXALTED', omens: [], acquired: [1], lost: [] },
        { action: 'EXALTED', omens: [], acquired: [2], lost: [] },
      ],
    })
    expect(screen.queryByRole('article', { name: 'Item card' })).toBeNull()
    expect(screen.queryByText(/Route success chance · 20%/)).toBeNull()
    expect(method).not.toHaveAccessibleName(/2 ex|20%/)
    expect(new URLSearchParams(location.search).get('route')).toBe('0')
    expect(new URLSearchParams(location.search).get('grades')).toBe('3.1')
    finish!(response(upgraded))
    const card = await screen.findByRole('article', { name: 'Item card' })
    expect(card).toHaveTextContent('+19 to maximum Life')
    expect(card).not.toHaveTextContent('Cold Resistance')
    expect(screen.getByText(/^Added .*maximum Life$/)).toBeVisible()
    expect(
      screen.getByRole('group', { name: 'Crafting cost through this step' }),
    ).toHaveTextContent('Step chance 40%')
    expect(method).toHaveAccessibleName(/11 ex · 60%/)
    fireEvent.click(within(history).getByRole('button', { name: 'Step 2' }))
    expect(card).toHaveTextContent('+11% to Cold Resistance')
    expect(screen.getByText(/^Added .*Cold Resistance$/)).toBeVisible()
    expect(
      within(history).getByRole('link', { name: 'Session overview' }),
    ).toHaveAttribute(
      'href',
      '/support-test/overview?request=' +
        encodeURIComponent(request.clientRequestId) +
        '&route=0&grades=3.1',
    )
    fireEvent.click(
      within(history).getByRole('button', {
        name: 'Step 1 · Perfect Exalted Orb',
      }),
    )
    fireEvent.click(
      within(history).getByRole('button', { name: 'Exalted Orb' }),
    )
    expect(method).toHaveAccessibleName(/2 ex · 20%/)
    expect(card).toHaveTextContent('+5% to Cold Resistance')
    expect(new URLSearchParams(location.search).has('grades')).toBe(false)
    expect(post).toHaveBeenCalledTimes(2)
  } finally {
    window.history.replaceState(null, '', '/')
  }
})

it.each([true, false])(
  'keeps an unavailable grade editable when the evaluation is complete=%s',
  async (complete) => {
    await setLocale('en')
    const step = { action: 'EXALTED', omens: [], acquired: [1], lost: [] }
    const base = {
      cards: [{ options: [{ number: 1, modifierIds: ['life'] }] }],
      states: [observed(15, null)],
      routes: [
        {
          cardIndex: 0,
          order: [1],
          steps: [step],
          stateIndices: [0],
          probability: 0.5,
        },
      ],
      complete: true,
    }
    vi.stubGlobal(
      'fetch',
      vi.fn(
        async (input: RequestInfo | URL) =>
          new Response(
            JSON.stringify(
              String(input).endsWith('/grade')
                ? { ...base, routes: [], states: [], complete }
                : base,
            ),
            {
              headers: {
                [rulesetHeader]: request.start.provenance.rulesetIdentity,
              },
            },
          ),
      ),
    )
    show()
    const history = await screen.findByRole('region', { name: 'History' })
    fireEvent.click(
      within(history).getByRole('button', { name: 'Step 1 · Exalted Orb' }),
    )
    fireEvent.click(
      within(history).getByRole('button', { name: 'Greater Exalted Orb' }),
    )
    const banner = await screen.findByRole('status')
    await waitFor(() =>
      expect(banner).toHaveTextContent(
        complete ? 'No matching crafting route found.' : 'Not fully calculated',
      ),
    )
    expect(screen.queryByRole('article', { name: 'Item card' })).toBeNull()
    fireEvent.click(
      within(history).getByRole('button', {
        name: 'Step 1 · Greater Exalted Orb',
      }),
    )
    fireEvent.click(
      within(history).getByRole('button', { name: 'Exalted Orb' }),
    )
    expect(
      screen.getByRole('article', { name: 'Item card' }),
    ).toHaveTextContent('+15 to maximum Life')
  },
)
it('retains the method and permits retry when the replay reports a different later action', async () => {
  await setLocale('en')
  const step = { action: 'EXALTED', omens: [], acquired: [1], lost: [] }
  const tail = { ...step, acquired: [2] }
  const route = {
    cardIndex: 0,
    order: [1, 2],
    steps: [step, tail],
    stateIndices: [0, 1],
    probability: 0.5,
  }
  const base = {
    cards: [{ options: [] }],
    states: [observed(15, null), observed(15, 7)],
    routes: [route],
    complete: true,
  }
  let attempts = 0
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: RequestInfo | URL) => {
      const replay = String(input).endsWith('/grade')
      if (replay) attempts++
      const value = replay
        ? {
            ...base,
            routes: [
              {
                ...route,
                steps: [
                  { ...step, action: 'GREATER_EXALTED' },
                  { ...tail, action: attempts === 1 ? 'CHAOS' : 'EXALTED' },
                ],
              },
            ],
          }
        : base
      return new Response(JSON.stringify(value), {
        headers: { [rulesetHeader]: request.start.provenance.rulesetIdentity },
      })
    }),
  )
  show()
  const history = await screen.findByRole('region', { name: 'History' })
  fireEvent.click(
    within(history).getByRole('button', { name: 'Step 1 · Exalted Orb' }),
  )
  fireEvent.click(
    within(history).getByRole('button', { name: 'Greater Exalted Orb' }),
  )
  const error = await screen.findByRole('alert')
  expect(screen.queryByRole('article', { name: 'Item card' })).toBeNull()
  fireEvent.click(within(error).getByRole('button', { name: 'Retry' }))
  expect(
    await screen.findByRole('article', { name: 'Item card' }),
  ).toHaveTextContent('+7% to Cold Resistance')
  expect(attempts).toBe(2)
})

it('labels both socket replacement choices with the localized material and one-based socket number', async () => {
  await setLocale('ko')
  serve({
    cards: [{ options: [{ number: 1, modifierIds: ['life'] }] }],
    states: [observed(null, null), observed(15, null)],
    routes: [0, 1].map((socketIndex) => ({
      stateIndices: [0, 1],
      cardIndex: 0,
      order: [1],
      probability: null,
      steps: [
        {
          action: 'Astrids_Creativity',
          socketIndex,
          omens: [],
          acquired: [],
          lost: [],
        },
        { action: 'EXALTED', omens: [], acquired: [1], lost: [] },
      ],
    })),
    complete: true,
  })
  show()
  const routes = await screen.findByRole('list', { name: '제작 방법' })
  const [first, second] = within(routes).getAllByRole('button')
  expect(first).toHaveTextContent('아스트리드의 창의성 (소켓 1)')
  expect(second).toHaveTextContent('아스트리드의 창의성 (소켓 2)')
  fireEvent.click(second!)
  const summary = screen.getByText('옵션 획득 순서', { selector: 'summary' })
  fireEvent.click(summary)
  const details = summary.closest('details')!
  const steps = within(details).getAllByRole('listitem')
  expect(steps[0]).toHaveTextContent(/^아스트리드의 창의성 \(소켓 2\)$/)
  expect(steps[1]).not.toHaveTextContent('소켓')
})

it('rejects a response from another ruleset', async () => {
  vi.stubGlobal(
    'fetch',
    vi.fn(
      async () => new Response('{}', { headers: { [rulesetHeader]: 'other' } }),
    ),
  )
  await expect(
    loadAcquisitionRoutes(request, new AbortController().signal),
  ).rejects.toThrow()
})
