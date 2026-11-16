import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { expect, it, vi } from 'vitest'
import { SupportRagTestPage } from './SupportRagTestPage'
import { jsonResponse } from '../../../shared/test/craftingFixtures'

vi.mock('../../../shared/site/SiteHeader', () => ({
  SiteHeader: () => <header>Site header</header>,
}))
vi.mock('../goal-filter/CraftStart', () => ({
  CraftStart: ({
    persistDraft,
    startLabel,
  }: {
    persistDraft: boolean
    startLabel: string
  }) => (
    <div data-testid="editor" data-persist={String(persistDraft)}>
      {startLabel}
    </div>
  ),
}))
function show(client = new QueryClient()) {
  return {
    client,
    ...render(
      <QueryClientProvider client={client}>
        <SupportRagTestPage />
      </QueryClientProvider>,
    ),
  }
}
it('opens the transient editor without credentials, caches status and makes no paid request', async () => {
  const fetcher = vi.fn<
    (url: RequestInfo | URL, init?: RequestInit) => Promise<Response>
  >(async () =>
    jsonResponse({
      enabled: true,
      configured: true,
      model: 'test',
      corpusVersion: 'v1',
    }),
  )
  vi.stubGlobal('fetch', fetcher)
  const store = vi.spyOn(Storage.prototype, 'setItem')
  const page = show()
  expect(screen.getByTestId('editor')).toHaveAttribute('data-persist', 'false')
  expect(document.querySelector('input[type=password]')).toBeNull()
  await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(1))
  expect(fetcher.mock.calls[0]?.[0]).toBe('/api/support-test/rag')
  expect(fetcher.mock.calls[0]?.[1]?.headers).not.toHaveProperty(
    'Authorization',
  )
  await waitFor(() => expect(page.client.isFetching()).toBe(0))
  page.unmount()
  show(page.client)
  expect(fetcher).toHaveBeenCalledTimes(1)
  expect(store).not.toHaveBeenCalled()
  store.mockRestore()
})
it('allows input before server configuration and offers a free status recheck', async () => {
  const fetcher = vi.fn(async () =>
    jsonResponse({
      enabled: false,
      configured: false,
      model: 'test',
      corpusVersion: 'v1',
    }),
  )
  vi.stubGlobal('fetch', fetcher)
  show()
  expect(screen.getByTestId('editor')).toBeVisible()
  await screen.findByText(/Set SUPPORT_RAG_ENABLED/)
  fireEvent.click(
    screen.getByRole('button', { name: 'Check connection again' }),
  )
  await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(2))
})
