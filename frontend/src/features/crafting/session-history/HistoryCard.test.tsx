import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, expect, it } from 'vitest'
import { initialFixture } from '../../../shared/test/craftingFixtures'
import { setLocale, type Locale } from '../../../shared/i18n/i18n'
import { modifierPickerMessages } from '../goal-filter/modifierPickerMessages'
import { itemCardMessages } from '../itemCardMessages'
import type { Definition } from '../craftingApi'
import { concreteInitial } from '../workbenchApi'
import { HistoryCard } from './HistoryCard'
import { qualityWorkMessages } from '../method-search/qualityWorkMessages'
import type { Film } from '../workbenchHistory'
import { requestFixture } from '../path-tree/fixtures.test-support'
import { acquisitionRouteFilm } from '../method-search/acquisitionRouteFilm'
afterEach(() => cleanup())
it('keeps known option sentences readable across ruleset changes without editing the saved item', async () => {
  setLocale('en')
  const state = {
    ...concreteInitial(initialFixture),
    baseItemId: 'Metadata/Items/Amulets/FourAmulet9',
    rarity: 'RARE' as const,
    explicits: [{ modifierId: 'p', values: { life: 17 } }],
  }
  const original = JSON.stringify(state)
  const client = new QueryClient({
    defaultOptions: { queries: { staleTime: Infinity, retry: false } },
  })
  client.setQueryData(
    ['crafting', 'initial', 'solar', state.itemLevel, 'manual'],
    initialFixture,
  )
  const frame = { state, action: null }
  render(
    <QueryClientProvider client={client}>
      <HistoryCard
        film={{
          id: 'old',
          createdAt: new Date().toISOString(),
          rulesetIdentity: 'old-ruleset',
          frames: [frame],
        }}
        frame={frame}
      />
    </QueryClientProvider>,
  )
  expect(await screen.findByText('+17 to maximum Life')).toBeInTheDocument()
  expect(screen.queryByText('p: life 17')).not.toBeInTheDocument()
  expect(JSON.stringify(state)).toBe(original)
})

it('uses the translated catalyst quality in a preserved session instead of the raw enum', async () => {
  await setLocale('ko')
  const state = {
    ...concreteInitial(initialFixture),
    baseItemId: 'Metadata/Items/Amulets/FourAmulet9',
    catalystQuality: { type: 'NECROTIC' as const, amount: 40 },
  }
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  client.setQueryData(
    ['crafting', 'initial', 'solar', state.itemLevel, 'manual'],
    initialFixture,
  )
  const frame = { state, action: null }
  render(
    <QueryClientProvider client={client}>
      <HistoryCard
        film={{
          id: 'quality',
          createdAt: new Date().toISOString(),
          rulesetIdentity: 'old-ruleset',
          frames: [frame],
        }}
        frame={frame}
      />
    </QueryClientProvider>,
  )
  expect(await screen.findByText('퀄리티 (괴사의 기폭제): +40%')).toBeVisible()
  expect(screen.queryByText(/NECROTIC/)).toBeNull()
  await setLocale('en')
})

it('shows the saved slot side before and after a desecration reveal alongside a fractured prefix', () => {
  setLocale('ko')
  const baseItemId = 'Metadata/Items/Amulets/FourAmulet9'
  const slot = { id: 'd', side: 'SUFFIX' as const, modifierId: null }
  const hidden = {
    ...concreteInitial(initialFixture),
    baseItemId,
    rarity: 'RARE' as const,
    explicits: [{ modifierId: 'p', values: { life: 17 }, fractured: true }],
    desecrated: [slot],
  }
  const revealed = {
    ...hidden,
    explicits: [
      ...hidden.explicits,
      { modifierId: 's', values: { strength: 6 } },
    ],
    desecrated: [{ ...slot, modifierId: 's' }],
  }
  const rulesetIdentity = initialFixture.rulesetIdentity
  const frames = [
    { action: null, state: hidden, rulesetIdentity },
    { action: 'DESECRATION_REVEAL', state: revealed, rulesetIdentity },
  ]
  const film = { id: 'reveal', createdAt: '', rulesetIdentity, frames }
  const client = new QueryClient()
  client.setQueryData(['crafting', 'initial', 'solar', 82, 'manual'], {
    ...initialFixture,
    state: { ...initialFixture.state, baseItemId },
  })
  const view = (index: number) => (
    <QueryClientProvider client={client}>
      <HistoryCard film={film} frame={frames[index]!} />
    </QueryClientProvider>
  )
  const { container, rerender } = render(view(0))
  const desecrated = () =>
    container.querySelector('.item-card__line--desecrated')!
  expect(desecrated()).toHaveTextContent('접미어')
  expect(desecrated()).not.toHaveTextContent(/T\d+/)
  expect(desecrated().querySelector('[role="img"]')).toBeVisible()
  rerender(view(1))
  expect(desecrated()).toHaveTextContent('S T1')
  expect(desecrated().textContent).toContain('6')
  expect(desecrated().querySelector('.abyss-unrevealed-rune')).toBeNull()
  expect(
    container.querySelector('[data-fractured] .item-card__affix'),
  ).toHaveTextContent('P T1')
  expect(
    container.querySelector('.item-card__line--implicit .item-card__affix'),
  ).toBeNull()
  setLocale('en')
})

