import { afterEach, describe, expect, it, vi } from 'vitest'
import type { AppliedItem, ConcreteItem, WorkbenchAction } from './workbenchApi'
import { applyCurrency } from './workbenchApi'
import { currentItemText } from './currentItemText'
import { extractSessionNotation } from './session-notation/extractSessionNotation'
import {
  currentFilm,
  emptyFilms,
  LocalFilmRepository,
  recordCraft,
  startFilm,
} from './workbenchHistory'
import {
  equipmentQualityPolicy,
  equipmentQualityRefusal,
  qualityTransitionMatches,
} from './qualityCurrency'

const armour = 'Metadata/Items/Armours/Gloves/FourGlovesStr1'
const bow = 'Metadata/Items/Weapons/TwoHandWeapons/Bows/FourBow1'
const wand = 'Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand3'
const sceptre = 'Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre1'
const staff = 'Metadata/Items/Weapons/TwoHandWeapons/Staves/FourStaff13'
const quarterstaff =
  'Metadata/Items/Weapons/TwoHandWeapons/Staves/FourQuarterstaff8Endgame'
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
    state: { ...item, equipmentQuality: 20 },
    action,
    applied: true,
    reason: '',
    events: [],
    consumedOmens: [],
    remainingOmens: ['Omen_of_Sanctification'],
    assumptions: [
      {
        id: equipmentQualityPolicy,
        candidateUnit: 'equipment quality target percentage',
        n: 1,
        candidates: [],
        min: 20,
        max: 20,
        sourceUrl: 'https://poe2db.tw/us/Quality',
        reason: 'User-selected simulator policy',
      },
    ],
  }
}
describe('user-selected equipment quality maximum policy', () => {
  afterEach(() => vi.unstubAllGlobals())
  it.each(['NORMAL', 'MAGIC', 'RARE'] as const)(
    'reaches the ordinary cap for %s and verifies the API transition',
    async (rarity) => {
      for (const [id, action] of [
        [armour, 'ARMOURERS_SCRAP'],
        [bow, 'BLACKSMITHS_WHETSTONE'],
        [quarterstaff, 'BLACKSMITHS_WHETSTONE'],
        [wand, 'ARCANISTS_ETCHER'],
        [sceptre, 'ARCANISTS_ETCHER'],
        [staff, 'ARCANISTS_ETCHER'],
      ] as const) {
        for (const amount of [0, 7, 19]) {
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
          expect(applied.state.equipmentQuality).toBe(20)
          expect(equipmentQualityRefusal(applied.state, action)).toBe('maximum')
          expect(applied.consumedOmens).toEqual([])
          expect(applied.remainingOmens).toEqual(['Omen_of_Sanctification'])
        }
      }
    },
  )
  it('rejects caps, unknown quality, Sanctified and mismatched target classes', () => {
    for (const amount of [20, 30, 40, 100, null])
      expect(
        equipmentQualityRefusal(
          state(armour, 'RARE', amount),
          'ARMOURERS_SCRAP',
        ),
      ).not.toBeNull()
    for (const condition of [
      'SANCTIFIED',
      'CORRUPTED',
      'MIRRORED',
      'UNIDENTIFIED',
    ])
      expect(
        equipmentQualityRefusal(
          { ...state(armour, 'RARE', 0), conditions: [condition] },
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
    for (const base of [wand, sceptre, staff])
      expect(
        equipmentQualityRefusal(
          state(base, 'NORMAL', 0),
          'BLACKSMITHS_WHETSTONE',
        ),
      ).toBe('class')
    for (const base of [armour, bow, quarterstaff])
      expect(
        equipmentQualityRefusal(state(base, 'NORMAL', 0), 'ARCANISTS_ETCHER'),
      ).toBe('class')
    expect(
      equipmentQualityRefusal(state(wand, 'NORMAL', null), 'ARCANISTS_ETCHER'),
    ).toBe('unknown')
    expect(
      equipmentQualityRefusal(state(wand, 'NORMAL', 30), 'ARCANISTS_ETCHER'),
    ).toBe('maximum')
    expect(
      equipmentQualityRefusal(
        { ...state(wand, 'NORMAL', 0), conditions: ['CORRUPTED'] },
        'ARCANISTS_ETCHER',
      ),
    ).toBe('condition')
    expect(
      equipmentQualityRefusal(
        state('Metadata/Items/Armours/Gloves/Unreviewed', 'RARE', 0),
        'ARMOURERS_SCRAP',
      ),
    ).toBe('class')
  })
  it('restores the etched item, material receipt and copyable quality after reloading a saved session', () => {
    const item = state(wand, 'MAGIC', 7)
    const applied = result(item, 'ARCANISTS_ETCHER')
    const history = recordCraft(
      startFilm(emptyFilms(), item, 'etched-wand', applied.rulesetIdentity),
      item,
      applied,
      'etched-wand',
    )
    const repository = new LocalFilmRepository(localStorage)
    repository.save(history)
    const restored = currentFilm(repository.load())!
    expect(
      restored.frames.map((frame) => frame.state.equipmentQuality),
    ).toEqual([7, 20])
    expect(extractSessionNotation(restored, 1).steps).toEqual([
      expect.objectContaining({
        currencyId: 'Arcanists_Etcher',
        uses: 1,
        consumedOmenIds: [],
      }),
    ])
    expect(
      currentItemText(restored.frames[1]!.state, {}, 'Attuned Wand', 'Wands'),
    ).toContain('Quality: +20%')
  })
  it('rejects a forged target, consumed omen, or dropped socket/observation/slot/roll', () => {
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

  it('rejects legacy +1, overshoot, false success at the cap and mismatched target assumptions', async () => {
    for (const amount of [0, 7, 19, 20, 30, 40]) {
      const item = state(armour, 'RARE', amount)
      for (const target of [amount + 1, 20, 40]) {
        const response = result(item, 'ARMOURERS_SCRAP')
        response.state.equipmentQuality = target
        expect(
          qualityTransitionMatches(item, response, 'ARMOURERS_SCRAP'),
        ).toBe(amount < 20 && target === 20)
      }
    }
    const item = state(armour, 'RARE', 7)
    for (const mutation of [
      'legacy-target',
      'legacy-policy',
      'legacy-bound',
      'candidate-unit',
      'overshoot',
    ] as const) {
      const response = result(item, 'ARMOURERS_SCRAP')
      if (mutation === 'legacy-target') response.state.equipmentQuality = 8
      if (mutation === 'overshoot') response.state.equipmentQuality = 40
      if (mutation === 'legacy-policy')
        response.assumptions[0]!.id =
          'user-selected-equipment-quality-plus-one-v1'
      if (mutation === 'legacy-bound') response.assumptions[0]!.min = 1
      if (mutation === 'candidate-unit')
        response.assumptions[0]!.candidateUnit =
          'equipment quality percentage point'
      vi.stubGlobal(
        'fetch',
        async () =>
          new Response(JSON.stringify(response), {
            status: 200,
            headers: { 'X-Crafting-Ruleset': 'fixture-ruleset' },
          }),
      )
      await expect(
        applyCurrency(
          item,
          'ARMOURERS_SCRAP',
          {},
          new AbortController().signal,
          ['Omen_of_Sanctification'],
          'fixture-ruleset',
        ),
      ).rejects.toThrow()
    }
  })

  it('accepts a refusal only with unchanged quality, including inherited overcap values', async () => {
    for (const amount of [20, 30, 40, 100, null]) {
      const item = state(armour, 'RARE', amount)
      const response: AppliedItem = {
        ...result(item, 'ARMOURERS_SCRAP'),
        applied: false,
        reason: 'Already at maximum or unknown quality',
        state: { ...item },
        assumptions: [],
      }
      expect(qualityTransitionMatches(item, response, 'ARMOURERS_SCRAP')).toBe(
        true,
      )
      vi.stubGlobal(
        'fetch',
        async () =>
          new Response(JSON.stringify(response), {
            status: 200,
            headers: { 'X-Crafting-Ruleset': 'fixture-ruleset' },
          }),
      )
      expect(
        (
          await applyCurrency(
            item,
            'ARMOURERS_SCRAP',
            {},
            new AbortController().signal,
            ['Omen_of_Sanctification'],
            'fixture-ruleset',
          )
        ).state,
      ).toEqual(item)
      response.state.equipmentQuality = amount === 20 ? 19 : 20
      expect(qualityTransitionMatches(item, response, 'ARMOURERS_SCRAP')).toBe(
        false,
      )
    }
  })
})
