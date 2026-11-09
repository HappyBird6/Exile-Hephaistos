import { describe, expect, it, vi } from 'vitest'
import { initialFixture } from '../../../shared/test/craftingFixtures'
import type { AbyssState } from '../AbyssPanel'
import { applyCurrency } from '../workbenchApi'
import {
  currentFilm,
  currentFrame,
  LocalFilmRepository,
  recordAbyss,
  recordCraft,
  viewFrame,
} from '../workbenchHistory'
import type { Film } from '../workbenchHistory'
import type { ChaosRepeatAction } from '../chaos-repeat/chaosRepeatReceipt'
import { extractSessionNotation } from './extractSessionNotation'
import {
  craft,
  freeze,
  identity,
  root,
  start,
  twoSteps,
} from './fixtures.test-support'

const abyss: AbyssState = { item: root, slots: [], offer: null }

describe('repeat notation receipts', () => {
  it.each([
    ['CHAOS', 'Chaos_Orb'],
    ['GREATER_CHAOS', 'Greater_Chaos_Orb'],
    ['PERFECT_CHAOS', 'Perfect_Chaos_Orb'],
  ] as const)(
    'projects exact %s uses without merging adjacent manual records',
    async (action, currencyId) => {
      const applied = { ...(await craft()), action }
      const history = recordCraft(
        recordCraft(start(), root, applied, 'unused'),
        applied.state,
        applied,
        'unused',
      )
      const film = structuredClone(currentFilm(history)!)
      const legacy = JSON.stringify(film.frames[1])
      film.frames[2]!.chaosRepeat = {
        version: 1,
        action,
        uses: Number.MAX_SAFE_INTEGER,
      }
      film.frames[2]!.evidence!.consumedOmens = [
        'Omen_of_Whittling',
        'Omen_of_Whittling',
      ]
      const result = extractSessionNotation(film, 0)
      expect(result.steps).toHaveLength(2)
      expect(result.steps[0]).toMatchObject({
        currencyId,
        uses: 1,
        repeated: false,
      })
      expect(result.steps[1]).toMatchObject({
        currencyId,
        uses: Number.MAX_SAFE_INTEGER,
        repeated: true,
        consumedOmenIds: ['Omen_of_Whittling', 'Omen_of_Whittling'],
        omensKnown: true,
      })
      expect(result.omittedFrames).toBe(0)
      expect(JSON.stringify(film.frames[1])).toBe(legacy)
      film.frames[2]!.chaosRepeat.uses = 1
      expect(extractSessionNotation(film, 2).steps[1]).toMatchObject({
        uses: 1,
        repeated: true,
      })
    },
  )

  it('omits damaged repeat records rather than treating them as single uses', async () => {
    const applied = { ...(await craft()), action: 'CHAOS' as ChaosRepeatAction }
    const history = recordCraft(start(), root, applied, 'unused')
    for (const metadata of [
      undefined,
      null,
      { version: 1, action: 'CHAOS', uses: 0 },
      { version: 1, action: 'GREATER_CHAOS', uses: 2 },
      { version: 2, action: 'CHAOS', uses: 2 },
    ]) {
      const film = structuredClone(currentFilm(history)!)
      Object.assign(film.frames[1]!, { chaosRepeat: metadata })
      expect(extractSessionNotation(film, 1)).toMatchObject({
        steps: [],
        omittedFrames: 1,
      })
    }
    const film = structuredClone(currentFilm(history)!)
    film.frames[1]!.chaosRepeat = { version: 1, action: 'CHAOS', uses: 2 }
    delete film.frames[1]!.evidence
    expect(extractSessionNotation(film, 1).omittedFrames).toBe(1)
  })
})

