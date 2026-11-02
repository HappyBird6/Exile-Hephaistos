import { afterEach, expect, it, vi } from 'vitest'
import ringCapture from '../../../shared/test/ring-essence-responses.json'
import { initialFixture } from '../../../shared/test/craftingFixtures'
import type { Initial } from '../craftingApi'
import { applyCurrency, concreteInitial } from '../workbenchApi'
import type { AppliedItem, ConcreteItem } from '../workbenchApi'
import { catalystProjection } from '../catalystQuality'
import {
  LocalFilmRepository,
  currentFrame,
  emptyFilms,
  recordCraft,
  startFilm,
  verifiedHistoryState,
} from '../workbenchHistory'
import { catalystCurrencyAction } from './catalystCurrency'

afterEach(() => vi.unstubAllGlobals())

const initial = ringCapture.initial as unknown as Initial
const definition = Object.values(initial.modifiers).find(
  (d) =>
    d.stats?.length === 1 &&
    d.stats[0]?.id === 'base_maximum_life' &&
    (d.weight ?? 0) > 0,
)!
const before: ConcreteItem = {
  ...concreteInitial(initial),
  rarity: 'RARE',
  explicits: [
    {
      modifierId: definition.id,
      values: { base_maximum_life: definition.stats![0]!.max },
    },
  ],
}

function result(
  state: ConcreteItem,
  action: AppliedItem['action'],
): AppliedItem {
  return {
    state,
    action,
    applied: true,
    rulesetIdentity: initial.rulesetIdentity,
    ruleVersion: 'fixture-rules',
    ledgerVersion: 'fixture-ledger',
    snapshotId: before.snapshotId,
    reason: '',
    events: [],
    assumptions: [],
    consumedOmens: [],
    remainingOmens: [],
    qualityLimit: { ruleVersion: 'quality-limit-v1', maximumQuality: 20 },
  }
}

it('resolves all 26 existing catalyst currencies and rejects unrelated selections', () => {
  for (const id of [
    'Flesh',
    'Neural',
    'Carapace',
    'Uul-Netols',
    'Xophs',
    'Tuls',
    'Eshs',
    'Chayulas',
    'Reaver',
    'Sibilant',
    'Skittering',
    'Adaptive',
    'Necrotic',
  ]) {
    expect(catalystCurrencyAction(`${id}_Catalyst`)).toMatch(/^CATALYST_/)
    expect(catalystCurrencyAction(`Refined_${id}_Catalyst`)).toMatch(
      /^REFINED_CATALYST_/,
    )
  }
  expect(catalystCurrencyAction('Divine_Orb')).toBeNull()
  expect(catalystCurrencyAction('unknown')).toBeNull()
})

it('uses the existing apply endpoint, projects quality once and restores the resulting film unchanged', async () => {
  const action = catalystCurrencyAction('Flesh_Catalyst')!
  const next: ConcreteItem = {
    ...before,
    catalystQuality: { type: 'FLESH', amount: 20 },
  }
  const response = result(next, action)
  const fetch = vi.fn().mockResolvedValue(
    new Response(JSON.stringify(response), {
      headers: { 'X-Crafting-Ruleset': initial.rulesetIdentity },
    }),
  )
  vi.stubGlobal('fetch', fetch)
  const bytes = JSON.stringify(before)
  const applied = await applyCurrency(
    before,
    action,
    initial.modifiers,
    new AbortController().signal,
    [],
    initial.rulesetIdentity,
  )
  expect(fetch).toHaveBeenCalledWith(
    '/api/v1/crafting/workbench/apply',
    expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ state: before, action, activeOmens: [] }),
    }),
  )
  expect(JSON.stringify(before)).toBe(bytes)
  expect(applied.state.explicits).toEqual(before.explicits)
  const projected = catalystProjection(
    definition,
    before.explicits[0]!.values,
    next.catalystQuality,
  )
  expect(projected.status).toBe('SCALED_INTEGER')
  expect(projected.values.base_maximum_life).toBe(
    Math.round(definition.stats![0]!.max * 1.2),
  )
  const films = recordCraft(
    startFilm(emptyFilms(), before, 'catalyst', initial.rulesetIdentity),
    before,
    applied,
    'step',
  )
  const repository = new LocalFilmRepository(localStorage)
  repository.save(films)
  expect(currentFrame(repository.load())!.state).toEqual(next)
  expect(
    verifiedHistoryState(currentFrame(repository.load())!.state, initial),
  ).toBe(true)
})

it('rejects an ordinary Divine response that changes a base implicit', async () => {
  const solar: ConcreteItem = {
    ...concreteInitial(initialFixture),
    baseItemId: 'Metadata/Items/Amulets/FourAmulet9',
    rarity: 'RARE',
    explicits: [{ modifierId: 'p', values: { life: 10 } }],
  }
  const implicit = solar.implicits[0]!
  const changed: ConcreteItem = {
    ...solar,
    implicits: [
      {
        ...implicit,
        values: Object.fromEntries(
          Object.entries(implicit.values).map(([id, value]) => [id, value + 1]),
        ),
      },
    ],
  }
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          ...result(changed, 'DIVINE'),
          snapshotId: solar.snapshotId,
          rulesetIdentity: initialFixture.rulesetIdentity,
        }),
        {
          headers: { 'X-Crafting-Ruleset': initialFixture.rulesetIdentity },
        },
      ),
    ),
  )
  await expect(
    applyCurrency(
      solar,
      'DIVINE',
      initialFixture.modifiers,
      new AbortController().signal,
      [],
      initialFixture.rulesetIdentity,
    ),
  ).rejects.toThrow()
})