it('restores the workbench affected-option mark at each saved step and clears it at step zero', async () => {
  const { twoSteps } = await import('../session-notation/fixtures.test-support')
  const { history } = await twoSteps(),
    film = history.films[0]!
  const initial = structuredClone(initialFixture)
  initial.state.baseItemId = 'Metadata/Items/Amulets/FourAmulet9'
  for (const frame of film.frames)
    frame.state.baseItemId = initial.state.baseItemId
  const saved = JSON.stringify(film)
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false, staleTime: Infinity } },
  })
  client.setQueryData(['crafting', 'initial', 'solar', 82, 'manual'], initial)
  const view = (index: number) => (
    <QueryClientProvider client={client}>
      <HistoryCard film={film} frame={film.frames[index]!} />
    </QueryClientProvider>
  )
  const rendered = render(view(1))
  expect(await screen.findByText('+17 to maximum Life')).toHaveAttribute(
    'data-last-currency-affected',
  )
  rendered.rerender(view(2))
  expect(await screen.findByText('+6 to Strength')).toHaveAttribute(
    'data-last-currency-affected',
  )
  expect(screen.getByText('+17 to maximum Life')).not.toHaveAttribute(
    'data-last-currency-affected',
  )
  rendered.rerender(view(0))
  expect(
    rendered.container.querySelector('[data-last-currency-affected]'),
  ).toBeNull()
  expect(JSON.stringify(film)).toBe(saved)
})

const countess: Definition = {
  id: 'amulet:prefix:countess',
  name: 'Countess',
  text: '+(47—50) to Spirit',
  tier: 1,
  affixType: 'PREFIX',
  familyIds: ['BaseSpirit'],
  stats: [{ id: 'base_spirit_from_equipment', min: 47, max: 50 }],
}

const spiritSentences: Record<Locale, string> = {
  en: '+47 to Spirit',
  ko: '정신력 +47',
  'zh-CN': '+47 精魂',
  'zh-TW': '+47精魂',
  ja: 'スピリット +47',
  es: '+47 al espíritu',
  ru: '+47 к духу',
  th: 'พลังวิญญาณ +47',
}

