import { expect, it } from 'vitest'
import type { Definition } from '../craftingApi'
import { modifierPool, poolProbability } from './modifierPool'

function definition(id: string, values: Partial<Definition> = {}): Definition {
  return {
    id,
    name: id,
    text: '+(1—10) to maximum Life',
    tier: 2,
    affixType: 'PREFIX',
    layer: 'EXPLICIT',
    familyIds: ['life'],
    requiredItemLevel: 1,
    weight: 100,
    stats: [{ id: 'life', min: 1, max: 10 }],
    ...values,
  }
}
it('groups eligible tiers without mixing affix types, stat identities or special outcomes', () => {
  const unweighted = definition('unweighted')
  delete unweighted.weight
  const unknownLevel = definition('unknown-level')
  delete unknownLevel.requiredItemLevel
  const definitions = [
    definition('life-low'),
    definition('life-high', { tier: 1, requiredItemLevel: 80, weight: 20 }),
    definition('life-locked', { tier: 0, requiredItemLevel: 90, weight: 5 }),
    definition('hybrid', {
      stats: [
        { id: 'life', min: 1, max: 5 },
        { id: 'armour', min: 1, max: 10 },
      ],
    }),
    definition('suffix', { affixType: 'SUFFIX', weight: 40 }),
    definition('implicit', { layer: 'IMPLICIT', affixType: 'NONE' }),
    definition('essence', { weight: 0 }),
    unweighted,
    unknownLevel,
  ]
  const original = structuredClone(definitions)
  const [prefix, suffix] = modifierPool(definitions, 82)
  expect(prefix!.totalWeight).toBe(220)
  expect(
    prefix!.groups.map((group) => group.tiers.map((tier) => tier.id)),
  ).toEqual([['life-high', 'life-low'], ['hybrid']])
  expect(suffix!.totalWeight).toBe(40)
  expect(suffix!.groups).toHaveLength(1)
  expect(definitions).toEqual(original)
  expect(modifierPool(definitions, 1)[0]!.totalWeight).toBe(200)
})

it('normalizes each complete affix pool separately and handles an empty pool', () => {
  const sides = modifierPool(
    [
      definition('life'),
      definition('energy', {
        familyIds: ['energy'],
        stats: [{ id: 'energy', min: 1, max: 10 }],
        weight: 300,
      }),
      definition('suffix', { affixType: 'SUFFIX', weight: 50 }),
    ],
    82,
  )
  for (const side of sides)
    expect(
      side.groups.reduce(
        (sum, group) => sum + poolProbability(group.weight, side.totalWeight),
        0,
      ),
    ).toBeCloseTo(1)
  expect(
    poolProbability(sides[0]!.groups[0]!.weight, sides[0]!.totalWeight),
  ).toBe(0.25)
  expect(
    poolProbability(sides[1]!.groups[0]!.weight, sides[1]!.totalWeight),
  ).toBe(1)
  expect(poolProbability(0, 0)).toBe(0)
})
