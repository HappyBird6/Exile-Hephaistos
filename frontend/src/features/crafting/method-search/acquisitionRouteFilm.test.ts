import { expect, it } from 'vitest'
import type { PriceSnapshot } from '../currency-prices/api'
import { craftingCost } from '../currency-prices/cost'
import { requestFixture } from '../path-tree/fixtures.test-support'
import type { ConcreteItem } from '../workbenchApi'
import { filmRulesetStatus } from '../workbenchHistory'
import type { AcquisitionResult, AcquisitionStep } from './acquisitionRouteApi'
import {
  acquisitionRouteCost,
  acquisitionRouteFilm,
} from './acquisitionRouteFilm'

const request = requestFixture()
const state = request.start.item as ConcreteItem
const snapshot: PriceSnapshot = {
  league: 'test',
  updatedAt: '2026-11-09T00:00:00Z',
  exaltedPerDivine: 100,
  prices: { 'Divine Orb': { name: 'Divine Orb', divine: 1, exalted: 100 } },
}
function routeFilm(step: Partial<AcquisitionStep>) {
  const result: AcquisitionResult = {
    cards: [{ options: [] }],
    complete: true,
    states: [state],
    routes: [
      {
        cardIndex: 0,
        order: [],
        stateIndices: [0],
        probability: 1,
        steps: [
          { action: 'DIVINE', omens: [], acquired: [], lost: [], ...step },
        ],
      },
    ],
  }
  const film = acquisitionRouteFilm(request, result, 0, 'en')!
  return { film, cost: acquisitionRouteCost(result.routes[0]!, snapshot) }
}
it('uses the same verified expected Divine cost in the list and the restored film', () => {
  const { film, cost } = routeFilm({
    expectedUses: 2.5,
    maxUses: 1000,
    chance: { probability: 0.4 },
  })
  const saved = JSON.parse(JSON.stringify(film)) as typeof film
  expect(
    filmRulesetStatus(saved, request.start.provenance.rulesetIdentity),
  ).toBe('current')
  expect(saved.frames[1]!.projectedChance).toEqual({ probability: 0.4 })
  expect(saved.frames[1]!.projectedCurrencies![0]).toMatchObject({
    expectedUses: 2.5,
    maxUses: 1000,
  })
  expect(cost).toMatchObject({ divine: 2.5, exalted: 250, incomplete: false })
  expect(craftingCost(saved, 1, snapshot)).toEqual(cost)
})
it('keeps unknown Divine expectations as a lower bound and preserves explicit one-use intermediate Divine', () => {
  for (const expectedUses of [null, undefined]) {
    const { film, cost } = routeFilm({
      ...(expectedUses === undefined ? {} : { expectedUses }),
      chance: { probability: null },
    })
    expect(film.frames[1]!.projectedChance).toEqual({ probability: null })
    expect(cost).toMatchObject({
      incomplete: true,
      unknownUses: true,
      divine: 1,
    })
    expect(craftingCost(film, 1, snapshot)).toEqual(cost)
  }
  const { film, cost } = routeFilm({ expectedUses: 1 })
  expect(cost).toMatchObject({ divine: 1, incomplete: false })
  expect(film.frames[1]).not.toHaveProperty('projectedChance')
})
it('keeps the concrete tier and omens when the selected action is stored', () => {
  const { film } = routeFilm({
    action: 'GREATER_EXALTED',
    omens: ['Omen_of_Dextral_Exaltation'],
    chance: { probability: 0 },
  })
  expect(film.frames[1]).toMatchObject({
    projectedChance: { probability: 0 },
    projectedCurrencies: [
      {
        currencyId: 'Greater_Exalted_Orb',
        omenIds: ['Omen_of_Dextral_Exaltation'],
      },
    ],
  })
})
