import { afterEach, expect, it, vi } from 'vitest'
import snapshot from '../../shared/test/sapphire-catalog.json'
import { catalystProjection, catalystTypes } from './catalystQuality'
import { applyCurrency } from './workbenchApi'
import type { ConcreteItem, AppliedItem, WorkbenchAction } from './workbenchApi'
import type { Definition, Initial } from './craftingApi'
import {
  emptyFilms,
  startFilm,
  recordCraft,
  LocalFilmRepository,
  verifiedHistoryState,
  currentFrame,
} from './workbenchHistory'
import { sapphireBase, sapphireCastSpeed } from './sapphireJewel'
import {
  qualityCapChangeMatches,
  qualityCapChangeVersion,
} from './qualityCapChangePolicy'
import { gameName, locales } from '../../shared/i18n/i18n'
import templates from '../../shared/i18n/modifierTemplates.json'

const definitions = Object.fromEntries(
  snapshot.modifiers.map((d) => [d.id, d]),
) as Record<string, Definition>
const root: ConcreteItem = {
  snapshotId: snapshot.metadata.snapshotId,
  baseItemId: sapphireBase,
  itemLevel: 82,
  rarity: 'MAGIC',
  implicits: [],
  explicits: [
    {
      modifierId: sapphireCastSpeed,
      values: { display_cast_speed_percent: 3 },
    },
  ],
  conditions: [],
}
const initial = {
  rulesetIdentity: 'fixture-ruleset',
  metadata: snapshot.metadata,
  state: { ...root, modifierIds: [] },
  modifiers: definitions,
  actions: [],
  id: 'sapphire',
  ruleVersion: 'sapphire',
} as unknown as Initial
const reply = (state: ConcreteItem, action: WorkbenchAction): AppliedItem => ({
  rulesetIdentity: 'fixture-ruleset',
  ruleVersion: 'sapphire',
  ledgerVersion: 'sapphire',
  snapshotId: root.snapshotId,
  state,
  action,
  applied: true,
  reason: 'Simulator policy',
  events: [],
  assumptions: [],
  consumedOmens: [],
  remainingOmens: [],
  qualityLimit: { ruleVersion: 'quality-limit-v1', maximumQuality: 20 },
})
afterEach(() => vi.unstubAllGlobals())

it('accepts reviewed Sapphire removal and source-range reroll while preserving typed quality', async () => {
  const before: ConcreteItem = {
    ...root,
    catalystQuality: { type: 'SIBILANT', amount: 20 },
  }
  for (const action of ['ANNULMENT', 'DIVINE'] as const) {
    const next: ConcreteItem = {
      ...before,
      explicits:
        action === 'ANNULMENT'
          ? []
          : [
              {
                modifierId: sapphireCastSpeed,
                values: { display_cast_speed_percent: 4 },
              },
            ],
    }
    const result = {
      ...reply(next, action),
      events: [
        {
          kind: action === 'ANNULMENT' ? 'REMOVE' : 'REROLL_EXPLICIT',
          modifierId: sapphireCastSpeed,
          values:
            action === 'ANNULMENT' ? {} : { display_cast_speed_percent: 4 },
          selectionProbability: 1,
        },
      ],
    }
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify(result), {
          headers: { 'X-Crafting-Ruleset': 'fixture-ruleset' },
          status: 200,
        }),
      ),
    )
    const accepted = await applyCurrency(
      before,
      action,
      definitions,
      new AbortController().signal,
      [],
      'fixture-ruleset',
    )
    expect(accepted.state).toEqual(next)
    expect(verifiedHistoryState(next, initial)).toBe(true)
    if (action === 'DIVINE') {
      expect(
        catalystProjection(
          definitions[sapphireCastSpeed]!,
          next.explicits[0]!.values,
          next.catalystQuality,
        ).values.display_cast_speed_percent,
      ).toBe(5)
      expect(next.explicits[0]!.values.display_cast_speed_percent).toBe(4)
    }
  }
})

