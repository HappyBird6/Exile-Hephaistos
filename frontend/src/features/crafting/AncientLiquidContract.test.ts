import { afterEach, expect, it, vi } from 'vitest'
import actual from '../../shared/test/ancient-liquid-responses.json'
import { jsonResponse } from '../../shared/test/craftingFixtures'
import { applyCurrency } from './workbenchApi'
import { verifiedHistoryState, verifiedFrameEvidence } from './workbenchHistory'
import type { ConcreteItem, WorkbenchAction, AppliedItem } from './workbenchApi'
import type { Definition, Initial } from './craftingApi'

afterEach(() => vi.unstubAllGlobals())
it.each(actual.captures.map((capture, index) => ({ ...capture, index })))(
  'accepts actual $base response $index ($action)',
  async ({ base, before, action, activeOmens, result }) => {
    const initial = (actual.initials as unknown as Record<string, Initial>)[
      base
    ]!
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(result)))
    await expect(
      applyCurrency(
        before as ConcreteItem,
        action as WorkbenchAction,
        initial.modifiers as Record<string, Definition>,
        new AbortController().signal,
        activeOmens,
      ),
    ).resolves.toEqual(result)
    expect(
      verifiedFrameEvidence(
        {
          state: result.state as ConcreteItem,
          action,
          evidence: result as unknown as AppliedItem,
        },
        initial,
      ),
    ).toEqual(result)
    expect(verifiedHistoryState(result.state as ConcreteItem, initial)).toBe(
      true,
    )
  },
)
