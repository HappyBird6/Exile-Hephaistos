import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { CraftingExplorer } from './CraftingExplorer'
import { CraftSupport } from './CraftSupport'
import {
  fixtureFetch,
  initialFixture,
  jsonResponse,
} from '../../shared/test/craftingFixtures'
import { concreteInitial } from './workbenchApi'

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
      if (String(url).includes('/initial')) {
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

it('preserves Explorer state/history identity across refetch and isolates a new session from old query cache', async () => {
  const { client, requests, change } = setup()
  const page = render(
    <QueryClientProvider client={client}>
      <CraftingExplorer
        level={82}
        revision={1}
        requestCount={0}
        requestedAction={null}
      />
    </QueryClientProvider>,
  )
  fireEvent.click(
    await screen.findByRole('button', { name: 'Preview Orb of Transmutation' }),
  )
  fireEvent.click(
    (await screen.findAllByRole('button', { name: 'Explore this state' }))[0]!,
  )
  await screen.findByRole('button', { name: 'Preview Orb of Augmentation' })
  const saved = structuredClone(
    client.getQueryData(['crafting', 'current', 1, 0]),
  )
  const count = requests.length
  change()
  await act(async () => {
    await client.refetchQueries({ queryKey: ['crafting', 'initial', 82] })
  })
  await waitFor(() =>
    expect(
      client.getQueryData<{ rulesetIdentity: string }>([
        'crafting',
        'initial',
        82,
      ])?.rulesetIdentity,
    ).toBe(newIdentity),
  )
  await act(async () => {
    fireEvent.click(
      screen.getByRole('button', { name: 'Preview Orb of Augmentation' }),
    )
  })
  expect(requests.slice(count)).toEqual([])
  expect(await screen.findByRole('alert')).toHaveTextContent('ruleset changed')
  expect(
    screen.queryByRole('button', { name: 'Explore this state' }),
  ).not.toBeInTheDocument()
  expect(client.getQueryData(['crafting', 'current', 1, 0])).toEqual(saved)
  expect(requests).toHaveLength(count)
  fireEvent.click(screen.getByRole('button', { name: 'Return to step 1' }))
  const restored = client.getQueryData<{ rulesetIdentity: string }>([
    'crafting',
    'current',
    1,
    0,
  ])!
  expect(restored.rulesetIdentity).toBe(oldIdentity)
  expect(requests).toHaveLength(count)
  fireEvent.click(
    screen.getByRole('button', {
      name: 'Start a new session with current rules',
    }),
  )
  fireEvent.click(
    await screen.findByRole('button', { name: 'Preview Orb of Transmutation' }),
  )
  await screen.findAllByRole('button', { name: 'Explore this state' })
  expect(requests.at(-1)?.identity).toBe(newIdentity)
  expect(
    client.getQueryCache().find({
      queryKey: [
        'crafting',
        'transitions',
        oldIdentity,
        'root',
        'TRANSMUTATION',
      ],
    }),
  ).toBeDefined()
  expect(
    client.getQueryCache().find({
      queryKey: [
        'crafting',
        'transitions',
        newIdentity,
        'root',
        'TRANSMUTATION',
      ],
    }),
  ).toBeDefined()
  page.unmount()
  client.clear()
})

it.each(['base', 'manual', 'text'] as const)(
  'retains Support %s provenance and blocks assessment after a same-snapshot ruleset refetch',
  async (source) => {
    const { client, requests, change } = setup()
    const page = render(
      <QueryClientProvider client={client}>
        <CraftSupport active />
      </QueryClientProvider>,
    )
    fireEvent.click(
      await screen.findByText('Advanced family / tier comparison'),
    )
    const button = await screen.findByRole('button', {
      name: 'Check starting state and goal',
    })
    await waitFor(() => expect(button).toBeEnabled())
    fireEvent.change(screen.getByLabelText('Support input source'), {
      target: { value: source },
    })
    if (source === 'text') {
      fireEvent.change(screen.getByLabelText('Support item text'), {
        target: { value: 'preserved item text' },
      })
      fireEvent.click(
        screen.getByRole('button', { name: 'Validate Support text' }),
      )
      await waitFor(() => expect(button).toBeEnabled())
    }
    fireEvent.click(button)
    await screen.findByRole('heading', {
      name: 'Goal ready for sequence comparison',
    })
    const count = requests.length
    change()
    await act(async () => {
      await client.refetchQueries({ queryKey: ['support', 'initial', 82] })
    })
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0))
    })
    await act(async () => {
      fireEvent.click(button)
    })
    expect(requests.slice(count)).toEqual([])
    await waitFor(() => expect(button).toBeDisabled())
    expect(
      screen.queryByRole('heading', {
        name: 'Goal ready for sequence comparison',
      }),
    ).not.toBeInTheDocument()
    fireEvent.click(button)
    fireEvent.click(
      screen.getByRole('button', { name: 'Compare currency sequences' }),
    )
    expect(requests).toHaveLength(count)
    expect(screen.getByLabelText('Support input source')).toHaveValue(source)
    if (source === 'text')
      expect(screen.getByLabelText('Support item text')).toHaveValue(
        'preserved item text',
      )
    fireEvent.click(
      screen.getByRole('button', {
        name: 'Start a new session with current rules',
      }),
    )
    await waitFor(() => expect(button).toBeEnabled())
    fireEvent.click(button)
    await screen.findByRole('heading', {
      name: 'Goal ready for sequence comparison',
    })
    expect(requests.at(-1)?.identity).toBe(newIdentity)
    page.unmount()
    client.clear()
  },
)

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
  expect(screen.getByRole('button', { name: 'Add modifier' })).toBeDisabled()
  fireEvent.change(screen.getByLabelText('Starting item base'), {
    target: { value: 'solar' },
  })
  await waitFor(() => expect(start).toBeEnabled())
  page.unmount()
  client.clear()
})

