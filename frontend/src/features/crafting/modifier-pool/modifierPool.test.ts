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

it('recalculates the available pool after additions and restores all tiers in static view', async () => {
  const { modifierPoolSections } = await import('./modifierPool')
  const { initialFixture } =
    await import('../../../shared/test/craftingFixtures')
  const { concreteInitial } = await import('../workbenchApi')
  const defs = [
    definition('life'),
    definition('energy', {
      familyIds: ['energy'],
      stats: [{ id: 'energy', min: 1, max: 10 }],
      weight: 300,
    }),
    definition('locked', { familyIds: ['locked'], requiredItemLevel: 90 }),
    definition('strength', { affixType: 'SUFFIX', familyIds: ['strength'] }),
  ]
  const initial = {
    ...initialFixture,
    modifiers: Object.fromEntries(defs.map((d) => [d.id, d])),
  }
  const item = {
    ...concreteInitial(initial),
    rarity: 'RARE' as const,
    explicits: [{ modifierId: 'life', values: { life: 1 } }],
  }
  const dynamic = modifierPoolSections(initial, 82, 'dynamic', item)
  expect(dynamic[0]!.totalWeight).toBe(300)
  expect(dynamic[0]!.groups[0]!.tiers[0]!.id).toBe('energy')
  const all = modifierPoolSections(initial, 82, 'static', item)
  expect(all[0]!.totalWeight).toBe(500)
  expect(all[0]!.groups.flatMap((g) => g.tiers.map((t) => t.id))).toContain(
    'locked',
  )
  const magic = { ...item, rarity: 'MAGIC' as const }
  expect(
    modifierPoolSections(initial, 82, 'dynamic', magic)[0]!.totalWeight,
  ).toBe(0)
  const hidden = {
    ...magic,
    explicits: [],
    desecrated: [{ id: 'hidden', side: 'PREFIX' as const, modifierId: null }],
  }
  expect(
    modifierPoolSections(initial, 82, 'dynamic', hidden)[0]!.totalWeight,
  ).toBe(0)
  expect(
    modifierPoolSections(initial, 82, 'dynamic', { ...item, explicits: [] })[0]!
      .totalWeight,
  ).toBe(400)
})
it('keeps reveal-only and essence outcomes separate from ordinary weights', async () => {
  const { modifierPoolSections } = await import('./modifierPool')
  const { initialFixture } =
    await import('../../../shared/test/craftingFixtures')
  const { concreteInitial } = await import('../workbenchApi')
  const reveal = definition('reveal:spell', {
    requiredItemLevel: 40,
    weight: 0,
    tags: ['reveal_only', 'amanamu_mod'],
  })
  const essence = definition('amulet:prefix:essence-global-defences', {
    weight: 0,
  })
  const initial = {
    ...initialFixture,
    modifiers: {
      ...initialFixture.modifiers,
      [reveal.id]: reveal,
      [essence.id]: essence,
    },
  }
  const item = {
    ...concreteInitial(initial),
    rarity: 'RARE' as const,
    desecrated: [
      {
        id: 'hidden',
        side: 'PREFIX' as const,
        modifierId: null,
        minimumModifierLevel: 40 as const,
        revealPool: 'AMANAMU',
      },
    ],
  }
  const sections = modifierPoolSections(initial, 82, 'dynamic', item)
  expect(
    sections.find((s) => s.kind === 'desecrated' && s.affix === 'PREFIX')!
      .groups[0]!.tiers[0]!.id,
  ).toBe(reveal.id)
  const target = sections.find(
    (s) => s.kind === 'essence' && s.affix === 'PREFIX',
  )!.groups[0]!
  expect(target.tiers[0]!.id).toBe(essence.id)
  expect(target.actions).toContain('PERFECT_ESSENCE_ENHANCEMENT')
  expect(sections[0]!.groups.flatMap((g) => g.tiers)).not.toContainEqual(reveal)
  expect(sections[0]!.groups.flatMap((g) => g.tiers)).not.toContainEqual(
    essence,
  )
})
