import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import {
  fireEvent,
  render,
  screen,
  within,
  waitFor,
} from '@testing-library/react'
import { beforeEach, afterEach, expect, it, vi } from 'vitest'
import { CraftingPage } from './CraftingPage'
import { useItemDraft } from './draft'
import {
  fixtureFetch,
  initialFixture,
} from '../../shared/test/craftingFixtures'
import { concreteInitial } from './workbenchApi'
import { startFilm, emptyFilms, historyStorageKey } from './workbenchHistory'
let client: QueryClient
beforeEach(() => {
  localStorage.clear()
  client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  useItemDraft.getState().setBase()
  vi.stubGlobal('fetch', fixtureFetch)
})
afterEach(() => client.clear())
function show() {
  return render(
    <QueryClientProvider client={client}>
      <CraftingPage />
    </QueryClientProvider>,
  )
}
it('keeps outer tabs independent and registers a material in ten inner favorites', () => {
  show()
  const inner = within(
    document.querySelector('.workbench-stash') as HTMLElement,
  )
  const outer = within(document.querySelector('.stash-heading') as HTMLElement)
  expect(outer.getByRole('tab', { name: 'Currency' })).toHaveAttribute(
    'aria-selected',
    'true',
  )
  fireEvent.click(inner.getByRole('tab', { name: 'Essence' }))
  expect(outer.getByRole('tab', { name: 'Currency' })).toHaveAttribute(
    'aria-selected',
    'true',
  )
  expect(
    inner.queryByRole('button', { name: 'Lesser Essence of the Body' }),
  ).not.toBeInTheDocument()
  fireEvent.click(
    inner.getByRole('button', { name: 'Greater Essence of the Body' }),
  )
  const favorites = inner.getByRole('group', {
    name: 'Shared material favorites',
  })
  expect(within(favorites).getAllByRole('button')).toHaveLength(10)
  fireEvent.click(within(favorites).getAllByRole('button')[0]!)
  expect(within(favorites).getAllByRole('button')[0]).toHaveAccessibleName(
    'Favorite slot 1: Greater Essence of the Body',
  )
  expect(
    document.querySelector(
      '.currency-catalog [data-material-tooltip="Preserved_Rib"]',
    ),
  ).toBeNull()
})
it('opens the left base menu independently from the text dialog', () => {
  show()
  fireEvent.click(screen.getByRole('button', { name: 'Select base' }))
  expect(document.getElementById('workbench-base-picker')).toBeVisible()
  expect(document.getElementById('item-input-panel')).not.toHaveAttribute(
    'open',
  )
  expect(
    screen.queryByLabelText('Existing catalyst quality'),
  ).not.toBeInTheDocument()
  expect(screen.getByRole('spinbutton', { name: 'Item level' })).toHaveValue(82)
})
it('renames, favorites, deletes and restores the exact saved session', async () => {
  const state = concreteInitial(initialFixture)
  localStorage.setItem(
    historyStorageKey,
    JSON.stringify(
      startFilm(emptyFilms(), state, 'one', initialFixture.rulesetIdentity),
    ),
  )
  show()
  fireEvent.click(screen.getByRole('button', { name: 'Crafting session' }))
  fireEvent.click(screen.getByRole('button', { name: /^Rename:/ }))
  fireEvent.change(screen.getByRole('textbox', { name: /^Session title:/ }), {
    target: { value: 'My craft' },
  })
  fireEvent.click(screen.getByRole('button', { name: 'Save' }))
  fireEvent.click(screen.getByRole('button', { name: 'Favorite: My craft' }))
  let saved = JSON.parse(localStorage.getItem(historyStorageKey)!)
  expect(saved.films[0]).toMatchObject({
    id: 'one',
    title: 'My craft',
    favorite: true,
  })
  const frames = saved.films[0].frames
  fireEvent.click(screen.getByRole('button', { name: 'Delete: My craft' }))
  fireEvent.click(screen.getByRole('button', { name: 'Delete this session' }))
  await waitFor(() =>
    expect(
      JSON.parse(localStorage.getItem(historyStorageKey)!).films,
    ).toHaveLength(0),
  )
  fireEvent.click(screen.getByRole('button', { name: 'Undo deletion' }))
  saved = JSON.parse(localStorage.getItem(historyStorageKey)!)
  expect(saved.films[0].frames).toEqual(frames)
  expect(saved.films[0]).toMatchObject({
    id: 'one',
    title: 'My craft',
    favorite: true,
  })
})

it('preserves saved sessions when a metadata write fails', () => {
  const state = concreteInitial(initialFixture)
  const original = JSON.stringify(
    startFilm(emptyFilms(), state, 'one', initialFixture.rulesetIdentity),
  )
  localStorage.setItem(historyStorageKey, original)
  show()
  fireEvent.click(screen.getByRole('button', { name: 'Crafting session' }))
  const write = vi
    .spyOn(Storage.prototype, 'setItem')
    .mockImplementation(() => {
      throw new Error('Quota exceeded')
    })
  try {
    fireEvent.click(screen.getByRole('button', { name: /^Favorite:/ }))
    expect(localStorage.getItem(historyStorageKey)).toBe(original)
    expect(screen.getByRole('button', { name: /^Favorite:/ })).toHaveAttribute(
      'aria-pressed',
      'false',
    )
  } finally {
    write.mockRestore()
  }
})