it('does not expose an Explorer response arriving after the session rules change', async () => {
  const { client, change } = setup()
  const originalFetch = globalThis.fetch
  let finish!: (response: Response) => void
  vi.stubGlobal(
    'fetch',
    vi.fn((url: RequestInfo | URL, init?: RequestInit) =>
      String(url).endsWith('/transitions')
        ? new Promise<Response>((resolve) => {
            finish = resolve
          })
        : originalFetch(url, init),
    ),
  )
  const page = render(
    <QueryClientProvider client={client}>
      <CraftingExplorer
        level={82}
        revision={1}
        requestCount={0}
        requestedAction={null}
      />
    </QueryClientProvider>,
  )
  fireEvent.click(
    await screen.findByRole('button', { name: 'Preview Orb of Transmutation' }),
  )
  await screen.findByRole('button', { name: 'Cancel preview' })
  change()
  await act(async () => {
    await client.refetchQueries({ queryKey: ['crafting', 'initial', 82] })
  })
  await screen.findByRole('alert')
  await act(async () => {
    finish(
      await fixtureFetch('/api/v1/crafting/transitions', {
        body: JSON.stringify({
          state: initialFixture.state,
          action: 'TRANSMUTATION',
        }),
      }),
    )
  })
  expect(
    screen.queryByRole('button', { name: 'Explore this state' }),
  ).not.toBeInTheDocument()
  expect(screen.getByRole('alert')).toHaveTextContent('ruleset changed')
  page.unmount()
  client.clear()
})

it('aborts a Support comparison across refetch and never starts recommendation from a late assessment', async () => {
  const { client, change } = setup()
  const originalFetch = globalThis.fetch
  let finish!: (response: Response) => void
  let signal: AbortSignal | undefined
  const fetch = vi.fn((url: RequestInfo | URL, init?: RequestInit) => {
    if (String(url).endsWith('/support/assess')) {
      signal = init?.signal as AbortSignal
      return new Promise<Response>((resolve) => {
        finish = resolve
      })
    }
    return originalFetch(url, init)
  })
  vi.stubGlobal('fetch', fetch)
  const page = render(
    <QueryClientProvider client={client}>
      <CraftSupport active />
    </QueryClientProvider>,
  )
  fireEvent.click(await screen.findByText('Advanced family / tier comparison'))
  const button = screen.getByRole('button', {
    name: 'Compare currency sequences',
  })
  await waitFor(() => expect(button).toBeEnabled())
  fireEvent.click(button)
  await waitFor(() => expect(signal).toBeDefined())
  change()
  await act(async () => {
    await client.refetchQueries({ queryKey: ['support', 'initial', 82] })
  })
  await waitFor(() => expect(signal?.aborted).toBe(true))
  await act(async () => {
    finish(
      jsonResponse({
        status: 'READY',
        valid: true,
        feasible: true,
        achieved: false,
        requiredMatched: 0,
        candidatesMatched: 0,
        matches: {},
        issues: [],
      }),
    )
  })
  expect(
    fetch.mock.calls.some(([url]) =>
      String(url).endsWith('/support/recommend'),
    ),
  ).toBe(false)
  expect(
    screen.queryByRole('heading', {
      name: 'Goal ready for sequence comparison',
    }),
  ).not.toBeInTheDocument()
  page.unmount()
  client.clear()
})