describe('session notation from persisted successful applications', () => {
  it('retains the whole Film while excluding root and non-currency observations', async () => {
    const { history } = await twoSteps()
    const film = currentFilm(history)!
    const complete = [
      {
        number: 1,
        frameIndex: 1,
        currencyId: 'Orb_of_Transmutation',
        consumedOmenIds: [],
        omensKnown: true,
        uses: 1,
        repeated: false,
      },
      {
        number: 2,
        frameIndex: 2,
        currencyId: 'Orb_of_Augmentation',
        consumedOmenIds: [],
        omensKnown: true,
        uses: 1,
        repeated: false,
      },
    ]
    for (const cursor of [0, 1, 2])
      expect(extractSessionNotation(film, cursor).steps).toEqual(complete)
    for (const cursor of [-1, 0.5, Number.NaN, 3])
      expect(extractSessionNotation(film, cursor).steps).toEqual([])
    expect(extractSessionNotation(undefined, 0).steps).toEqual([])
    expect(extractSessionNotation(null, 0).steps).toEqual([])
    const imported = recordAbyss(
      start(),
      root,
      abyss,
      'IMPORT',
      'unused',
      identity,
    )
    const base = recordAbyss(
      imported,
      root,
      abyss,
      'BASE_SELECTION',
      'unused',
      identity,
    )
    expect(extractSessionNotation(currentFilm(base), base.cursor)).toEqual({
      steps: [],
      omittedFrames: 0,
      incompleteOmens: false,
    })
  })

  it('never turns rejected or cancelled attempts into successful rows', async () => {
    const before = start()
    const result = await craft()
    const failed = recordCraft(
      before,
      root,
      { ...result, applied: false },
      'failed',
    )
    expect(failed).toBe(before)
    expect(
      extractSessionNotation(currentFilm(failed), failed.cursor).steps,
    ).toEqual([])

    vi.stubGlobal(
      'fetch',
      (_input: unknown, init?: RequestInit) =>
        new Promise<Response>((_resolve, reject) => {
          init?.signal?.addEventListener(
            'abort',
            () => reject(new DOMException('Cancelled', 'AbortError')),
            { once: true },
          )
        }),
    )
    // An aborted application never reaches the writer's completion callback.
    const controller = new AbortController()
    let cancelled = before
    const pending = applyCurrency(
      root,
      'TRANSMUTATION',
      initialFixture.modifiers,
      controller.signal,
      [],
      identity,
    ).then((result) => {
      cancelled = recordCraft(before, root, result, 'cancelled')
    })
    controller.abort()
    await expect(pending).rejects.toMatchObject({ name: 'AbortError' })
    expect(cancelled).toBe(before)
    expect(
      extractSessionNotation(currentFilm(cancelled), cancelled.cursor).steps,
    ).toEqual([])
  })

  it('records consumed omen combinations on the same currency step, never remaining omens', async () => {
    const applied = await craft()
    const consumed = [
      'Omen_of_Greater_Exaltation',
      'Omen_of_Sinistral_Exaltation',
    ]
    const history = recordCraft(
      start(),
      root,
      {
        ...applied,
        action: 'EXALTED',
        consumedOmens: consumed,
        remainingOmens: ['Omen_of_Whittling'],
      },
      'unused',
    )
    expect(
      extractSessionNotation(currentFilm(history), history.cursor),
    ).toEqual({
      steps: [
        {
          number: 1,
          frameIndex: 1,
          currencyId: 'Exalted_Orb',
          consumedOmenIds: consumed,
          omensKnown: true,
          uses: 1,
          repeated: false,
        },
      ],
      omittedFrames: 0,
      incompleteOmens: false,
    })
  })

  it('resolves tiered, essence, quality and bone actions through the existing resolvers', () => {
    const actionIds = [
      ['GREATER_EXALTED', 'Greater_Exalted_Orb'],
      ['PERFECT_ESSENCE_MIND', 'Perfect_Essence_of_the_Mind'],
      ['ARMOURERS_SCRAP', 'Armourers_Scrap'],
      ['Preserved_Cranium', 'Preserved_Cranium'],
    ]
    for (const [action, id] of actionIds) {
      const history = recordAbyss(
        start(),
        root,
        abyss,
        action!,
        'unused',
        identity,
      )
      expect(
        extractSessionNotation(currentFilm(history), history.cursor).steps[0]
          ?.currencyId,
      ).toBe(id)
    }
  })

  it('omits reveal/options/selection observations without adding currency steps or unknown warnings', () => {
    let history = recordAbyss(
      start(),
      root,
      abyss,
      'Preserved_Cranium',
      'unused',
      identity,
      ['Omen_of_Sinistral_Necromancy'],
    )
    for (const operation of [
      'PREPARE',
      'OBSERVE',
      'OFFER',
      'REVEAL',
      'OPTIONS',
      'CHOOSE',
      'SELECT',
      'ADD',
      'RESTORE',
    ])
      history = recordAbyss(
        history,
        currentFrame(history)!.state,
        abyss,
        `ABYSS_${operation}`,
        'unused',
        identity,
      )
    const result = extractSessionNotation(currentFilm(history), history.cursor)
    expect(result.steps).toHaveLength(1)
    expect(result.steps[0]).toMatchObject({
      number: 1,
      frameIndex: 1,
      currencyId: 'Preserved_Cranium',
      consumedOmenIds: ['Omen_of_Sinistral_Necromancy'],
    })
    expect(result.omittedFrames).toBe(0)
    expect(result.incompleteOmens).toBe(false)
  })

  it('distinguishes missing legacy omen details from explicit empty consumption', async () => {
    const applied = await craft()
    const history = recordCraft(start(), root, applied, 'unused')
    const film = structuredClone(currentFilm(history)!)
    const legacy = structuredClone(film.frames[1]!)
    delete legacy.evidence
    film.frames.push(legacy)
    film.frames.push({
      state: root,
      action: 'ABYSS_APPLY',
      manual: { action: 'ABYSS_APPLY', rulesetIdentity: identity },
    })
    expect(extractSessionNotation(film, 3)).toMatchObject({
      omittedFrames: 2,
      incompleteOmens: false,
    })

    const manual = recordAbyss(
      start(),
      root,
      abyss,
      'Preserved_Cranium',
      'unused',
      identity,
    )
    const missing = structuredClone(currentFilm(manual)!)
    delete missing.frames[1]!.manual!.consumedOmens
    expect(extractSessionNotation(missing, 1)).toMatchObject({
      incompleteOmens: true,
      steps: [{ omensKnown: false, consumedOmenIds: [] }],
    })
    missing.frames[1]!.manual!.consumedOmens = []
    expect(extractSessionNotation(missing, 1)).toMatchObject({
      incompleteOmens: false,
      steps: [{ omensKnown: true, consumedOmenIds: [] }],
    })
  })

  it('does not infer consumption from contradictory receipts, unknown IDs or malformed legacy arrays', async () => {
    const applied = await craft()
    const history = recordCraft(start(), root, applied, 'unused')
    const film = structuredClone(currentFilm(history)!)
    film.frames[1]!.evidence!.action = 'EXALTED'
    expect(extractSessionNotation(film, 1)).toMatchObject({
      steps: [],
      omittedFrames: 1,
    })
    film.frames[1]!.evidence!.action = 'TRANSMUTATION'
    film.frames[1]!.manual = {
      action: 'TRANSMUTATION',
      rulesetIdentity: identity,
    }
    expect(extractSessionNotation(film, 1)).toMatchObject({
      steps: [],
      omittedFrames: 1,
    })
    delete film.frames[1]!.manual
    film.frames[1]!.evidence!.consumedOmens = ['unknown-omen']
    expect(extractSessionNotation(film, 1)).toMatchObject({
      incompleteOmens: true,
      steps: [{ consumedOmenIds: ['unknown-omen'], omensKnown: false }],
    })
    for (const consumed of [
      null,
      [3],
      ['Omen_of_Whittling', 'Omen_of_Whittling'],
    ]) {
      const malformed = JSON.parse(JSON.stringify(film)) as Film
      Object.assign(malformed.frames[1]!.evidence!, { consumedOmens: consumed })
      expect(extractSessionNotation(malformed, 1)).toMatchObject({
        incompleteOmens: true,
        steps: [{ consumedOmenIds: [], omensKnown: false }],
      })
    }
  })

  it('retains the selected prefix and numbers the new branch after its inherited history', async () => {
    const { history, second } = await twoSteps()
    const browsed = viewFrame(history, 1)
    const branched = recordCraft(
      browsed,
      currentFrame(browsed)!.state,
      second,
      'branch',
    )
    expect(currentFilm(branched)?.id).toBe('branch')
    expect(
      extractSessionNotation(currentFilm(branched), branched.cursor).steps,
    ).toMatchObject([
      { number: 1, currencyId: 'Orb_of_Transmutation', frameIndex: 1 },
      { number: 2, currencyId: 'Orb_of_Augmentation', frameIndex: 2 },
    ])
    expect(
      extractSessionNotation(currentFilm(history), history.cursor).steps,
    ).toHaveLength(2)
    const repository = new LocalFilmRepository(localStorage)
    repository.save(browsed)
    const restored = repository.load()
    expect(
      extractSessionNotation(currentFilm(restored), restored.cursor),
    ).toEqual(extractSessionNotation(currentFilm(browsed), 1))
    expect(
      extractSessionNotation(currentFilm(viewFrame(restored, 2)), 2).steps,
    ).toHaveLength(2)
    expect(restored.films[0]!.sourceDocument).toBe('original\r\ntext')
  })

  it('does not mutate films, history, state, evidence or consumed omen arrays', async () => {
    const { history } = await twoSteps()
    history.films[0]!.frames[1]!.evidence!.consumedOmens = ['Omen_of_Whittling']
    const original = JSON.stringify(history)
    freeze(history)
    const projected = extractSessionNotation(
      currentFilm(history),
      history.cursor,
    )
    projected.steps[0]!.consumedOmenIds.push('output-only')
    projected.steps.reverse()
    expect(JSON.stringify(history)).toBe(original)
    expect(
      extractSessionNotation(currentFilm(history), history.cursor).steps[0]!
        .consumedOmenIds,
    ).toEqual(['Omen_of_Whittling'])
  })
})

