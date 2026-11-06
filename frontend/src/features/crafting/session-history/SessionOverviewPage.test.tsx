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
    client.setQueryData(['history-catalog', 'solar', 82], initialFixture)
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