it.each(Object.entries(spiritSentences))(
  'shows the acquired roll and eligible tier range before Divine, and preserves actual rolls after transfer in %s',
  async (language, acquiredText) => {
    const locale = language as Locale
    await setLocale(locale)
    const baseItemId = 'Metadata/Items/Amulets/FourAmulet9'
    const held = { modifierId: 's', values: { strength: 7 }, fractured: true }
    const start = {
      ...concreteInitial(initialFixture),
      baseItemId,
      rarity: 'RARE' as const,
      explicits: [held],
    }
    const dummy = {
      ...start,
      explicits: [held, { modifierId: 'p', values: { life: 12 } }],
    }
    const spiritState = (value: number) => ({
      ...start,
      explicits: [
        held,
        {
          modifierId: countess.id,
          values: { base_spirit_from_equipment: value },
        },
      ],
    })
    const hit = spiritState(47)
    const divined = spiritState(49)
    const request = requestFixture()
    request.start.item = {
      ...request.start.item,
      baseItemId,
      snapshotId: start.snapshotId,
      rarity: start.rarity,
      implicits: start.implicits.map((mod) => ({ ...mod, fractured: false })),
      explicits: [held],
    }
    request.start.provenance.rulesetIdentity = initialFixture.rulesetIdentity
    const film = acquisitionRouteFilm(
      request,
      {
        cards: [
          {
            options: [
              {
                number: 1,
                modifierIds: [countess.id],
                hasLowerBound: true,
                hasUpperBound: true,
              },
            ],
          },
        ],
        states: [dummy, hit, divined],
        complete: true,
        routes: [
          {
            cardIndex: 0,
            stateIndices: [0, 1, 2],
            order: [1],
            probability: 1,
            steps: [
              {
                action: 'EXALTED',
                dummy: true,
                omens: [],
                acquired: [],
                lost: [],
                chance: { probability: 1 },
              },
              {
                action: 'CHAOS',
                omens: [],
                acquired: [1],
                lost: [],
                repeatUntilHit: true,
                expectedUses: 3,
                chance: { probability: 1 },
              },
              {
                action: 'DIVINE',
                omens: [],
                acquired: [],
                lost: [],
                maxUses: 1000,
                expectedUses: 2,
              },
            ],
          },
        ],
      },
      0,
      locale,
    )!
    const client = new QueryClient()
    client.setQueryData(['crafting', 'initial', 'solar', 82, 'manual'], {
      ...initialFixture,
      state: { ...initialFixture.state, baseItemId },
      modifiers: { ...initialFixture.modifiers, [countess.id]: countess },
    })
    const view = (f: typeof film, index: number) => (
      <QueryClientProvider client={client}>
        <HistoryCard film={f} frame={f.frames[index]!} />
      </QueryClientProvider>
    )
    const rendered = render(view(film, 1))
    expect(
      await screen.findByText(itemCardMessages[locale].replaceable),
    ).toBeVisible()
    expect(
      screen.getByTitle(modifierPickerMessages[locale].prefix),
    ).not.toHaveTextContent(/T\d+/)
    expect(screen.getByText('+7 to Strength')).toBeVisible()
    rendered.rerender(view(film, 2))
    expect(await screen.findByText(acquiredText)).toBeVisible()
    const range = rendered.container.querySelector('.item-card__tier-range')!
    expect(range).toBeVisible()
    expect(range).toHaveTextContent(itemCardMessages[locale].tierRange)
    expect(range).toHaveTextContent('+47–50')
    expect(film.frames[2]!.state).toEqual(hit)
    rendered.rerender(view(film, 3))
    expect(
      await screen.findByText(acquiredText.replace('47', '49')),
    ).toBeVisible()
    expect(
      rendered.container.querySelector('.item-card__tier-range'),
    ).toBeNull()
    const stored = structuredClone(film)
    rendered.rerender(view(stored, 2))
    expect(await screen.findByText(acquiredText)).toBeVisible()
    expect(
      rendered.container.querySelector('.item-card__tier-range'),
    ).toBeNull()
    expect(stored.frames[2]!.state).toEqual(hit)
    expect(stored.frames[3]!.state).toEqual(divined)
  },
)

it('restores untyped quality after the checkpoint until a later manual catalyst records its type', async () => {
  await setLocale('ko')
  const state = {
    ...concreteInitial(initialFixture),
    baseItemId: 'Metadata/Items/Amulets/FourAmulet9',
    catalystQuality: { type: 'NECROTIC' as const, amount: 20 },
  }
  const rulesetIdentity = initialFixture.rulesetIdentity
  const film: Film = {
    id: 'continued',
    createdAt: '',
    rulesetIdentity,
    frames: [
      { action: null, state, rulesetIdentity },
      {
        action: 'SESSION_PLAN',
        state,
        rulesetIdentity,
        projectedCurrencies: [],
        projectedWork: 'UNSPECIFIED_CATALYST_40',
      },
      {
        action: 'SESSION_PLAN',
        state,
        rulesetIdentity,
        projectedCurrencies: [{ currencyId: 'Exalted_Orb', omenIds: [] }],
      },
      {
        action: 'CATALYST_NECROTIC',
        state: { ...state, catalystQuality: { type: 'NECROTIC', amount: 21 } },
        rulesetIdentity,
      },
    ],
  }
  const client = new QueryClient()
  client.setQueryData(['crafting', 'initial', 'solar', 82, 'manual'], {
    ...initialFixture,
    state: { ...initialFixture.state, baseItemId: state.baseItemId },
  })
  const view = (index: number, record = film) => (
    <QueryClientProvider client={client}>
      <HistoryCard film={record} frame={record.frames[index]!} />
    </QueryClientProvider>
  )
  const { rerender } = render(view(0))
  expect(screen.getByText('퀄리티 (괴사의 기폭제): +20%')).toBeVisible()
  for (const index of [1, 2]) {
    rerender(view(index))
    expect(screen.getByText(qualityWorkMessages.ko.completed)).toBeVisible()
    expect(screen.queryByText('퀄리티 (괴사의 기폭제): +20%')).toBeNull()
  }
  rerender(view(3))
  expect(screen.getByText('퀄리티 (괴사의 기폭제): +21%')).toBeVisible()
  expect(screen.queryByText(qualityWorkMessages.ko.completed)).toBeNull()
  expect(film.frames[2]!.state.catalystQuality?.amount).toBe(20)
  const legacy = structuredClone(film)
  legacy.frames[1]!.projectedWork = 'QUALITY_40'
  for (const index of [1, 2]) {
    rerender(view(index, legacy))
    expect(screen.getByText('퀄리티 (괴사의 기폭제): +20%')).toBeVisible()
    expect(screen.queryByText(qualityWorkMessages.ko.completed)).toBeNull()
  }
  await setLocale('en')
})
