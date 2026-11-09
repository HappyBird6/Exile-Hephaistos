import { describe, expect, it, vi } from 'vitest'
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
  verifiedFrameEvidence,
  filmSessionNumber,
  updateFilmMetadata,
  deleteFilm,
  undoFilmDeletion,
  recordChaosRepeat,
  filmRulesetStatus,
} from './workbenchHistory'
import {
  createChaosRepeatReceipt,
  validChaosRepeatFrame,
} from './chaos-repeat/chaosRepeatReceipt'
import type {
  ChaosRepeatAction,
  ChaosRepeatReceipt,
} from './chaos-repeat/chaosRepeatReceipt'

const root = concreteInitial(initialFixture)

describe('session library metadata and individual deletion', () => {
  function sessions() {
    return startFilm(
      startFilm(emptyFilms(), root, 'one', 'fixture-ruleset', 'exact\r\nraw'),
      root,
      'two',
      'fixture-ruleset',
    )
  }
  it('round trips titles and favorites without altering legacy frames, raw text or ruleset evidence', () => {
    const legacy = sessions()
    delete legacy.films[0]!.sessionNumber
    const original = legacy.films[0]!
    const updated = updateFilmMetadata(legacy, 'one', {
      title: ' My exact title\n',
      favorite: true,
    })
    expect(updated.films[0]!.frames).toBe(original.frames)
    expect(updated.films[0]!.sourceDocument).toBe(original.sourceDocument)
    expect(updated.films[0]!.rulesetIdentity).toBe(original.rulesetIdentity)
    expect(filmSessionNumber(original, 0)).toBe(1)
    expect(original.title).toBeUndefined()
    const repository = new LocalFilmRepository(localStorage)
    repository.save(updated)
    expect(repository.load()).toEqual(updated)
    expect(updateFilmMetadata(updated, 'missing', { title: 'no' })).toBe(
      updated,
    )
  })
  it('deletes only the named session, safely selects an adjacent session and restores the exact record', () => {
    const original = sessions()
    const removed = deleteFilm(original, 'two')!
    expect(removed.history.films.map((film) => film.id)).toEqual(['one'])
    expect(removed.history.active).toBe('one')
    expect(currentFrame(removed.history)?.state).toEqual(root)
    expect(undoFilmDeletion(removed.history, removed.deletion)).toEqual(
      original,
    )
    expect(deleteFilm(original, 'missing')).toBeNull()
    const inactive = deleteFilm(original, 'one')!
    expect(inactive.history.active).toBe('two')
    expect(inactive.history.cursor).toBe(original.cursor)
    const last = deleteFilm(removed.history, 'one')!
    expect(last.history.active).toBeNull()
    expect(last.history.cursor).toBe(0)
    expect(last.history.films).toEqual([])
  })
  it('keeps legacy numbers stable across deletion and never reuses the deleted highest number', () => {
    const legacy = sessions()
    for (const film of legacy.films) delete film.sessionNumber
    delete legacy.nextSessionNumber
    const removedFirst = deleteFilm(legacy, 'one')!
    expect(removedFirst.history.films[0]!.sessionNumber).toBe(2)
    const removedLast = deleteFilm(legacy, 'two')!
    const next = startFilm(
      removedLast.history,
      root,
      'three',
      'fixture-ruleset',
    )
    expect(next.films.at(-1)!.sessionNumber).toBe(3)
    expect(next.nextSessionNumber).toBe(4)
  })
  it('undo preserves intervening edits, new sessions and an explicitly changed selection', () => {
    const original = sessions()
    const removed = deleteFilm(original, 'one')!
    const continued = updateFilmMetadata(
      startFilm(removed.history, root, 'three', 'fixture-ruleset'),
      'two',
      { title: 'kept' },
    )
    const restored = undoFilmDeletion(continued, removed.deletion)
    expect(restored.films.map((film) => film.id)).toEqual([
      'one',
      'two',
      'three',
    ])
    expect(restored.films[1]!.title).toBe('kept')
    expect(restored.active).toBe('three')
    expect(restored.films[0]!.sourceDocument).toBe('exact\r\nraw')
    expect(undoFilmDeletion(restored, removed.deletion)).toBe(restored)
  })
})
async function craft() {
  return (await (
    await fixtureFetch('/api/v1/crafting/workbench/apply', {
      body: JSON.stringify({ state: root, action: 'TRANSMUTATION' }),
    })
  ).json()) as AppliedItem
}

