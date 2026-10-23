import { expect, it } from 'vitest'
import { requestFixture } from '../path-tree/fixtures.test-support'
import type { AutomaticNode } from './automaticApi'
import {
  validateTierLaws,
  presentAutomaticItem,
  automaticItemIsDistribution,
} from './automaticTierLaws'
const node = (): AutomaticNode => ({
  id: 1,
  depth: 1,
  quality40: false,
  progress: { goal: 'MATCH', entries: [] },
  discoveryFinished: true,
  directFinished: true,
  item: {
    ...requestFixture().start.item,
    explicits: [{ modifierId: 'low', values: { life: 1 }, fractured: false }],
  },
  tiers: {
    low: {
      variants: [
        {
          modifierId: 'low',
          stat: 'life',
          min: 1,
          max: 2,
          probability: { numerator: '2', denominator: '3' },
        },
        {
          modifierId: 'high',
          stat: 'life',
          min: 4,
          max: 5,
          probability: { numerator: '1', denominator: '3' },
        },
      ],
    },
  },
})
it('preserves exact conditional weights and renders one property without a fabricated tier label', () => {
  const n = node(),
    before = JSON.stringify(n)
  expect(() => validateTierLaws(n)).not.toThrow()
  const card = presentAutomaticItem(n, 'en', (item) => ({
    name: 'Amulet',
    rarity: 'RARE',
    base: 'Amulet',
    itemClass: 'Amulet',
    itemLevel: 82,
    properties: [],
    requirements: [],
    flags: [],
    modifiers: item.explicits.map((m) => ({
      id: m.modifierId,
      text: `+${m.values.life} life`,
      affixLabel: `Tier ${m.modifierId}`,
    })),
  }))
  expect(card.modifiers).toHaveLength(1)
  expect(card.modifiers[0]!.affixLabel).toBeUndefined()
  expect(card.modifiers[0]!.text).toBe('life')
  expect(card.modifiers[0]!.tierRows!.map((r) => r.chance).join(' ')).toContain(
    '66.66666%',
  )
  expect(card.modifiers[0]!.tierRows!.map((r) => r.chance).join(' ')).toContain(
    '33.33333%',
  )
  expect(card.modifiers[0]!.tierRows!.map((r) => r.value)).toEqual([
    '+1–2',
    '+4–5',
  ])
  expect(JSON.stringify(n)).toBe(before)
})
it('rejects non-conserving laws even if percentages round to the same label', () => {
  const n = node()
  n.tiers!.low!.variants[1]!.probability.numerator = '2'
  expect(() => validateTierLaws(n)).toThrow('conserve mass')
})
it('accepts old saved nodes with no tier law', () => {
  const n = node()
  delete n.tiers
  expect(() => validateTierLaws(n)).not.toThrow()
})

it('shows current attribute support in T1-first order without moving its exact weights to another tier', () => {
  const n = node()
  n.attributes = {
    low: [
      {
        modifierId: 'low',
        stat: 'life',
        min: 1,
        max: 2,
        tier: 2,
        role: 'life',
      },
      {
        modifierId: 'high',
        stat: 'life',
        min: 4,
        max: 5,
        tier: 1,
        role: 'life',
      },
    ],
  }
  const card = presentAutomaticItem(n, 'en', (item) => ({
    name: 'Amulet',
    rarity: 'RARE',
    base: 'Amulet',
    itemClass: 'Amulet',
    itemLevel: 82,
    properties: [],
    requirements: [],
    flags: [],
    modifiers: item.explicits.map((m) => ({
      id: m.modifierId,
      text: `+${m.values.life} life`,
    })),
  }))
  expect(card.modifiers[0]!.tierRows!.map((r) => r.label)).toEqual(['T1', 'T2'])
  expect(card.modifiers[0]!.tierRows![0]!.chance).toContain('33.33333%')
  expect(card.modifiers[0]!.tierRows![1]!.chance).toContain('66.66666%')
})

it('allows a concrete generated point to be edited even when its source history is recorded', () => {
  const n = node()
  delete n.tiers
  n.attributes = {
    low: [
      {
        modifierId: 'low',
        stat: 'life',
        min: 1,
        max: 1,
        tier: 1,
        role: 'life',
      },
    ],
  }
  n.joint = [{ method: 'draw', source: 0, kind: 'GOAL', retained: {} }]
  expect(automaticItemIsDistribution(n)).toBe(false)
})
it('requires a concrete item before editing conditional ranges or alternative tier identities', () => {
  const n = node()
  expect(automaticItemIsDistribution(n)).toBe(true)
  n.attributes = {
    low: [
      {
        modifierId: 'low',
        stat: 'life',
        min: 1,
        max: 2,
        tier: 2,
        role: 'life',
      },
    ],
  }
  expect(automaticItemIsDistribution(n)).toBe(true)
  n.attributes.low![0]!.max = 1
  n.attributes.low!.push({
    modifierId: 'high',
    stat: 'life',
    min: 1,
    max: 1,
    tier: 1,
    role: 'life',
  })
  expect(automaticItemIsDistribution(n)).toBe(true)
})