it.each(['DESECRATION_REVEAL', 'ABYSS_SELECT', 'ABYSS_CHOOSE'])(
  'projects a completed %s as its own action with no extra currency consumption',
  (action) => {
    const hidden: AbyssState = {
      item: root,
      slots: [
        { id: 'hidden', side: 'PREFIX', modifier: null, desecrated: true },
      ],
      offer: null,
    }
    const bone = recordAbyss(
      start(),
      root,
      hidden,
      'Preserved_Cranium',
      'unused',
      identity,
    )
    const next: AbyssState = {
      ...hidden,
      slots: [
        {
          ...hidden.slots[0]!,
          modifier: { modifierId: 'p', values: { life: 10 } },
        },
      ],
    }
    const revealed = recordAbyss(
      bone,
      currentFrame(bone)!.state,
      next,
      action,
      'unused',
      identity,
    )
    const original = JSON.stringify(revealed)
    const notation = extractSessionNotation(
      currentFilm(revealed),
      revealed.cursor,
    )
    expect(notation.steps).toHaveLength(2)
    expect(notation.steps[1]).toMatchObject({
      kind: 'reveal',
      currencyId: 'DESECRATION_REVEAL',
      frameIndex: 2,
      consumedOmenIds: [],
      uses: 1,
    })
    expect(notation.omittedFrames).toBe(0)
    expect(JSON.stringify(revealed)).toBe(original)
    expect(currentFilm(revealed)!.frames).toHaveLength(3)
  },
)
