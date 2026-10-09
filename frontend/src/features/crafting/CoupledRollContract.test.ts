import { describe, expect, it, vi } from 'vitest'
import {
  initialFixture,
  jsonResponse,
} from '../../shared/test/craftingFixtures'
import { applyCurrency, concreteInitial } from './workbenchApi'
import type { AppliedItem } from './workbenchApi'

const definitions = {
  ...initialFixture.modifiers,
  p: {
    ...initialFixture.modifiers.p!,
    stats: [
      { id: 'life', min: 10, max: 20 },
      { id: 'signed', min: -2, max: -1 },
    ],
  },
}
const input = concreteInitial(initialFixture)
function fixture(): AppliedItem {
  const values = { life: 15, signed: -2 }
  return {
    rulesetIdentity: 'fixture-ruleset',
    ruleVersion: 'contract-fixture',
    ledgerVersion: 'contract-fixture',
    snapshotId: input.snapshotId,
    action: 'TRANSMUTATION',
    applied: true,
    reason: '',
    state: {
      ...input,
      rarity: 'MAGIC',
      explicits: [{ modifierId: 'p', values }],
    },
    events: [
      { kind: 'ADD', modifierId: 'p', values, selectionProbability: 0.6 },
    ],
    consumedOmens: [],
    remainingOmens: [],
    assumptions: [
      {
        id: 'user-coupled-ratio-half-up-v1',
        candidateUnit: 'assumed model ticks',
        n: 10001,
        candidates: ['p'],
        min: 0,
        max: 10000,
        ratioTick: 5000,
        sourceUrl: 'https://poe2db.tw/us/Gloves_str',
        reason: 'User conjecture, not verified game outcomes.',
      },
    ],
  }
}
const apply = () =>
  applyCurrency(
    input,
    'TRANSMUTATION',
    definitions,
    new AbortController().signal,
    [],
    'fixture-ruleset',
  )

describe('coupled roll evidence contract', () => {
  it('verifies one shared tick including negative half ties without changing the input', async () => {
    vi.stubGlobal('fetch', () => Promise.resolve(jsonResponse(fixture())))
    const applied = await apply()
    expect(applied.state.explicits[0]?.values).toEqual({ life: 15, signed: -2 })
    expect(input.explicits).toEqual([])
  })

  it('rejects independent in-range values and missing or forged tick bindings', async () => {
    const valid = fixture()
    const variants = [
      { ...valid, assumptions: [] },
      {
        ...valid,
        assumptions: [{ ...valid.assumptions[0]!, ratioTick: undefined }],
      },
      { ...valid, assumptions: [{ ...valid.assumptions[0]!, ratioTick: -1 }] },
      {
        ...valid,
        assumptions: [{ ...valid.assumptions[0]!, ratioTick: 10001 }],
      },
      {
        ...valid,
        assumptions: [{ ...valid.assumptions[0]!, ratioTick: 5000.5 }],
      },
      { ...valid, assumptions: [{ ...valid.assumptions[0]!, n: 32 }] },
      {
        ...valid,
        assumptions: [{ ...valid.assumptions[0]!, candidates: ['s'] }],
      },
      { ...valid, assumptions: [valid.assumptions[0]!, valid.assumptions[0]!] },
      {
        ...valid,
        events: [{ ...valid.events[0]!, values: { life: 15, signed: -1 } }],
        state: {
          ...valid.state,
          explicits: [{ modifierId: 'p', values: { life: 15, signed: -1 } }],
        },
      },
    ]
    for (const result of variants) {
      vi.stubGlobal('fetch', () => Promise.resolve(jsonResponse(result)))
      await expect(apply()).rejects.toThrow(
        'Could not verify the coupled roll model',
      )
      expect(input.explicits).toEqual([])
    }
  })
})
