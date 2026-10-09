import { afterEach, expect, it, vi } from 'vitest'
import {
  initialFixture,
  jsonResponse,
} from '../../shared/test/craftingFixtures'
import { loadInitial } from './craftingApi'
import { concreteInitial, applyCurrency } from './workbenchApi'
import type { AppliedItem } from './workbenchApi'
import { verifiedHistoryState } from './workbenchHistory'

afterEach(() => vi.unstubAllGlobals())
const initial = structuredClone(initialFixture)
initial.state.baseItemId = 'Metadata/Items/Armours/Gloves/FourGlovesStr1'
initial.state.implicits = []
const root = concreteInitial(initial)

it('loads Workbench-only glove catalog with zero implicits', async () => {
  const fetch = vi.fn<typeof globalThis.fetch>(() =>
    Promise.resolve(jsonResponse(initial)),
  )
  vi.stubGlobal('fetch', fetch)
  expect(await loadInitial(82, new AbortController().signal, 'stocky')).toEqual(
    initial,
  )
  expect(fetch.mock.calls[0]?.[0]).toContain('workbench/initial?base=stocky')
  expect(verifiedHistoryState(root, initial)).toBe(true)
  vi.stubGlobal('fetch', () => Promise.resolve(jsonResponse(initialFixture)))
  await expect(
    loadInitial(82, new AbortController().signal, 'stocky'),
  ).rejects.toThrow('verify')
})

it('counts the first explicit and its family when a base has no implicit', () => {
  const definition = initial.modifiers.p!
  const values = Object.fromEntries(definition.stats!.map((s) => [s.id, s.min]))
  const one = { modifierId: definition.id, values }
  expect(
    verifiedHistoryState(
      { ...root, rarity: 'MAGIC', explicits: [one] },
      initial,
    ),
  ).toBe(true)
  expect(
    verifiedHistoryState(
      { ...root, rarity: 'MAGIC', explicits: [one, one] },
      initial,
    ),
  ).toBe(false)
  expect(
    verifiedHistoryState(
      { ...root, rarity: 'NORMAL', explicits: [one] },
      initial,
    ),
  ).toBe(false)
  expect(
    verifiedHistoryState(
      { ...root, implicits: initialFixture.state.implicits },
      initial,
    ),
  ).toBe(false)
})

it('accepts a guaranteed glove essence result and rejects an invented implicit', async () => {
  const id = 'stocky-mitts:prefix:layered'
  const definition = {
    ...initial.modifiers.p!,
    id,
    stats: [{ id: 'local_armour_+%', min: 27, max: 42 }],
  }
  const definitions = { [id]: definition }
  const input = { ...root, rarity: 'MAGIC' as const }
  const values = { 'local_armour_+%': 30 }
  const result: AppliedItem = {
    rulesetIdentity: 'fixture-ruleset',
    ruleVersion: 'fixture',
    ledgerVersion: 'fixture',
    snapshotId: root.snapshotId,
    state: {
      ...input,
      rarity: 'RARE',
      explicits: [{ modifierId: id, values }],
    },
    action: 'LESSER_ESSENCE_ENHANCEMENT',
    applied: true,
    reason: '',
    events: [{ kind: 'ADD', modifierId: id, values, selectionProbability: 1 }],
    consumedOmens: [],
    remainingOmens: [],
    assumptions: [],
  }
  vi.stubGlobal('fetch', () => Promise.resolve(jsonResponse(result)))
  expect(
    (
      await applyCurrency(
        input,
        result.action,
        definitions,
        new AbortController().signal,
        [],
        'fixture-ruleset',
      )
    ).state.implicits,
  ).toEqual([])
  result.state.implicits = initialFixture.state.implicits
  await expect(
    applyCurrency(
      input,
      result.action,
      definitions,
      new AbortController().signal,
      [],
      'fixture-ruleset',
    ),
  ).rejects.toThrow('unchanged')
})

it('uses the source-proven glove Life target and rejects a Solar target on gloves', async () => {
  const id = 'stocky-mitts:prefix:sanguine'
  const definition = {
    ...initial.modifiers.p!,
    id,
    stats: [{ id: 'base_maximum_life', min: 30, max: 39 }],
  }
  const solarId = 'amulet:prefix:healthy'
  const input = { ...root, rarity: 'MAGIC' as const }
  const values = { base_maximum_life: 35 }
  const result: AppliedItem = {
    rulesetIdentity: 'fixture-ruleset',
    ruleVersion: 'stocky-workbench-basic-essence-v18',
    ledgerVersion: 'fixture',
    snapshotId: root.snapshotId,
    state: {
      ...input,
      rarity: 'RARE',
      explicits: [{ modifierId: id, values }],
    },
    action: 'LESSER_ESSENCE_BODY',
    applied: true,
    reason: '',
    events: [{ kind: 'ADD', modifierId: id, values, selectionProbability: 1 }],
    consumedOmens: [],
    remainingOmens: [],
    assumptions: [],
  }
  const definitions = {
    [id]: definition,
    [solarId]: { ...definition, id: solarId },
  }
  vi.stubGlobal('fetch', () => Promise.resolve(jsonResponse(result)))
  expect(
    (
      await applyCurrency(
        input,
        result.action,
        definitions,
        new AbortController().signal,
        [],
        'fixture-ruleset',
      )
    ).state.explicits[0]?.modifierId,
  ).toBe(id)
  result.state.explicits = [{ modifierId: solarId, values }]
  result.events[0]!.modifierId = solarId
  await expect(
    applyCurrency(
      input,
      result.action,
      definitions,
      new AbortController().signal,
      [],
      'fixture-ruleset',
    ),
  ).rejects.toThrow('unchanged')
  expect(input.explicits).toEqual([])
})
