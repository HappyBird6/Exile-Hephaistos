import { act, fireEvent, render, screen } from '@testing-library/react'
import { expect, it, vi } from 'vitest'
import { requestFixture } from '../path-tree/fixtures.test-support'
import { RagStrategySearch } from './RagStrategySearch'
import { RagAvailabilityContext } from './availabilityContext'
import { jsonResponse } from '../../../shared/test/craftingFixtures'
import { ragMessages } from './messages'
import { locales } from '../../../shared/i18n/i18n'

const report = {
  model: 'test-model',
  corpusVersion: 'test-corpus',
  proposalStatus: 'COMPLETE',
  candidateMatch: false,
  executionMatch: false,
  reference: [{}],
  missing: [0],
  extra: [0],
  validationErrors: [],
  documents: ['support.common-policy'],
  inputTokens: 10,
  outputTokens: 20,
  cached: false,
  elapsedMillis: 10,
  executions: [],
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
  const request = requestFixture()
  const props = {
    request,
    inputGeneration: 1,
    onRecalculate: vi.fn(),
    presentItem: () => ({
      name: 'Solar Amulet',
      base: 'Solar Amulet',
      rarity: 'RARE' as const,
      itemClass: 'Amulets',
      itemLevel: 82,
      properties: [],
      requirements: [],
      modifiers: [],
      flags: [],
    }),
  }
  const rendered = render(
    <RagAvailabilityContext.Provider value={ready}>
      <RagStrategySearch {...props} />
    </RagAvailabilityContext.Provider>,
  )
  return { ...rendered, props }
}
it('makes no paid request on mount and sends exactly one explicit request without legacy observations', async () => {
  const fetcher = vi.fn<
    (url: RequestInfo | URL, init?: RequestInit) => Promise<Response>
  >(async () => jsonResponse(report))
  vi.stubGlobal('fetch', fetcher)
  show()
  expect(fetcher).not.toHaveBeenCalled()
  fireEvent.click(screen.getByRole('button', { name: 'Generate strategies' }))
  fireEvent.click(
    screen.getByRole('button', { name: 'Generating strategies…' }),
  )
  await screen.findByText(report.strategies[0]!.explanation)
  expect(fetcher).toHaveBeenCalledTimes(1)
  const body = JSON.parse(String(fetcher.mock.calls[0]?.[1]?.body))
  expect(body.request.version).toBe(2)
  expect(body.request.observations).toBeUndefined()
  expect(body.mode).toBe('ALL')
  expect(body.locale).toBe('en')
  expect(document.querySelector('.rag-results script')).toBeNull()
  expect(screen.getByText(/Differences found/)).toBeVisible()
})
it('disables generation without server configuration', () => {
  const fetcher = vi.fn()
  vi.stubGlobal('fetch', fetcher)
  show(false)
  expect(
    screen.getByRole('button', { name: 'Generate strategies' }),
  ).toBeDisabled()
  expect(fetcher).not.toHaveBeenCalled()
})
it('aborts an abandoned comparison and does not show its late response', async () => {
  let finish: ((response: Response) => void) | undefined
  const fetcher = vi.fn<
    (url: RequestInfo | URL, init?: RequestInit) => Promise<Response>
  >(
    () =>
      new Promise<Response>((resolve) => {
        finish = resolve
      }),
  )
  vi.stubGlobal('fetch', fetcher)
  const page = show()
  fireEvent.click(screen.getByRole('button', { name: 'Generate strategies' }))
  const signal = fetcher.mock.calls[0]![1]!.signal!
  page.unmount()
  expect(signal.aborted).toBe(true)
  await act(async () => finish?.(jsonResponse(report)))
  expect(
    screen.queryByText(report.strategies[0]!.explanation),
  ).not.toBeInTheDocument()
})
it('reports failures and never automatically retries paid requests', async () => {
  const fetcher = vi.fn(async () => new Response('', { status: 401 }))
  vi.stubGlobal('fetch', fetcher)
  show()
  fireEvent.click(screen.getByRole('button', { name: 'Generate strategies' }))
  await screen.findByRole('alert')
  expect(fetcher).toHaveBeenCalledTimes(1)
})
it('clears prior results when retrieval or stage changes', async () => {
  vi.stubGlobal(
    'fetch',
    vi.fn(async () => jsonResponse(report)),
  )
  show()
  fireEvent.click(screen.getByRole('button', { name: 'Generate strategies' }))
  await screen.findByText(report.strategies[0]!.explanation)
  fireEvent.change(screen.getByLabelText('Documents'), {
    target: { value: 'RETRIEVED' },
  })
  expect(
    screen.queryByText(report.strategies[0]!.explanation),
  ).not.toBeInTheDocument()
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
