import { describe, expect, it } from 'vitest'
import {
  initialFixture,
  fixtureFetch,
} from '../../shared/test/craftingFixtures'
import { concreteInitial } from './workbenchApi'
import type { AppliedItem } from './workbenchApi'
import {
  LocalFilmRepository,
  emptyFilms,
  currentFrame,
  currentFilm,
  startFilm,
  recordCraft,
  viewFrame,
  verifiedHistoryState,
  historyStorageKey,
  historyStorageLimit,
  upgradeCompatibleFilms,
  verifiedFrameEvidence,
} from './workbenchHistory'

const root = concreteInitial(initialFixture)
async function craft() {
  return (await (
    await fixtureFetch('/api/v1/crafting/workbench/apply', {
      body: JSON.stringify({ state: root, action: 'TRANSMUTATION' }),
    })
  ).json()) as AppliedItem
}
describe('linear crafting films', () => {
  it('persists detached craft evidence while legacy films remain readable without invented provenance', async () => {
    const result = await craft()
    const films = recordCraft(
      startFilm(emptyFilms(), root, 'evidence'),
      root,
      result,
      'unused',
    )
    result.assumptions[0]!.reason = 'changed after recording'
    const repository = new LocalFilmRepository(localStorage)
    repository.save(films)
    const bytes = localStorage.getItem(historyStorageKey)
    const restored = repository.load()
    const evidence = verifiedFrameEvidence(
      currentFrame(restored)!,
      initialFixture,
    )
    expect(evidence?.ruleVersion).toBe(result.ruleVersion)
    expect(evidence?.assumptions[0]?.reason).toBe(
      'No published numeric roll weights.',
    )
    expect(localStorage.getItem(historyStorageKey)).toBe(bytes)
    const legacy = structuredClone(restored)
    for (const film of legacy.films)
      for (const frame of film.frames) delete frame.evidence
    repository.save(legacy)
    const legacyBytes = localStorage.getItem(historyStorageKey)
    expect(repository.load()).toEqual(legacy)
    expect(
      verifiedFrameEvidence(currentFrame(legacy)!, initialFixture),
    ).toBeUndefined()
    expect(localStorage.getItem(historyStorageKey)).toBe(legacyBytes)
  })

  it('keeps original evidence provenance when an explicitly compatible snapshot is upgraded', async () => {
    const result = await craft()
    const films = recordCraft(
      startFilm(emptyFilms(), root, 'evidence'),
      root,
      result,
      'unused',
    )
    const initial = {
      ...initialFixture,
      metadata: { ...initialFixture.metadata, snapshotId: 'new-snapshot' },
      state: { ...initialFixture.state, snapshotId: 'new-snapshot' },
      compatibleSnapshotIds: [root.snapshotId],
    }
    const upgraded = upgradeCompatibleFilms(films, initial)
    const frame = currentFrame(upgraded)!
    expect(frame.state.snapshotId).toBe('new-snapshot')
    expect(frame.evidence?.snapshotId).toBe(root.snapshotId)
    expect(verifiedFrameEvidence(frame, initial)).toEqual(
      currentFrame(films)!.evidence,
    )
  })

  it('rejects malformed optional evidence without discarding frames or rewriting storage', async () => {
    const films = recordCraft(
      startFilm(emptyFilms(), root, 'evidence'),
      root,
      await craft(),
      'unused',
    )
    const evidence = currentFrame(films)!.evidence!
    const malformed = [
      null,
      { ...evidence, events: null },
      { ...evidence, snapshotId: 'unknown' },
      { ...evidence, action: 'DIVINE' },
      { ...evidence, assumptions: [{ ...evidence.assumptions[0]!, n: 0 }] },
      {
        ...evidence,
        assumptions: [
          { ...evidence.assumptions[0]!, sourceUrl: 'javascript:alert(1)' },
        ],
      },
      {
        ...evidence,
        events: [{ ...evidence.events[0]!, values: { life: 19 } }],
      },
    ]
    for (const bad of malformed) {
      const saved = structuredClone(films)
      Object.assign(currentFrame(saved)!, { evidence: bad })
      const bytes = JSON.stringify(saved)
      localStorage.setItem(historyStorageKey, bytes)
      const loaded = new LocalFilmRepository(localStorage).load()
      expect(
        verifiedHistoryState(currentFrame(loaded)!.state, initialFixture),
      ).toBe(true)
      expect(
        verifiedFrameEvidence(currentFrame(loaded)!, initialFixture),
      ).toBeUndefined()
      expect(localStorage.getItem(historyStorageKey)).toBe(bytes)
    }
  })
  it('upgrades only an explicitly compatible valid snapshot without changing films, rolls, locks or stored bytes', async () => {
    const result = await craft()
    const old = recordCraft(
      startFilm(emptyFilms(), root, 'old'),
      root,
      result,
      'unused',
    )
    const nextInitial = {
      ...initialFixture,
      metadata: { ...initialFixture.metadata, snapshotId: 'expanded' },
      state: { ...initialFixture.state, snapshotId: 'expanded' },
      compatibleSnapshotIds: [root.snapshotId],
    }
    const repository = new LocalFilmRepository(window.localStorage)
    repository.save(old)
    const bytes = window.localStorage.getItem(historyStorageKey)
    const upgraded = upgradeCompatibleFilms(old, nextInitial)
    expect(window.localStorage.getItem(historyStorageKey)).toBe(bytes)
    expect(upgraded.active).toBe(old.active)
    expect(upgraded.cursor).toBe(old.cursor)
    expect(upgraded.films).toHaveLength(1)
    for (const [index, frame] of upgraded.films[0]!.frames.entries()) {
      expect(frame).toEqual({
        ...old.films[0]!.frames[index],
        state: {
          ...old.films[0]!.frames[index]!.state,
          snapshotId: 'expanded',
        },
      })
      expect(verifiedHistoryState(frame.state, nextInitial)).toBe(true)
    }
    const before = currentFrame(upgraded)!.state
    const continued = recordCraft(
      upgraded,
      before,
      { ...result, state: before },
      'must-not-fork',
    )
    expect(continued.films).toHaveLength(1)
    expect(continued.films[0]!.frames).toHaveLength(3)
    const unknown = { ...root, snapshotId: 'unknown' }
    const invalid = {
      ...root,
      explicits: [{ modifierId: 'p', values: { life: 999999 } }],
    }
    for (const state of [unknown, invalid]) {
      const preserved = startFilm(emptyFilms(), state, 'preserve')
      expect(upgradeCompatibleFilms(preserved, nextInitial)).toEqual(preserved)
      expect(verifiedHistoryState(state, nextInitial)).toBe(false)
    }
  })
  it('browses one film and forks only on successful crafting while preserving original future', async () => {
    const result = await craft()
    const original = recordCraft(
      startFilm(emptyFilms(), root, 'first'),
      root,
      result,
      'unused',
    )
    const browsed = viewFrame(original, 0)
    expect(browsed.films).toHaveLength(1)
    expect(currentFrame(browsed)?.state).toEqual(root)
    expect(
      recordCraft(browsed, root, { ...result, applied: false }, 'failed'),
    ).toBe(browsed)
    const fork = recordCraft(browsed, root, result, 'second')
    expect(fork.films).toHaveLength(2)
    expect(fork.films[0]).toEqual(original.films[0])
    expect(currentFilm(fork)?.id).toBe('second')
    expect(currentFilm(fork)?.frames).toHaveLength(2)
    expect(viewFrame(fork, 2)).toBe(fork)
  })
  it('archives the prior film when starting another item and restores the selected step', async () => {
    const first = recordCraft(
      startFilm(emptyFilms(), root, 'one'),
      root,
      await craft(),
      'unused',
    )
    const second = startFilm(first, { ...root, itemLevel: 70 }, 'two')
    const selected = viewFrame({ ...second, active: 'one' }, 0)
    const repository = new LocalFilmRepository(window.localStorage)
    repository.save(selected)
    const restored = repository.load()
    expect(restored).toEqual(selected)
    expect(restored.films[0]).toEqual(first.films[0])
    expect(restored.films[1]?.frames[0]?.state.itemLevel).toBe(70)
    expect(currentFrame(restored)?.state).toEqual(root)
  })
  it('preserves existing bytes on quota failure and on invalid or oversized reads', () => {
    const repository = new LocalFilmRepository(window.localStorage)
    const valid = startFilm(emptyFilms(), root, 'one')
    repository.save(valid)
    const previous = window.localStorage.getItem(historyStorageKey)
    const failing = new LocalFilmRepository({
      getItem: () => previous,
      setItem: () => {
        throw new DOMException('Quota exceeded', 'QuotaExceededError')
      },
    })
    expect(() => failing.save(startFilm(valid, root, 'two'))).toThrow('Quota')
    expect(window.localStorage.getItem(historyStorageKey)).toBe(previous)
    expect(() =>
      new LocalFilmRepository({
        getItem: () => '{broken',
        setItem: () => {
          throw Error('must not write')
        },
      }).load(),
    ).toThrow()
    expect(() =>
      new LocalFilmRepository({
        getItem: () => 'x'.repeat(historyStorageLimit),
        setItem: () => {
          throw Error('must not write')
        },
      }).load(),
    ).toThrow('size')
  })
  it('requires current catalog identity, legal slots, families and numeric values on restored states', async () => {
    const state = (await craft()).state
    expect(verifiedHistoryState(state, initialFixture)).toBe(true)
    expect(
      verifiedHistoryState({ ...state, snapshotId: 'old' }, initialFixture),
    ).toBe(false)
    expect(
      verifiedHistoryState(
        { ...state, explicits: [...state.explicits, ...state.explicits] },
        initialFixture,
      ),
    ).toBe(false)
    expect(
      verifiedHistoryState(
        { ...state, explicits: [{ modifierId: 'p', values: { life: 9000 } }] },
        initialFixture,
      ),
    ).toBe(false)
    expect(
      verifiedHistoryState(
        { ...state, conditions: ['CORRUPTED'] },
        initialFixture,
      ),
    ).toBe(false)
  })
})