it('accepts all thirteen refined API results, repeated use and type replacement without changing original rolls', async () => {
  let state = root
  for (const type of Object.keys(
    catalystTypes,
  ) as (keyof typeof catalystTypes)[]) {
    const action = `REFINED_CATALYST_${type}` as WorkbenchAction
    const next = { ...state, catalystQuality: { type, amount: 20 } }
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify(reply(next, action)), {
          headers: { 'X-Crafting-Ruleset': 'fixture-ruleset' },
          status: 200,
        }),
      ),
    )
    const result = await applyCurrency(
      state,
      action,
      definitions,
      new AbortController().signal,
      [],
      'fixture-ruleset',
    )
    expect(result.state.explicits).toEqual(root.explicits)
    expect(result.state.catalystQuality).toEqual({ type, amount: 20 })
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify(reply(next, action)), {
          headers: { 'X-Crafting-Ruleset': 'fixture-ruleset' },
          status: 200,
        }),
      ),
    )
    expect(
      (
        await applyCurrency(
          next,
          action,
          definitions,
          new AbortController().signal,
          [],
          'fixture-ruleset',
        )
      ).state,
    ).toEqual(next)
    state = next
  }
})
it('rejects an ordinary catalyst falsely applied to Sapphire', async () => {
  const response = reply(
    { ...root, catalystQuality: { type: 'FLESH', amount: 20 } },
    'CATALYST_FLESH',
  )
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue(
      new Response(JSON.stringify(response), {
        headers: { 'X-Crafting-Ruleset': 'fixture-ruleset' },
        status: 200,
      }),
    ),
  )
  await expect(
    applyCurrency(
      root,
      'CATALYST_FLESH',
      definitions,
      new AbortController().signal,
      [],
      'fixture-ruleset',
    ),
  ).rejects.toThrow('catalyst policy')
})
it('scales the actual matching suffix once and honestly leaves other types without a match', () => {
  for (const type of Object.keys(
    catalystTypes,
  ) as (keyof typeof catalystTypes)[]) {
    const result = catalystProjection(
      definitions[sapphireCastSpeed]!,
      root.explicits[0]!.values,
      { type, amount: 20 },
    )
    const matching = ['SIBILANT', 'SKITTERING'].includes(type)
    expect(result.status).toBe(matching ? 'SCALED_INTEGER' : 'NO_MATCH')
    expect(result.values.display_cast_speed_percent).toBe(matching ? 4 : 3)
    expect(root.explicits[0]!.values.display_cast_speed_percent).toBe(3)
  }
})
it('restores typed Jewel film and rejects Normal, special and out-of-scope starting states', () => {
  const applied = reply(
    { ...root, catalystQuality: { type: 'SKITTERING', amount: 20 } },
    'REFINED_CATALYST_SKITTERING',
  )
  const films = recordCraft(
    startFilm(emptyFilms(), root, 'sapphire', 'fixture-ruleset'),
    root,
    applied,
    'unused',
  )
  const storage = { getItem: vi.fn(), setItem: vi.fn() }
  const repo = new LocalFilmRepository(storage)
  repo.save(films)
  storage.getItem.mockReturnValue(storage.setItem.mock.calls[0]![1])
  expect(currentFrame(repo.load())?.state).toEqual(applied.state)
  expect(verifiedHistoryState(applied.state, initial)).toBe(true)
  expect(verifiedHistoryState({ ...root, rarity: 'NORMAL' }, initial)).toBe(
    false,
  )
  expect(
    verifiedHistoryState(
      { ...root, explicits: [...root.explicits, ...root.explicits] },
      initial,
    ),
  ).toBe(false)
  expect(
    verifiedHistoryState({ ...root, conditions: ['CORRUPTED'] }, initial),
  ).toBe(false)
})
it('has source-bound Sapphire names and selected suffix templates in six languages', () => {
  for (const locale of locales) {
    expect(gameName('Sapphire', 'missing', locale)).not.toBe('missing')
    expect(
      templates.templates[locale]['sapphire-existing-cast-speed-v1'].template,
    ).toContain('{v0}')
  }
})

const breach = 'amulet:prefix:essence-maximum-quality'
const capDefinitions: Record<string, Definition> = {
  [breach]: {
    id: breach,
    name: 'Breach',
    tier: 1,
    affixType: 'PREFIX',
    familyIds: ['LocalMaximumQuality'],
    text: '+20% to Maximum Quality',
    stats: [{ id: 'local_maximum_quality_+', min: 20, max: 20 }],
  },
}
const before: ConcreteItem = {
  ...root,
  baseItemId: 'Metadata/Items/Amulets/FourAmulet9',
  rarity: 'RARE',
  explicits: [
    { modifierId: breach, values: { 'local_maximum_quality_+': 20 } },
  ],
  catalystQuality: { type: 'FLESH', amount: 40 },
}
const clamp = {
  ...reply(
    {
      ...before,
      explicits: [],
      catalystQuality: { type: 'FLESH', amount: 20 },
    },
    'ANNULMENT',
  ),
  events: [
    {
      kind: 'REMOVE' as const,
      modifierId: breach,
      values: {},
      selectionProbability: 1,
    },
  ],
  assumptions: [
    {
      id: qualityCapChangeVersion,
      candidateUnit: 'quality amount',
      n: 1,
      candidates: [],
      min: 20,
      max: 40,
      sourceUrl: 'https://poe2db.tw/us/Quality',
      reason: 'UNVERIFIED simulator policy',
    },
  ],
}
it('preserves quality after cap loss and rejects clamp, forged type and old ledger', () => {
  const preserved = {
    ...clamp,
    state: { ...clamp.state, catalystQuality: before.catalystQuality },
    assumptions: [],
  }
  expect(qualityCapChangeMatches(before, preserved, capDefinitions)).toBe(true)
  expect(qualityCapChangeMatches(before, clamp, capDefinitions)).toBe(false)
  expect(
    qualityCapChangeMatches(
      before,
      { ...preserved, assumptions: clamp.assumptions },
      capDefinitions,
    ),
  ).toBe(false)
  expect(
    qualityCapChangeMatches(
      before,
      {
        ...preserved,
        state: {
          ...preserved.state,
          catalystQuality: { type: 'NEURAL', amount: 40 },
        },
      },
      capDefinitions,
    ),
  ).toBe(false)
})
it('accepts non-autorefill after cap growth and rejects forged automatic quality increase', () => {
  const low = {
    ...before,
    explicits: [],
    catalystQuality: { type: 'FLESH' as const, amount: 20 },
  }
  const growth = {
    ...reply(
      { ...before, catalystQuality: { type: 'FLESH', amount: 20 } },
      'ESSENCE_BREACH',
    ),
  }
  expect(qualityCapChangeMatches(low, growth, capDefinitions)).toBe(true)
  expect(
    qualityCapChangeMatches(low, { ...growth, state: before }, capDefinitions),
  ).toBe(false)
})
