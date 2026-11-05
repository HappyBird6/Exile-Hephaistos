import { readFileSync } from 'node:fs'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { CraftingPage } from '../CraftingPage'
import { useItemDraft } from '../draft'
import { baseStartingRarity, selectableBaseKeys } from '../baseRegistry'
import { topBase } from '../topBases'
import { ItemCard } from '../ItemCard'
import { basePreview, workbenchBaseChoices } from './baseChoices'
import { concreteInitial } from '../workbenchApi'
import type { Definition, Initial } from '../craftingApi'
import {
  emptyFilms,
  historyStorageKey,
  LocalFilmRepository,
  startFilm,
} from '../workbenchHistory'
import { locales, setLocale } from '../../../shared/i18n/i18n'

it('supports confirmed rings with sourced affix capacities', () => {
  for (const [key, prefixes, suffixes] of [
    ['biostatic-ring', 3, 3],
    ['oneiric-ring', 3, 3],
    ['dusk-ring', 4, 2],
    ['gloam-ring', 2, 4],
    ['penumbra-ring', 5, 1],
    ['tenebrous-ring', 1, 5],
  ] as const) {
    expect(selectableBaseKeys).toContain(key)
    const base = topBase(key)!
    expect(base.family).toBe('rings')
    expect(base.affixLimits).toMatchObject({
      rarePrefixes: prefixes,
      rareSuffixes: suffixes,
    })
    expect(base.implicitStats?.length).toBeGreaterThan(0)
  }
})
it('shows amulet previews without internal policy notes or temporary skill examples in every locale', () => {
  for (const locale of locales)
    for (const choice of workbenchBaseChoices(locale).filter(
      (choice) => choice.itemClass === 'Amulets',
    )) {
      const base = topBase(choice.key)
      const original = JSON.stringify(base)
      const card = basePreview(choice, 82, locale)
      expect(
        card.properties.some((line) => line.id === 'temporary-passive-policy'),
      ).toBe(false)
      if (
        base?.temporaryBaseOption?.kind === 'SOURCE_FIRST_GRANTED_SKILL_EXAMPLE'
      ) {
        expect(card.properties.map((line) => line.text)).toEqual([])
        const view = render(<ItemCard item={card} />)
        for (const skill of base.skillLines![locale])
          expect(screen.getByRole('article')).not.toHaveTextContent(skill)
        expect(screen.getByRole('article')).not.toHaveTextContent(
          /user policy|사용자 정책|ユーザー方針|用户策略|使用者方針|política del usuario/i,
        )
        view.unmount()
      }
      expect(JSON.stringify(base)).toBe(original)
    }
})
it.each(
  (['lament-amulet', 'portent-amulet', 'absent-amulet'] as const).flatMap(
    (key) => locales.map((locale) => [key, locale] as const),
  ),
)(
  'hides temporary skill examples for restored %s in %s without changing saved data',
  (key, locale) => {
    localStorage.clear()
    setLocale(locale)
    useItemDraft.getState().setBase(82, key)
    const base = topBase(key)!
    const catalog = JSON.parse(
      readFileSync(
        `../backend/src/main/resources/catalog/${base.pool}/catalog.json`,
        'utf8',
      ),
    )
    const initial: Initial = {
      ruleVersion: 'fixture',
      rulesetIdentity: 'fixture-ruleset',
      metadata: catalog.metadata,
      id: 'fixture',
      actions: [],
      baseRules: catalog.base,
      state: {
        snapshotId: catalog.metadata.snapshotId,
        baseItemId: base.id,
        itemLevel: 82,
        rarity: baseStartingRarity(key),
        conditions: [],
        modifierIds: [],
        implicits: [
          {
            modifierId: base.implicitModifierId!,
            values: Object.fromEntries(
              base.implicitStats!.map((stat) => [stat.id, stat.max]),
            ),
          },
        ],
      },
      modifiers: Object.fromEntries(
        catalog.modifiers.map((definition: Definition) => [
          definition.id,
          definition,
        ]),
      ),
    }
    const state = concreteInitial(initial)
    const original = JSON.stringify(state)
    const history = startFilm(
      emptyFilms(),
      state,
      'amulet-display',
      initial.rulesetIdentity,
    )
    new LocalFilmRepository(localStorage).save(history)
    const stored = localStorage.getItem(historyStorageKey)
    vi.stubGlobal('fetch', () => new Promise(() => {}))
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })
    client.setQueryData(['crafting', 'initial', key, 82, 'manual'], initial)
    const view = render(
      <QueryClientProvider client={client}>
        <CraftingPage />
      </QueryClientProvider>,
    )
    const article = screen.getByRole('article')
    expect(article).not.toHaveTextContent('임시')
    expect(article).not.toHaveTextContent('사용자 정책')
    expect(article).not.toHaveTextContent('Heavy Ammunition')
    for (const skill of base.skillLines![locale])
      expect(article).not.toHaveTextContent(skill)
    expect(JSON.stringify(state)).toBe(original)
    expect(localStorage.getItem(historyStorageKey)).toBe(stored)
    view.unmount()
    client.clear()
  },
)
it('renders a Rare fallback while the Rare-only base catalog loads', () => {
  localStorage.clear()
  useItemDraft.getState().setBase(82, 'dusk-amulet')
  vi.stubGlobal('fetch', () => new Promise(() => {}))
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  const view = render(
    <QueryClientProvider client={client}>
      <CraftingPage />
    </QueryClientProvider>,
  )
  expect(baseStartingRarity('dusk-amulet')).toBe('RARE')
  expect(screen.getByRole('article')).toHaveClass('item-card--rare')
  view.unmount()
  client.clear()
})
afterEach(() => {
  vi.unstubAllGlobals()
  useItemDraft.getState().setBase()
  setLocale('en')
  localStorage.clear()
})
