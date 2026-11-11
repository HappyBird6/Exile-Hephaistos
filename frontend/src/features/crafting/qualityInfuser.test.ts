import { afterEach, expect, it, vi } from 'vitest'
import { initialFixture } from '../../shared/test/craftingFixtures'
import { applyCurrency, concreteInitial } from './workbenchApi'
import type { AppliedItem } from './workbenchApi'
import {
  infuserSupportsBase,
  infuserTransitionMatches,
  qualityInfuserPolicy,
} from './qualityInfuser'
import { sameItemState } from './crafting-state/itemIdentity'
import { favoriteCheckpoint } from './base-favorites/repository'
it('validates a one-use jewellery infusion without changing rolls and retains its flag in favorite checkpoints', () => {
  const before = {
    ...concreteInitial(initialFixture),
    baseItemId: 'Metadata/Items/Amulets/FourAmulet9',
    catalystQuality: { type: 'NECROTIC' as const, amount: 7 },
  }
  const state = {
    ...before,
    qualityInfused: true,
    catalystQuality: { ...before.catalystQuality, amount: 17 },
  }
  const result = {
    state,
    applied: true,
    events: [],
    consumedOmens: [],
    assumptions: [{ id: qualityInfuserPolicy, min: 17, max: 17 }],
  } as unknown as AppliedItem
  expect(
    infuserTransitionMatches(
      before,
      result,
      'VAAL_CATALYSING_INFUSER',
      initialFixture.modifiers,
    ),
  ).toBe(true)
  expect(
    infuserTransitionMatches(
      state,
      {
        ...result,
        state: {
          ...state,
          catalystQuality: { ...state.catalystQuality, amount: 27 },
        },
      },
      'VAAL_CATALYSING_INFUSER',
      initialFixture.modifiers,
    ),
  ).toBe(false)
  expect(
    infuserTransitionMatches(
      state,
      { ...result, state: { ...state, qualityInfused: false } },
      'EXALTED',
      initialFixture.modifiers,
    ),
  ).toBe(false)
  expect(sameItemState(before, { ...before, qualityInfused: false })).toBe(true)
  const saved = favoriteCheckpoint({
    id: 'test',
    createdAt: new Date().toISOString(),
    frames: [{ state, action: null }],
  })
  expect(saved.frames[0]?.state.qualityInfused).toBe(true)
})
it('keeps four infuser targets distinct', () => {
  expect(
    infuserSupportsBase(
      'Vaal_Arcanists_Infuser',
      'Metadata/Items/Weapons/OneHandWeapons/Wands/Wand1',
    ),
  ).toBe(true)
  expect(
    infuserSupportsBase(
      'Vaal_Blacksmiths_Infuser',
      'Metadata/Items/Weapons/OneHandWeapons/Wands/Wand1',
    ),
  ).toBe(false)
  expect(
    infuserSupportsBase(
      'Vaal_Catalysing_Infuser',
      'Metadata/Items/Jewels/JewelInt',
    ),
  ).toBe(false)
})

afterEach(() => vi.unstubAllGlobals())
it('accepts infusion through the complete response validator instead of the unchanged-quality policy', async () => {
  const before = {
    ...concreteInitial(initialFixture),
    baseItemId: 'Metadata/Items/Amulets/FourAmulet9',
    catalystQuality: { type: 'NECROTIC' as const, amount: 7 },
  }
  const state = {
    ...before,
    qualityInfused: true,
    catalystQuality: { ...before.catalystQuality, amount: 17 },
  }
  const result = {
    state,
    snapshotId: before.snapshotId,
    rulesetIdentity: 'fixture-ruleset',
    ruleVersion: 'test',
    ledgerVersion: 'test',
    action: 'VAAL_CATALYSING_INFUSER',
    applied: true,
    reason: '',
    events: [],
    consumedOmens: [],
    remainingOmens: [],
    assumptions: [
      {
        id: qualityInfuserPolicy,
        min: 17,
        max: 17,
        candidateUnit: 'quality',
        n: 1,
        candidates: [],
        sourceUrl: 'https://poe2db.tw/us/Vaal_Catalysing_Infuser',
        reason: 'Simulator policy',
      },
    ],
  }
  vi.stubGlobal(
    'fetch',
    async () =>
      new Response(JSON.stringify(result), {
        status: 200,
        headers: { 'X-Crafting-Ruleset': 'fixture-ruleset' },
      }),
  )
  expect(
    (
      await applyCurrency(
        before,
        'VAAL_CATALYSING_INFUSER',
        initialFixture.modifiers,
        new AbortController().signal,
        [],
        'fixture-ruleset',
      )
    ).state,
  ).toEqual(state)
})
