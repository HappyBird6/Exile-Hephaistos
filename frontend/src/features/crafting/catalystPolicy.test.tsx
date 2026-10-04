import { expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { roundQualityRatio } from './qualityRoundingPolicy'
import {
  catalystActionType,
  workbenchCurrencyActions,
  applyCurrency,
  concreteInitial,
} from './workbenchApi'
import { initialFixture } from '../../shared/test/craftingFixtures'
import { catalystProjection } from './catalystQuality'
import { ItemCard } from './ItemCard'

it('uses one replaceable signed half-up quality ratio policy', () => {
  expect(roundQualityRatio(3480n, 100n)).toBe(35)
  expect(roundQualityRatio(3450n, 100n)).toBe(35)
  expect(roundQualityRatio(-3450n, 100n)).toBe(-35)
  expect(roundQualityRatio(3449n, 100n)).toBe(34)
})
it('connects all 26 stable catalyst identities to distinct actions', () => {
  const actions = Object.entries(workbenchCurrencyActions).filter(([id]) =>
    id.endsWith('_Catalyst'),
  )
  expect(actions).toHaveLength(26)
  expect(new Set(actions.map(([, action]) => action)).size).toBe(26)
  expect(actions.every(([, action]) => catalystActionType(action))).toBe(true)
})
it('derives from canonical rolls once when quality types change repeatedly', () => {
  const definition = {
    ...initialFixture.modifiers.p!,
    id: 'life',
    weight: 1000,
    tags: ['life'],
    stats: [{ id: 'base_maximum_life', min: 20, max: 29 }],
  }
  const values = { base_maximum_life: 29 }
  for (let i = 0; i < 5; i++) {
    expect(
      catalystProjection(definition, values, { type: 'FLESH', amount: 20 })
        .values,
    ).toEqual({ base_maximum_life: 35 })
    expect(
      catalystProjection(definition, values, { type: 'NEURAL', amount: 20 })
        .values,
    ).toBe(values)
  }
  expect(values.base_maximum_life).toBe(29)
})
it('rejects a catalyst response that silently changes canonical rolls', async () => {
  const state = {
    ...concreteInitial(initialFixture),
    baseItemId: 'Metadata/Items/Amulets/FourAmulet9',
  }
  const next = {
    ...state,
    catalystQuality: { type: 'FLESH', amount: 20 },
    explicits: [{ modifierId: 'p', values: { life: 17 } }],
  }
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          ruleVersion: 'test',
          ledgerVersion: 'test',
          snapshotId: state.snapshotId,
          state: next,
          action: 'CATALYST_FLESH',
          applied: true,
          reason: '',
          events: [],
          assumptions: [],
          consumedOmens: [],
          remainingOmens: [],
        }),
      ),
    ),
  )
  try {
    await expect(
      applyCurrency(
        state,
        'CATALYST_FLESH',
        initialFixture.modifiers,
        new AbortController().signal,
      ),
    ).rejects.toThrow('verify')
  } finally {
    vi.unstubAllGlobals()
  }
})
it('places fractured suffix first normally and returns to its original position for Alt display', () => {
  const item = {
    rarity: 'RARE' as const,
    name: 'Synthetic',
    base: 'Synthetic',
    itemClass: 'Amulet',
    itemLevel: 82,
    properties: [],
    requirements: [],
    flags: [],
    modifiers: [
      { id: 'prefix', text: '+29 Life', kind: 'explicit' as const },
      {
        id: 'suffix',
        text: '+10 Resistance',
        kind: 'explicit' as const,
        fractured: true,
      },
    ],
  }
  const view = render(<ItemCard item={item} />)
  const lines = () =>
    Array.from(
      screen.getByRole('article').querySelectorAll('.item-card__line'),
    ).map((e) => e.textContent)
  expect(lines()[0]).toContain('Resistance')
  expect(screen.getByRole('article').textContent).not.toContain('[Fractured]')
  expect(
    screen.getByRole('article').querySelector('[data-fractured]'),
  ).toBeTruthy()
  view.rerender(<ItemCard item={item} showOriginalOrder />)
  expect(lines()[0]).toContain('Life')
})
