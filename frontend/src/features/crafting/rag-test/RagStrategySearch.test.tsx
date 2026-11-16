import { StrictMode } from 'react'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { expect, it, vi } from 'vitest'
import { requestFixture } from '../path-tree/fixtures.test-support'
import { RagStrategySearch } from './RagStrategySearch'
import { RagAvailabilityContext } from './availabilityContext'
import { jsonResponse } from '../../../shared/test/craftingFixtures'
import { ragMessages } from './messages'
import { locales } from '../../../shared/i18n/i18n'
const preview = {
  promptId: 'prepared-id',
  prompt: {
    system: 'Reviewed rules only.',
    user: '{"documents":["retrieved knowledge"],"goal":"projectile +3"}',
  },
}
const response = {
  status: 'COMPLETE',
  strategies: [
    {
      initial: [
        {
          currency: 'EXALTED',
          omens: [],
          purpose: 'TEMPORARY_AFFIX_FOR_REMOVAL',
        },
      ],
      cycle: [{ currency: 'CHAOS', omens: [], purpose: 'GOAL_ACQUISITION' }],
      documents: ['support.common-policy'],
      explanation: '<script>do not execute</script> Prepare once, then repeat.',
      preparationSuccess: 'STOP_ON_FIRST_HIT',
    },
  ],
}

function show(ready = true) {
  const props = {
    request: requestFixture(),
    inputGeneration: 1,
    onRecalculate: vi.fn(),
    presentItem: vi.fn(),
  }
  const client = new QueryClient()
  return render(
    <StrictMode>
      <QueryClientProvider client={client}>
        <RagAvailabilityContext.Provider value={ready}>
          <RagStrategySearch {...props} />
        </RagAvailabilityContext.Provider>
      </QueryClientProvider>
    </StrictMode>,
  )
}
const api = (
  generate: (init?: RequestInit) => Promise<Response> = async () =>
    jsonResponse(response),
) => {
  const fetcher = vi.fn<
    (url: RequestInfo | URL, init?: RequestInit) => Promise<Response>
  >(async (url, init) =>
    String(url).endsWith('/prompt') ? jsonResponse(preview) : generate(init),
  )
  vi.stubGlobal('fetch', fetcher)
  return fetcher
}
async function readyButton() {
  const button = screen.getByRole('button', { name: 'Request strategies' })
  await waitFor(() => expect(button).toBeEnabled())
  return button
}
it('prepares only once in StrictMode, shows the exact prompt, and calls AI only on an explicit click', async () => {
  const fetcher = api()
  show()
  const button = await readyButton()
  expect(fetcher).toHaveBeenCalledTimes(1)
  expect(fetcher.mock.calls[0]?.[0]).toBe('/api/support-test/rag/prompt')
  const body = JSON.parse(String(fetcher.mock.calls[0]?.[1]?.body))
  expect(body.request.version).toBe(2)
  expect(body.request.observations).toBeUndefined()
  expect(body.mode).toBe('RETRIEVED')
  expect(body.locale).toBe('en')
  expect(screen.getByRole('textbox', { name: 'Prompt to send' })).toHaveValue(
    `SYSTEM\n${preview.prompt.system}\n\nUSER\n${preview.prompt.user}`,
  )
  fireEvent.click(button)
  fireEvent.click(
    screen.getByRole('button', { name: 'Generating strategies…' }),
  )
  await screen.findByText(response.strategies[0]!.explanation)
  expect(fetcher).toHaveBeenCalledTimes(2)
  expect(fetcher.mock.calls[1]?.[0]).toBe('/api/support-test/rag/generate')
  expect(JSON.parse(String(fetcher.mock.calls[1]?.[1]?.body))).toEqual({
    promptId: preview.promptId,
  })
  expect(document.querySelector('.rag-results-layout script')).toBeNull()
  expect(document.querySelector('.rag-results-layout .item-card')).toBeNull()
  expect(screen.queryByText(/Engine comparison/)).not.toBeInTheDocument()
  expect(screen.queryByRole('combobox')).not.toBeInTheDocument()
})
it('disables requests without server configuration', () => {
  const fetcher = api()
  show(false)
  expect(
    screen.getByRole('button', { name: 'Request strategies' }),
  ).toBeDisabled()
  expect(fetcher).not.toHaveBeenCalled()
})
it('aborts an abandoned model request and discards its late response', async () => {
  let finish: ((response: Response) => void) | undefined
  const fetcher = api(
    () =>
      new Promise<Response>((resolve) => {
        finish = resolve
      }),
  )
  const page = show()
  fireEvent.click(await readyButton())
  const signal = fetcher.mock.calls[1]![1]!.signal!
  page.unmount()
  expect(signal.aborted).toBe(true)
  await act(async () => finish?.(jsonResponse(response)))
  expect(
    screen.queryByText(response.strategies[0]!.explanation),
  ).not.toBeInTheDocument()
})
it('shows the error code and never automatically retries a failed paid request', async () => {
  const fetcher = api(
    async () =>
      new Response(JSON.stringify({ code: 'RAG_PROVIDER_RATE_LIMIT' }), {
        status: 502,
      }),
  )
  show()
  fireEvent.click(await readyButton())
  expect(await screen.findByRole('alert')).toHaveTextContent(
    'RAG_PROVIDER_RATE_LIMIT',
  )
  expect(fetcher).toHaveBeenCalledTimes(2)
})
it('requires a new manual request after refreshing an expired prompt', async () => {
  const fetcher = api(
    async () =>
      new Response(JSON.stringify({ code: 'RAG_PROMPT_EXPIRED' }), {
        status: 410,
      }),
  )
  show()
  fireEvent.click(await readyButton())
  await screen.findByRole('alert')
  fireEvent.click(screen.getByRole('button', { name: 'Reload prompt' }))
  await readyButton()
  expect(fetcher).toHaveBeenCalledTimes(3)
  expect(fetcher.mock.calls[2]?.[0]).toBe('/api/support-test/rag/prompt')
})
it('provides every test-screen message in all eight locales', () => {
  const keys = Object.keys(ragMessages.en).sort()
  for (const locale of locales) {
    expect(Object.keys(ragMessages[locale]).sort()).toEqual(keys)
    expect(
      Object.values(ragMessages[locale]).every((v) => v.trim().length > 0),
    ).toBe(true)
  }
})
