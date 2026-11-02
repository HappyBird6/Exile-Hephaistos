import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { beforeEach, afterEach, expect, it, vi } from 'vitest'
import { setLocale } from '../../shared/i18n/i18n'
import { AbyssPanel } from './AbyssPanel'
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
function panel() {
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
      <AbyssPanel source={source} initial={initial} />
    </QueryClientProvider>,
  )
}
it('shows an unrevealed green slot and records three observed candidates without a currency action or probability', async () => {
  panel()
  fireEvent.click(screen.getByRole('button', { name: 'Abyss crafting' }))
  fireEvent.click(
    await screen.findByRole('button', { name: 'Add unrevealed slot' }),
  )
  await waitFor(() =>
    expect(document.querySelector('.abyss-slot')).toHaveTextContent(
      'Unrevealed desecrated modifier',
    ),
  )
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
  fireEvent.change(select, { target: { value: 'hidden' } })
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
  client.clear()
})
