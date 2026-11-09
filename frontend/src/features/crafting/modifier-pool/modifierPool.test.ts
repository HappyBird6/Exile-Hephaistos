import { expect, it } from 'vitest'
import { initialFixture } from '../../../shared/test/craftingFixtures'
import type { PoolEntry } from '../workbenchReferenceApi'
import type { Definition } from '../craftingApi'
import { modifierPoolSections, poolProbability } from './modifierPool'

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
it('keeps server-excluded tiers visible and out of the probability denominator', () => {
  const definitions = [
    definition('low'),
    definition('high', { tier: 1, weight: 20 }),
    definition('energy', {
      familyIds: ['energy'],
      weight: 300,
      stats: [{ id: 'energy', min: 1, max: 10 }],
    }),
  ]
  const initial = {
    ...initialFixture,
    modifiers: Object.fromEntries(definitions.map((d) => [d.id, d])),
  }
  const entries: PoolEntry[] = definitions.map((d) => ({
    modifierId: d.id,
    kind: 'ordinary',
    eligible: d.id !== 'high',
    reasons: d.id === 'high' ? ['ITEM_LEVEL'] : [],
    actions: [],
  }))
  const original = structuredClone(initial)
  const side = modifierPoolSections(initial, { entries })[0]!
  expect(side.totalWeight).toBe(400)
  expect(side.groups).toHaveLength(2)
  expect(side.groups[0]!.tiers.map((t) => t.id)).toContain('high')
  expect(side.groups[0]!.weight).toBe(100)
  expect(poolProbability(side.groups[0]!.weight, side.totalWeight)).toBe(0.25)
  expect(initial).toEqual(original)
})
it('uses server membership for special outcomes without natural weights', () => {
  const initial = {
    ...initialFixture,
    modifiers: { special: definition('special', { weight: 0 }) },
  }
  const entry: PoolEntry = {
    modifierId: 'special',
    kind: 'essence',
    eligible: true,
    reasons: [],
    actions: ['PERFECT_ESSENCE_ENHANCEMENT'],
  }
  const sides = modifierPoolSections(initial, { entries: [entry] })
  expect(sides[0]!.groups).toHaveLength(0)
  expect(sides[4]!.groups[0]!.actions).toEqual(entry.actions)
  expect(sides[4]!.groups[0]!.tiers[0]!.id).toBe('special')
})
it('normalizes eligible prefixes and suffixes separately and handles empty pools', () => {
  const initial = {
    ...initialFixture,
    modifiers: {
      p: definition('p'),
      s: definition('s', { affixType: 'SUFFIX', weight: 50 }),
    },
  }
  const sides = modifierPoolSections(initial, {
    entries: ['p', 's'].map((modifierId) => ({
      modifierId,
      kind: 'ordinary',
      eligible: true,
      reasons: [],
      actions: [],
    })),
  })
  expect(sides[0]!.totalWeight).toBe(100)
  expect(sides[1]!.totalWeight).toBe(50)
  for (const side of sides.slice(0, 2))
    expect(
      side.groups.reduce(
        (sum, g) => sum + poolProbability(g.weight, side.totalWeight),
        0,
      ),
    ).toBe(1)
  expect(poolProbability(0, 0)).toBe(0)
})
