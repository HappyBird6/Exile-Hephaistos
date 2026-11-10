import { afterEach, expect, it, vi } from 'vitest'
import {
  augments,
  augmentEffect,
  augmentShape,
  installAugment,
  selectableAugments,
  isSelectableAugment,
  canInstallAugment,
  unidentifiedRuneEffects,
} from './socketAugments'
import {
  initialFixture,
  jsonResponse,
} from '../../shared/test/craftingFixtures'
import { concreteInitial } from './workbenchApi'
import { setLocale } from '../../shared/i18n/i18n'
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
it('enforces ordinary limited Idols and shared Ancient limits for the actual replacement slot', async () => {
  const limited = {
    ...state,
    installedAugments: [{ slotIndex: 0, augmentId: 'Idol_of_Sirrius' }],
  }
  expect(canInstallAugment(limited, 'Idol_of_Sirrius', 0)).toBe(true)
  expect(canInstallAugment(limited, 'Idol_of_Sirrius')).toBe(true)
  expect(canInstallAugment(limited)).toBe(false)
  expect(canInstallAugment(limited, 'Idol_of_Sirrius', 1)).toBe(false)
  const eye = {
    ...state,
    installedAugments: [{ slotIndex: 0, augmentId: 'Tecrods_Gaze' }],
  }
  expect(canInstallAugment(eye, 'Kurgals_Gaze', 0)).toBe(true)
  expect(canInstallAugment(eye, 'Kurgals_Gaze', 1)).toBe(false)
  expect(
    augmentShape({
      ...eye,
      installedAugments: [
        ...eye.installedAugments,
        { slotIndex: 1, augmentId: 'Kurgals_Gaze' },
      ],
    }),
  ).toBe(false)
  expect(
    augmentShape({
      ...eye,
      installedAugments: [...eye.installedAugments, null],
    }),
  ).toBe(false)
  const fetch = vi.fn()
  vi.stubGlobal('fetch', fetch)
  await expect(
    installAugment(
      eye,
      1,
      'Kurgals_Gaze',
      initial.rulesetIdentity,
      new AbortController().signal,
    ),
  ).rejects.toThrow()
  expect(fetch).not.toHaveBeenCalled()
  const unlimited = {
    ...state,
    installedAugments: [{ slotIndex: 0, augmentId: 'Snake_Idol' }],
  }
  expect(canInstallAugment(unlimited, 'Snake_Idol', 1)).toBe(true)
  const old = {
    ...state,
    baseItemId: 'Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand2',
    installedAugments: [0, 1].map((slotIndex) => ({
      slotIndex,
      augmentId: 'Soul_Core_of_Vizoma',
    })),
  }
  expect(augmentShape(old)).toBe(true)
  expect(canInstallAugment(old, 'Soul_Core_of_Vizoma', 0)).toBe(false)
})
it('registers all ordinary tiers and Soul Cores with sourced effects and six names', () => {
  expect(augments).toHaveLength(277)
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
it('preserves all original limited Soul Core states and enforces their limits on new installations', () => {
  const bow = {
    ...state,
    baseItemId: 'Metadata/Items/Weapons/TwoHandWeapons/Bows/FourBow8Endgame',
  }
  const legacyLimited = augments.filter((entry) => entry.legacyLimitExempt)
  expect(legacyLimited).toHaveLength(9)
  for (const entry of legacyLimited) {
    const installedAugments = [0, 1].map((slotIndex) => ({
      slotIndex,
      augmentId: entry.id,
    }))
    expect(augmentShape({ ...bow, installedAugments })).toBe(true)
    expect(
      canInstallAugment(
        { ...bow, installedAugments: installedAugments.slice(0, 1) },
        entry.id,
        1,
      ),
    ).toBe(false)
    expect(
      canInstallAugment(
        { ...bow, installedAugments: installedAugments.slice(0, 1) },
        entry.id,
        0,
      ),
    ).toBe(true)
  }
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
  expect(selectableAugments).toHaveLength(232)
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

it('shows an unidentified Rune effect without choosing an identity or allowing blind replacement', () => {
  setLocale('ko')
  const observed = {
    ...state,
    baseItemId: 'Metadata/Items/Weapons/TwoHandWeapons/Bows/FourBow1',
    observations: {
      socketCount: null,
      augmentEffects: ['+150 to Accuracy Rating (rune)'],
      grantedSkills: [],
    },
  }
  const before = JSON.stringify(observed)
  try {
    expect(unidentifiedRuneEffects(observed)).toEqual([
      {
        id: 'observed-augment-0',
        text: '미확인 룬: 정확도 +150',
        kind: 'rune',
      },
    ])
    expect(canInstallAugment(observed, 'Perfect_Vision_Rune', 0)).toBe(false)
    expect(JSON.stringify(observed)).toBe(before)
  } finally {
    setLocale('en')
  }
})
