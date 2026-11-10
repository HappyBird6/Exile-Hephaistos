import { afterEach, expect, it, vi } from 'vitest'
import {
  initialFixture,
  jsonResponse,
} from '../../../shared/test/craftingFixtures'
import { concreteInitial } from '../workbenchApi'
import { applyCustomExalt } from './api'
import type { CustomExaltContext } from './api'
afterEach(() => vi.unstubAllGlobals())
it('accepts omitted nullable properties and false flags but refuses changes to existing item properties', async () => {
  const context: CustomExaltContext = {
    state: { ...concreteInitial(initialFixture), rarity: 'RARE' },
    action: 'EXALTED',
    activeOmens: [],
  }
  const makeResult = () => ({
    result: {
      state: {
        ...context.state,
        installedAugments: [],
        equipmentQuality: null,
        catalystQuality: null,
        implicits: context.state.implicits.map((m) => ({
          ...m,
          fractured: false,
        })),
        explicits: [
          { modifierId: 'p', values: { life: 15 }, fractured: false },
        ],
      },
      action: 'EXALTED',
      ruleVersion: 'test',
      ledgerVersion: 'test',
      snapshotId: context.state.snapshotId,
      applied: true,
      reason: '',
      events: [],
      assumptions: [],
      consumedOmens: [],
      remainingOmens: [],
    },
    choice: { modifierId: 'p', probability: 0.1 },
    abyss: null,
  })
  let response = makeResult()
  vi.stubGlobal('fetch', () => Promise.resolve(jsonResponse(response)))
  expect(
    (
      await applyCustomExalt(
        context,
        initialFixture,
        new AbortController().signal,
        'p',
      )
    ).choice.probability,
  ).toBe(0.1)
  response = makeResult()
  response.result.state.itemLevel = 1
  await expect(
    applyCustomExalt(
      context,
      initialFixture,
      new AbortController().signal,
      'p',
    ),
  ).rejects.toThrow('Invalid custom Exalted result')
  response = makeResult()
  response.result.state.implicits[0]!.fractured = true
  await expect(
    applyCustomExalt(
      context,
      initialFixture,
      new AbortController().signal,
      'p',
    ),
  ).rejects.toThrow('Invalid custom Exalted result')
})

it.each([
  { activeOmens: [] },
  { activeOmens: ['Omen_of_Sinistral_Necromancy'] },
])(
  'accepts reordered revealed slots and preserves $activeOmens',
  async ({ activeOmens }) => {
    const suffix = { modifierId: 's', values: { strength: 6 } }
    const prefix = { modifierId: 'p', values: { life: 15 } }
    const state = {
      ...concreteInitial(initialFixture),
      rarity: 'RARE' as const,
      explicits: [suffix],
      desecrated: [
        { id: 'revealed', side: 'SUFFIX' as const, modifierId: 's' },
      ],
    }
    const source = {
      item: { ...state, explicits: [], desecrated: [] },
      slots: [
        {
          id: 'revealed',
          side: 'SUFFIX' as const,
          modifier: suffix,
          desecrated: true,
        },
      ],
      offer: null,
    }
    vi.stubGlobal('fetch', () =>
      Promise.resolve(
        jsonResponse({
          result: {
            state: { ...state, explicits: [prefix, suffix] },
            action: 'EXALTED',
            ruleVersion: 'test',
            ledgerVersion: 'test',
            snapshotId: state.snapshotId,
            applied: true,
            reason: '',
            events: [],
            assumptions: [],
            consumedOmens: [],
            remainingOmens: [],
          },
          choice: { modifierId: 'p', probability: 0.1 },
          abyss: {
            ...source,
            slots: [
              ...source.slots,
              {
                id: 'added',
                side: 'PREFIX',
                modifier: prefix,
                desecrated: false,
              },
            ],
          },
        }),
      ),
    )
    const result = await applyCustomExalt(
      { state, abyss: source, action: 'EXALTED', activeOmens },
      initialFixture,
      new AbortController().signal,
      'p',
    )
    expect(result.result.remainingOmens).toEqual(activeOmens)
    expect(result.result.state.explicits).toHaveLength(2)
    expect(result.abyss?.slots[0]).toEqual(source.slots[0])
  },
)
