import { expect, it } from 'vitest'
import { initialFixture } from '../../../shared/test/craftingFixtures'
import { startingAffixOptions, startingItem } from './baseAffixSetup'
it('keeps ordinary choices unlabelled and distinguishes guaranteed, exclusive desecrated and Rune sources', () => {
  const initial = {
    ...initialFixture,
    modifiers: {
      ordinary: {
        id: 'ordinary',
        name: 'Ordinary',
        text: 'Ordinary',
        layer: 'EXPLICIT',
        affixType: 'PREFIX',
        tier: 1,
        familyIds: ['a'],
        weight: 10,
        stats: [],
        tags: [],
      },
      essence: {
        id: 'essence',
        name: 'Essence',
        text: 'Essence',
        layer: 'EXPLICIT',
        affixType: 'PREFIX',
        tier: 1,
        familyIds: ['b'],
        weight: 0,
        stats: [],
        tags: [],
      },
      hidden: {
        id: 'hidden',
        name: 'Hidden',
        text: 'Hidden',
        layer: 'EXPLICIT',
        affixType: 'SUFFIX',
        tier: 0,
        familyIds: ['c'],
        weight: 0,
        stats: [],
        tags: ['reveal_only'],
      },
      rune: {
        id: 'rune',
        name: 'Rune',
        text: 'Rune',
        layer: 'EXPLICIT',
        affixType: 'SUFFIX',
        tier: 0,
        familyIds: ['d'],
        weight: 0,
        stats: [],
        tags: ['augment_pool', 'destruction_pool'],
      },
    },
  } as unknown as typeof initialFixture
  const ref = {
    entries: [
      {
        modifierId: 'ordinary',
        kind: 'ordinary',
        eligible: true,
        reasons: [],
        actions: [],
      },
      {
        modifierId: 'essence',
        kind: 'essence',
        eligible: true,
        reasons: [],
        actions: ['ESSENCE_HORROR'],
      },
      {
        modifierId: 'hidden',
        kind: 'desecrated',
        eligible: true,
        reasons: [],
        actions: [],
      },
      {
        modifierId: 'rune',
        kind: 'ordinary',
        eligible: true,
        reasons: [],
        actions: [],
      },
    ],
  } as const
  const options = startingAffixOptions(
    initial,
    ref as unknown as Parameters<typeof startingAffixOptions>[1],
  )
  expect(options.map((o) => [o.definition.id, o.source])).toEqual([
    ['ordinary', ''],
    ['essence', 'Essence'],
    ['hidden', 'Desecrated'],
    ['rune', 'Thruds_Might'],
  ])
  const item = startingItem(initial, 'RARE', [
    { modifierId: 'essence', values: {}, source: 'Essence', kind: 'crafted' },
    {
      modifierId: 'hidden',
      values: {},
      source: 'Desecrated',
      kind: 'desecrated',
    },
  ])
  expect(item.explicits[0]?.crafted).toBe(true)
  expect(item.desecrated).toEqual([
    { id: 'starting-0', side: 'SUFFIX', modifierId: 'hidden' },
  ])
})
