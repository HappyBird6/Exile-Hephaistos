import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from '@testing-library/react'
import { beforeEach, expect, it } from 'vitest'
import { CraftingPage } from '../CraftingPage'
import { useItemDraft } from '../draft'
import { initialFixture } from '../../../shared/test/craftingFixtures'

beforeEach(() => useItemDraft.getState().setBase())
it('shows all before explicit selection and filters the selected/restored base without erasing favorites', async () => {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  client.setQueryData(['crafting', 'initial', 'solar', 82, 'manual'], {
    ...initialFixture,
    baseMaterialActions: ['CATALYST_FLESH'],
  })
  const show = () =>
    render(
      <QueryClientProvider client={client}>
        <CraftingPage />
      </QueryClientProvider>,
    )
  let page = show()
  fireEvent.click(screen.getByRole('tab', { name: 'Catalysts' }))
  const list = () => document.querySelector('.workbench-stash__resources')!
  expect(list().querySelectorAll('button')).toHaveLength(26)
  fireEvent.click(screen.getByRole('button', { name: 'Select base' }))
  fireEvent.click(
    within(screen.getByRole('dialog', { name: 'Select base' })).getByRole(
      'button',
      { name: 'Select base' },
    ),
  )
  await waitFor(() => expect(list().querySelectorAll('button')).toHaveLength(1))
  expect(screen.getByRole('button', { name: 'Flesh Catalyst' })).toBeVisible()
  fireEvent.click(screen.getByRole('button', { name: 'New craft' }))
  await waitFor(() => expect(list().querySelectorAll('button')).toHaveLength(1))
  page.unmount()
  page = show()
  fireEvent.click(screen.getByRole('tab', { name: 'Catalysts' }))
  expect(list().querySelectorAll('button')).toHaveLength(1)
  expect(screen.getAllByRole('button', { name: /Favorite slot/ })).toHaveLength(
    10,
  )
  page.unmount()
  client.clear()
})
