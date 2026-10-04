import { afterEach, expect, it, vi } from 'vitest'
import {
  initialFixture,
  jsonResponse,
} from '../../shared/test/craftingFixtures'
import actual from '../../shared/test/wand-essence-responses.json'
import { applyCurrency } from './workbenchApi'
import type { AppliedItem, ConcreteItem, WorkbenchAction } from './workbenchApi'
import type { Definition } from './craftingApi'
import { loadInitial } from './craftingApi'
import { maximumQuality } from './qualityLimit'
import { supportsConcreteStateShape } from './workbenchStateShape'

afterEach(() => vi.unstubAllGlobals())
const definitions = actual.definitions as Record<string, Definition>
const base = 'Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand3'
it.each(actual.captures)(
  'accepts actual $action with Wand-specific fixed targets',
  async ({ action, before, result }) => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(result)))
    await expect(
      applyCurrency(
        before as ConcreteItem,
        action as WorkbenchAction,
        definitions,
        new AbortController().signal,
        ['Omen_of_the_Blessed'],
      ),
    ).resolves.toMatchObject({ applied: true })
  },
)
it.each(['missing', 'extra', 'out-of-range', 'source', 'missing-ledger'])(
  'rejects forged %s replacement data',
  async (kind) => {
    const capture = actual.captures.find(
      (c) => c.action === 'PERFECT_ESSENCE_ALACRITY',
    )!
    const response = structuredClone(capture.result) as AppliedItem
    const event = response.events.find((e) => e.kind === 'ADD')!,
      modifier = response.state.explicits.find(
        (m) => m.modifierId === event.modifierId,
      )!,
      key = Object.keys(modifier.values)[0]!
    if (kind === 'missing') {
      delete modifier.values[key]
      delete event.values[key]
    } else if (kind === 'extra') {
      modifier.values['forged'] = 1
      event.values['forged'] = 1
    } else if (kind === 'out-of-range') {
      modifier.values[key] =
        definitions[modifier.modifierId]!.stats![0]!.max + 1
      event.values[key] = modifier.values[key]!
    } else if (kind === 'source')
      response.assumptions.find(
        (a) => a.id === 'assumed-source-integer-roll-v1',
      )!.sourceUrl = 'https://poe2db.tw/us/hover?s=Data%5CMods%2FUnverified'
    else
      response.assumptions = response.assumptions.filter(
        (a) => a.id !== 'assumed-source-integer-roll-v1',
      )
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(response)))
    await expect(
      applyCurrency(
        capture.before as ConcreteItem,
        capture.action as WorkbenchAction,
        definitions,
        new AbortController().signal,
        ['Omen_of_the_Blessed'],
      ),
    ).rejects.toThrow('Could not verify')
  },
)
it('dispatches Wand initial separately and rejects a valid Bow response delivered for Wand', async () => {
  const initial = {
    ...initialFixture,
    state: { ...initialFixture.state, baseItemId: base, implicits: [] },
    augmentSockets: null,
  }
  const fetch = vi
    .fn()
    .mockResolvedValueOnce(jsonResponse(initial))
    .mockResolvedValueOnce(
      jsonResponse({
        ...initial,
        state: {
          ...initial.state,
          baseItemId: 'Metadata/Items/Weapons/TwoHandWeapons/Bows/FourBow1',
        },
      }),
    )
  vi.stubGlobal('fetch', fetch)
  await expect(
    loadInitial(82, new AbortController().signal, 'wand'),
  ).resolves.toMatchObject({ state: { baseItemId: base } })
  expect(fetch.mock.calls[0]![0]).toBe(
    '/api/v1/crafting/workbench/initial?base=wand&itemLevel=82',
  )
  await expect(
    loadInitial(82, new AbortController().signal, 'wand'),
  ).rejects.toThrow('verify')
})
it('preserves the known cap and refuses unimplemented sockets or innate-skill projections', () => {
  const state = actual.captures[0]!.before as ConcreteItem
  expect(maximumQuality(state, definitions)).toBe(20)
  expect(supportsConcreteStateShape(state)).toBe(true)
  expect(supportsConcreteStateShape({ ...state, augmentSockets: 0 })).toBe(
    false,
  )
  expect(
    supportsConcreteStateShape({
      ...state,
      innateSkill: { name: 'Mana Drain', level: 20 },
    }),
  ).toBe(false)
})
