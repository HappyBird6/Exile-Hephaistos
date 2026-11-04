import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, afterEach, expect, it, vi } from 'vitest'
import { setLocale } from '../../shared/i18n/i18n'
import { AbyssPanel, type AbyssState } from './AbyssPanel'
import { validAbyssResponse } from './abyssResponse'
import type { ConcreteItem } from './workbenchApi'
import type { Initial } from './craftingApi'

const source: ConcreteItem = {
  snapshotId: 'snapshot',
  baseItemId: 'base',
  itemLevel: 82,
  rarity: 'RARE',
  implicits: [],
  explicits: [],
  conditions: [],
}
const initial: Initial = {
  rulesetIdentity: 'rules',
  ruleVersion: 'test',
  metadata: { snapshotId: 'snapshot', retrievedAt: '', sourceUrl: '' },
  id: 'root',
  state: { ...source, modifierIds: [] },
  actions: [],
  modifiers: {
    life: {
      id: 'life',
      name: 'Life',
      text: '+# to maximum Life',
      tier: 1,
      layer: 'EXPLICIT',
      affixType: 'PREFIX',
      familyIds: ['life'],
      weight: 100,
      stats: [{ id: 'maximum_life', min: 1, max: 10 }],
    },
  },
}
const empty = { item: source, slots: [], offer: null }
const calls: { url: string; body: Record<string, unknown> }[] = []

it('blocks direct selection with the external busy prop and the synchronous owner guard', async () => {
  const hidden: AbyssState = {
    ...empty,
    slots: [{ id: 'hidden', side: 'PREFIX', modifier: null, desecrated: true }],
  }
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  let allowed = true
  const content = (externalBusy: boolean) => (
    <QueryClientProvider client={client}>
      <AbyssPanel
        source={source}
        initial={initial}
        prepared={hidden}
        directSelection
        externalBusy={externalBusy}
        canMutate={() => allowed}
      />
    </QueryClientProvider>
  )
  const mounted = render(content(true))
  const reveal = await screen.findByRole('button', { name: 'Reveal' })
  expect(reveal).toBeDisabled()
  fireEvent.click(reveal)
  expect(screen.queryByText('Choose modifier')).toBeNull()
  mounted.rerender(content(false))
  allowed = false
  fireEvent.click(reveal)
  expect(screen.queryByText('Choose modifier')).toBeNull()
  allowed = true
  fireEvent.click(reveal)
  expect(await screen.findByText('Choose modifier')).toBeVisible()
  expect(calls.some((call) => call.url.endsWith('/select'))).toBe(false)
  client.clear()
})

it('notifies the owner synchronously and prevents a duplicate mutation before pending renders', async () => {
  const hidden: AbyssState = {
    ...empty,
    slots: [{ id: 'hidden', side: 'PREFIX', modifier: null, desecrated: true }],
  }
  const onBusyChange = vi.fn()
  let complete!: (response: Response) => void
  const originalFetch = globalThis.fetch
  const observe = vi.fn(
    () =>
      new Promise<Response>((resolve) => {
        complete = resolve
      }),
  )
  vi.stubGlobal('fetch', (url: string, options: RequestInit) =>
    url.endsWith('/observe') ? observe() : originalFetch(url, options),
  )
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  render(
    <QueryClientProvider client={client}>
      <AbyssPanel
        source={source}
        initial={initial}
        prepared={hidden}
        onBusyChange={onBusyChange}
      />
    </QueryClientProvider>,
  )
  await screen.findByRole('combobox', { name: 'Desecrated slot' })
  for (const index of [1, 2, 3])
    fireEvent.change(screen.getByLabelText(`Candidate ${index}`), {
      target: { value: 'life' },
    })
  onBusyChange.mockClear()
  const button = screen.getByRole('button', {
    name: 'Reveal / replace observed candidates',
  })
  fireEvent.click(button)
  expect(onBusyChange).toHaveBeenCalledWith(true)
  fireEvent.click(button)
  await waitFor(() => expect(observe).toHaveBeenCalledTimes(1))
  complete(
    new Response(
      JSON.stringify({
        ...hidden,
        offer: {
          slotId: 'hidden',
          candidates: Array.from({ length: 3 }, () => ({
            modifierId: 'life',
            values: { maximum_life: 1 },
            fractured: false,
          })),
        },
      }),
      {
        headers: {
          'Content-Type': 'application/json',
          'X-Crafting-Ruleset': 'rules',
        },
      },
    ),
  )
  await waitFor(() => expect(onBusyChange).toHaveBeenLastCalledWith(false))
  client.clear()
})

