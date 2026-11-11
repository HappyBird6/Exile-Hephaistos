import { describe, expect, it } from 'vitest'
import type { ConcreteItem } from '../workbenchApi'
import type { Definition } from '../craftingApi'
import { sameRolls } from './equivalence'
import {
  sanctificationShape,
  validSanctification,
  sanctifiedValues,
} from './sanctification'
const d: Definition = {
  id: 'mod',
  name: 'mod',
  text: '+1',
  tier: 1,
  familyIds: ['mod'],
  affixType: 'PREFIX',
  stats: [{ id: 'base_strength', min: 1, max: 20 }],
}
const state: ConcreteItem = {
  snapshotId: 'test',
  baseItemId: 'test',
  itemLevel: 82,
  rarity: 'RARE',
  implicits: [],
  explicits: [
    { modifierId: 'mod', values: { base_strength: 11 }, fractured: true },
  ],
  conditions: ['SANCTIFIED'],
  sanctification: {
    policy: 'independent-stat-percent-v1',
    implicits: {},
    explicits: { mod: { base_strength: 79 } },
  },
}
describe('lossless sanctification projection', () => {
  it('accepts legacy omitted false flags without accepting changed roll values', () => {
    const original = [{ modifierId: 'mod', values: { base_strength: 11 } }]
    expect(sameRolls(original, [{ ...original[0]!, fractured: false }])).toBe(
      true,
    )
    expect(sameRolls(original, [{ ...original[0]!, fractured: true }])).toBe(
      false,
    )
    expect(
      sameRolls(original, [
        { modifierId: 'mod', values: { base_strength: 12 } },
      ]),
    ).toBe(false)
  })
  it('keeps original rolls and factors through JSON roundtrip, including fractured identity', () => {
    const restored = JSON.parse(JSON.stringify(state)) as ConcreteItem
    expect(validSanctification(restored, { mod: d })).toBe(true)
    expect(sanctifiedValues(restored, restored.explicits[0]!)).toEqual({
      base_strength: 9,
    })
    expect(restored.explicits[0]).toEqual(state.explicits[0])
  })
  it('rejects missing, out-of-bound, structural or phantom factors', () => {
    const wire = state as unknown as Record<string, unknown>
    expect(sanctificationShape(wire)).toBe(true)
    expect(
      validSanctification(
        {
          ...state,
          sanctification: { ...state.sanctification!, explicits: {} },
        },
        { mod: d },
      ),
    ).toBe(false)
    for (const factors of [
      { base_strength: 77 },
      { base_strength: 123 },
      { 'local_maximum_quality_+': 100 },
    ]) {
      expect(
        sanctificationShape({
          ...wire,
          sanctification: {
            ...state.sanctification,
            explicits: { mod: factors },
          },
        }),
      ).toBe(false)
    }
  })
})
