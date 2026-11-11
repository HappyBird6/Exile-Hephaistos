import { expect, it, vi } from 'vitest'
import { initialFixture } from '../../../shared/test/craftingFixtures'
import { performanceSettings } from '../../../shared/performance'
import { concreteInitial } from '../workbenchApi'
import { loadModifierPools } from '../workbenchReferenceApi'
import {
  acceptPoolUpdate,
  cachedPool,
  poolHeaders,
  rememberPool,
} from './poolSnapshots'
import type { PoolEntry } from './poolReference'
const root = concreteInitial(initialFixture)
const revision = 'a'.repeat(64)
const nextRevision = 'b'.repeat(64)
const entry: PoolEntry = {
  modifierId: 'p',
  kind: 'ordinary',
  eligible: true,
  reasons: [],
  actions: [],
}
it('reconstructs changes without an HTTP request and isolates rules, modes and state', async () => {
  const rules = 'delta-test'
  rememberPool(root, true, rules, { entries: [entry], revision })
  const next = { ...root, itemLevel: root.itemLevel + 1 }
  const changed: PoolEntry = {
    ...entry,
    eligible: false,
    reasons: ['FAMILY_OCCUPIED'],
  }
  acceptPoolUpdate(
    {
      fromRevision: revision,
      revision: nextRevision,
      reset: false,
      entries: [changed],
      removed: [],
    },
    root,
    next,
    rules,
    initialFixture.modifiers,
  )
  const fetch = vi.fn()
  vi.stubGlobal('fetch', fetch)
  try {
    expect(
      await loadModifierPools(
        { ...initialFixture, rulesetIdentity: rules },
        next,
        true,
        new AbortController().signal,
      ),
    ).toEqual({ entries: [changed], revision: nextRevision })
    expect(fetch).not.toHaveBeenCalled()
    expect(poolHeaders(next, rules)).toEqual({
      'X-Modifier-Pool-Revision': nextRevision,
    })
    expect(cachedPool(next, false, rules)).toBeUndefined()
    expect(cachedPool(next, true, 'other-rules')).toBeUndefined()
    expect(cachedPool(root, true, rules)?.entries).toEqual([entry])
  } finally {
    vi.unstubAllGlobals()
  }
})
it('ignores stale, malformed and unknown deltas; supports a full reset after server eviction', () => {
  const rules = 'reset-test'
  rememberPool(root, true, rules, { entries: [entry], revision })
  const next = { ...root, itemLevel: root.itemLevel + 2 }
  const valid = {
    fromRevision: revision,
    revision: nextRevision,
    reset: false,
    entries: [],
    removed: [],
  }
  for (const invalid of [
    null,
    { ...valid, fromRevision: nextRevision },
    { ...valid, entries: [{ ...entry, modifierId: 'unknown' }] },
    { ...valid, removed: ['unknown'] },
    { ...valid, entries: [entry, entry] },
  ]) {
    acceptPoolUpdate(invalid, root, next, rules, initialFixture.modifiers)
    expect(cachedPool(next, true, rules)).toBeUndefined()
  }
  acceptPoolUpdate(
    { ...valid, fromRevision: null, reset: true, entries: [entry] },
    root,
    next,
    rules,
    initialFixture.modifiers,
  )
  expect(cachedPool(next, true, rules)?.entries).toEqual([entry])
  acceptPoolUpdate(
    { ...valid, removed: ['ordinary:p'] },
    root,
    next,
    rules,
    initialFixture.modifiers,
  )
  expect(cachedPool(next, true, rules)?.entries).toEqual([])
})
it('reuses numeric rerolls, honors wire defaults and retains future fields in its key', () => {
  const rules = 'key-test'
  const state = {
    ...root,
    explicits: [{ modifierId: 'p', values: { life: 10 } }],
  }
  rememberPool(state, true, rules, { entries: [entry], revision })
  expect(
    cachedPool(
      {
        ...state,
        explicits: [
          { modifierId: 'p', values: { life: 20 }, fractured: false },
        ],
      },
      true,
      rules,
    ),
  ).toBeDefined()
  expect(
    cachedPool(
      {
        ...state,
        explicits: [{ modifierId: 'p', values: { life: 20 }, fractured: true }],
      },
      true,
      rules,
    ),
  ).toBeUndefined()
  expect(
    cachedPool(
      { ...state, futureMechanism: true } as typeof state,
      true,
      rules,
    ),
  ).toBeUndefined()
})
it('bounds retention so visiting many bases and sessions cannot grow memory forever', () => {
  const rules = 'bounded-test'
  rememberPool(root, true, rules, { entries: [entry], revision })
  for (let i = 0; i < performanceSettings.poolSnapshotEntries; i++)
    rememberPool({ ...root, snapshotId: `snapshot-${i}` }, true, rules, {
      entries: [entry],
      revision,
    })
  expect(cachedPool(root, true, rules)).toBeUndefined()
})

it('reconstructs a static-basis delta without carrying exclusions from the previous item', () => {
  const rules = 'stateless-basis-test'
  rememberPool(root, true, rules, {
    revision,
    staticRevision: nextRevision,
    entries: [{ ...entry, eligible: false, reasons: ['FAMILY_OCCUPIED'] }],
  })
  expect(poolHeaders(root, rules)).toEqual({
    'X-Modifier-Pool-Revision': revision,
    'X-Modifier-Pool-Baseline': nextRevision,
  })
  const next = { ...root, itemLevel: root.itemLevel + 1 }
  acceptPoolUpdate(
    {
      fromRevision: nextRevision,
      revision: 'e'.repeat(64),
      staticRevision: nextRevision,
      reset: false,
      entries: [],
      removed: [],
    },
    root,
    next,
    rules,
    initialFixture.modifiers,
  )
  expect(cachedPool(next, true, rules)?.entries).toEqual([entry])
})
it('shares static references across levels while isolating socket and future state context', () => {
  const rules = 'static-level-test'
  rememberPool(root, false, rules, { entries: [entry], revision })
  expect(cachedPool({ ...root, itemLevel: 1 }, false, rules)).toBeDefined()
  expect(
    cachedPool(
      { ...root, installedAugments: [{ slotIndex: 0, augmentId: 'other' }] },
      false,
      rules,
    ),
  ).toBeUndefined()
})

it('applies bounded static ordinal groups and rejects ambiguous indexes without poisoning the cache', () => {
  const rules = 'ordinal-group-test'
  rememberPool(root, true, rules, {
    revision,
    staticRevision: nextRevision,
    entries: [entry],
  })
  const next = { ...root, itemLevel: root.itemLevel + 1 }
  const update = {
    fromRevision: nextRevision,
    revision: 'e'.repeat(64),
    staticRevision: nextRevision,
    reset: false,
    entries: [],
    removed: [],
    groups: [{ eligible: false, reasons: ['AFFIX_FULL'], indices: [0] }],
  }
  for (const indices of [[-1], [1], [0, 0], [0.5]]) {
    acceptPoolUpdate(
      { ...update, groups: [{ ...update.groups[0], indices }] },
      root,
      next,
      rules,
      initialFixture.modifiers,
    )
    expect(cachedPool(next, true, rules)).toBeUndefined()
  }
  acceptPoolUpdate(update, root, next, rules, initialFixture.modifiers)
  expect(cachedPool(next, true, rules)?.entries).toEqual([
    { ...entry, eligible: false, reasons: ['AFFIX_FULL'] },
  ])
})
