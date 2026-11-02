import { afterEach, expect, it, vi } from 'vitest'
import actual from '../../shared/test/reviewed-belts-contract.json'
import { jsonResponse } from '../../shared/test/craftingFixtures'
import { applyCurrency } from './workbenchApi'
import { baseRarityAllowed } from './baseRegistry'
import type { ConcreteItem } from './workbenchApi'
import type { Definition } from './craftingApi'

afterEach(() => vi.unstubAllGlobals())
it.each(actual.captures)(
  'handles captured $key craft under the current rarity policy',
  async ({ before, result, definitions }) => {
    if (!baseRarityAllowed(before.baseItemId, before.rarity)) {
      await expect(
        applyCurrency(
          before as ConcreteItem,
          'TRANSMUTATION',
          definitions as Record<string, Definition>,
          new AbortController().signal,
          [],
          'fixture-ruleset',
        ),
      ).rejects.toThrow('unsupported properties')
      return
    }
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(result)))
    await expect(
      applyCurrency(
        before as ConcreteItem,
        'TRANSMUTATION',
        definitions as Record<string, Definition>,
        new AbortController().signal,
        [],
        'fixture-ruleset',
      ),
    ).resolves.toMatchObject({ applied: true, qualityLimit: null })
  },
)
it.each(
  actual.captures.filter(({ before }) =>
    baseRarityAllowed(before.baseItemId, before.rarity),
  ),
)(
  'rejects invented $key quality cap',
  async ({ before, result, definitions }) => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        jsonResponse({
          ...result,
          qualityLimit: { ruleVersion: 'quality-limit-v1', maximumQuality: 20 },
        }),
      ),
    )
    await expect(
      applyCurrency(
        before as ConcreteItem,
        'TRANSMUTATION',
        definitions as Record<string, Definition>,
        new AbortController().signal,
        [],
        'fixture-ruleset',
      ),
    ).rejects.toThrow('verify')
  },
)
