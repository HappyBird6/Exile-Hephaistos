import { expect, it } from 'vitest'
import type { PriceSnapshot } from '../currency-prices/api'
import { craftingCost } from '../currency-prices/cost'
import { requestFixture } from '../path-tree/fixtures.test-support'
import type { ConcreteItem } from '../workbenchApi'
import { planTitle } from './probabilityPresentation'
import {
  filmRulesetStatus,
  validStepChance,
  projectedWhittlingOutcomes,
  validProjectedCurrencies,
} from '../workbenchHistory'
import { formatExpectedUses } from '../currency-prices/formatCurrencyAmount'
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
  prices: {
    'Divine Orb': { name: 'Divine Orb', divine: 1, exalted: 100 },
    'Chaos Orb': { name: 'Chaos Orb', divine: 0.02, exalted: 2 },
    'Omen of Whittling': { name: 'Omen of Whittling', divine: 1, exalted: 100 },
  },
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
it('preserves a bounded step independently of the session success probability', () => {
  const chance = {
    probability: null,
    probabilityBounds: { lower: 0.02, upper: 0.9 },
  }
  const { film } = routeFilm({ chance })
  expect(film.projectedRouteChance).toEqual({ probability: 1 })
  expect(film.frames[1]?.projectedChance).toEqual(chance)
  expect(
    validStepChance({
      probability: null,
      probabilityBounds: { lower: 0.9, upper: 0.02 },
    }),
  ).toBe(false)
  expect(
    validStepChance({
      probability: 0.95,
      probabilityBounds: chance.probabilityBounds,
    }),
  ).toBe(false)
})
it('restores the three outcomes only for a valid Whittling Chaos combination', () => {
  const omenOutcomes = {
    success: { probability: 0.2 },
    recoverable: { probability: 0.55 },
    failure: { probability: 0.25 },
  }
  for (const side of [
    null,
    'Omen_of_Sinistral_Erasure',
    'Omen_of_Dextral_Erasure',
  ]) {
    const omens = side ? ['Omen_of_Whittling', side] : ['Omen_of_Whittling']
    const step = { action: 'CHAOS', omens, omenOutcomes }
    const { film } = routeFilm(step)
    expect(projectedWhittlingOutcomes(film.frames[1]!)).toEqual(omenOutcomes)
  }
  const plain = routeFilm({ action: 'CHAOS', omens: [], omenOutcomes })
  expect(projectedWhittlingOutcomes(plain.film.frames[1]!)).toBeUndefined()
  const overlaps = routeFilm({
    action: 'CHAOS',
    omens: ['Omen_of_Whittling'],
    omenOutcomes: { ...omenOutcomes, failure: { probability: 0.8 } },
  })
  expect(projectedWhittlingOutcomes(overlaps.film.frames[1]!)).toBeUndefined()
})
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

it('charges a fresh omen on each verified same-base repeat, including after saving', () => {
  const { film, cost } = routeFilm({
    action: 'CHAOS',
    omens: ['Omen_of_Whittling'],
    expectedUses: 2.5,
    maxUses: 1000,
  })
  expect(cost).toMatchObject({ divine: 2.55, exalted: 255, incomplete: false })
  expect(craftingCost(JSON.parse(JSON.stringify(film)), 1, snapshot)).toEqual(
    cost,
  )
})

