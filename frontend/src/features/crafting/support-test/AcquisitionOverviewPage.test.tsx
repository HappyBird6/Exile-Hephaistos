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

function saveRequest() {
  saveSupport('support-test-start', {
    base: 'solar',
    text: '',
    item: null,
    rulesetIdentity: null,
    abstractQuality40: false,
    editing: false,
    started: true,
    levelEdited: false,
    searchRequest: request,
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

it('rebuilds the requested route from the saved input when the overview is reloaded', async () => {
  saveRequest()
  const result = {
    complete: true,
    cards: [
      { options: [] },
      { options: [{ number: 1, modifierIds: ['life'] }] },
    ],
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
