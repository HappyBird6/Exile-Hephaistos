import { afterEach, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { ItemCard } from './ItemCard'
import { whittlingCandidates } from './omenRemovalCandidates'
import {
  applyCurrency,
  compatibleOmenPair,
  concreteInitial,
} from './workbenchApi'
import type { AppliedItem } from './workbenchApi'
import type { Definition } from './craftingApi'
import {
  initialFixture,
  jsonResponse,
} from '../../shared/test/craftingFixtures'

afterEach(() => vi.unstubAllGlobals())
const pair = ['Omen_of_Sinistral_Erasure', 'Omen_of_Whittling']
const defs: Record<string, Definition> = {
  ...initialFixture.modifiers,
  p: { ...initialFixture.modifiers.p!, requiredItemLevel: 20 },
  p2: {
    ...initialFixture.modifiers.p!,
    id: 'p2',
    familyIds: ['p2'],
    requiredItemLevel: 20,
  },
  p3: {
    ...initialFixture.modifiers.p!,
    id: 'p3',
    familyIds: ['p3'],
    requiredItemLevel: 50,
  },
  s: { ...initialFixture.modifiers.s!, requiredItemLevel: 1 },
}
const mods = ['p', 'p2', 's'].map((id) => ({
  modifierId: id,
  values: Object.fromEntries(defs[id]!.stats!.map((s) => [s.id, s.min])),
}))
const before = {
  ...concreteInitial(initialFixture),
  rarity: 'RARE' as const,
  explicits: mods,
}

it('filters prefix first and highlights every minimum-level tie without fetching', () => {
  const fetch = vi.fn()
  vi.stubGlobal('fetch', fetch)
  expect(whittlingCandidates(before, defs, ['Omen_of_Whittling'])).toEqual([
    's',
  ])
  const ids = whittlingCandidates(before, defs, pair)
  expect(ids).toEqual(['p', 'p2'])
  render(
    <ItemCard
      item={{
        rarity: 'RARE',
        name: 'Fixture',
        base: null,
        itemClass: 'Amulets',
        itemLevel: 82,
        properties: [],
        requirements: [],
        flags: [],
        modifiers: mods.map((m) => ({
          id: m.modifierId,
          text: m.modifierId,
          kind: 'explicit',
          removalCandidate: ids.includes(m.modifierId),
        })),
      }}
    />,
  )
  expect(
    screen.getAllByTitle('Eligible Whittling removal candidate'),
  ).toHaveLength(2)
  expect(screen.getByText('s')).not.toHaveClass(
    'item-card__line--removal-candidate',
  )
  expect(fetch).not.toHaveBeenCalled()
})

it('preserves unresolved missing-level and fracture boundaries and supports symmetric Dextral', () => {
  const missing = { ...defs.p! }
  delete missing.requiredItemLevel
  expect(whittlingCandidates(before, { ...defs, p: missing }, pair)).toEqual([])
  expect(
    whittlingCandidates(
      before,
      { ...defs, p: { ...defs.p!, requiredItemLevel: 0 } },
      pair,
    ),
  ).toEqual([])
  expect(
    whittlingCandidates(
      {
        ...before,
        explicits: [{ ...mods[0]!, fractured: true }, ...mods.slice(1)],
      },
      defs,
      pair,
    ),
  ).toEqual([])
  expect(
    whittlingCandidates(before, defs, [
      'Omen_of_Dextral_Erasure',
      'Omen_of_Whittling',
    ]),
  ).toEqual(['s'])
  expect(compatibleOmenPair(...(pair as [string, string]))).toBe(true)
  expect(
    compatibleOmenPair(
      'Omen_of_Sinistral_Annulment',
      'Omen_of_Greater_Annulment',
    ),
  ).toBe(true)
  expect(
    compatibleOmenPair(
      'Omen_of_Dextral_Annulment',
      'Omen_of_Greater_Annulment',
    ),
  ).toBe(true)
})

function result(): AppliedItem {
  const added = { modifierId: 'p3', values: mods[0]!.values }
  return {
    rulesetIdentity: 'fixture-ruleset',
    ruleVersion: 'omen-composition-v1',
    ledgerVersion: 'fixture',
    snapshotId: before.snapshotId,
    action: 'PERFECT_CHAOS',
    applied: true,
    reason: '',
    state: { ...before, explicits: [mods[1]!, mods[2]!, added] },
    events: [
      {
        kind: 'REMOVE',
        modifierId: 'p',
        values: {},
        selectionProbability: 0.5,
      },
      { ...added, kind: 'ADD', selectionProbability: 1 },
    ],
    assumptions: [
      {
        id: 'uniform-removal-v1',
        candidateUnit: 'eligible explicit modifier instance',
        n: 2,
        candidates: ['p', 'p2'],
        min: null,
        max: null,
        sourceUrl: 'https://example.test',
        reason: 'Uniform model',
      },
    ],
    consumedOmens: pair,
    remainingOmens: ['Omen_of_the_Blessed'],
  }
}
async function apply(value: AppliedItem) {
  vi.stubGlobal('fetch', () => Promise.resolve(jsonResponse(value)))
  return applyCurrency(
    before,
    value.action,
    defs,
    new AbortController().signal,
    [...pair, 'Omen_of_the_Blessed'],
    'fixture-ruleset',
  )
}
it('accepts both consumed omens and rejects wrong side, tie probability, ledger and partial consumption', async () => {
  expect((await apply(result())).consumedOmens).toEqual(pair)
  const wrongSide = result()
  wrongSide.events[0]!.modifierId = 's'
  await expect(apply(wrongSide)).rejects.toThrow('Your item is unchanged')
  const wrongProbability = result()
  wrongProbability.events[0]!.selectionProbability = 1
  await expect(apply(wrongProbability)).rejects.toThrow(
    'Your item is unchanged',
  )
  const wrongLedger = result()
  wrongLedger.assumptions[0]!.candidates = ['p', 's']
  await expect(apply(wrongLedger)).rejects.toThrow('Your item is unchanged')
  const partial = result()
  partial.consumedOmens = [pair[0]!]
  partial.remainingOmens = [pair[1]!, 'Omen_of_the_Blessed']
  await expect(apply(partial)).rejects.toThrow('Your item is unchanged')
})
