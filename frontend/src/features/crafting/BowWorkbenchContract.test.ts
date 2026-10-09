import { afterEach, expect, it, vi } from 'vitest'
import {
  initialFixture,
  jsonResponse,
} from '../../shared/test/craftingFixtures'
import { loadInitial } from './craftingApi'
import { applyCurrency, concreteInitial } from './workbenchApi'
import type { AppliedItem, ConcreteItem } from './workbenchApi'
import type { Definition } from './craftingApi'
import actual from '../../shared/test/bow-cold-essence-response.json'
import { maximumQuality } from './qualityLimit'
import { supportsConcreteStateShape } from './workbenchStateShape'

const base = 'Metadata/Items/Weapons/TwoHandWeapons/Bows/FourBow1'
const initial = {
  ...initialFixture,
  state: { ...initialFixture.state, baseItemId: base, implicits: [] },
  augmentSockets: null,
}
afterEach(() => vi.unstubAllGlobals())

const result = actual.result as AppliedItem
const before: ConcreteItem = { ...result.state, rarity: 'MAGIC', explicits: [] }
const definitions = { [actual.definition.id]: actual.definition as Definition }

it('accepts actual multi-stat Essence values when event JSON key order differs', async () => {
  const response = structuredClone(result)
  response.events[0]!.values = Object.fromEntries(
    Object.entries(response.state.explicits[0]!.values).reverse(),
  )
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(response)))
  await expect(
    applyCurrency(
      before,
      'LESSER_ESSENCE_ICE',
      definitions,
      new AbortController().signal,
      result.remainingOmens,
      'fixture-ruleset',
    ),
  ).resolves.toMatchObject({ applied: true })
})

it.each(['changed', 'extra', 'missing', 'out-of-range'])(
  'rejects %s event stat data while accepting key reordering',
  async (kind) => {
    const response = structuredClone(result)
    const key = Object.keys(response.events[0]!.values)[0]!
    if (kind === 'changed')
      response.events[0]!.values[key] = response.events[0]!.values[key]! + 1
    else if (kind === 'extra') response.events[0]!.values['forged_stat'] = 1
    else if (kind === 'missing') delete response.events[0]!.values[key]
    else
      response.events[0]!.values[key] =
        actual.definition.stats.find((s) => s.id === key)!.max + 1
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(response)))
    await expect(
      applyCurrency(
        before,
        'LESSER_ESSENCE_ICE',
        definitions,
        new AbortController().signal,
        result.remainingOmens,
        'fixture-ruleset',
      ),
    ).rejects.toThrow('Could not verify the coupled roll model')
  },
)

it('accepts an unchanged Fractured modifier with reordered JSON stat keys', async () => {
  const locked: ConcreteItem = {
    ...result.state,
    explicits: result.state.explicits.map((m) => ({ ...m, fractured: true })),
  }
  const response: AppliedItem = {
    ...result,
    action: 'DIVINE',
    applied: false,
    reason: 'No non-Fractured numeric range can be rerolled.',
    state: {
      ...locked,
      explicits: locked.explicits.map((m) => ({
        ...m,
        values: Object.fromEntries(Object.entries(m.values).reverse()),
      })),
    },
    events: [],
    assumptions: [],
    consumedOmens: [],
    remainingOmens: [],
  }
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(response)))
  await expect(
    applyCurrency(
      locked,
      'DIVINE',
      definitions,
      new AbortController().signal,
      [],
      'fixture-ruleset',
    ),
  ).resolves.toMatchObject({ applied: false })
})

it('loads the separate Bow catalog without inheriting Solar implicit or socket assumptions', async () => {
  const fetch = vi.fn().mockResolvedValue(jsonResponse(initial))
  vi.stubGlobal('fetch', fetch)
  const loaded = await loadInitial(82, new AbortController().signal, 'bow')
  expect(fetch.mock.calls[0]?.[0]).toBe(
    '/api/v1/crafting/workbench/initial?base=bow&itemLevel=82',
  )
  const concrete = concreteInitial(loaded)
  expect(concrete.implicits).toEqual([])
  expect(concrete.augmentSockets).toBeNull()
  expect(maximumQuality(concrete, loaded.modifiers)).toBe(20)
})

it('rejects a valid glove response delivered for Bow selection', async () => {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue(
      jsonResponse({
        ...initial,
        state: {
          ...initial.state,
          baseItemId: 'Metadata/Items/Armours/Gloves/FourGlovesStr1',
        },
      }),
    ),
  )
  await expect(
    loadInitial(82, new AbortController().signal, 'bow'),
  ).rejects.toThrow('verify')
})

it('preserves the affix boundary instead of accepting unimplemented Bow sockets or computed damage', () => {
  const state = concreteInitial(initial)
  expect(supportsConcreteStateShape(state)).toBe(true)
  expect(supportsConcreteStateShape({ ...state, augmentSockets: 0 })).toBe(
    false,
  )
  expect(
    supportsConcreteStateShape({ ...state, physicalDamage: [5, 10] }),
  ).toBe(false)
})
