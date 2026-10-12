import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import fixture from '../../../../../contracts/crafting-methods-v2/synthetic-diamond.json'
import { MethodSearch } from './MethodSearch'
import { acceptSnapshot } from './api'
import type { Snapshot } from './types'
import { requestFixture } from '../path-tree/fixtures.test-support'
import { createItemPresentation } from '../path-tree/presentation'
import { setLocale } from '../../../shared/i18n/i18n'

afterEach(() => vi.unstubAllGlobals())
it('keeps unknown goal evaluation distinct from a failed goal', async () => {
  setLocale('en')
  const request = requestFixture()
  request.clientRequestId = fixture.clientRequestId
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
it('renders both diamond branches, all alternatives, and distinct immediate/final recommendations', async () => {
  setLocale('en')
  const request = requestFixture()
  request.clientRequestId = fixture.clientRequestId
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
  expect(
    acceptSnapshot(undefined, fixture as Snapshot).graph.methods,
  ).toHaveLength(6)
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
  expect(transport.mock.calls).toHaveLength(1)
  view.unmount()
  client.clear()
})
