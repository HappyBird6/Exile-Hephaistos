import { afterEach, expect, it, vi } from 'vitest'
import {
  initialFixture,
  jsonResponse,
  fixtureFetch,
} from '../../shared/test/craftingFixtures'
import {
  checkSessionCompatibility,
  loadModifierPools,
} from './workbenchReferenceApi'
import { applyCurrency, concreteInitial } from './workbenchApi'
import {
  recordCraft,
  viewFrame,
  continueFilm,
  currentFilm,
  emptyFilms,
  LocalFilmRepository,
  startFilm,
} from './workbenchHistory'

afterEach(() => vi.unstubAllGlobals())
const root = concreteInitial(initialFixture)
const signal = () => new AbortController().signal
it('uses server eligibility and sends the current identity and exact item', async () => {
  const entries = [
    {
      modifierId: 'p',
      kind: 'ordinary',
      eligible: false,
      reasons: ['FAMILY_OCCUPIED'],
      actions: [],
    },
  ]
  const fetch = vi.fn().mockResolvedValue(jsonResponse({ entries }))
  vi.stubGlobal('fetch', fetch)
  expect(await loadModifierPools(initialFixture, root, true, signal())).toEqual(
    { entries },
  )
  const options = fetch.mock.calls[0]![1] as RequestInit
  expect(JSON.parse(options.body as string)).toEqual({
    state: root,
    dynamic: true,
  })
  expect(options.headers).toHaveProperty(
    'X-Crafting-Ruleset',
    initialFixture.rulesetIdentity,
  )
})
it.each([
  [
    {
      modifierId: 'missing',
      kind: 'ordinary',
      eligible: true,
      reasons: [],
      actions: [],
    },
  ],
  [
    {
      modifierId: 'p',
      kind: 'ordinary',
      eligible: true,
      reasons: ['ITEM_LEVEL'],
      actions: [],
    },
  ],
  [
    {
      modifierId: 'p',
      kind: 'ordinary',
      eligible: false,
      reasons: ['made-up'],
      actions: [],
    },
  ],
])('rejects inconsistent server pools %#', async (entries) => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({ entries })))
  await expect(
    loadModifierPools(initialFixture, root, true, signal()),
  ).rejects.toThrow('Invalid modifier reference')
})
it('accepts an exact legacy checkpoint with wire defaults, preserves its original film and persists lineage', async () => {
  const past = { ...root, snapshotId: 'past', qualityInfused: true }
  const original = startFilm(
    emptyFilms(),
    past,
    'original',
    'past-rules',
    'exact original text',
  )
  const bytes = JSON.stringify(original)
  const responseState = {
    ...root,
    qualityInfused: true,
    installedAugments: [],
    desecrated: [],
    catalystQuality: null,
    augmentSockets: null,
    equipmentQuality: null,
    observations: null,
    sanctification: null,
  }
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue(
      jsonResponse({
        compatible: true,
        sameRules: false,
        compatibilityId: 'item-rules-v1:current',
        state: responseState,
        abyss: null,
        reasons: [],
      }),
    ),
  )
  const result = await checkSessionCompatibility(
    initialFixture,
    past,
    undefined,
    undefined,
    signal(),
  )
  const next = continueFilm(
    original,
    original.films[0]!,
    0,
    result.state!,
    null,
    'new',
    initialFixture.rulesetIdentity,
    result.compatibilityId!,
  )
  expect(JSON.stringify(original)).toBe(bytes)
  expect(next.films[0]).toBe(original.films[0])
  expect(currentFilm(next)?.sourceDocument).toBeUndefined()
  expect(currentFilm(next)?.continuationOf).toEqual({
    sessionId: 'original',
    frameIndex: 0,
    rulesetIdentity: 'past-rules',
  })
  const storage = new Map<string, string>()
  const repo = new LocalFilmRepository({
    getItem: (key) => storage.get(key) ?? null,
    setItem: (key, value) => {
      storage.set(key, value)
    },
  })
  repo.save(next)
  expect(currentFilm(repo.load())?.compatibilityId).toBe(result.compatibilityId)
  expect(repo.load().films[0]).toEqual(original.films[0])
  expect(currentFilm(repo.load())?.frames[0]?.state.qualityInfused).toBe(true)
})
it('rejects a response which changes item values or claims a different compatibility key', async () => {
  for (const data of [
    { state: { ...root, itemLevel: 81 }, sameRules: false },
    { state: root, sameRules: true },
  ]) {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        jsonResponse({
          compatible: true,
          compatibilityId: 'unexpected',
          abyss: null,
          reasons: [],
          ...data,
        }),
      ),
    )
    await expect(
      checkSessionCompatibility(
        initialFixture,
        root,
        undefined,
        'previous',
        signal(),
      ),
    ).rejects.toThrow('Invalid continued checkpoint')
  }
})
it('rejects reference responses from a different ruleset', async () => {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ entries: [] }), {
        headers: { 'X-Crafting-Ruleset': 'other' },
      }),
    ),
  )
  await expect(
    loadModifierPools(initialFixture, root, true, signal()),
  ).rejects.toThrow()
})

it('keeps per-item identity when rewinding into a separate craft film', async () => {
  vi.stubGlobal('fetch', vi.fn(fixtureFetch))
  const result = await applyCurrency(
    root,
    'TRANSMUTATION',
    initialFixture.modifiers,
    signal(),
    [],
    initialFixture.rulesetIdentity,
  )
  const history = recordCraft(
    startFilm(
      emptyFilms(),
      root,
      'root',
      initialFixture.rulesetIdentity,
      undefined,
      'per-base-key',
    ),
    root,
    result,
    'unused',
  )
  const original = structuredClone(history.films[0])
  const branched = recordCraft(viewFrame(history, 0), root, result, 'branch')
  expect(currentFilm(branched)?.compatibilityId).toBe('per-base-key')
  expect(branched.films[0]).toEqual(original)
})

it('receives a pool delta with a verified craft, reuses it without a second request and strips it from history', async () => {
  const { rememberPool, cachedPool } =
    await import('./modifier-pool/poolSnapshots')
  const identity = initialFixture.rulesetIdentity
  const revision = 'c'.repeat(64)
  rememberPool(root, true, identity, { revision, entries: [] })
  const fetch = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const response = await fixtureFetch(input, init)
    const value = await response.json()
    return jsonResponse({
      ...value,
      modifierPool: {
        fromRevision: revision,
        revision: 'd'.repeat(64),
        reset: false,
        entries: [
          {
            modifierId: 'p',
            kind: 'ordinary',
            eligible: false,
            reasons: ['FAMILY_OCCUPIED'],
            actions: [],
          },
        ],
        removed: [],
      },
    })
  })
  vi.stubGlobal('fetch', fetch)
  const result = await applyCurrency(
    root,
    'TRANSMUTATION',
    initialFixture.modifiers,
    signal(),
    [],
    identity,
  )
  expect(result).not.toHaveProperty('modifierPool')
  expect(fetch.mock.calls[0]![1]?.headers).toHaveProperty(
    'X-Modifier-Pool-Revision',
    revision,
  )
  const pool = await loadModifierPools(
    initialFixture,
    result.state,
    true,
    signal(),
  )
  expect(pool.entries[0]?.reasons).toEqual(['FAMILY_OCCUPIED'])
  expect(cachedPool(result.state, true, identity)?.revision).toBe(
    'd'.repeat(64),
  )
  expect(fetch).toHaveBeenCalledTimes(1)
})
