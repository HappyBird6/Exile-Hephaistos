import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { CraftSupport } from './CraftSupport'
import {
  fixtureFetch,
  initialFixture,
  jsonResponse,
} from '../../shared/test/craftingFixtures'
import { concreteInitial } from './workbenchApi'
import { requestFixture } from './path-tree/fixtures.test-support'

afterEach(() => vi.unstubAllGlobals())
const oldIdentity = initialFixture.rulesetIdentity
const newIdentity = 'changed-rules-same-snapshot'
function setup() {
  let identity = oldIdentity
  const requests: { url: string; identity: string | null }[] = []
  vi.stubGlobal(
    'fetch',
    vi.fn(async (url: RequestInfo | URL, init?: RequestInit) => {
      if (init?.method === 'POST')
        requests.push({
          url: String(url),
          identity: new Headers(init.headers).get('X-Crafting-Ruleset'),
        })
      let response: Response
      if (
        String(url).endsWith('/basic-paths/provenance') ||
        String(url).includes('/method-searches/provenance?')
      ) {
        response = jsonResponse({
          ...requestFixture().start.provenance,
          rulesetIdentity: identity,
        })
      } else if (String(url).includes('/initial')) {
        const initial = await (await fixtureFetch(url, init)).json()
        response = jsonResponse({ ...initial, rulesetIdentity: identity })
      } else if (String(url).endsWith('/workbench/registry'))
        response = jsonResponse({ workbenchBases: { solar: {} }, entries: [] })
      else if (String(url).includes('/support/families'))
        response = jsonResponse([
          {
            id: 'Life',
            affix: 'PREFIX',
            effectExamples: ['+Life'],
            tiers: [
              {
                tier: 1,
                requiredItemLevel: 1,
                exampleText: '+Life',
                modifierId: 'p',
              },
            ],
          },
        ])
      else if (String(url).endsWith('/map-text'))
        response = jsonResponse({
          mapped: true,
          state: concreteInitial(initialFixture),
          issues: [],
        })
      else if (String(url).endsWith('/support/assess'))
        response = jsonResponse({
          status: 'READY',
          valid: true,
          feasible: true,
          achieved: false,
          requiredMatched: 0,
          candidatesMatched: 0,
          matches: {},
          issues: [],
        })
      else response = await fixtureFetch(url, init)
      response.headers.set('X-Crafting-Ruleset', identity)
      return response
    }),
  )
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return {
    client,
    requests,
    change: () => {
      identity = newIdentity
    },
  }
}

it('preserves edited modifier rolls and blocks starting after a ruleset change', async () => {
  const { client, requests, change } = setup()
  const page = render(
    <QueryClientProvider client={client}>
      <CraftSupport active />
    </QueryClientProvider>,
  )
  const start = screen.getByRole('button', { name: 'Start Crafting' })
  await waitFor(() => expect(start).toBeEnabled())
  fireEvent.change(screen.getByRole('combobox', { name: 'Starting rarity' }), {
    target: { value: 'MAGIC' },
  })
  const picker = screen.getByRole('combobox', { name: 'Add modifier' })
  fireEvent.change(picker, { target: { value: 'Life' } })
  fireEvent.keyDown(picker, { key: 'Enter' })
  fireEvent.change(
    screen.getByRole('spinbutton', { name: /Value.*maximum Life/ }),
    {
      target: { value: '18' },
    },
  )
  const text = (
    screen.getByLabelText('Starting item text') as HTMLTextAreaElement
  ).value
  const count = requests.length
  change()
  await act(async () => {
    await client.refetchQueries({ queryKey: ['craftStart', 'inventory'] })
  })
  await waitFor(() => expect(start).toBeDisabled())
  expect(screen.getByLabelText('Starting item text')).toHaveValue(text)
  expect(
    screen.getByRole('region', { name: 'Starting item preview' }),
  ).toHaveTextContent('+18 to maximum Life')
  fireEvent.click(start)
  expect(
    screen.queryByRole('region', { name: 'Crafting tree preview' }),
  ).not.toBeInTheDocument()
  expect(
    requests.slice(count).some((r) => /recommend|first-hit/.test(r.url)),
  ).toBe(false)
  page.unmount()
  client.clear()
})

it('preserves CraftStart editor provenance and disables the old numeric input after inventory refetch', async () => {
  const { client, change } = setup()
  const page = render(
    <QueryClientProvider client={client}>
      <CraftSupport active />
    </QueryClientProvider>,
  )
  const start = screen.getByRole('button', { name: 'Start Crafting' })
  await waitFor(() => expect(start).toBeEnabled())
  const text = (
    screen.getByLabelText('Starting item text') as HTMLTextAreaElement
  ).value
  change()
  await act(async () => {
    await client.refetchQueries({ queryKey: ['craftStart', 'inventory'] })
  })
  await waitFor(() => expect(start).toBeDisabled())
  expect(screen.getByLabelText('Starting item text')).toHaveValue(text)
  expect(screen.getByRole('combobox', { name: 'Add modifier' })).toBeDisabled()
  fireEvent.change(screen.getByLabelText('Starting item base'), {
    target: { value: 'solar' },
  })
  await waitFor(() => expect(start).toBeEnabled())
  page.unmount()
  client.clear()
})
