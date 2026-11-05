import { afterEach, expect, it, vi } from 'vitest'
import {
  augments,
  augmentEffect,
  augmentShape,
  installAugment,
  selectableAugments,
  isSelectableAugment,
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
it('limits reviewed four-tier families to Perfect while retaining Tempered, Soul Cores and old installed tiers', async () => {
  expect(selectableAugments).toHaveLength(34)
  expect(
    selectableAugments.filter((a) => a.id.startsWith('Perfect_')),
  ).toHaveLength(15)
  for (const id of [
    'Lesser_Desert_Rune',
    'Desert_Rune',
    'Greater_Desert_Rune',
  ]) {
    expect(isSelectableAugment(id)).toBe(false)
    expect(
      augmentShape({
        ...state,
        installedAugments: [{ slotIndex: 0, augmentId: id }],
      }),
    ).toBe(true)
    const fetch = vi.fn()
    vi.stubGlobal('fetch', fetch)
    await expect(
      installAugment(
        state,
        0,
        id,
        initial.rulesetIdentity,
        new AbortController().signal,
      ),
    ).rejects.toThrow()
    expect(fetch).not.toHaveBeenCalled()
  }
  for (const id of [
    'Lesser_Tempered_Rune',
    'Tempered_Rune',
    'Greater_Tempered_Rune',
    'Perfect_Desert_Rune',
  ])
    expect(isSelectableAugment(id)).toBe(true)
  expect(
    selectableAugments.filter((a) => a.id.startsWith('Soul_Core_of_')),
  ).toHaveLength(16)
  expect(isSelectableAugment('Perfect_Tempered_Rune')).toBe(false)
})
it('accepts only the requested slot transition and rejects a forged unrelated change', async () => {
  const response = {
    rulesetIdentity: initial.rulesetIdentity,
    applied: true,
    action: 'INSTALL_AUGMENT',
    state: {
      ...state,
      desecrated: [],
      installedAugments: [{ slotIndex: 1, augmentId: 'Perfect_Desert_Rune' }],
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
        'Perfect_Desert_Rune',
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
      'Perfect_Desert_Rune',
      initial.rulesetIdentity,
      new AbortController().signal,
    ),
  ).rejects.toThrow()
})
