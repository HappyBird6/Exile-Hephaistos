import { afterEach, expect, it, vi } from 'vitest'
import actual from '../../shared/test/quiver-currency-responses.json'
import { jsonResponse } from '../../shared/test/craftingFixtures'
import { applyCurrency } from './workbenchApi'
import type { AppliedItem, ConcreteItem } from './workbenchApi'
import type { Definition } from './craftingApi'

afterEach(() => vi.unstubAllGlobals())

it.each(actual)(
  'accepts $key actual currency response with no quality cap',
  async (row) => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(row.result)))
    await expect(
      applyCurrency(
        row.before as ConcreteItem,
        'TRANSMUTATION',
        row.definitions as Record<string, Definition>,
        new AbortController().signal,
        [],
        'fixture-ruleset',
      ),
    ).resolves.toMatchObject({
      applied: true,
      qualityLimit: null,
      state: row.result.state,
    })
  },
)

it.each(actual)('rejects an invented quality cap for $key', async (row) => {
  const forged = structuredClone(row.result) as AppliedItem
  forged.qualityLimit = { ruleVersion: 'quality-limit-v1', maximumQuality: 20 }
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(forged)))
  await expect(
    applyCurrency(
      row.before as ConcreteItem,
      'TRANSMUTATION',
      row.definitions as Record<string, Definition>,
      new AbortController().signal,
      [],
      'fixture-ruleset',
    ),
  ).rejects.toThrow('Could not verify the applied item')
})
