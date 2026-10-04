import { afterEach, expect, it, vi } from 'vitest'
import { jsonResponse } from '../../shared/test/craftingFixtures'
import actual from '../../shared/test/bow-perfect-essence-responses.json'
import { applyCurrency } from './workbenchApi'
import type { AppliedItem, ConcreteItem, WorkbenchAction } from './workbenchApi'
import type { Definition } from './craftingApi'

afterEach(() => vi.unstubAllGlobals())
const definitions = actual.definitions as Record<string, Definition>
it.each(actual.captures)(
  'accepts actual $action with exact source and deterministic Battle',
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
    const capture = actual.captures[0]!
    const response = structuredClone(capture.result) as AppliedItem
    const added = response.events.find((e) => e.kind === 'ADD')!
    const modifier = response.state.explicits.find(
      (m) => m.modifierId === added.modifierId,
    )!
    const key = Object.keys(modifier.values)[0]!
    if (kind === 'missing') {
      delete modifier.values[key]
      delete added.values[key]
    } else if (kind === 'extra') {
      modifier.values['forged'] = 1
      added.values['forged'] = 1
    } else if (kind === 'out-of-range') {
      modifier.values[key] =
        definitions[modifier.modifierId]!.stats![0]!.max + 1
      added.values[key] = modifier.values[key]!
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
