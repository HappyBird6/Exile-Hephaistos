import { afterEach, expect, it, vi } from 'vitest'
import actual from '../../shared/test/belt-essence-responses.json'
import { jsonResponse } from '../../shared/test/craftingFixtures'
import { applyCurrency } from './workbenchApi'
import type { AppliedItem, ConcreteItem, WorkbenchAction } from './workbenchApi'
import type { Definition } from './craftingApi'
import { loadInitial } from './craftingApi'
import { maximumQuality } from './qualityLimit'
import { supportsConcreteStateShape } from './workbenchStateShape'
import { craftProbabilityEvidence } from './craftProbabilityEvidence'

afterEach(() => vi.unstubAllGlobals())
const definitions = actual.initial.modifiers as Record<string, Definition>

it.each(actual.captures)(
  'accepts actual $action with exact Belt targets and numeric source',
  async ({ action, before, result }) => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(result)))
    const verified = await applyCurrency(
      before as ConcreteItem,
      action as WorkbenchAction,
      definitions,
      new AbortController().signal,
      ['Omen_of_the_Blessed'],
      'fixture-ruleset',
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
    )!.sourceUrl = 'https://poe2db.tw/us/Belt_Armours_str'
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(forged)))
    await expect(
      applyCurrency(
        before as ConcreteItem,
        action as WorkbenchAction,
        definitions,
        new AbortController().signal,
        ['Omen_of_the_Blessed'],
        'fixture-ruleset',
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
])('refuses a forged %s Belt response', async (kind) => {
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
      'fixture-ruleset',
    ),
  ).rejects.toThrow('verify')
})

it('selects the actual seventh-base initial endpoint and refuses a substituted old base', async () => {
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
    loadInitial(82, new AbortController().signal, 'belt'),
  ).resolves.toMatchObject({
    state: { baseItemId: 'Metadata/Items/Belts/FourBelt1' },
  })
  expect(fetch.mock.calls[0]![0]).toBe(
    '/api/v1/crafting/workbench/initial?base=belt&itemLevel=82',
  )
  await expect(
    loadInitial(82, new AbortController().signal, 'belt'),
  ).rejects.toThrow('verify')
})

it('keeps unknown quality separate from unmodeled implicit, Charm and sockets', () => {
  const state = actual.captures[0]!.before as ConcreteItem
  expect(maximumQuality(state, definitions)).toBeNull()
  expect(supportsConcreteStateShape(state)).toBe(true)
  for (const extension of [
    { augmentSockets: 0 },
    { quality: 20 },
    { charmSlots: 2 },
    { flaskRecovery: 27 },
  ])
    expect(supportsConcreteStateShape({ ...state, ...extension })).toBe(false)
})

it('refuses an applied Divine response for an explicit-only Belt projection', async () => {
  const before = actual.captures[0]!.before as ConcreteItem
  const forged = structuredClone(actual.captures[0]!.result) as AppliedItem
  forged.action = 'DIVINE'
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(forged)))
  await expect(
    applyCurrency(
      before,
      'DIVINE',
      definitions,
      new AbortController().signal,
      ['Omen_of_the_Blessed'],
      'fixture-ruleset',
    ),
  ).rejects.toThrow('verify')
})
it('refuses an invented quality maximum for Belt responses', async () => {
  const capture = actual.captures[0]!
  const forged = structuredClone(capture.result) as AppliedItem
  forged.qualityLimit = { ruleVersion: 'quality-limit-v1', maximumQuality: 20 }
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(forged)))
  await expect(
    applyCurrency(
      capture.before as ConcreteItem,
      capture.action as WorkbenchAction,
      definitions,
      new AbortController().signal,
      ['Omen_of_the_Blessed'],
      'fixture-ruleset',
    ),
  ).rejects.toThrow('verify')
})
