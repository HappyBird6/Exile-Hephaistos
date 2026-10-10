import { expect, it } from 'vitest'
import { createItemPresentation } from './presentation'
import { createPathSearchRequest } from './request'
import { requestFixture } from './fixtures.test-support'
import type { Definition } from '../craftingApi'
it('renders an added ordinary stat from catalog data without a renderer branch', () => {
  const item = requestFixture().start.item
  item.explicits = [
    {
      modifierId: 'added-modifier',
      values: { 'added-stat': 7 },
      fractured: false,
    },
  ]
  const definition: Definition = {
    id: 'added-modifier',
    name: 'Added',
    text: '+(1–9) to Focus',
    tier: 1,
    affixType: 'PREFIX',
    familyIds: ['added-family'],
    stats: [{ id: 'added-stat', min: 1, max: 9 }],
  }
  const render = createItemPresentation(
    { name: 'Amulet', itemClass: 'Amulets' },
    { 'added-modifier': definition },
  )
  expect(render(item, 'en').modifiers[0]?.text).toContain('7')
  const removed = createItemPresentation(
    { name: 'Amulet', itemClass: 'Amulets' },
    {},
  )
  expect(removed(item, 'en').modifiers[0]?.text).toBe('Not available')
  expect(item.explicits[0]!.values['added-stat']).toBe(7)
})
it('freezes original rolls/goal and creates canonical string observations', () => {
  const input = requestFixture()
  expect(input.start.item.catalystQuality).toBeNull()
  const item = { ...input.start.item, catalystQuality: null }
  const result = createPathSearchRequest(
    item,
    input.goal,
    input.start.provenance,
  )
  input.start.item.explicits[0]!.values['synthetic:value'] = 9
  expect(result.start.item.explicits[0]!.values['synthetic:value']).toBe(0)
  expect(result.observations).toEqual(['100', '300', '500'])
  expect(() =>
    createPathSearchRequest(item, input.goal, input.start.provenance, [
      'unsupported-omen',
    ]),
  ).toThrow()
})
