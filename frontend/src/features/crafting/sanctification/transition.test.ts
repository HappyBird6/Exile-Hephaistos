import { afterEach, describe, expect, it, vi } from 'vitest'
import { initialFixture } from '../../../shared/test/craftingFixtures'
import { applyCurrency, concreteInitial } from '../workbenchApi'
import type { AppliedItem, ConcreteItem } from '../workbenchApi'
import type { Definition } from '../craftingApi'
import { reviewedBasicJewel } from '../basicJewel'
import { supportsConcreteStateShape } from '../workbenchStateShape'
import sapphire from '../../../shared/test/sapphire-catalog.json'
import { validSanctification, sanctificationTargets } from './sanctification'

const state: ConcreteItem = {
  ...concreteInitial(initialFixture),
  baseItemId: 'Metadata/Items/Amulets/FourAmulet9',
  rarity: 'RARE',
  explicits: [{ modifierId: 'p', values: { life: 17 }, fractured: true }],
  desecrated: [{ id: 'unknown', side: 'SUFFIX', modifierId: null }],
}
const response = (): AppliedItem => ({
  rulesetIdentity: 'fixture-ruleset',
  ruleVersion: 'test',
  ledgerVersion: 'test',
  snapshotId: state.snapshotId,
  action: 'DIVINE',
  applied: true,
  reason: '',
  events: [],
  consumedOmens: ['Omen_of_Sanctification'],
  remainingOmens: [],
  assumptions: [
    {
      id: 'sanctification-explicit-rounded-v2',
      candidateUnit: 'numeric modifier stat',
      n: 45,
      candidates: [],
      min: 78,
      max: 122,
      sourceUrl: 'https://poe2db.tw/us/Sanctified_Items',
      reason: 'Simulator policy',
    },
  ],
  state: {
    ...state,
    conditions: ['SANCTIFIED'],
    sanctification: {
      policy: 'explicit-stat-percent-v2',
      implicits: {},
      explicits: { p: { life: 122 } },
    },
  },
})
describe('validated Sanctification wire transition', () => {
  afterEach(() => vi.unstubAllGlobals())
  it('accepts exact original rolls plus complete factors and rejects erased slots or factors', async () => {
    for (const mutation of [
      'valid',
      'lost-slot',
      'lost-factor',
      'changed-implicit',
    ] as const) {
      const value = response()
      if (mutation === 'changed-implicit')
        value.state.sanctification!.implicits = {
          implicit: { base_spirit_from_equipment: 122 },
        }
      if (mutation === 'lost-slot') value.state.desecrated = []
      if (mutation === 'lost-factor') value.state.sanctification!.explicits = {}
      vi.stubGlobal(
        'fetch',
        async () =>
          new Response(JSON.stringify(value), {
            status: 200,
            headers: { 'X-Crafting-Ruleset': 'fixture-ruleset' },
          }),
      )
      const request = applyCurrency(
        state,
        'DIVINE',
        initialFixture.modifiers,
        new AbortController().signal,
        ['Omen_of_Sanctification'],
        'fixture-ruleset',
      )
      if (mutation === 'valid')
        expect((await request).state.sanctification).toEqual(
          value.state.sanctification,
        )
      else await expect(request).rejects.toThrow()
    }
  })
  it('preserves the reviewed Basic Jewel shape after Sanctification', () => {
    const definitions = Object.fromEntries(
      sapphire.modifiers.map((d) => [d.id, d]),
    ) as Record<string, Definition>
    const d = sapphire.modifiers.find((d) => d.affixType === 'PREFIX')!
    const item: ConcreteItem = {
      snapshotId: sapphire.metadata.snapshotId,
      baseItemId: sapphire.base.id,
      itemLevel: 82,
      rarity: 'RARE',
      implicits: [],
      explicits: [
        {
          modifierId: d.id,
          values: Object.fromEntries(d.stats.map((s) => [s.id, s.min])),
        },
      ],
      conditions: ['SANCTIFIED'],
    }
    const modifier = item.explicits[0]!
    item.sanctification = {
      policy: 'explicit-stat-percent-v2',
      implicits: {},
      explicits: {
        [d.id]: Object.fromEntries(
          sanctificationTargets(definitions[d.id]!, modifier.values).map(
            (id) => [id, 78],
          ),
        ),
      },
    }
    expect(supportsConcreteStateShape(item)).toBe(true)
    expect(validSanctification(item, definitions)).toBe(true)
    expect(reviewedBasicJewel(item)).toBe(true)
    expect(JSON.parse(JSON.stringify(item))).toEqual(item)
  })
})
