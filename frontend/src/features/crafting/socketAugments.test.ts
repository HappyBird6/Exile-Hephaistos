import { afterEach, expect, it, vi } from 'vitest'
import {
  augments,
  augmentEffect,
  augmentShape,
  installAugment,
} from './socketAugments'
import {
  initialFixture,
  jsonResponse,
} from '../../shared/test/craftingFixtures'
import { concreteInitial } from './workbenchApi'
import { supportsConcreteStateShape } from './workbenchStateShape'

afterEach(() => vi.unstubAllGlobals())
const initial = {
  ...initialFixture,
  state: {
    ...initialFixture.state,
    baseItemId: 'Metadata/Items/Armours/Gloves/FourGlovesStr1',
    implicits: [],
  },
}
const state = { ...concreteInitial(initial), augmentSockets: 2 }
it('registers all ordinary tiers and Soul Cores with sourced effects and six names', () => {
  expect(augments).toHaveLength(79)
  expect(augments.filter((a) => a.id.startsWith('Soul_Core_of_'))).toHaveLength(
    16,
  )
  for (const a of augments) {
    for (const locale of ['en', 'ko', 'ja', 'es', 'zh-CN', 'zh-TW'])
      expect(a.names[locale]).toBeTruthy()
    expect(Object.keys(a.effects).length).toBeGreaterThan(0)
  }
  expect(augmentEffect('Desert_Rune', state.baseItemId)).toContain('14')
  expect(augmentEffect('Perfect_Desert_Rune', state.baseItemId)).toContain('22')
})
it('rejects malformed, duplicate, unknown and excess installed slots without erasing legacy unknown counts', () => {
  expect(
    augmentShape({
      ...state,
      installedAugments: [{ slotIndex: 0, augmentId: 'Desert_Rune' }],
    }),
  ).toBe(true)
  for (const entries of [
    [{ slotIndex: 2, augmentId: 'Desert_Rune' }],
    [{ slotIndex: 0, augmentId: 'Unknown' }],
    [
      { slotIndex: 0, augmentId: 'Desert_Rune' },
      { slotIndex: 0, augmentId: 'Storm_Rune' },
    ],
  ])
    expect(
      supportsConcreteStateShape({ ...state, installedAugments: entries }),
    ).toBe(false)
  expect(
    supportsConcreteStateShape({ ...state, augmentSockets: undefined }),
  ).toBe(true)
})
it('accepts only the requested slot transition and rejects a forged unrelated change', async () => {
  const response = {
    rulesetIdentity: initial.rulesetIdentity,
    applied: true,
    action: 'INSTALL_AUGMENT',
    state: {
      ...state,
      desecrated: [],
      installedAugments: [{ slotIndex: 1, augmentId: 'Desert_Rune' }],
    },
    events: [],
    assumptions: [],
    consumedOmens: [],
  }
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(response)))
  expect(
    (
      await installAugment(
        state,
        1,
        'Desert_Rune',
        initial.rulesetIdentity,
        new AbortController().signal,
      )
    ).state.installedAugments,
  ).toHaveLength(1)
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue(
      jsonResponse({
        ...response,
        state: { ...response.state, rarity: 'RARE' },
      }),
    ),
  )
  await expect(
    installAugment(
      state,
      1,
      'Desert_Rune',
      initial.rulesetIdentity,
      new AbortController().signal,
    ),
  ).rejects.toThrow()
})
