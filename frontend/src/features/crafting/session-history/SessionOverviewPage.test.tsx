import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { expect, it } from 'vitest'
import { locales, setLocale } from '../../../shared/i18n/i18n'
import { initialFixture } from '../../../shared/test/craftingFixtures'
import { twoSteps } from '../session-notation/fixtures.test-support'
import { historyStorageKey, LocalFilmRepository } from '../workbenchHistory'
import { SessionOverviewPage } from './SessionOverviewPage'
import { historyMessages } from './messages'
import { sessionTitle } from '../session-library/sessionTitle'

it.each(locales)(
  'keeps %s history read-only with an overview header and no sharing actions',
  async (locale) => {
    setLocale(locale)
    const { history } = await twoSteps()
    const film = history.films[0]!
    new LocalFilmRepository(localStorage).save(history)
    const saved = localStorage.getItem(historyStorageKey)
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false, staleTime: Infinity } },
    })
    client.setQueryData(['session-overview', film.id], film)
    client.setQueryData(
      ['crafting', 'initial', 'solar', 82, 'manual'],
      initialFixture,
    )
    const view = render(
      <QueryClientProvider client={client}>
        <SessionOverviewPage id={film.id} />
      </QueryClientProvider>,
    )
    expect(view.container.querySelector('.wb-session-notation')).toBeNull()
    expect(
      view.container.querySelectorAll('.session-flow details'),
    ).toHaveLength(0)
    expect(view.container.querySelectorAll('[data-flow-edge]')).toHaveLength(2)
    expect(
      screen.queryByRole('button', { name: historyMessages[locale].export }),
    ).not.toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: historyMessages[locale].snapshot }),
    ).toBeVisible()
    expect(screen.getAllByRole('article')).toHaveLength(3)
    expect(
      view.container.querySelector(
        '.history-heading, .workspace-nav, .locale-selector, .wb-base-image',
      ),
    ).toBeNull()
    expect(view.container.querySelector('header h1')).toHaveTextContent(
      sessionTitle(film, locale),
    )
    for (const paragraph of view.container.querySelectorAll('main > p'))
      expect(paragraph).not.toHaveTextContent(film.rulesetIdentity!)
    expect(view.container.querySelector('.history-share-confirm')).toBeNull()
    expect(localStorage.getItem(historyStorageKey)).toBe(saved)
    view.unmount()
    client.clear()
  },
)

it.each(['simulation', 'history'] as const)(
  'returns to its %s entry point',
  async (origin) => {
    window.history.replaceState({}, '', `/history/saved?from=${origin}`)
    setLocale('en')
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })
    const view = render(
      <QueryClientProvider client={client}>
        <SessionOverviewPage id="saved" />
      </QueryClientProvider>,
    )
    const href = screen
      .getByRole('link', { name: 'Workbench' })
      .getAttribute('href')!
    expect(new URL(href, location.origin).searchParams.get('workbench')).toBe(
      origin,
    )
    if (origin === 'history')
      expect(new URL(href, location.origin).searchParams.get('session')).toBe(
        'saved',
      )
    view.unmount()
    client.clear()
    window.history.replaceState({}, '', '/')
  },
)

it('places the full-session spending to the left of the state count and keeps snapshot metadata within the flow', async () => {
  setLocale('en')
  const { history } = await twoSteps(),
    film = history.films[0]!
  film.title = 'A saved craft'
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false, staleTime: Infinity } },
  })
  client.setQueryData(['session-overview', film.id], film)
  client.setQueryData(
    ['crafting', 'initial', 'solar', 82, 'manual'],
    initialFixture,
  )
  client.setQueryData(['economy', 'prices'], {
    snapshot: {
      league: 'Test',
      updatedAt: '2026-11-10T00:00:00Z',
      exaltedPerDivine: 500,
      prices: {
        'Orb of Transmutation': {
          name: 'Orb of Transmutation',
          exalted: 1,
          divine: 0.002,
        },
        'Orb of Augmentation': {
          name: 'Orb of Augmentation',
          exalted: 2,
          divine: 0.004,
        },
      },
    },
    unavailable: false,
    stale: false,
    nextRefreshAllowedAt: '2026-11-10T00:05:00Z',
  })
  const view = render(
    <QueryClientProvider client={client}>
      <SessionOverviewPage id={film.id} />
    </QueryClientProvider>,
  )
  const summary = view.container.querySelector('.history-session-summary')!
  expect(summary.firstElementChild).toHaveClass('crafting-cost-line')
  expect(summary.lastElementChild).toHaveClass('history-state-count')
  expect(summary.querySelectorAll('img')).toHaveLength(2)
  expect(
    summary.querySelector('[aria-label="3 Exalted Orb"]'),
  ).toBeInTheDocument()
  const captured = view.container.querySelector('.session-flow-summary')!
  expect(captured).toHaveTextContent('A saved craft')
  expect(
    captured.querySelector('[aria-label="3 Exalted Orb"]'),
  ).toBeInTheDocument()
  view.unmount()
  client.clear()
})

it('renders a public source without reading or importing any private session', async () => {
  const { vi } = await import('vitest')
  const { IndexedFilmRepository } =
    await import('../session-library/IndexedFilmRepository')
  const load = vi.spyOn(IndexedFilmRepository.prototype, 'load')
  const { history } = await twoSteps(),
    film = history.films[0]!
  const before = localStorage.getItem(historyStorageKey)
  setLocale('en')
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false, staleTime: Infinity } },
  })
  client.setQueryData(
    ['crafting', 'initial', 'solar', 82, 'manual'],
    initialFixture,
  )
  const view = render(
    <QueryClientProvider client={client}>
      <SessionOverviewPage
        id="public-post"
        source={{
          film,
          title: 'Published craft',
          backHref: '/community/post/public-post?from=%3Fsort%3Dpopular',
          backLabel: 'View details',
        }}
      />
    </QueryClientProvider>,
  )
  expect(screen.getByRole('heading', { name: 'Published craft' })).toBeVisible()
  expect(screen.getByRole('link', { name: 'View details' })).toHaveAttribute(
    'href',
    '/community/post/public-post?from=%3Fsort%3Dpopular',
  )
  expect(view.container.querySelectorAll('[data-flow-state]')).toHaveLength(
    film.frames.length,
  )
  expect(load).not.toHaveBeenCalled()
  expect(localStorage.getItem(historyStorageKey)).toBe(before)
  view.unmount()
  client.clear()
  load.mockRestore()
})
