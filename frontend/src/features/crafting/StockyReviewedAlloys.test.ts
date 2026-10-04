import { describe, expect, it, vi } from 'vitest'
import {
  initialFixture,
  jsonResponse,
} from '../../shared/test/craftingFixtures'
import { applyCurrency, concreteInitial, rolledText } from './workbenchApi'
import type { AppliedItem, ConcreteItem, WorkbenchAction } from './workbenchApi'

describe.each([
  {
    max: 15,
    requiredLevel: 20,
    families: ['IncreasedAttackSpeed'],
    action: 'ADAPTIVE_ALLOY' as WorkbenchAction,
    code: 'AlloyAttackSpeedIfMissingWardRecently1',
    target: 'stocky-mitts:suffix:alloy-attack-speed-missing-ward',
    min: 10,
    text: '(10—15)% increased Attack Speed while missing Runic Ward',
    level: 25,
    name: 'Adaptive Alloy',
    material: 'Adaptive_Alloy',
    sourceUrl:
      'https://poe2db.tw/us/hover?s=Data%5CMods%2FAlloyAttackSpeedIfMissingWardRecently1',
    affix: 'SUFFIX',
    stat: 'attack_speed_+%_while_missing_ward',
  },
  {
    max: 12,
    requiredLevel: 36,
    families: ['IncreasedCastSpeed', 'IncreasedAttackSpeed'],
    action: 'SWIFT_ALLOY' as WorkbenchAction,
    code: 'AlloyCastSpeedGloves1',
    target: 'stocky-mitts:suffix:alloy-cast-speed',
    min: 9,
    text: '(9—12)% increased Cast Speed',
    level: 45,
    name: 'Swift Alloy',
    material: 'Swift_Alloy',
    sourceUrl:
      'https://poe2db.tw/us/hover?s=Data%5CMods%2FAlloyCastSpeedGloves1',
    affix: 'SUFFIX',
    stat: 'base_cast_speed_+%',
  },
  {
    max: 30,
    requiredLevel: 20,
    families: ['LocalRunicWardPercent'],
    action: 'SOVEREIGN_ALLOY' as WorkbenchAction,
    code: 'AlloyLocalWardIncreasePercent1',
    target: 'stocky-mitts:prefix:alloy-local-runic-ward',
    min: 24,
    text: '(24—30)% increased Runic Ward',
    level: 25,
    name: 'Sovereign Alloy',
    material: 'Sovereign_Alloy',
    sourceUrl:
      'https://poe2db.tw/us/hover?s=Data%5CMods%2FAlloyLocalWardIncreasePercent1',
    affix: 'PREFIX',
    stat: 'local_ward_+%',
  },
])(
  '$action contract',
  ({
    action,
    target,
    families,
    stat,
    min,
    max,
    code,
    text,
    level,
    requiredLevel,
    affix,
  }) => {
    const definition = {
      ...initialFixture.modifiers.s!,
      id: target,
      familyIds: families,
      affixType: affix as 'PREFIX' | 'SUFFIX',
      stats: [{ id: stat, min, max }],
      text,
    }
    const definitions = { ...initialFixture.modifiers, [target]: definition }
    const before: ConcreteItem = {
      ...concreteInitial(initialFixture),
      baseItemId: 'Metadata/Items/Armours/Gloves/FourGlovesStr1',
      itemLevel: level,
      implicits: [],
      rarity: 'RARE',
      explicits: [
        { modifierId: 'p', values: { life: 17 }, fractured: true },
        { modifierId: 's', values: { strength: 6 } },
      ],
    }
    const result: AppliedItem = {
      action,
      applied: true,
      reason: '',
      ruleVersion: 'stocky-workbench-reviewed-alloys-v25',
      ledgerVersion: 'stocky-unverified-numeric-assumptions-v11',
      snapshotId: before.snapshotId,
      state: {
        ...before,
        explicits: [
          { modifierId: target, values: { [stat]: min } },
          before.explicits[0]!,
        ],
      },
      events: [
        {
          kind: 'REMOVE',
          modifierId: 's',
          values: {},
          selectionProbability: 1,
        },
        {
          kind: 'ADD',
          modifierId: target,
          values: { [stat]: min },
          selectionProbability: 1,
        },
      ],
      consumedOmens: [],
      remainingOmens: [],
      assumptions: [
        {
          id: 'uniform-removal-v1',
          candidateUnit: 'eligible explicit modifier instance',
          n: 1,
          candidates: ['s'],
          min: null,
          max: null,
          sourceUrl:
            'https://poe2db.tw/us/' +
            action
              .replace('_ALLOY', '')
              .toLowerCase()
              .replace(/^./, (c) => c.toUpperCase()) +
            '_Alloy',
          reason: 'Uniform removal assumption',
        },
        {
          id: 'assumed-source-integer-roll-v1',
          candidateUnit: stat,
          n: max - min + 1,
          candidates: [],
          min,
          max,
          sourceUrl: 'https://poe2db.tw/us/hover?s=Data%5CMods%2F' + code,
          reason: 'UNVERIFIED source integer model',
        },
      ],
    }
    const apply = () =>
      applyCurrency(before, action, definitions, new AbortController().signal)

    it('accepts exact replacement and unverified source model without treating it as game probability', async () => {
      vi.stubGlobal('fetch', () => Promise.resolve(jsonResponse(result)))
      expect((await apply()).assumptions[1]).toEqual(result.assumptions[1])
      expect(rolledText(definition, { [stat]: min })).toBe(
        text.replace(/\([^)]*\)/, String(min)),
      )
    })

    it('rejects missing/altered/duplicated numeric evidence, unsupported numeric bounds and lost unverified label', async () => {
      const model = result.assumptions[1]!
      for (const assumptions of [
        [result.assumptions[0]!],
        [...result.assumptions, model],
        [result.assumptions[0]!, { ...model, n: model.n + 1 }],
        [result.assumptions[0]!, { ...model, min: min - 1 }],
        [result.assumptions[0]!, { ...model, candidateUnit: 'other' }],
        [
          result.assumptions[0]!,
          { ...model, sourceUrl: 'https://example.invalid' },
        ],
        [
          result.assumptions[0]!,
          { ...model, reason: 'Established game distribution' },
        ],
      ]) {
        vi.stubGlobal('fetch', () =>
          Promise.resolve(jsonResponse({ ...result, assumptions })),
        )
        await expect(apply()).rejects.toThrow('Your item is unchanged')
      }
      for (const value of [min - 1, max + 1, min + 0.5]) {
        const wrong = structuredClone(result)
        wrong.events[1]!.values = { [stat]: value }
        wrong.state.explicits[0]!.values = { [stat]: value }
        vi.stubGlobal('fetch', () => Promise.resolve(jsonResponse(wrong)))
        await expect(apply()).rejects.toThrow('Your item is unchanged')
      }
    })

    it('rejects applied results at effective character level 36 and catalog-unsupported ilvl 44 or on Solar', async () => {
      for (const input of [
        { ...before, itemLevel: requiredLevel },
        { ...before, itemLevel: level - 1 },
        { ...before, baseItemId: initialFixture.state.baseItemId },
      ]) {
        vi.stubGlobal('fetch', () =>
          Promise.resolve(
            jsonResponse({
              ...result,
              state: {
                ...result.state,
                itemLevel: input.itemLevel,
                baseItemId: input.baseItemId,
              },
            }),
          ),
        )
        await expect(
          applyCurrency(
            input,
            action,
            definitions,
            new AbortController().signal,
          ),
        ).rejects.toThrow('Your item is unchanged')
      }
    })

    it('rejects any target family shared with a preserved explicit', async () => {
      for (const family of families) {
        vi.stubGlobal('fetch', () => Promise.resolve(jsonResponse(result)))
        await expect(
          applyCurrency(
            before,
            action,
            { ...definitions, p: { ...definitions.p!, familyIds: [family] } },
            new AbortController().signal,
          ),
        ).rejects.toThrow('Your item is unchanged')
      }
    })
    it('preserves unrelated Crystallisation including both and rejects forged consumption', async () => {
      const omens = [
        'Omen_of_Sinistral_Crystallisation',
        'Omen_of_Dextral_Crystallisation',
        'Omen_of_the_Blessed',
      ]
      const actual = { ...result, remainingOmens: omens }
      vi.stubGlobal('fetch', () => Promise.resolve(jsonResponse(actual)))
      expect(
        (
          await applyCurrency(
            before,
            action,
            definitions,
            new AbortController().signal,
            omens,
          )
        ).remainingOmens,
      ).toEqual(omens)
      vi.stubGlobal('fetch', () =>
        Promise.resolve(
          jsonResponse({
            ...actual,
            consumedOmens: [omens[0]],
            remainingOmens: omens.slice(1),
          }),
        ),
      )
      await expect(
        applyCurrency(
          before,
          action,
          definitions,
          new AbortController().signal,
          omens,
        ),
      ).rejects.toThrow('Your item is unchanged')
    })
  },
)
