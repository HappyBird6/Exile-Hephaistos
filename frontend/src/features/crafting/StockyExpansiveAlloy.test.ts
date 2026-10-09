import { describe, expect, it, vi } from 'vitest'
import {
  initialFixture,
  jsonResponse,
} from '../../shared/test/craftingFixtures'
import { applyCurrency, concreteInitial, rolledText } from './workbenchApi'
import type { AppliedItem, ConcreteItem, WorkbenchAction } from './workbenchApi'

describe.each([
  {
    action: 'EXPANSIVE_ALLOY' as WorkbenchAction,
    target: 'stocky-mitts:suffix:alloy-remnant-pickup-range',
    family: 'RemnantPickupRadius',
    stat: 'remnant_pickup_range_+%',
    min: 35,
    max: 50,
    code: 'AlloyRemnantPickupRange1',
    text: 'Remnants can be collected from (35—50)% further away',
  },
])(
  '$action contract',
  ({ action, target, family, stat, min, max, code, text }) => {
    const definition = {
      ...initialFixture.modifiers.s!,
      id: target,
      familyIds: [family],
      stats: [{ id: stat, min, max }],
      text,
    }
    const definitions = { ...initialFixture.modifiers, [target]: definition }
    const before: ConcreteItem = {
      ...concreteInitial(initialFixture),
      baseItemId: 'Metadata/Items/Armours/Gloves/FourGlovesStr1',
      itemLevel: 25,
      implicits: [],
      rarity: 'RARE',
      explicits: [
        { modifierId: 'p', values: { life: 17 }, fractured: true },
        { modifierId: 's', values: { strength: 6 } },
      ],
    }
    const result: AppliedItem = {
      rulesetIdentity: 'fixture-ruleset',
      action,
      applied: true,
      reason: '',
      ruleVersion: 'stocky-workbench-scalar-alloys-v24',
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
          sourceUrl: 'https://poe2db.tw/us/Expansive_Alloy',
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
      applyCurrency(
        before,
        action,
        definitions,
        new AbortController().signal,
        [],
        'fixture-ruleset',
      )

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
        { ...before, itemLevel: 20 },
        { ...before, itemLevel: 24 },
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
            [],
            'fixture-ruleset',
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
            'fixture-ruleset',
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
          'fixture-ruleset',
        ),
      ).rejects.toThrow('Your item is unchanged')
    })
  },
)
