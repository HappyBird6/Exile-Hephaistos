import { describe, expect, it, vi, afterEach } from 'vitest'
import {
  initialFixture,
  fixtureFetch,
} from '../../shared/test/craftingFixtures'
import type { Definition } from './craftingApi'
import { concreteInitial, applyCurrency } from './workbenchApi'
import type { AppliedItem, ConcreteItem } from './workbenchApi'
import { maximumQuality, qualityLimitMatches } from './qualityLimit'
import {
  LocalFilmRepository,
  emptyFilms,
  startFilm,
  recordCraft,
  currentFrame,
  verifiedFrameEvidence,
  historyStorageKey,
} from './workbenchHistory'

const id = 'amulet:prefix:essence-maximum-quality'
const stat = 'local_maximum_quality_+'
const definition: Definition = {
  id,
  name: "Breachlord's",
  text: '+20% to Maximum Quality',
  tier: 1,
  affixType: 'PREFIX',
  familyIds: ['LocalMaximumQuality'],
  stats: [{ id: stat, min: 20, max: 20 }],
}
const definitions = { ...initialFixture.modifiers, [id]: definition }
const root = concreteInitial(initialFixture)
const solar = 'Metadata/Items/Amulets/FourAmulet9'
const stocky = 'Metadata/Items/Armours/Gloves/FourGlovesStr1'
const withBreach: ConcreteItem = {
  ...root,
  baseItemId: solar,
  rarity: 'RARE',
  explicits: [{ modifierId: id, values: { [stat]: 20 } }],
}

describe('source-reviewed quality maximum, not applied quality', () => {
  afterEach(() => vi.unstubAllGlobals())
  it.each([20, 40])(
    'validates returned API cap%d against the concrete result',
    async (cap) => {
      const state = { ...root, baseItemId: solar }
      vi.stubGlobal('fetch', async (_input: unknown, options: RequestInit) => {
        const response = await fixtureFetch(
          '/api/v1/crafting/workbench/apply',
          options,
        )
        const value = (await response.json()) as AppliedItem
        value.qualityLimit = {
          ruleVersion: 'quality-limit-v1',
          maximumQuality: cap,
        }
        return new Response(JSON.stringify(value), {
          headers: { 'X-Crafting-Ruleset': 'fixture-ruleset' },
          status: 200,
        })
      })
      const result = applyCurrency(
        state,
        'TRANSMUTATION',
        definitions,
        new AbortController().signal,
        [],
        'fixture-ruleset',
      )
      if (cap === 20)
        expect((await result).qualityLimit?.maximumQuality).toBe(20)
      else await expect(result).rejects.toThrow()
    },
  )
  it.each([solar, stocky])(
    'uses the default20 without changing state for %s',
    (baseItemId) => {
      const state = { ...root, baseItemId }
      const bytes = JSON.stringify(state)
      expect(maximumQuality(state, definitions)).toBe(20)
      expect(JSON.stringify(state)).toBe(bytes)
      expect(
        qualityLimitMatches(
          { ruleVersion: 'quality-limit-v1', maximumQuality: 20 },
          state,
          definitions,
        ),
      ).toBe(true)
    },
  )
  it('replays the fixed Breach cap40, including a fractured instance, and removal restores20', () => {
    expect(maximumQuality(withBreach, definitions)).toBe(40)
    expect(
      maximumQuality(
        {
          ...withBreach,
          explicits: [{ ...withBreach.explicits[0]!, fractured: true }],
        },
        definitions,
      ),
    ).toBe(40)
    expect(maximumQuality({ ...withBreach, explicits: [] }, definitions)).toBe(
      20,
    )
  })
  it('does not infer a cap from another base, forged bounds, duplicates or an unreviewed modifier', () => {
    expect(
      maximumQuality({ ...withBreach, baseItemId: stocky }, definitions),
    ).toBeNull()
    expect(
      maximumQuality({ ...root, baseItemId: 'unreviewed' }, definitions),
    ).toBeNull()
    expect(
      maximumQuality(withBreach, {
        ...definitions,
        [id]: { ...definition, stats: [{ id: stat, min: 20, max: 40 }] },
      }),
    ).toBeNull()
    expect(
      maximumQuality(
        {
          ...withBreach,
          explicits: [...withBreach.explicits, ...withBreach.explicits],
        },
        definitions,
      ),
    ).toBeNull()
    expect(
      maximumQuality(
        {
          ...withBreach,
          explicits: [{ modifierId: 'unreviewed', values: { [stat]: 20 } }],
        },
        definitions,
      ),
    ).toBeNull()
  })
  it.each([
    null,
    { ruleVersion: 'quality-limit-v1', maximumQuality: 20 },
    {
      rulesetIdentity: 'fixture-ruleset',
      ruleVersion: 'unknown',
      maximumQuality: 40,
    },
    { ruleVersion: 'quality-limit-v1', maximumQuality: '40' },
  ])('rejects contradictory or unsupported cap evidence %j', (value) => {
    expect(qualityLimitMatches(value, withBreach, definitions)).toBe(false)
  })
  it('stores detached cap evidence, reads legacy frames and preserves bytes when optional evidence is invalid', async () => {
    const reviewedRoot = { ...root, baseItemId: solar }
    const reviewedInitial = {
      ...initialFixture,
      state: { ...initialFixture.state, baseItemId: solar },
    }
    const response = await fixtureFetch('/api/v1/crafting/workbench/apply', {
      body: JSON.stringify({ state: reviewedRoot, action: 'TRANSMUTATION' }),
    })
    const result = (await response.json()) as AppliedItem
    result.qualityLimit = {
      ruleVersion: 'quality-limit-v1',
      maximumQuality: 20,
    }
    const films = recordCraft(
      startFilm(emptyFilms(), reviewedRoot, 'quality', 'fixture-ruleset'),
      reviewedRoot,
      result,
      'unused',
    )
    result.qualityLimit.maximumQuality = 40
    const repository = new LocalFilmRepository(localStorage)
    repository.save(films)
    const bytes = localStorage.getItem(historyStorageKey)
    const loaded = repository.load()
    expect(
      verifiedFrameEvidence(currentFrame(loaded)!, reviewedInitial)
        ?.qualityLimit?.maximumQuality,
    ).toBe(20)
    expect(localStorage.getItem(historyStorageKey)).toBe(bytes)
    const frame = currentFrame(loaded)!
    frame.evidence!.qualityLimit!.maximumQuality = 40
    expect(verifiedFrameEvidence(frame, reviewedInitial)).toBeUndefined()
    expect(localStorage.getItem(historyStorageKey)).toBe(bytes)
    delete frame.evidence!.qualityLimit
    expect(verifiedFrameEvidence(frame, reviewedInitial)).toBeDefined()
  })
})