it('restores a Sanctified hidden slot for reading and disables revealing it', async () => {
  const item: ConcreteItem = { ...source, conditions: ['SANCTIFIED'] }
  const state: AbyssState = {
    item,
    slots: [{ id: 'locked', side: 'PREFIX', modifier: null, desecrated: true }],
    offer: null,
  }
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  render(
    <QueryClientProvider client={client}>
      <AbyssPanel
        source={item}
        initial={initial}
        restoredState={state}
        directSelection
        localScenario={false}
      />
    </QueryClientProvider>,
  )
  const reveal = await screen.findByRole('button', { name: 'Reveal' })
  expect(reveal).toBeDisabled()
  expect(calls.some((call) => call.url.endsWith('/validate'))).toBe(true)
  expect(
    calls.some(
      (call) => call.url.endsWith('/eligible') || call.url.endsWith('/select'),
    ),
  ).toBe(false)
})

beforeEach(() => {
  localStorage.clear()
  setLocale('en')
  calls.length = 0
  vi.stubGlobal(
    'fetch',
    vi.fn(async (url: string, options: RequestInit) => {
      const body = JSON.parse(options.body as string) as Record<string, unknown>
      calls.push({ url, body })
      let value: unknown = empty
      if (url.endsWith('/validate')) value = body.state
      if (url.endsWith('/plan')) value = { available: false, branches: [] }
      if (url.endsWith('/add'))
        value = {
          ...empty,
          slots: [
            {
              id: body.slotId,
              side: body.side,
              modifier: null,
              desecrated: true,
            },
          ],
        }
      if (url.endsWith('/observe'))
        value = {
          ...(body.state as object),
          offer: { slotId: body.slotId, candidates: body.candidates },
        }
      return new Response(JSON.stringify(value), {
        status: 200,
        headers: {
          'Content-Type': 'application/json',
          'X-Crafting-Ruleset': 'rules',
        },
      })
    }),
  )
})
afterEach(() => vi.unstubAllGlobals())
function panel(prepared?: AbyssState) {
  return render(
    <QueryClientProvider
      client={
        new QueryClient({
          defaultOptions: {
            queries: { retry: false },
            mutations: { retry: false },
          },
        })
      }
    >
      <AbyssPanel
        source={source}
        initial={initial}
        {...(prepared ? { prepared } : {})}
      />
    </QueryClientProvider>,
  )
}
it('does not offer a separate Abyss start or a currency-free slot for an ordinary rare item', () => {
  panel()
  expect(screen.queryByRole('button')).not.toBeInTheDocument()
  expect(document.querySelector('.abyss-panel')).not.toBeInTheDocument()
  expect(calls).toHaveLength(0)
})

it('renders a prepared rune slot and preserves legacy observed triples without a currency action or reveal probability', async () => {
  panel({
    ...empty,
    slots: [{ id: 'hidden', side: 'PREFIX', modifier: null, desecrated: true }],
  })
  await waitFor(() =>
    expect(
      screen.getByRole('img', { name: 'Unrevealed desecrated modifier' }),
    ).toBeInTheDocument(),
  )
  expect(screen.queryByText('Unrevealed desecrated modifier')).toBeNull()
  expect(
    screen.queryByRole('button', { name: 'Add unrevealed slot' }),
  ).toBeNull()
  const selected = screen.getByLabelText('Desecrated slot') as HTMLSelectElement
  fireEvent.change(selected, { target: { value: selected.options[1]!.value } })
  for (const index of [1, 2, 3])
    fireEvent.change(screen.getByLabelText(`Candidate ${index}`), {
      target: { value: 'life' },
    })
  fireEvent.click(
    screen.getByRole('button', {
      name: 'Reveal / replace observed candidates',
    }),
  )
  expect(await screen.findAllByRole('button', { name: 'Choose' })).toHaveLength(
    3,
  )
  const observed = calls.find((c) => c.url.endsWith('/observe'))!
  expect(observed.body).not.toHaveProperty('action')
  expect(observed.body.candidates).toHaveLength(3)
  expect(JSON.stringify(observed.body)).not.toContain('probability')
  expect(calls.some((c) => c.url.endsWith('/add'))).toBe(false)
  expect(localStorage.length).toBeGreaterThan(0)
  expect(selected).toHaveFocus()
})
it('rejects malformed slots and invented reveal candidates before rendering them', () => {
  expect(validAbyssResponse('import', empty, source, initial)).toBe(true)
  expect(
    validAbyssResponse(
      'import',
      {
        ...empty,
        slots: [{ id: 'x', side: 'PREFIX', modifier: null, desecrated: false }],
      },
      source,
      initial,
    ),
  ).toBe(false)
  expect(
    validAbyssResponse(
      'observe',
      { ...empty, offer: { slotId: 'x', candidates: [1, 2, 3] } },
      source,
      initial,
    ),
  ).toBe(false)
  expect(
    validAbyssResponse(
      'compare',
      {
        baseline: { targetWeight: 1, totalWeight: 0 },
        afterBlocking: { targetWeight: 1, totalWeight: 2 },
        blockerModifierId: 'life',
      },
      source,
      initial,
    ),
  ).toBe(false)
})

