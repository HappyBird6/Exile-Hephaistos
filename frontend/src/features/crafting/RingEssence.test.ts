import { afterEach, expect, it, vi } from 'vitest'
import actual from '../../shared/test/ring-essence-responses.json'
import { jsonResponse } from '../../shared/test/craftingFixtures'
import { applyCurrency, rolledText } from './workbenchApi'
import type { AppliedItem, ConcreteItem, WorkbenchAction } from './workbenchApi'
import type { Definition } from './craftingApi'
import { loadInitial } from './craftingApi'
import { maximumQuality } from './qualityLimit'
import { supportsConcreteStateShape } from './workbenchStateShape'
import { craftProbabilityEvidence } from './craftProbabilityEvidence'

afterEach(() => vi.unstubAllGlobals())
const definitions = actual.initial.modifiers as Record<string, Definition>

it.each(actual.captures)(
  'accepts actual $action with exact Ring targets and numeric source',
  async ({ action, before, result }) => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(result)))
    const verified = await applyCurrency(
      before as ConcreteItem,
      action as WorkbenchAction,
      definitions,
      new AbortController().signal,
      ['Omen_of_the_Blessed'],
    )
    expect(verified.applied).toBe(true)
    expect(craftProbabilityEvidence(verified).weighted).toBe(false)
  },
)

it.each(actual.captures)(
  'rejects forged numeric provenance for $action',
  async ({ action, before, result }) => {
    const forged = structuredClone(result) as AppliedItem
    forged.assumptions.find(
      (a) => a.id === 'assumed-source-integer-roll-v1',
    )!.sourceUrl = 'https://poe2db.tw/us/Rings_str'
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(forged)))
    await expect(
      applyCurrency(
        before as ConcreteItem,
        action as WorkbenchAction,
        definitions,
        new AbortController().signal,
        ['Omen_of_the_Blessed'],
      ),
    ).rejects.toThrow('numeric model')
  },
)

it.each([
  'missing-value',
  'extra-value',
  'range',
  'missing-ledger',
  'below-level',
  'wrong-base',
])('refuses a forged %s Ring response', async (kind) => {
  const capture = actual.captures[0]!,
    response = structuredClone(capture.result) as AppliedItem
  const added = response.events.at(-1)!,
    target = response.state.explicits.find(
      (m) => m.modifierId === added.modifierId,
    )!,
    stat = definitions[added.modifierId]!.stats![0]!
  const before = structuredClone(capture.before) as ConcreteItem
  if (kind === 'missing-value') {
    delete target.values[stat.id]
    delete added.values[stat.id]
  }
  if (kind === 'extra-value') {
    target.values.unknown = 1
    added.values.unknown = 1
  }
  if (kind === 'range') {
    target.values[stat.id] = stat.max + 1
    added.values[stat.id] = stat.max + 1
  }
  if (kind === 'missing-ledger')
    response.assumptions = response.assumptions.filter(
      (a) => a.id !== 'assumed-source-integer-roll-v1',
    )
  if (kind === 'below-level') {
    before.itemLevel = 71
    response.state.itemLevel = 71
  }
  if (kind === 'wrong-base') {
    before.baseItemId = 'Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand3'
    response.state.baseItemId = before.baseItemId
  }
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(response)))
  await expect(
    applyCurrency(
      before,
      capture.action as WorkbenchAction,
      definitions,
      new AbortController().signal,
      ['Omen_of_the_Blessed'],
    ),
  ).rejects.toThrow('verify')
})

it('selects the actual ninth-base initial endpoint and refuses a substituted old base', async () => {
  const fetch = vi
    .fn()
    .mockResolvedValueOnce(jsonResponse(actual.initial))
    .mockResolvedValueOnce(
      jsonResponse({
        ...actual.initial,
        state: {
          ...actual.initial.state,
          baseItemId: 'Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand3',
        },
      }),
    )
  vi.stubGlobal('fetch', fetch)
  await expect(
    loadInitial(82, new AbortController().signal, 'ring'),
  ).resolves.toMatchObject({
    state: { baseItemId: 'Metadata/Items/Rings/FourRing1' },
  })
  expect(fetch.mock.calls[0]![0]).toBe(
    '/api/v1/crafting/workbench/initial?base=ring&itemLevel=82',
  )
  await expect(
    loadInitial(82, new AbortController().signal, 'ring'),
  ).rejects.toThrow('verify')
})

