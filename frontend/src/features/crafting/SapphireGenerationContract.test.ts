import { afterEach, expect, it, vi } from 'vitest'
import actual from '../../shared/test/sapphire-generation-responses.json'
import { jsonResponse } from '../../shared/test/craftingFixtures'
import { applyCurrency } from './workbenchApi'
import type { ConcreteItem, WorkbenchAction } from './workbenchApi'
import type { Definition } from './craftingApi'

afterEach(() => vi.unstubAllGlobals())

it.each(actual.captures.map((capture, index) => ({ ...capture, index })))(
  'accepts actual Sapphire API response $index ($action)',
  async ({ before, action, activeOmens, result }) => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(result)))
    await expect(
      applyCurrency(
        before as ConcreteItem,
        action as WorkbenchAction,
        actual.definitions as Record<string, Definition>,
        new AbortController().signal,
        activeOmens,
        'fixture-ruleset',
      ),
    ).resolves.toEqual(result)
  },
)
