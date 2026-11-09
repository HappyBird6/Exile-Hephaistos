import { expect, it } from 'vitest'
import { initialFixture } from '../../../shared/test/craftingFixtures'
import { concreteInitial } from '../workbenchApi'
import { emptyFilms, startFilm, currentFilm } from '../workbenchHistory'
import type { Film, Frame } from '../workbenchHistory'
import { craftingCost, currencyQuote } from './cost'
import type { PriceSnapshot } from './api'
const root = concreteInitial(initialFixture)
const frame = (action: string, omens: string[] = []): Frame => ({
  state: root,
  action,
  rulesetIdentity: 'rules',
  manual: { rulesetIdentity: 'rules', action, consumedOmens: omens },
})
const snapshot: PriceSnapshot = {
  league: 'Current League',
  updatedAt: '2026-11-09T00:00:00Z',
  exaltedPerDivine: 500,
  prices: {
    'Exalted Orb': { name: 'Exalted Orb', exalted: 1, divine: 0.002 },
    'Chaos Orb': { name: 'Chaos Orb', exalted: 10, divine: 0.02 },
  },
}
function film(frames: Frame[]): Film {
  return {
    ...currentFilm(startFilm(emptyFilms(), root, 'session', 'rules'))!,
    frames: [
      { state: root, action: null, rulesetIdentity: 'rules' },
      ...frames,
    ],
  }
}
it('counts only the selected path and retains the original currency receipts', () => {
  const session = film([frame('EXALTED'), frame('CHAOS'), frame('EXALTED')])
  const original = structuredClone(session)
  expect(craftingCost(session, 1, snapshot)).toMatchObject({
    exalted: 1,
    divine: 0.002,
    incomplete: false,
  })
  expect(craftingCost(session, 2, snapshot)).toMatchObject({
    exalted: 11,
    divine: 0.022,
    incomplete: false,
  })
  expect(craftingCost(session, 3, snapshot)).toMatchObject({
    exalted: 12,
    divine: 0.024,
  })
  expect(session).toEqual(original)
})
it('keeps missing currency prices distinct from a zero cost', () => {
  const cost = craftingCost(
    film([frame('EXALTED'), frame('DIVINE')]),
    2,
    snapshot,
  )
  expect(cost.exalted).toBe(1)
  expect(cost.unknown).toEqual(['Divine_Orb'])
  expect(cost.incomplete).toBe(true)
  expect(craftingCost(film([frame('EXALTED')]), 1, null).unknown).toEqual([
    'Exalted_Orb',
  ])
})
it('does not charge reveal observations or future receipts', () => {
  const session = film([
    frame('EXALTED'),
    { state: root, action: 'ABYSS_OBSERVE' },
    { state: root, action: 'invalid-future' },
  ])
  expect(craftingCost(session, 1, snapshot)).toMatchObject({
    exalted: 1,
    incomplete: false,
  })
  expect(craftingCost(session, 2, snapshot)).toMatchObject({
    exalted: 1,
    incomplete: false,
  })
})
it('looks up stable English resource names rather than localized labels', () => {
  expect(currencyQuote(snapshot, 'Exalted_Orb')?.exalted).toBe(1)
  expect(currencyQuote(snapshot, 'unknown')).toBeUndefined()
  expect(craftingCost(undefined, 0, snapshot)).toMatchObject({
    exalted: 0,
    hasCosts: false,
    incomplete: false,
  })
})
