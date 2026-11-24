import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { setLocale } from '../../../shared/i18n/i18n'
import { saveSupport } from '../method-search/supportStorage'
import { requestFixture } from '../path-tree/fixtures.test-support'
import { rulesetHeader } from '../rulesetIdentity'
import type { Film } from '../workbenchHistory'
import AcquisitionOverviewPage from './AcquisitionOverviewPage'

vi.mock('../session-history/SessionOverviewPage', () => ({
  SessionOverviewPage: ({
    source,
  }: {
    source: { film: Film; backHref: string }
  }) => (
    <>
      <h1>{source.film.title}</h1>
      <p>{source.film.frames.length} frames</p>
      <p>{source.film.frames.at(-1)?.projectedCurrencies?.[0]?.currencyId}</p>
      <a href={source.backHref}>Return</a>
    </>
  ),
}))

const request = requestFixture()
const params = new URLSearchParams({
  request: request.clientRequestId,
  route: '1',
})
const href = '/support-test/overview?' + params

afterEach(() => {
  window.history.replaceState(null, '', '/')
  localStorage.clear()
  vi.unstubAllGlobals()
})

function saveRequest(input = request) {
  saveSupport('support-test-start', {
    base: 'solar',
    text: '',
    item: null,
    rulesetIdentity: null,
    abstractQuality40: false,
    editing: false,
    started: true,
    levelEdited: false,
    searchRequest: input,
  })
}

async function openOverview(url = href, client = new QueryClient()) {
  await setLocale('en')
  window.history.replaceState(null, '', url)
  return render(
    <QueryClientProvider client={client}>
      <AcquisitionOverviewPage />
    </QueryClientProvider>,
  )
}

it('opens the selected session directly from its cached film', async () => {
  const client = new QueryClient()
  client.setQueryData(['plan-film', href], {
    id: 'selected',
    title: 'Selected session',
    frames: [],
  })
  const fetch = vi.fn()
  vi.stubGlobal('fetch', fetch)
  await openOverview(href, client)
  expect(
    screen.getByRole('heading', { name: 'Selected session' }),
  ).toBeVisible()
  expect(screen.getByRole('link', { name: 'Return' })).toHaveAttribute(
    'href',
    '/support-test?' + params,
  )
  expect(fetch).not.toHaveBeenCalled()
})

it('keeps a saved total overview from recomputing a retired goal', async () => {
  const legacy = structuredClone(request)
  legacy.goal.groups[0]!.entries[0]!.statId =
    'hephaistos:v1:pseudo:total_fire_resistance'
  saveRequest(legacy)
  const fetch = vi.fn()
  vi.stubGlobal('fetch', fetch)
  await openOverview()
  expect(screen.getByRole('status')).toHaveTextContent('Not available')
  expect(fetch).not.toHaveBeenCalled()
})

const result = {
  complete: true,
  cards: [{ options: [] }, { options: [{ number: 1, modifierIds: ['life'] }] }],
  states: [request.start.item],
  routes: [
    { cardIndex: 0, order: [], probability: 1, steps: [], stateIndices: [] },
    {
      cardIndex: 1,
      order: [1],
      probability: 0.5,
      steps: [{ action: 'EXALTED', omens: [], acquired: [1], lost: [] }],
      stateIndices: [0],
    },
  ],
}

it('rebuilds the requested route from the saved input when the overview is reloaded', async () => {
  saveRequest()
  const fetch = vi.fn().mockResolvedValue(
    new Response(JSON.stringify(result), {
      headers: { [rulesetHeader]: request.start.provenance.rulesetIdentity },
    }),
  )
  vi.stubGlobal('fetch', fetch)
  await openOverview()
  expect(
    await screen.findByRole('heading', { name: 'Target 2 · 1' }),
  ).toBeVisible()
  expect(screen.getByText('2 frames')).toBeVisible()
  expect(fetch).toHaveBeenCalledTimes(1)
})

it.each(['request=older&route=0', 'request=older&route=-1'])(
  'never substitutes a different saved session for %s',
  async (search) => {
    saveRequest()
    const fetch = vi.fn()
    vi.stubGlobal('fetch', fetch)
    await openOverview('/support-test/overview?' + search)
    expect(screen.getByRole('status')).toHaveTextContent('Not available')
    expect(fetch).not.toHaveBeenCalled()
  },
)

it('replays the selected material grade after a reload and returns to that exact selection', async () => {
  saveRequest()
  const action = { ...result.routes[1]!.steps[0]!, action: 'PERFECT_EXALTED' }
  const alternative = {
    ...result,
    cards: [result.cards[1]],
    routes: [{ ...result.routes[1], cardIndex: 0, steps: [action] }],
  }
  const fetch = vi.fn((url: string) =>
    Promise.resolve(
      new Response(
        JSON.stringify(url.endsWith('/grade') ? alternative : result),
        {
          headers: {
            [rulesetHeader]: request.start.provenance.rulesetIdentity,
          },
        },
      ),
    ),
  )
  vi.stubGlobal('fetch', fetch)
  await openOverview(href + '&grades=3')
  expect(await screen.findByText('Perfect_Exalted_Orb')).toBeVisible()
  expect(screen.getByRole('heading', { name: 'Target 2 · 1' })).toBeVisible()
  expect(screen.getByRole('link', { name: 'Return' })).toHaveAttribute(
    'href',
    '/support-test?' + params + '&grades=3',
  )
  expect(fetch.mock.calls.map((call) => call[0])).toEqual([
    '/api/v1/crafting/support/goal-filters/test-routes',
    '/api/v1/crafting/support/goal-filters/test-routes/grade',
  ])
})

it.each([
  [true, 'Not available'],
  [false, 'Not fully calculated'],
] as const)(
  'keeps an empty grade replay explicit (complete=%s)',
  async (complete, message) => {
    saveRequest()
    vi.stubGlobal(
      'fetch',
      vi.fn((url: string) =>
        Promise.resolve(
          new Response(
            JSON.stringify(
              url.endsWith('/grade')
                ? { cards: [], routes: [], states: [], complete }
                : result,
            ),
            {
              headers: {
                [rulesetHeader]: request.start.provenance.rulesetIdentity,
              },
            },
          ),
        ),
      ),
    )
    await openOverview(href + '&grades=3')
    expect(await screen.findByText(message)).toBeVisible()
    expect(screen.queryByText('Exalted_Orb')).toBeNull()
  },
)