it('opens a prepared automatic route with its dummy and preserves the selected omen action', async () => {
  const prepared = {
    ...empty,
    slots: [
      {
        id: 'route-dummy',
        side: 'PREFIX' as const,
        modifier: null,
        desecrated: true,
      },
    ],
  }
  render(
    <QueryClientProvider
      client={
        new QueryClient({ defaultOptions: { queries: { retry: false } } })
      }
    >
      <AbyssPanel
        source={source}
        initial={initial}
        prepared={prepared}
        initialAction="ESSENCE_HYSTERIA"
        initialOmens={['omen-of-dextral-crystallisation']}
      />
    </QueryClientProvider>,
  )
  await waitFor(() =>
    expect(
      calls.some(
        (c) =>
          c.url.endsWith('/validate') &&
          JSON.stringify(c.body.state) === JSON.stringify(prepared),
      ),
    ).toBe(true),
  )
  await waitFor(() =>
    expect(
      calls.some(
        (c) =>
          c.url.endsWith('/plan') &&
          c.body.action === 'ESSENCE_HYSTERIA' &&
          JSON.stringify(c.body.activeOmens) ===
            '["omen-of-dextral-crystallisation"]',
      ),
    ).toBe(true),
  )
  expect(screen.getByRole('combobox', { name: 'Next currency' })).toBeEnabled()
  fireEvent.change(screen.getByRole('combobox', { name: 'Next currency' }), {
    target: { value: 'ANNULMENT' },
  })
  await waitFor(() =>
    expect(
      calls.some(
        (c) =>
          c.url.endsWith('/plan') &&
          c.body.action === 'ANNULMENT' &&
          JSON.stringify(c.body.activeOmens) === '[]',
      ),
    ).toBe(true),
  )
  expect(calls.some((c) => c.url.endsWith('/import'))).toBe(false)
})

it('direct selection lists eligible options, sends one chosen modifier and hides observed triples and probabilities', async () => {
  const hidden = {
    ...empty,
    slots: [
      {
        id: 'hidden',
        side: 'PREFIX' as const,
        modifier: null,
        desecrated: true,
      },
    ],
  }
  const onTransition = vi.fn()
  const fetch = vi.fn(async (url: string, options: RequestInit) => {
    const body = JSON.parse(options.body as string)
    let value: unknown = hidden
    if (url.endsWith('/eligible')) value = ['life']
    if (url.endsWith('/plan')) value = { available: false, branches: [] }
    if (url.endsWith('/select'))
      value = {
        ...hidden,
        slots: [{ ...hidden.slots[0], modifier: body.candidate }],
      }
    return new Response(JSON.stringify(value), {
      headers: {
        'Content-Type': 'application/json',
        'X-Crafting-Ruleset': 'rules',
      },
    })
  })
  vi.stubGlobal('fetch', fetch)
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  render(
    <QueryClientProvider client={client}>
      <AbyssPanel
        source={source}
        initial={initial}
        restoredState={hidden}
        directSelection
        embedded
        allowAdd={false}
        localScenario={false}
        onTransition={onTransition}
      />
    </QueryClientProvider>,
  )
  const select = await screen.findByLabelText('Desecrated slot')
  expect(screen.queryByLabelText('Candidate 1')).not.toBeInTheDocument()
  expect(fetch.mock.calls.some(([url]) => url.endsWith('/eligible'))).toBe(
    false,
  )
  fireEvent.change(select, { target: { value: 'hidden' } })
  fireEvent.click(screen.getByRole('button', { name: 'Reveal' }))
  await screen.findByRole('option', { name: /maximum Life/ })
  fireEvent.change(screen.getByLabelText('Candidate 1'), {
    target: { value: 'life' },
  })
  expect(
    screen.queryByText('Enter three observed candidates'),
  ).not.toBeInTheDocument()
  expect(screen.queryByText('Removal probability')).not.toBeInTheDocument()
  expect(
    screen.queryByRole('button', { name: 'Compare' }),
  ).not.toBeInTheDocument()
  fireEvent.click(screen.getByRole('button', { name: 'Apply chosen modifier' }))
  await waitFor(() =>
    expect(onTransition).toHaveBeenCalledWith(
      expect.objectContaining({
        slots: [
          expect.objectContaining({
            modifier: {
              modifierId: 'life',
              values: { maximum_life: 1 },
              fractured: false,
            },
          }),
        ],
      }),
      { operation: 'select' },
    ),
  )
  const call = fetch.mock.calls.find(([url]) => url.endsWith('/select'))!
  expect(JSON.parse(call[1].body as string)).not.toHaveProperty('candidates')
  expect(fetch.mock.calls.some(([url]) => url.endsWith('/observe'))).toBe(false)
  expect(fetch.mock.calls.some(([url]) => url.endsWith('/add'))).toBe(false)
  expect(fetch.mock.calls.some(([url]) => url.endsWith('/desecrate'))).toBe(
    false,
  )
  expect(screen.queryByLabelText('Candidate 1')).not.toBeInTheDocument()
  client.clear()
})
