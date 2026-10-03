import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
import { expect, it, vi } from 'vitest'
import { CraftingPage } from './CraftingPage'
import {
  initialFixture,
  fixtureFetch,
  jsonResponse,
} from '../../shared/test/craftingFixtures'
import {
  applyCurrency,
  concreteInitial,
  compatibleOmenPair,
} from './workbenchApi'
import type { AppliedItem, ConcreteItem } from './workbenchApi'
import type { Definition } from './craftingApi'

const omen = 'Omen_of_Homogenising_Exaltation'
const greater = 'Omen_of_Greater_Exaltation'
const definitions: Record<string, Definition> = {
  ...initialFixture.modifiers,
  seed: {
    ...initialFixture.modifiers.p!,
    id: 'seed',
    familyIds: ['seed'],
    layer: 'EXPLICIT',
    requiredItemLevel: 1,
    weight: 1,
    tags: ['life'],
  },
  bridge: {
    ...initialFixture.modifiers.s!,
    id: 'bridge',
    familyIds: ['bridge'],
    layer: 'EXPLICIT',
    requiredItemLevel: 1,
    weight: 2,
    tags: ['life', 'damage'],
  },
  same: {
    ...initialFixture.modifiers.p!,
    id: 'same',
    familyIds: ['same'],
    layer: 'EXPLICIT',
    requiredItemLevel: 1,
    weight: 3,
    tags: ['life'],
  },
  other: {
    ...initialFixture.modifiers.s!,
    id: 'other',
    familyIds: ['other'],
    layer: 'EXPLICIT',
    requiredItemLevel: 1,
    weight: 999,
    tags: ['damage'],
  },
}
const before: ConcreteItem = {
  ...concreteInitial(initialFixture),
  rarity: 'RARE',
  explicits: [{ modifierId: 'seed', values: { life: 17 }, fractured: true }],
}
const additions = [
  { modifierId: 'bridge', values: { strength: 6 } },
  { modifierId: 'same', values: { life: 17 } },
]
const result: AppliedItem = {
  ruleVersion: 'homogenising-legacy-v1',
  ledgerVersion: 'fixture-model',
  snapshotId: before.snapshotId,
  state: { ...before, explicits: [...before.explicits, ...additions] },
  action: 'EXALTED',
  applied: true,
  reason: '',
  events: additions.map((m, i) => ({
    ...m,
    kind: 'ADD',
    selectionProbability: i === 0 ? 2 / 5 : 1,
  })),
  assumptions: [],
  consumedOmens: [omen, greater],
  remainingOmens: [],
}
const apply = (
  response: AppliedItem,
  omens = [omen, greater],
  state = before,
  action: 'EXALTED' | 'REGAL' = 'EXALTED',
) => {
  vi.stubGlobal('fetch', () => Promise.resolve(jsonResponse(response)))
  return applyCurrency(
    state,
    action,
    definitions,
    new AbortController().signal,
    omens,
  )
}

it('accepts frozen original tags, conditional table weights and preserved Fracture', async () => {
  expect((await apply(result)).state.explicits).toEqual(result.state.explicits)
  expect(compatibleOmenPair(omen, greater)).toBe(true)
  expect(compatibleOmenPair(omen, 'Omen_of_Sinistral_Exaltation')).toBe(false)
})

it('rejects tag expansion, forged weights, lost consumption, forbidden conflict and changed originals', async () => {
  for (const invalid of [
    {
      ...result,
      events: [
        { ...result.events[0]!, selectionProbability: 0.5 },
        result.events[1]!,
      ],
    },
    { ...result, consumedOmens: [greater], remainingOmens: [omen] },
    {
      ...result,
      state: {
        ...result.state,
        explicits: [
          { ...before.explicits[0]!, values: { life: 18 } },
          ...additions,
        ],
      },
    },
    {
      ...result,
      state: {
        ...result.state,
        explicits: [
          ...before.explicits,
          additions[0]!,
          { modifierId: 'other', values: { strength: 6 } },
        ],
      },
      events: [
        result.events[0]!,
        { ...result.events[1]!, modifierId: 'other', values: { strength: 6 } },
      ],
    },
  ])
    await expect(apply(invalid)).rejects.toThrow('Your item is unchanged')
  await expect(
    apply(
      {
        ...result,
        consumedOmens: [omen, greater, 'Omen_of_Sinistral_Exaltation'],
      },
      [omen, greater, 'Omen_of_Sinistral_Exaltation'],
    ),
  ).rejects.toThrow('Your item is unchanged')
})

it('accepts ordinary tagless fallback and Coronation consuming only its own trigger', async () => {
  const state = { ...before, rarity: 'MAGIC' as const, explicits: [] }
  const cor = 'Omen_of_Homogenising_Coronation'
  const r: AppliedItem = {
    ...result,
    action: 'REGAL',
    state: { ...state, rarity: 'RARE', explicits: [additions[0]!] },
    events: [{ ...result.events[0]!, selectionProbability: 2 / 1005 }],
    consumedOmens: [cor],
    remainingOmens: [omen],
  }
  expect((await apply(r, [cor, omen], state, 'REGAL')).consumedOmens).toEqual([
    cor,
  ])
})

it('default stash hides legacy; explicit opt-in names availability and unverified boundaries', async () => {
  vi.stubGlobal('fetch', fixtureFetch)
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  render(
    <QueryClientProvider client={client}>
      <CraftingPage />
    </QueryClientProvider>,
  )
  fireEvent.click(screen.getByRole('tab', { name: 'Omen' }))
  expect(
    screen.queryByRole('button', { name: 'Omen of Homogenising Exaltation' }),
  ).toBeNull()
  fireEvent.click(
    screen.getByRole('checkbox', { name: 'Show legacy Homogenising Omens' }),
  )
  expect(
    screen.getByRole('button', { name: 'Omen of Homogenising Exaltation' }),
  ).toHaveTextContent('(Legacy)')
  expect(
    screen.getByText(/actual game failure consumption is unverified/),
  ).toBeVisible()
  expect(
    screen.getByRole('link', { name: 'Official availability source' }),
  ).toHaveAttribute(
    'href',
    'https://www.pathofexile.com/forum/view-thread/3883495/filter-account-type/staff',
  )
  client.clear()
})