it.each([false, true])(
  'keeps unknown Light retry costs in the list and in a saved reveal (shown bone %s)',
  (shownBone) => {
    const slot = { id: 'd', side: 'PREFIX' as const, modifierId: null }
    const hidden = { ...state, desecrated: [slot] }
    const after = {
      ...state,
      desecrated: [{ ...slot, modifierId: 'chosen' }],
      explicits: [
        ...state.explicits,
        { modifierId: 'chosen', values: { life: 3 } },
      ],
    }
    const req = {
      ...request,
      start: {
        ...request.start,
        item: {
          ...request.start.item,
          ...(shownBone ? {} : { desecrated: [slot] }),
        },
      },
    }
    const setup: AcquisitionStep[] = shownBone
      ? [{ action: 'Preserved_Collarbone', omens: [], acquired: [], lost: [] }]
      : []
    const result: AcquisitionResult = {
      cards: [{ options: [] }],
      complete: false,
      states: [hidden, after],
      routes: [
        {
          cardIndex: 0,
          order: [2],
          probability: null,
          stateIndices: shownBone ? [0, 1] : [1],
          steps: [
            ...setup,
            {
              action: 'REVEAL',
              maxUses: 1000,
              chance: { probability: null },
              omens: [],
              acquired: [2],
              lost: [],
            },
          ],
        },
      ],
    }
    const cap = 1000
    const market = {
      ...snapshot,
      prices: {
        ...snapshot.prices,
        'Preserved Collarbone': {
          name: 'Preserved Collarbone',
          divine: 0.1,
          exalted: 10,
        },
      },
    }
    const route = result.routes[0]!
    const film = acquisitionRouteFilm(req, result, 0, 'en')!
    const saved = JSON.parse(JSON.stringify(film)) as typeof film
    const cost = acquisitionRouteCost(route, market)
    expect(saved.frames.at(-1)).toMatchObject({
      projectedRevealLimit: cap,
      projectedChance: { probability: null },
    })
    expect(filmRulesetStatus(saved, req.start.provenance.rulesetIdentity)).toBe(
      'current',
    )
    expect(cost).toMatchObject({
      hasCosts: true,
      incomplete: true,
      unknownUses: true,
      retryMaterials: [
        {
          currencyId: 'Orb_of_Annulment',
          omenIds: ['Omen_of_Light'],
          bone: shownBone ? 'Preserved_Collarbone' : null,
        },
      ],
      exalted: shownBone ? 10 : 0,
    })
    expect(craftingCost(saved, saved.frames.length - 1, market)).toEqual(cost)
    if (shownBone)
      expect(craftingCost(saved, 1, market)).toMatchObject({
        incomplete: false,
        exalted: 10,
      })
    expect(planTitle(cost, 'en', 'Unresolved')).toBe(
      'Expected number of uses unknown · Unresolved',
    )
    const normal = structuredClone(route)
    delete normal.steps.at(-1)!.maxUses
    expect(acquisitionRouteCost(normal, market)).not.toHaveProperty(
      'retryMaterials',
    )
  },
)

it('preserves a bounded Chaos mean below one and prices every fractional orb and omen', () => {
  const options = {
    omens: ['Omen_of_Whittling'],
    maxUses: 1000,
    expectedUses: 0.125,
  }
  for (const [action, id] of [
    ['CHAOS', 'Chaos_Orb'],
    ['GREATER_CHAOS', 'Greater_Chaos_Orb'],
    ['PERFECT_CHAOS', 'Perfect_Chaos_Orb'],
  ] as const) {
    const { film, cost } = routeFilm({ ...options, action })
    const saved = JSON.parse(JSON.stringify(film)) as typeof film
    expect(saved.frames[1]!.projectedCurrencies).toMatchObject([
      { currencyId: id, expectedUses: 0.125, omensPerUse: true },
    ])
    expect(
      filmRulesetStatus(saved, request.start.provenance.rulesetIdentity),
    ).toBe('current')
    expect(craftingCost(saved, 1, snapshot)).toEqual(cost)
    if (action === 'CHAOS')
      expect(cost).toMatchObject({
        divine: 0.1275,
        exalted: 12.75,
        incomplete: false,
      })
  }
  expect(formatExpectedUses(0.00012, 'Chaos_Orb', 'en')).toBe('0.00012')
  expect(
    validProjectedCurrencies([
      { currencyId: 'Chaos_Orb', omenIds: [], ...options, expectedUses: 0 },
    ]),
  ).toBe(false)
  expect(
    validProjectedCurrencies([
      { currencyId: 'Chaos_Orb', omenIds: [], expectedUses: 0.125 },
    ]),
  ).toBe(false)
  expect(
    validProjectedCurrencies([
      { currencyId: 'Divine_Orb', omenIds: [], ...options },
    ]),
  ).toBe(false)
})
it('shows the materials for unknown bounded Chaos consumption without assuming even one guaranteed attempt', () => {
  const { film, cost } = routeFilm({
    action: 'CHAOS',
    omens: ['Omen_of_Whittling'],
    maxUses: 1000,
    chance: { probability: null },
  })
  expect(cost).toMatchObject({
    divine: 0,
    exalted: 0,
    incomplete: true,
    unknownUses: true,
    retryMaterials: [
      { currencyId: 'Chaos_Orb', omenIds: ['Omen_of_Whittling'] },
    ],
  })
  expect(craftingCost(film, 1, snapshot)).toEqual(cost)
  expect(planTitle(cost, 'en', 'Unresolved')).toBe(
    'Expected number of uses unknown · Unresolved',
  )
})
