import { afterEach, expect, it, vi } from 'vitest'
import actual from '../../shared/test/reviewed-belts-contract.json'
import { jsonResponse } from '../../shared/test/craftingFixtures'
import { applyCurrency } from './workbenchApi'
import type { ConcreteItem } from './workbenchApi'
import type { Definition } from './craftingApi'

afterEach(() => vi.unstubAllGlobals())
it.each(actual.captures)(
  'accepts actual $key craft with unsupported quality cap',
  async ({ before, result, definitions }) => {
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
it.each(actual.captures)(
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
