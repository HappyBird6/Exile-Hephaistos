import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { CraftingPage } from '../CraftingPage'
import { useItemDraft } from '../draft'
import { baseStartingRarity, selectableBaseKeys } from '../baseRegistry'
import { topBase } from '../topBases'
import { temporaryAmuletPassive } from './temporaryAmuletPassive'

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
it('labels the temporary passive example in every locale without inventing effects', () => {
  for (const locale of ['en', 'ko', 'ja', 'zh-CN', 'zh-TW', 'es'] as const)
    expect(temporaryAmuletPassive(locale)).toContain('Heavy Ammunition')
})
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
})
