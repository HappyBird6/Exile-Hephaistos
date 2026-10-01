import { describe, expect, it } from 'vitest'
import { groupOutcomes } from './outcomeGroups'
import {
  firstOutcomes,
  initialFixture,
} from '../../shared/test/craftingFixtures'
import type { Definition } from './craftingApi'

const definitions: Record<string, Definition> = {
  p: {
    ...initialFixture.modifiers.p!,
    layer: 'EXPLICIT',
    tags: ['life'],
    stats: [{ id: 'life', min: 10, max: 20 }],
  },
  p2: {
    ...initialFixture.modifiers.p!,
    id: 'p2',
    tier: 2,
    text: '+(1?9) to maximum Life',
    layer: 'EXPLICIT',
    tags: ['life'],
    stats: [{ id: 'life', min: 1, max: 9 }],
  },
}
const outcomes = [
  firstOutcomes[0]!,
  {
    ...firstOutcomes[0]!,
    id: 'tier-2',
    state: { ...firstOutcomes[0]!.state, modifierIds: ['p2'] },
    probability: 0.4,
  },
]
describe('conditional outcome grouping', () => {
  it('groups declared fixed stat tiers while preserving unrelated numeric constants', () => {
    const fixed = {
      p: {
        ...definitions.p!,
        text: '+1 to Level of all Spell Skills',
        stats: [{ id: 'skill_level', min: 1, max: 1 }],
      },
      p2: {
        ...definitions.p2!,
        text: '+2 to Level of all Spell Skills',
        stats: [{ id: 'skill_level', min: 2, max: 2 }],
      },
    }
    expect(groupOutcomes(outcomes, fixed)).toHaveLength(1)
    expect(
      groupOutcomes(outcomes, {
        ...fixed,
        p2: {
          ...fixed.p2,
          text: '+2 to Level of all Spell Skills per 10 Strength',
        },
      }),
    ).toHaveLength(2)
  })
  it('combines tier ranges and retains original outcomes and mass', () => {
    const groups = groupOutcomes(outcomes, definitions)
    expect(groups).toHaveLength(1)
    expect(groups[0]!.probability).toBe(1)
    expect(groups[0]!.outcomes).toEqual(outcomes)
    expect(outcomes[0]!.probability).toBe(0.6)
  })
  it('keeps unknown identities, different families, stats and fixed text separate', () => {
    expect(groupOutcomes(outcomes, initialFixture.modifiers)).toHaveLength(2)
    for (const change of [
      { familyIds: ['Other'] },
      { stats: [{ id: 'other', min: 1, max: 9 }] },
      { text: '+(1?9) to maximum Life per 10 Strength' },
    ]) {
      expect(
        groupOutcomes(outcomes, {
          ...definitions,
          p2: { ...definitions.p2!, ...change },
        }),
      ).toHaveLength(2)
    }
  })
  it('keeps state conditions, level, rarity and implicit values separate', () => {
    for (const change of [
      { conditions: ['different'] },
      { itemLevel: 70 },
      { rarity: 'RARE' as const },
      {
        implicits: [
          {
            modifierId: 'implicit',
            values: { base_spirit_from_equipment: 20 },
          },
        ],
      },
    ]) {
      expect(
        groupOutcomes(
          [
            outcomes[0]!,
            { ...outcomes[1]!, state: { ...outcomes[1]!.state, ...change } },
          ],
          definitions,
        ),
      ).toHaveLength(2)
    }
  })
})
