import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
import { expect, it } from 'vitest'
import { locales, setLocale } from '../../../shared/i18n/i18n'
import { initialFixture } from '../../../shared/test/craftingFixtures'
import { twoSteps } from '../session-notation/fixtures.test-support'
import { historyStorageKey, LocalFilmRepository } from '../workbenchHistory'
import { SessionHistoryPage } from './SessionHistoryPage'
import { historyMessages } from './messages'

it.each(locales)(
  'keeps %s session actions and privacy guidance without showing internal identity in the summary',
  async (locale) => {
    setLocale(locale)
    const { history } = await twoSteps()
    const film = history.films[0]!
    new LocalFilmRepository(localStorage).save(history)
    const saved = localStorage.getItem(historyStorageKey)
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false, staleTime: Infinity } },
    })
    client.setQueryData(['session-history', film.id, false], film)
    client.setQueryData(['history-catalog', 'solar', 82], initialFixture)
    const view = render(
      <QueryClientProvider client={client}>
        <SessionHistoryPage id={film.id} />
      </QueryClientProvider>,
    )
    expect(
      screen.queryByText(historyMessages[locale].privacy),
    ).not.toBeInTheDocument()
    expect(view.container.querySelector('.wb-session-notation')).toBeNull()
    expect(
      view.container.querySelectorAll('.session-flow details'),
    ).toHaveLength(0)
    expect(view.container.querySelectorAll('[data-flow-edge]')).toHaveLength(2)
    expect(
      screen.getByRole('button', { name: historyMessages[locale].export }),
    ).toBeVisible()
    expect(
      screen.getByRole('button', { name: historyMessages[locale].share }),
    ).toBeVisible()
    expect(
      screen.getByRole('button', { name: historyMessages[locale].snapshot }),
    ).toBeVisible()
    expect(screen.getAllByRole('article')).toHaveLength(3)
    for (const paragraph of view.container.querySelectorAll('main > p'))
      expect(paragraph).not.toHaveTextContent(film.rulesetIdentity!)
    fireEvent.click(
      screen.getByRole('button', { name: historyMessages[locale].share }),
    )
    expect(screen.getByText(historyMessages[locale].privacy)).toBeVisible()
    expect(localStorage.getItem(historyStorageKey)).toBe(saved)
    view.unmount()
    client.clear()
  },
)
