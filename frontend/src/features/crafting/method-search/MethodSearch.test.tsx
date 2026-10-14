import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import fixture from '../../../../../contracts/crafting-methods-v2/certified-diamond.json'
import { MethodSearch } from './MethodSearch'
import { normalizeView } from './certifiedApi'
import type { MapView } from './certifiedApi'
import { requestFixture } from '../path-tree/fixtures.test-support'
import { createItemPresentation } from '../path-tree/presentation'
import { setLocale } from '../../../shared/i18n/i18n'

afterEach(() => vi.unstubAllGlobals())
it('keeps unknown goal evaluation distinct from a failed goal', async () => {
  setLocale('en')
  const request = requestFixture()
  request.clientRequestId = fixture.search.clientRequestId
  const unknown = structuredClone(fixture)
  unknown.graph.buckets.forEach((b) => {
    b.goal = 'UNKNOWN'
  })
  vi.stubGlobal(
    'fetch',
    vi.fn(
      async () =>
        new Response(JSON.stringify(unknown), {
          headers: {
            'X-Crafting-Ruleset': request.start.provenance.rulesetIdentity,
          },
        }),
    ),
  )
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  const view = render(
    <QueryClientProvider client={client}>
      <MethodSearch
        request={request}
        inputGeneration={1}
        presentItem={createItemPresentation(
          { name: 'Solar Amulet', itemClass: 'Amulet' },
          {},
        )}
        onRecalculate={() => {}}
      />
    </QueryClientProvider>,
  )
  await waitFor(() =>
    expect(screen.getAllByText('Not fully calculated')).toHaveLength(5),
  )
  expect(screen.queryByText('Goal not reached yet')).not.toBeInTheDocument()
  view.unmount()
  client.clear()
})

it.each(['RUNNING', 'PAUSED'])(
  'honors stop while an automatic compute-lease resume returns %s',
  async (resumedStatus) => {
    setLocale('en')
    const request = requestFixture(),
      value = structuredClone(fixture) as MapView
    request.clientRequestId = value.search.clientRequestId
    value.search.status = 'PAUSED'
    value.search.reason = 'COMPUTE_BUDGET'
    value.search.resumable = true
    value.search.enumerationComplete = false
    value.search.strategies = []
    let releaseResume: (() => void) | undefined
    const operations: string[] = []
    const respond = (body: unknown) =>
      new Response(JSON.stringify(body), {
        headers: {
          'X-Crafting-Ruleset': request.start.provenance.rulesetIdentity,
        },
      })
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string, init?: RequestInit) => {
        if (url.endsWith('/control')) {
          const operation = (
            JSON.parse(String(init?.body)) as { operation: string }
          ).operation
          operations.push(operation)
          if (operation === 'RESUME')
            await new Promise<void>((resolve) => {
              releaseResume = resolve
            })
          value.search.generation++
          value.search.revision++
          value.graph.revision++
          value.search.status =
            operation === 'RESUME' ? resumedStatus : 'CANCELLED'
          value.search.reason = null
          return respond({})
        }
        return respond(value)
      }),
    )
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })
    const view = render(
      <QueryClientProvider client={client}>
        <MethodSearch
          request={request}
          inputGeneration={1}
          presentItem={createItemPresentation(
            { name: 'Solar Amulet', itemClass: 'Amulet' },
            {},
          )}
          onRecalculate={() => {}}
        />
      </QueryClientProvider>,
    )
    await waitFor(() => expect(operations).toEqual(['RESUME']))
    fireEvent.click(screen.getByRole('button', { name: 'Stop' }))
    releaseResume!()
    await waitFor(() => expect(operations).toEqual(['RESUME', 'CANCEL']))
    await waitFor(() =>
      expect(
        screen.queryByRole('button', { name: 'Stop' }),
      ).not.toBeInTheDocument(),
    )
    view.unmount()
    client.clear()
  },
)

it('reloads the user-opened alternative pages at a new revision without mixing stale arrows', async () => {
  setLocale('en')
  const request = requestFixture(),
    value = structuredClone(fixture) as MapView
  request.clientRequestId = value.search.clientRequestId
  const original = structuredClone(value.graph.methods)
  value.graph.methods = original.slice(0, 2)
  value.graph.nextOffset = 2
  const readPages: string[] = []
  vi.stubGlobal(
    'fetch',
    vi.fn(async (url: string) => {
      let body: unknown = value
      if (url.includes('/map?')) {
        readPages.push(url)
        body = { ...value.graph, methods: original.slice(2, 4), nextOffset: 4 }
      }
      return new Response(JSON.stringify(body), {
        headers: {
          'X-Crafting-Ruleset': request.start.provenance.rulesetIdentity,
        },
      })
    }),
  )
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  const view = render(
    <QueryClientProvider client={client}>
      <MethodSearch
        request={request}
        inputGeneration={1}
        presentItem={createItemPresentation(
          { name: 'Solar Amulet', itemClass: 'Amulet' },
          {},
        )}
        onRecalculate={() => {}}
      />
    </QueryClientProvider>,
  )
  await waitFor(() =>
    expect(
      view.container.querySelectorAll('.method-map__methods > article'),
    ).toHaveLength(2),
  )
  fireEvent.click(await screen.findByRole('button', { name: /more/i }))
  await waitFor(() =>
    expect(
      view.container.querySelectorAll('.method-map__methods > article'),
    ).toHaveLength(4),
  )
  value.search.revision++
  value.graph.revision++
  await client.invalidateQueries({
    queryKey: ['certified-method-map-v1', request.clientRequestId],
  })
  await waitFor(() =>
    expect(readPages.some((url) => url.includes('revision=2&offset=2'))).toBe(
      true,
    ),
  )
  await waitFor(() =>
    expect(
      view.container.querySelectorAll('.method-map__methods > article'),
    ).toHaveLength(4),
  )
  view.unmount()
  client.clear()
})
it('renders both diamond branches, all alternatives, and distinct immediate/final recommendations', async () => {
  setLocale('en')
  const request = requestFixture()
  request.clientRequestId = fixture.search.clientRequestId
  const transport = vi.fn(
    async () =>
      new Response(JSON.stringify(fixture), {
        status: 200,
        headers: {
          'Content-Type': 'application/json',
          'X-Crafting-Ruleset': request.start.provenance.rulesetIdentity,
        },
      }),
  )
  vi.stubGlobal('fetch', transport)
  expect(normalizeView(fixture as MapView).graph.methods).toHaveLength(6)
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  const view = render(
    <QueryClientProvider client={client}>
      <MethodSearch
        request={request}
        inputGeneration={1}
        presentItem={createItemPresentation(
          { name: 'Solar Amulet', itemClass: 'Amulet' },
          {},
        )}
        onRecalculate={() => {}}
      />
    </QueryClientProvider>,
  )
  await waitFor(() =>
    expect(screen.getByText('Calculation complete')).toBeInTheDocument(),
  )
  expect(screen.getAllByText('Best immediate outcome')).toHaveLength(1)
  expect(screen.getAllByText('Recommended strategy')).toHaveLength(1)
  expect(
    view.container.querySelectorAll('.method-map__methods > article'),
  ).toHaveLength(6)
  expect(
    view.container.querySelectorAll('.method-map__overview path'),
  ).toHaveLength(8)
  expect(transport.mock.calls).toHaveLength(2)
  view.unmount()
  client.clear()
})