it('keeps the source cap separate from unmodeled quality, Armour and sockets', () => {
  const state = actual.captures[0]!.before as ConcreteItem
  expect(maximumQuality(state, definitions)).toBeNull()
  expect(supportsConcreteStateShape(state)).toBe(true)
  for (const extension of [
    { augmentSockets: 0 },
    { quality: 20 },
    { armour: 29 },
  ])
    expect(supportsConcreteStateShape({ ...state, ...extension })).toBe(false)
})

it('accepts actual Ring Divine while preserving the fixed-implicit projection', async () => {
  const capture = actual.divineCapture
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue(jsonResponse(capture.result)),
  )
  const r = await applyCurrency(
    capture.before as ConcreteItem,
    'DIVINE',
    definitions,
    new AbortController().signal,
  )
  expect(r.applied).toBe(true)
  expect(r.state.implicits).toEqual(capture.before.implicits)
  expect(r.qualityLimit).toBeNull()
})
it.each([
  { ruleVersion: 'quality-limit-v1', maximumQuality: 20 },
  { ruleVersion: 'quality-limit-v1', maximumQuality: 40 },
])('refuses a forged Ring quality limit %s', async (limit) => {
  const capture = actual.captures[0]!,
    forged = structuredClone(capture.result) as AppliedItem
  forged.qualityLimit = limit as NonNullable<AppliedItem['qualityLimit']> | null
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(forged)))
  await expect(
    applyCurrency(
      capture.before as ConcreteItem,
      capture.action as WorkbenchAction,
      definitions,
      new AbortController().signal,
      ['Omen_of_the_Blessed'],
    ),
  ).rejects.toThrow('verify')
})

it.each([
  'missing',
  'wrong-minimum',
  'wrong-maximum',
  'extra-stat',
  'fractured',
])('rejects forged fixed Ring implicit %s on Divine', async (kind) => {
  const capture = actual.divineCapture,
    forged = structuredClone(capture.result) as AppliedItem
  if (kind === 'missing') forged.state.implicits = []
  else {
    const m = forged.state.implicits[0]!
    if (kind === 'wrong-minimum')
      m.values.attack_minimum_added_physical_damage = 2
    if (kind === 'wrong-maximum')
      m.values.attack_maximum_added_physical_damage = 5
    if (kind === 'extra-stat') m.values.unknown = 1
    if (kind === 'fractured') m.fractured = true
  }
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(forged)))
  await expect(
    applyCurrency(
      capture.before as ConcreteItem,
      'DIVINE',
      definitions,
      new AbortController().signal,
    ),
  ).rejects.toThrow('verify')
})
it.each([
  'missing',
  'wrong-minimum',
  'wrong-maximum',
  'extra-stat',
  'fractured',
])('rejects forged fixed Ring implicit %s on initial', async (kind) => {
  const forged = structuredClone(actual.initial)
  if (kind === 'missing') forged.state.implicits = []
  else {
    const m = forged.state.implicits[0]!
    if (kind === 'wrong-minimum')
      m.values.attack_minimum_added_physical_damage = 2
    if (kind === 'wrong-maximum')
      m.values.attack_maximum_added_physical_damage = 5
    if (kind === 'extra-stat') (m.values as Record<string, number>).unknown = 1
    if (kind === 'fractured') m.fractured = true
  }
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(forged)))
  await expect(
    loadInitial(82, new AbortController().signal, 'ring'),
  ).rejects.toThrow('verify')
})

it('shows the sourced fixed implicit instead of internal stat IDs', () => {
  const d = definitions['iron-ring:implicit:added-physical-damage-to-attacks']!
  const values = actual.initial.state.implicits[0]!.values
  expect(rolledText(d, values)).toBe('Adds 1 to 4 Physical Damage to Attacks')
  expect(
    rolledText(d, {
      attack_minimum_added_physical_damage: 1,
      attack_maximum_added_physical_damage: 4,
    }),
  ).toBe('Adds 1 to 4 Physical Damage to Attacks')
})
it('does not present forged fixed endpoints as the sourced implicit', () => {
  const d = definitions['iron-ring:implicit:added-physical-damage-to-attacks']!
  expect(
    rolledText(d, {
      attack_minimum_added_physical_damage: 2,
      attack_maximum_added_physical_damage: 4,
    }),
  ).not.toBe(d.text)
})
