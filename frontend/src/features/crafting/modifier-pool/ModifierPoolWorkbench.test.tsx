import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, fireEvent, render, screen, within } from '@testing-library/react'
import { expect, it } from 'vitest'
import { CraftingPage } from '../CraftingPage'
import { useItemDraft } from '../draft'
import type { Initial } from '../craftingApi'
import { initialFixture } from '../../../shared/test/craftingFixtures'

function catalog(level: number, jewel = false): Initial {
  return {
    ...initialFixture,
    metadata: {
      ...initialFixture.metadata,
      weightPolicy: jewel
        ? 'USER_APPROVED_UNIFORM_CANDIDATES_NOT_GAME_WEIGHTS'
        : 'POE2DB_AS_PUBLISHED',
    },
    state: {
      ...initialFixture.state,
      itemLevel: level,
      baseItemId: jewel
        ? 'Metadata/Items/Jewels/JewelInt'
        : 'Metadata/Items/Amulets/FourAmulet9',
    },
    modifiers: {
      ...initialFixture.modifiers,
      p: {
        ...initialFixture.modifiers.p!,
        layer: 'EXPLICIT',
        requiredItemLevel: 80,
        weight: 100,
      },
      s: {
        ...initialFixture.modifiers.s!,
        layer: 'EXPLICIT',
        requiredItemLevel: 1,
        weight: 50,
      },
    },
  }
}
it('places the reference panel after the whole work area, follows base/level and hides mismatched imported data', () => {
  useItemDraft.getState().setBase()
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  client.setQueryData(
    ['crafting', 'initial', 'solar', 82, 'manual'],
    catalog(82),
  )
  client.setQueryData(['crafting', 'initial', 'solar', 1, 'manual'], catalog(1))
  client.setQueryData(
    ['crafting', 'initial', 'sapphire', 82, 'manual'],
    catalog(82, true),
  )
  const mounted = render(
    <QueryClientProvider client={client}>
      <CraftingPage />
    </QueryClientProvider>,
  )
  let panel = screen.getByRole('region', {
    name: 'Modifier weights & probabilities',
  })
  expect(panel.previousElementSibling).toHaveClass('stash-panel')
  expect(panel.previousElementSibling).toContainElement(
    document.querySelector('.bench-lower'),
  )
  expect(within(panel).getByText('Item level 82')).toBeVisible()
  expect(
    within(panel)
      .getByRole('region', { name: 'Prefixes' })
      .querySelectorAll('details'),
  ).toHaveLength(1)
  act(() => useItemDraft.getState().setBase(1))
  panel = screen.getByRole('region', {
    name: 'Modifier weights & probabilities',
  })
  expect(within(panel).getByText('Item level 1')).toBeVisible()
  expect(
    within(panel)
      .getByRole('region', { name: 'Prefixes' })
      .querySelectorAll('details'),
  ).toHaveLength(0)
  act(() => useItemDraft.getState().setBase(82, 'sapphire'))
  panel = screen.getByRole('region', {
    name: 'Modifier weights & probabilities',
  })
  expect(
    within(panel).getByText(
      'Weights and probabilities for this item are not published. Browse its modifiers and required levels below.',
      { exact: false },
    ),
  ).toBeVisible()
  expect(panel.querySelector('.modifier-pool__bar')).toBeNull()
  fireEvent.click(screen.getByRole('tab', { name: 'History' }))
  expect(panel).not.toBeVisible()
  fireEvent.click(screen.getByRole('tab', { name: 'Crafting Workbench' }))
  act(() =>
    useItemDraft.getState().acceptText('Unrecognized original item text'),
  )
  panel = screen.getByRole('region', {
    name: 'Modifier weights & probabilities',
  })
  expect(within(panel).getByRole('status')).toHaveTextContent(
    'Modifier data is unavailable for this item.',
  )
  expect(panel.querySelector('details')).toBeNull()
  mounted.unmount()
  client.clear()
})
