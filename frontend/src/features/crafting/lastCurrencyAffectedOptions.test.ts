import { expect, it } from 'vitest'
import { lastCurrencyAffectedOptions } from './lastCurrencyAffectedOptions'
import { initialFixture } from '../../shared/test/craftingFixtures'
import { concreteInitial } from './workbenchApi'
import type { AppliedItem, ConcreteItem, WorkbenchAction } from './workbenchApi'
import {
  currentFilm,
  emptyFilms,
  recordAbyss,
  startFilm,
  storedAbyssItem,
} from './workbenchHistory'
import type { Film } from './workbenchHistory'
import {
  repeatInitial,
  requestFor,
} from './chaos-repeat/api/fixtures.test-support'
import { repeatItem } from './chaos-repeat/api/stateValidation'

const p = { modifierId: 'p', values: { life: 17 } }
const s = { modifierId: 's', values: { strength: 6 } }
const rare: ConcreteItem = {
  ...concreteInitial(initialFixture),
  rarity: 'RARE',
  explicits: [p, s],
}
function history(
  before: ConcreteItem,
  after: ConcreteItem,
  action: WorkbenchAction = 'CHAOS',
  events: AppliedItem['events'] = [],
): Film {
  return {
    id: 'film',
    createdAt: '',
    rulesetIdentity: initialFixture.rulesetIdentity,
    frames: [
      {
        state: before,
        action: null,
        rulesetIdentity: initialFixture.rulesetIdentity,
      },
      {
        state: after,
        action,
        rulesetIdentity: initialFixture.rulesetIdentity,
        evidence: {
          action,
          rulesetIdentity: initialFixture.rulesetIdentity,
          snapshotId: 'fixture-v1',
          ruleVersion: 'test',
          ledgerVersion: 'test',
          consumedOmens: [],
          events,
          assumptions: [],
        },
      },
    ],
  }
}
it('marks only the added option and resets at the root or without history', () => {
  const before = concreteInitial(initialFixture)
  const after = { ...before, rarity: 'MAGIC' as const, explicits: [p] }
  const film = history(before, after, 'TRANSMUTATION')
  expect([...lastCurrencyAffectedOptions(film, 1, initialFixture)]).toEqual([
    'p',
  ])
  expect([...lastCurrencyAffectedOptions(film, 0, initialFixture)]).toEqual([])
  expect([
    ...lastCurrencyAffectedOptions(undefined, 0, initialFixture),
  ]).toEqual([])
})
it('marks an actual reroll even when it produces the same numeric value', () => {
  const film = history(rare, structuredClone(rare), 'DIVINE', [
    {
      kind: 'REROLL_EXPLICIT',
      modifierId: 'p',
      values: p.values,
      selectionProbability: 1,
    },
  ])
  expect([...lastCurrencyAffectedOptions(film, 1, initialFixture)]).toEqual([
    'p',
  ])
})
it('marks changed rolls and fracture state without marking untouched options', () => {
  const after = { ...rare, explicits: [{ ...p, fractured: true }, s] }
  expect([
    ...lastCurrencyAffectedOptions(
      history(rare, after, 'FRACTURING'),
      1,
      initialFixture,
    ),
  ]).toEqual(['p'])
  const rolled = { ...rare, explicits: [p, { ...s, values: { strength: 8 } }] }
  expect([
    ...lastCurrencyAffectedOptions(history(rare, rolled), 1, initialFixture),
  ]).toEqual(['s'])
})
it('does not create a marker for a removed option or a failed/no-op currency', () => {
  const after = { ...rare, explicits: [s] }
  const film = history(rare, after, 'ANNULMENT', [
    { kind: 'REMOVE', modifierId: 'p', values: {}, selectionProbability: 1 },
  ])
  expect([...lastCurrencyAffectedOptions(film, 1, initialFixture)]).toEqual([])
  expect([
    ...lastCurrencyAffectedOptions(history(rare, rare), 1, initialFixture),
  ]).toEqual([])
})
it('uses each selected step independently, preserving future frames', () => {
  const first = { ...rare, explicits: [{ ...p, values: { life: 18 } }, s] }
  const second = {
    ...first,
    explicits: [first.explicits[0]!, { ...s, values: { strength: 8 } }],
  }
  const film = history(rare, first)
  film.frames.push(history(first, second).frames[1]!)
  const original = JSON.stringify(film)
  expect([...lastCurrencyAffectedOptions(film, 1, initialFixture)]).toEqual([
    'p',
  ])
  expect([...lastCurrencyAffectedOptions(film, 2, initialFixture)]).toEqual([
    's',
  ])
  expect(JSON.stringify(film)).toBe(original)
})
it('marks only options whose catalyst projection changed', () => {
  const initial = {
    ...initialFixture,
    state: {
      ...initialFixture.state,
      baseItemId: 'Metadata/Items/Amulets/FourAmulet9',
    },
    modifiers: {
      ...initialFixture.modifiers,
      p: {
        ...initialFixture.modifiers.p!,
        weight: 100,
        tags: ['life'],
        stats: [{ id: 'base_maximum_life', min: 10, max: 20 }],
      },
    },
  }
  const before = {
    ...concreteInitial(initial),
    rarity: 'RARE' as const,
    explicits: [{ modifierId: 'p', values: { base_maximum_life: 17 } }, s],
  }
  const after = {
    ...before,
    catalystQuality: { type: 'FLESH' as const, amount: 20 },
  }
  expect([
    ...lastCurrencyAffectedOptions(
      history(before, after, 'CATALYST_FLESH'),
      1,
      initial,
    ),
  ]).toEqual(['p'])
})
it('preserves the affected slot across a manual Abyss reveal without attributing unrelated options to it', () => {
  const request = requestFor('ABYSS')
  if (request.state.kind !== 'ABYSS') throw new Error('Abyss fixture required')
  const hidden = request.state.abyss
  const before = { ...repeatItem(request.state), desecrated: [] }
  let films = recordAbyss(
    startFilm(emptyFilms(), before, 'abyss', repeatInitial.rulesetIdentity),
    before,
    hidden,
    'Preserved_Rib',
    'unused',
    repeatInitial.rulesetIdentity,
  )
  const film = currentFilm(films)!
  expect([...lastCurrencyAffectedOptions(film, 1, repeatInitial)]).toEqual([
    'hidden',
  ])
  const revealed = {
    ...hidden,
    slots: hidden.slots.map((slot) =>
      slot.id === 'hidden' ? { ...slot, modifier: s } : slot,
    ),
  }
  films = recordAbyss(
    films,
    storedAbyssItem(hidden),
    revealed,
    'ABYSS_SELECT',
    'unused',
    repeatInitial.rulesetIdentity,
  )
  expect([
    ...lastCurrencyAffectedOptions(currentFilm(films), 2, repeatInitial),
  ]).toEqual(['s'])
})
it('marks a repeated currency record from its before/final states and actual final witness', () => {
  const after = {
    ...rare,
    explicits: [
      { ...p, values: { life: 18 } },
      { ...s, values: { strength: 8 } },
    ],
  }
  const film = history(rare, after, 'CHAOS', [
    { kind: 'REMOVE', modifierId: 'p', values: {}, selectionProbability: 1 },
    {
      kind: 'ADD',
      modifierId: 'p',
      values: { life: 18 },
      selectionProbability: 1,
    },
  ])
  film.frames[1]!.chaosRepeat = { version: 1, action: 'CHAOS', uses: 20 }
  expect([...lastCurrencyAffectedOptions(film, 1, initialFixture)]).toEqual([
    'p',
    's',
  ])
})
it('omits markers when stored source/evidence cannot be verified', () => {
  const film = history(rare, {
    ...rare,
    explicits: [{ ...p, values: { life: 18 } }, s],
  })
  film.frames[1]!.evidence!.rulesetIdentity = 'other'
  expect([...lastCurrencyAffectedOptions(film, 1, initialFixture)]).toEqual([])
})
