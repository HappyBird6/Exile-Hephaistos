import { afterEach, describe, expect, it, vi } from 'vitest'
import type { AppliedItem, ConcreteItem, WorkbenchAction } from './workbenchApi'
import { applyCurrency } from './workbenchApi'
import {
  equipmentQualityPolicy,
  equipmentQualityRefusal,
  qualityTransitionMatches,
} from './qualityCurrency'

const armour = 'Metadata/Items/Armours/Gloves/FourGlovesStr1'
const bow = 'Metadata/Items/Weapons/TwoHandWeapons/Bows/FourBow1'
function state(
  baseItemId: string,
  rarity: ConcreteItem['rarity'],
  equipmentQuality: number | null,
): ConcreteItem {
  return {
    snapshotId: 'fixture',
    baseItemId,
    itemLevel: 82,
    rarity,
    equipmentQuality,
    implicits: [],
    explicits: [],
    conditions: [],
  }
}
function result(item: ConcreteItem, action: WorkbenchAction): AppliedItem {
  return {
    rulesetIdentity: 'fixture-ruleset',
    ruleVersion: equipmentQualityPolicy,
    ledgerVersion: 'fixture',
    snapshotId: item.snapshotId,
    state: { ...item, equipmentQuality: item.equipmentQuality! + 1 },
    action,
    applied: true,
    reason: '',
    events: [],
    consumedOmens: [],
    remainingOmens: ['Omen_of_Sanctification'],
    assumptions: [
      {
        id: equipmentQualityPolicy,
        candidateUnit: 'equipment quality percentage point',
        n: 1,
        candidates: [],
        min: 1,
        max: 1,
        sourceUrl: 'https://poe2db.tw/us/Quality',
        reason: 'User-selected simulator policy',
      },
    ],
  }
}
describe('user-selected equipment quality +1 policy', () => {
  afterEach(() => vi.unstubAllGlobals())
  it.each(['NORMAL', 'MAGIC', 'RARE'] as const)(
    'applies exactly1 for %s and verifies the API transition',
    async (rarity) => {
      for (const [id, action] of [
        [armour, 'ARMOURERS_SCRAP'],
        [bow, 'BLACKSMITHS_WHETSTONE'],
      ] as const) {
        for (const amount of [0, 19]) {
          const item = state(id, rarity, amount),
            response = result(item, action)
          expect(equipmentQualityRefusal(item, action)).toBeNull()
          expect(qualityTransitionMatches(item, response, action)).toBe(true)
          vi.stubGlobal(
            'fetch',
            async () =>
              new Response(JSON.stringify(response), {
                status: 200,
                headers: { 'X-Crafting-Ruleset': 'fixture-ruleset' },
              }),
          )
          const applied = await applyCurrency(
            item,
            action,
            {},
            new AbortController().signal,
            ['Omen_of_Sanctification'],
            'fixture-ruleset',
          )
          expect(applied.state.equipmentQuality).toBe(amount + 1)
          expect(applied.consumedOmens).toEqual([])
          expect(applied.remainingOmens).toEqual(['Omen_of_Sanctification'])
        }
      }
    },
  )
  it('rejects caps, unknown quality, Sanctified and mismatched target classes', () => {
    for (const amount of [20, 30, null])
      expect(
        equipmentQualityRefusal(
          state(armour, 'RARE', amount),
          'ARMOURERS_SCRAP',
        ),
      ).not.toBeNull()
    expect(
      equipmentQualityRefusal(
        { ...state(armour, 'RARE', 0), conditions: ['SANCTIFIED'] },
        'ARMOURERS_SCRAP',
      ),
    ).toBe('condition')
    expect(
      equipmentQualityRefusal(state(bow, 'RARE', 0), 'ARMOURERS_SCRAP'),
    ).toBe('class')
    expect(
      equipmentQualityRefusal(
        state(armour, 'RARE', 0),
        'BLACKSMITHS_WHETSTONE',
      ),
    ).toBe('class')
    for (const category of [
      'OneHandWeapons/Wands',
      'TwoHandWeapons/Staves',
      'OneHandWeapons/Sceptres',
    ])
      expect(
        equipmentQualityRefusal(
          state(`Metadata/Items/Weapons/${category}/Test`, 'NORMAL', 0),
          'BLACKSMITHS_WHETSTONE',
        ),
      ).toBe('class')
  })
  it('rejects a forged increment, consumed omen, or dropped socket/observation/slot/roll', () => {
    const item: ConcreteItem = {
      ...state(armour, 'RARE', 7),
      augmentSockets: 0,
      observations: {
        socketCount: 0,
        augmentEffects: ['Preserved Augment'],
        grantedSkills: [],
      },
      explicits: [
        { modifierId: 'locked', values: { value: 11 }, fractured: true },
      ],
      desecrated: [{ id: 'unknown', side: 'SUFFIX', modifierId: null }],
    }
    expect(
      qualityTransitionMatches(
        item,
        result(item, 'ARMOURERS_SCRAP'),
        'ARMOURERS_SCRAP',
      ),
    ).toBe(true)
    for (const field of [
      'equipmentQuality',
      'augmentSockets',
      'observations',
      'desecrated',
      'explicits',
    ] as const) {
      const response = result(item, 'ARMOURERS_SCRAP')
      if (field === 'equipmentQuality') response.state.equipmentQuality = 12
      else if (field === 'explicits') response.state.explicits = []
      else delete response.state[field]
      expect(() =>
        qualityTransitionMatches(item, response, 'ARMOURERS_SCRAP'),
      ).not.toThrow()
      expect(qualityTransitionMatches(item, response, 'ARMOURERS_SCRAP')).toBe(
        false,
      )
    }
    const response = result(item, 'ARMOURERS_SCRAP')
    response.consumedOmens = ['Omen_of_Sanctification']
    expect(qualityTransitionMatches(item, response, 'ARMOURERS_SCRAP')).toBe(
      false,
    )
  })
})
