import { describe, expect, it, vi } from 'vitest'
import {
  initialFixture,
  jsonResponse,
} from '../../shared/test/craftingFixtures'
import { applyCurrency, concreteInitial, rolledText } from './workbenchApi'
import type { AppliedItem, ConcreteItem, WorkbenchAction } from './workbenchApi'

describe.each([
  {
    action: 'PERFECT_ESSENCE_GROUNDING' as WorkbenchAction,
    target: 'stocky-mitts:suffix:essence-lightning-recoup',
    family: 'LightningDamageTakenRecoupedAsLife',
    stat: 'lightning_damage_taken_goes_to_life_over_4_seconds_%',
    min: 26,
    max: 30,
    code: 'EssenceLightningRecoupLife1',
    text: '(26—30)% of Lightning Damage taken Recouped as Life',
  },
  {
    action: 'PERFECT_ESSENCE_OPULENCE' as WorkbenchAction,
    target: 'stocky-mitts:suffix:essence-gold-quantity',
    family: 'EssenceGoldDropped',
    stat: 'gold_+%_from_enemies',
    min: 10,
    max: 15,
    code: 'EssenceGoldDropped1',
    text: '(10—15)% increased Quantity of Gold Dropped by Slain Enemies',
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
      itemLevel: 72,
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
      ruleVersion: 'stocky-workbench-perfect-glove-v22',
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
            (action === 'PERFECT_ESSENCE_GROUNDING'
              ? 'Perfect_Essence_of_Grounding'
              : 'Perfect_Essence_of_Opulence'),
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

    it('accepts full-side conditional removal with an installed augment and rejects opposite-side removal', async () => {
      const extraDefinitions = Object.fromEntries(
        ['p2', 'p3', 's2', 's3'].map((id) => [
          id,
          {
            ...definitions[id.startsWith('p') ? 'p' : 's']!,
            id,
            familyIds: [id],
          },
        ]),
      )
      const full: ConcreteItem = {
        ...before,
        augmentSockets: 2,
        installedAugments: [{ slotIndex: 0, augmentId: 'Katlas_Gloom' }],
        explicits: [
          ...before.explicits,
          ...['p2', 'p3', 's2', 's3'].map((id) => ({
            modifierId: id,
            values: id.startsWith('p') ? { life: 17 } : { strength: 6 },
          })),
        ],
      }
      const response: AppliedItem = {
        ...result,
        state: {
          ...full,
          explicits: [
            ...full.explicits.filter((m) => m.modifierId !== 's'),
            { modifierId: target, values: { [stat]: min } },
          ],
        },
        events: [
          { ...result.events[0]!, selectionProbability: 1 / 3 },
          result.events[1]!,
        ],
        assumptions: [
          { ...result.assumptions[0]!, n: 3, candidates: ['s', 's2', 's3'] },
          result.assumptions[1]!,
        ],
      }
      const execute = () =>
        applyCurrency(
          full,
          action,
          { ...definitions, ...extraDefinitions },
          new AbortController().signal,
          [],
          'fixture-ruleset',
        )
      vi.stubGlobal('fetch', () => Promise.resolve(jsonResponse(response)))
      expect((await execute()).state.explicits).toHaveLength(6)
      const wrong = {
        ...response,
        events: [
          { ...response.events[0]!, modifierId: 'p2' },
          response.events[1]!,
        ],
      }
      vi.stubGlobal('fetch', () => Promise.resolve(jsonResponse(wrong)))
      await expect(execute()).rejects.toThrow()
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

    it('rejects applied results at effective character level 57 and catalog-unsupported ilvl 71 or on Solar', async () => {
      for (const input of [
        { ...before, itemLevel: 57 },
        { ...before, itemLevel: 71 },
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

    it('consumes only matching Crystallisation and rejects the other material target', async () => {
      const omens = ['Omen_of_Dextral_Crystallisation', 'Omen_of_the_Blessed']
      const actual = {
        ...result,
        consumedOmens: [omens[0]!],
        remainingOmens: [omens[1]!],
      }
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
      ).toEqual([omens[1]])
      const wrongId =
        action === 'PERFECT_ESSENCE_GROUNDING'
          ? 'stocky-mitts:suffix:essence-gold-quantity'
          : 'stocky-mitts:suffix:essence-lightning-recoup'
      const wrong = structuredClone(actual)
      wrong.events[1]!.modifierId = wrongId
      wrong.state.explicits[0]!.modifierId = wrongId
      vi.stubGlobal('fetch', () => Promise.resolve(jsonResponse(wrong)))
      await expect(
        applyCurrency(
          before,
          action,
          { ...definitions, [wrongId]: { ...definition, id: wrongId } },
          new AbortController().signal,
          omens,
          'fixture-ruleset',
        ),
      ).rejects.toThrow('Your item is unchanged')
    })
  },
)
