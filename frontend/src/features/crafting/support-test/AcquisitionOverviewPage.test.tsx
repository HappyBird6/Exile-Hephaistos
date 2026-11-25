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
      <p>
        {source.film.frames.at(-1)?.projectedCurrencies?.[0]?.currencyId ??
          source.film.frames.at(-1)?.projectedWork}
      </p>
      {source.film.frames.at(-1)?.projectedCurrencies?.[0]?.quantity && (
        <p>×{source.film.frames.at(-1)?.projectedCurrencies?.[0]?.quantity}</p>
      )}
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

it.each(['total', 'NOT'])(
  'keeps a saved overview from recomputing a retired %s goal',
  async (retired) => {
    const legacy = structuredClone(request)
    if (retired === 'NOT') legacy.goal.groups[0]!.type = 'NOT'
    else
      legacy.goal.groups[0]!.entries[0]!.statId =
        'hephaistos:v1:pseudo:total_fire_resistance'
    saveRequest(legacy)
    const fetch = vi.fn()
    vi.stubGlobal('fetch', fetch)
    await openOverview()
    expect(screen.getByRole('status')).toHaveTextContent('Not available')
    expect(fetch).not.toHaveBeenCalled()
  },
)

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

it.each([
  ['EXALTED', 'Exalted_Orb'],
  ['PERFECT_EXALTED', 'Perfect_Exalted_Orb'],
])(
  'restores the original %s grade from its base route when no grade override is saved',
  async (action, material) => {
    saveRequest()
    const base = structuredClone(result)
    base.routes[1]!.steps[0]!.action = action
    const fetch = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(base), {
        headers: { [rulesetHeader]: request.start.provenance.rulesetIdentity },
      }),
    )
    vi.stubGlobal('fetch', fetch)
    await openOverview()
    expect(
      await screen.findByRole('heading', { name: 'Target 2 · 1' }),
    ).toBeVisible()
    expect(screen.getByText('2 frames')).toBeVisible()
    expect(screen.getByText(material)).toBeVisible()
    expect(fetch).toHaveBeenCalledTimes(1)
  },
)

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

it.each([
  ['EXALTED', 'PERFECT_EXALTED', '3', 'Perfect_Exalted_Orb'],
  ['PERFECT_EXALTED', 'EXALTED', '1', 'Exalted_Orb'],
])(
  'replays %s as %s after a reload and returns to that exact selection',
  async (original, override, grade, material) => {
    saveRequest()
    const originalResult = structuredClone(result)
    originalResult.routes[1]!.steps[0]!.action = original
    const action = { ...result.routes[1]!.steps[0]!, action: override }
    const alternative = {
      ...result,
      cards: [result.cards[1]],
      routes: [{ ...result.routes[1], cardIndex: 0, steps: [action] }],
    }
    const fetch = vi.fn((url: string) =>
      Promise.resolve(
        new Response(
          JSON.stringify(url.endsWith('/grade') ? alternative : originalResult),
          {
            headers: {
              [rulesetHeader]: request.start.provenance.rulesetIdentity,
            },
          },
        ),
      ),
    )
    vi.stubGlobal('fetch', fetch)
    await openOverview(href + '&grades=' + grade)
    expect(await screen.findByText(material)).toBeVisible()
    expect(screen.getByRole('heading', { name: 'Target 2 · 1' })).toBeVisible()
    expect(screen.getByRole('link', { name: 'Return' })).toHaveAttribute(
      'href',
      '/support-test?' + params + '&grades=' + grade,
    )
    expect(fetch.mock.calls.map((call) => call[0])).toEqual([
      '/api/v1/crafting/support/goal-filters/test-routes',
      '/api/v1/crafting/support/goal-filters/test-routes/grade',
    ])
  },
)

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

it.each([undefined, 'Flesh_Catalyst'])(
  'reopens the quality plan including %s across search and fixed grade replay',
  async (qualityCatalyst) => {
    const saved = structuredClone(request)
    saved.goal.general.quality = 40
    if (qualityCatalyst) saved.qualityCatalyst = qualityCatalyst
    saveRequest(saved)
    const action = {
      action: qualityCatalyst ? 'CATALYST_FLESH' : 'QUALITY_40',
      expectedUses: qualityCatalyst ? 40 : null,
      acquired: [],
      lost: [],
      omens: [],
    }
    const baseRoute = result.routes[1]!
    const withQuality = {
      ...baseRoute,
      steps: [...baseRoute.steps, action],
      stateIndices: [0, 0],
    }
    const base = { ...result, routes: [result.routes[0], withQuality] }
    const replay = {
      ...result,
      cards: [result.cards[1]],
      routes: [
        {
          ...withQuality,
          cardIndex: 0,
          steps: [{ ...baseRoute.steps[0], action: 'GREATER_EXALTED' }, action],
        },
      ],
    }
    const fetch = vi.fn(async (url: string, init: RequestInit) => {
      const body = JSON.parse(init.body as string)
      expect(body.goal.general.quality).toBe(40)
      if (qualityCatalyst) expect(body.qualityCatalyst).toBe(qualityCatalyst)
      else expect(body).not.toHaveProperty('qualityCatalyst')
      return new Response(
        JSON.stringify(url.endsWith('/grade') ? replay : base),
        {
          headers: {
            [rulesetHeader]: request.start.provenance.rulesetIdentity,
          },
        },
      )
    })
    vi.stubGlobal('fetch', fetch)
    await openOverview(href + '&grades=2.1')
    expect(await screen.findByText('3 frames')).toBeVisible()
    expect(
      screen.getByText(qualityCatalyst ?? 'UNSPECIFIED_CATALYST_40'),
    ).toBeVisible()
    if (qualityCatalyst) expect(screen.getByText('×40')).toBeVisible()
    expect(fetch).toHaveBeenCalledTimes(2)
  },
)