describe('one-frame Chaos repeat history', () => {
  async function batch(action: ChaosRepeatAction = 'CHAOS', uses = 3) {
    const applied = await craft()
    const before = { ...applied.state, rarity: 'RARE' as const }
    const finalWitness = { ...applied, action, state: before }
    const history = startFilm(
      emptyFilms(),
      before,
      'repeat',
      initialFixture.rulesetIdentity,
      'exact\r\nsource',
    )
    const input = {
      before,
      action,
      uses,
      finalWitness,
      consumedOmens: ['Omen_of_Whittling'],
    }
    // The adapter's batch verifier is a fixture, not the single-use API validator.
    const receipt = createChaosRepeatReceipt(input, () => true)!
    return { history, before, finalWitness, input, receipt }
  }

  it.each(['CHAOS', 'GREATER_CHAOS', 'PERFECT_CHAOS'] as const)(
    'stores exactly one detached %s frame, final witness and confirmed aggregate',
    async (action) => {
      for (const uses of [1, 500, Number.MAX_SAFE_INTEGER]) {
        const { history, receipt, finalWitness } = await batch(action, uses)
        const saved = recordChaosRepeat(
          history,
          receipt,
          'unused',
          initialFixture,
        )
        const frame = currentFrame(saved)!
        expect(currentFilm(saved)!.frames).toHaveLength(2)
        expect(frame.chaosRepeat).toEqual({ version: 1, action, uses })
        expect(frame.evidence?.events).toEqual(finalWitness.events)
        expect(frame.evidence?.assumptions).toEqual(finalWitness.assumptions)
        expect(frame.evidence?.consumedOmens).toEqual(['Omen_of_Whittling'])
        expect(frame.evidence?.consumedOmens).not.toEqual(
          finalWitness.consumedOmens,
        )
        expect(verifiedFrameEvidence(frame, initialFixture)).toEqual(
          frame.evidence,
        )
        expect(
          filmRulesetStatus(
            currentFilm(saved)!,
            initialFixture.rulesetIdentity,
          ),
        ).toBe('current')
        expect(currentFilm(history)!.frames).toHaveLength(1)
        finalWitness.events.length = 0
        expect(frame.evidence?.events).toHaveLength(1)
      }
    },
  )

  it('undo returns to before the whole batch and redo restores it, retaining future and provenance on branching', async () => {
    const { history, receipt, before, input } = await batch()
    const first = recordChaosRepeat(history, receipt, 'unused', initialFixture)
    const two = recordChaosRepeat(first, receipt, 'unused', initialFixture)
    const original = currentFilm(two)!
    const bytes = JSON.stringify(original)
    const undo = viewFrame(two, 0)
    expect(currentFrame(undo)?.state).toEqual(before)
    expect(currentFrame(undo)?.chaosRepeat).toBeUndefined()
    expect(currentFrame(viewFrame(undo, 2))?.chaosRepeat?.uses).toBe(3)
    const branchReceipt = createChaosRepeatReceipt(
      { ...input, uses: 7 },
      () => true,
    )!
    const branched = recordChaosRepeat(
      undo,
      branchReceipt,
      'branch',
      initialFixture,
    )
    expect(branched.films).toHaveLength(2)
    expect(branched.films[0]).toBe(original)
    expect(JSON.stringify(original)).toBe(bytes)
    expect(currentFilm(branched)?.frames).toHaveLength(2)
    expect(currentFilm(branched)?.sourceDocument).toBe('exact\r\nsource')
    expect(currentFrame(branched)?.chaosRepeat?.uses).toBe(7)
    expect(original.frames[1]?.chaosRepeat?.uses).toBe(3)
    expect(original.frames[2]?.chaosRepeat?.uses).toBe(3)
  })

  it('keeps failed, zero-use, cancelled/unreceived and counterfeit receipts from creating frames', async () => {
    const { history, input } = await batch()
    for (const failed of [
      { ...input, uses: 0 },
      { ...input, finalWitness: { ...input.finalWitness, applied: false } },
    ]) {
      const receipt = createChaosRepeatReceipt(failed, () => true)
      expect(receipt).toBeNull()
      expect(
        recordChaosRepeat(history, receipt, 'unused', initialFixture),
      ).toBe(history)
    }
    expect(recordChaosRepeat(history, null, 'cancelled', initialFixture)).toBe(
      history,
    )
    expect(
      recordChaosRepeat(
        history,
        {} as ChaosRepeatReceipt,
        'forged',
        initialFixture,
      ),
    ).toBe(history)
  })

  it('retains state, evidence and ruleset checks even with a branded receipt', async () => {
    const { history, input, receipt } = await batch()
    const original = JSON.stringify(history)
    for (const invalid of [
      { ...input, before: { ...input.before, itemLevel: 101 } },
      {
        ...input,
        finalWitness: {
          ...input.finalWitness,
          state: { ...input.finalWitness.state, itemLevel: 0 },
        },
      },
      {
        ...input,
        finalWitness: {
          ...input.finalWitness,
          events: [{ ...input.finalWitness.events[0]!, values: { life: 999 } }],
        },
      },
    ]) {
      const invalidReceipt = createChaosRepeatReceipt(invalid, () => true)!
      expect(() =>
        recordChaosRepeat(history, invalidReceipt, 'invalid', initialFixture),
      ).toThrow('Could not verify repeat history')
    }
    const legacy = structuredClone(history)
    delete legacy.films[0]!.rulesetIdentity
    expect(() =>
      recordChaosRepeat(legacy, receipt, 'invalid', initialFixture),
    ).toThrow('Saved ruleset is unverified')
    expect(JSON.stringify(history)).toBe(original)
  })

  it('round trips new v1 extensions and leaves original legacy records and bytes untouched on read', async () => {
    const { history, receipt, before, finalWitness } = await batch()
    const legacy = recordCraft(history, before, finalWitness, 'unused')
    const storageBytes = new Map<string, string>()
    const storage = {
      getItem: (key: string) => storageBytes.get(key) ?? null,
      setItem: vi.fn((key: string, value: string) =>
        storageBytes.set(key, value),
      ),
    }
    const raw = JSON.stringify(legacy, null, 2)
    storageBytes.set(historyStorageKey, raw)
    const repository = new LocalFilmRepository(storage)
    const loaded = repository.load()
    expect(storage.setItem).not.toHaveBeenCalled()
    expect(storage.getItem(historyStorageKey)).toBe(raw)
    expect(currentFrame(loaded)).not.toHaveProperty('chaosRepeat')
    const saved = recordChaosRepeat(loaded, receipt, 'unused', initialFixture)
    repository.save(saved)
    expect(storage.getItem(historyStorageKey + '.preserved')).toBe(raw)
    expect(repository.load()).toEqual(saved)
    expect(currentFilm(repository.load())!.frames[1]).not.toHaveProperty(
      'chaosRepeat',
    )
    expect(currentFilm(repository.load())!.frames[2]?.chaosRepeat?.uses).toBe(3)
  })

  it('rejects damaged extensions without falling back to one use or overwriting saved bytes', async () => {
    const { history, receipt } = await batch()
    const valid = recordChaosRepeat(history, receipt, 'unused', initialFixture)
    const metadata = currentFrame(valid)!.chaosRepeat!
    const damaged = [
      null,
      undefined,
      {},
      { ...metadata, version: 2 },
      { ...metadata, action: 'EXALTED' },
      { ...metadata, action: 'GREATER_CHAOS' },
      ...[0, -1, 0.5, NaN, Infinity, 2 ** 53].map((uses) => ({
        ...metadata,
        uses,
      })),
    ]
    for (const extension of damaged) {
      const malformed = structuredClone(valid)
      Object.assign(currentFrame(malformed)!, { chaosRepeat: extension })
      expect(validChaosRepeatFrame(currentFrame(malformed)!)).toBe(false)
      expect(
        verifiedFrameEvidence(currentFrame(malformed)!, initialFixture),
      ).toBeUndefined()
      expect(
        filmRulesetStatus(
          currentFilm(malformed)!,
          initialFixture.rulesetIdentity,
        ),
      ).toBe('inconsistent')
      // JSON drops undefined properties; persist an empty damaged extension for that case.
      const raw = JSON.stringify(malformed, (key, value: unknown) =>
        key === 'chaosRepeat' && value === undefined ? {} : value,
      )
      const storage = { getItem: () => raw, setItem: vi.fn() }
      const repository = new LocalFilmRepository(storage)
      expect(() => repository.load()).toThrow('Saved history is invalid')
      expect(() => repository.save(malformed)).toThrow('Invalid repeat history')
      expect(storage.setItem).not.toHaveBeenCalled()
      expect(storage.getItem()).toBe(raw)
    }
    for (const damage of [
      'evidence',
      'consumedOmens',
      'ruleVersion',
      'evidenceAction',
      'manual',
      'root',
    ] as const) {
      const malformed = structuredClone(valid)
      if (damage === 'evidence') delete currentFrame(malformed)!.evidence
      else if (damage === 'consumedOmens')
        Object.assign(currentFrame(malformed)!.evidence!, {
          consumedOmens: undefined,
        })
      else if (damage === 'ruleVersion')
        currentFrame(malformed)!.evidence!.ruleVersion = ''
      else if (damage === 'evidenceAction')
        currentFrame(malformed)!.evidence!.action = 'PERFECT_CHAOS'
      else if (damage === 'manual')
        currentFrame(malformed)!.manual = {
          rulesetIdentity: initialFixture.rulesetIdentity,
          action: 'CHAOS',
          consumedOmens: [],
        }
      else
        malformed.films[0]!.frames[0] = structuredClone(
          currentFrame(malformed)!,
        )
      expect(
        filmRulesetStatus(
          currentFilm(malformed)!,
          initialFixture.rulesetIdentity,
        ),
      ).toBe('inconsistent')
      const raw = JSON.stringify(malformed)
      const storage = { getItem: () => raw, setItem: vi.fn() }
      expect(() => new LocalFilmRepository(storage).load()).toThrow()
      expect(storage.setItem).not.toHaveBeenCalled()
    }
  })
})
describe('linear crafting films', () => {
  it('round trips observed sockets, unresolved Augment effects and explicit skill levels without filling unknowns', async () => {
    const observations = {
      socketCount: 4,
      augmentEffects: ['20% increased Armour (rune)'],
      grantedSkills: [
        { name: 'Malice', level: 17 },
        { name: 'Other skill', level: null },
      ],
    }
    const observed = { ...root, observations }
    const films = startFilm(
      emptyFilms(),
      observed,
      'observed',
      'fixture-ruleset',
      'original',
    )
    const repository = new LocalFilmRepository(localStorage)
    repository.save(films)
    expect(currentFrame(repository.load())?.state.observations).toEqual(
      observations,
    )
    const result = await craft()
    const blocked = recordCraft(
      repository.load(),
      observed,
      { ...result, applied: false, state: observed },
      'unused',
    )
    expect(blocked).toEqual(films)
    expect(currentFrame(blocked)?.state.augmentSockets).toBe(
      root.augmentSockets,
    )
  })

  it('retains exact pasted provenance across storage, crafting and a history branch', async () => {
    const source = 'Item Class: Amulets\r\nRarity: Normal\r\nSolar Amulet\r\n'
    const initial = startFilm(
      emptyFilms(),
      root,
      'import',
      'fixture-ruleset',
      source,
    )
    const result = await craft()
    const crafted = recordCraft(initial, root, result, 'unused')
    const branched = recordCraft(viewFrame(crafted, 0), root, result, 'branch')
    const repository = new LocalFilmRepository(localStorage)
    repository.save(branched)
    const restored = repository.load()
    expect(restored.films.map((film) => film.sourceDocument)).toEqual([
      source,
      source,
    ])
    expect(currentFrame(restored)?.state).toEqual(result.state)
    expect(
      currentFilm(
        recordCraft(initial, root, { ...result, applied: false }, 'unused'),
      )?.sourceDocument,
    ).toBe(source)
    const unrelated = { ...root, itemLevel: root.itemLevel - 1 }
    expect(
      currentFilm(recordCraft(initial, unrelated, result, 'other'))
        ?.sourceDocument,
    ).toBeUndefined()
  })

  it('persists detached craft evidence while legacy films remain readable without invented provenance', async () => {
    const result = await craft()
    const films = recordCraft(
      startFilm(emptyFilms(), root, 'evidence', 'fixture-ruleset'),
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

  it('keeps original evidence provenance without relabeling an explicitly compatible snapshot', async () => {
    const result = await craft()
    const films = recordCraft(
      startFilm(emptyFilms(), root, 'evidence', 'fixture-ruleset'),
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
    const repository = new LocalFilmRepository(localStorage)
    repository.save(films)
    const upgraded = repository.load()
    const frame = currentFrame(upgraded)!
    expect(frame.state.snapshotId).toBe(root.snapshotId)
    expect(frame.evidence?.snapshotId).toBe(root.snapshotId)
    expect(verifiedFrameEvidence(frame, initial)).toEqual(
      currentFrame(films)!.evidence,
    )
  })

  it('rejects malformed optional evidence without discarding frames or rewriting storage', async () => {
    const films = recordCraft(
      startFilm(emptyFilms(), root, 'evidence', 'fixture-ruleset'),
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
  it('preserves explicitly compatible snapshots without changing films, rolls, locks or stored bytes', async () => {
    const result = await craft()
    const old = recordCraft(
      startFilm(emptyFilms(), root, 'old', 'fixture-ruleset'),
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
    const upgraded = repository.load()
    expect(window.localStorage.getItem(historyStorageKey)).toBe(bytes)
    expect(upgraded.active).toBe(old.active)
    expect(upgraded.cursor).toBe(old.cursor)
    expect(upgraded.films).toHaveLength(1)
    for (const [index, frame] of upgraded.films[0]!.frames.entries()) {
      expect(frame).toEqual({
        ...old.films[0]!.frames[index],
        state: {
          ...old.films[0]!.frames[index]!.state,
          snapshotId: root.snapshotId,
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
      const preserved = startFilm(
        emptyFilms(),
        state,
        'preserve',
        'fixture-ruleset',
      )
      repository.save(preserved)
      expect(repository.load()).toEqual(preserved)
      expect(verifiedHistoryState(state, nextInitial)).toBe(false)
    }
  })
  it('browses one film and forks only on successful crafting while preserving original future', async () => {
    const result = await craft()
    const original = recordCraft(
      startFilm(emptyFilms(), root, 'first', 'fixture-ruleset'),
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
      startFilm(emptyFilms(), root, 'one', 'fixture-ruleset'),
      root,
      await craft(),
      'unused',
    )
    const second = startFilm(
      first,
      { ...root, itemLevel: 70 },
      'two',
      'fixture-ruleset',
    )
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
    const valid = startFilm(emptyFilms(), root, 'one', 'fixture-ruleset')
    repository.save(valid)
    const previous = window.localStorage.getItem(historyStorageKey)
    const failing = new LocalFilmRepository({
      getItem: () => previous,
      setItem: () => {
        throw new DOMException('Quota exceeded', 'QuotaExceededError')
      },
    })
    expect(() =>
      failing.save(startFilm(valid, root, 'two', 'fixture-ruleset')),
    ).toThrow('Quota')
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

it('restores unrevealed slots without counting them as missing modifiers', () => {
  const state = {
    ...root,
    rarity: 'RARE' as const,
    desecrated: [{ id: 'hidden', side: 'PREFIX' as const, modifierId: null }],
  }
  expect(verifiedHistoryState(state, initialFixture)).toBe(true)
  const repository = new LocalFilmRepository(localStorage)
  repository.save(startFilm(emptyFilms(), state, 'hidden', 'fixture-ruleset'))
  expect(currentFrame(repository.load())!.state).toEqual(state)
  expect(
    verifiedHistoryState(
      {
        ...state,
        desecrated: Array.from({ length: 4 }, (_, i) => ({
          id: String(i),
          side: 'PREFIX' as const,
          modifierId: null,
        })),
      },
      initialFixture,
    ),
  ).toBe(false)
})

it('refuses duplicate session IDs before they can replace an original or reach browser storage', () => {
  const history = startFilm(emptyFilms(), root, 'original', 'fixture-ruleset')
  const repo = new LocalFilmRepository(localStorage)
  repo.save(history)
  const before = localStorage.getItem(historyStorageKey)
  expect(() => startFilm(history, root, 'original', 'fixture-ruleset')).toThrow(
    'Session ID already exists',
  )
  expect(() =>
    repo.save({
      ...history,
      films: [...history.films, structuredClone(history.films[0]!)],
    }),
  ).toThrow('Duplicate session IDs')
  expect(localStorage.getItem(historyStorageKey)).toBe(before)
  expect(repo.load()).toEqual(history)
})
