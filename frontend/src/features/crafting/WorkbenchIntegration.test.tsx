import { act } from '@testing-library/react'
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
  jsonResponse,
} from '../../shared/test/craftingFixtures'
import { concreteInitial } from './workbenchApi'
import {
  startFilm,
  emptyFilms,
  historyStorageKey,
  updateFilmMetadata,
} from './workbenchHistory'
let client: QueryClient
beforeEach(() => {
  localStorage.clear()
  client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  useItemDraft.getState().setBase()
  vi.stubGlobal('fetch', fixtureFetch)
})
afterEach(() => client.clear())
async function show() {
  const mounted = render(
    <QueryClientProvider client={client}>
      <CraftingPage />
    </QueryClientProvider>,
  )
  await waitFor(() =>
    expect(screen.getByRole('button', { name: 'Select base' })).toBeEnabled(),
  )
  return mounted
}
it('uses one selector for right content and registers a material in ten bottom favorites', async () => {
  await show()
  const inner = within(
    document.querySelector('.workbench-stash') as HTMLElement,
  )
  const outer = within(
    screen.getByRole('tablist', { name: 'Material stash tabs' }),
  )
  expect(outer.getByRole('tab', { name: 'Currency' })).toHaveAttribute(
    'aria-selected',
    'true',
  )
  expect(inner.queryByRole('tablist')).toBeNull()
  expect(document.querySelector('.stash-canvas')).toContainElement(
    outer.getByRole('tab', { name: 'Currency' }),
  )
  await act(async () => {
    fireEvent.click(outer.getByRole('tab', { name: 'Essence' }))
  })
  expect(outer.getByRole('tab', { name: 'Currency' })).toHaveAttribute(
    'aria-selected',
    'false',
  )
  expect(
    inner.queryByRole('button', { name: 'Lesser Essence of the Body' }),
  ).not.toBeInTheDocument()
  await act(async () => {
    fireEvent.click(
      inner.getByRole('button', { name: 'Greater Essence of the Body' }),
    )
  })
  const favorites = screen.getByRole('group', {
    name: 'Shared material favorites',
  })
  expect(document.querySelector('.stash-canvas__content')).toContainElement(
    favorites,
  )
  expect(within(favorites).getAllByRole('button')).toHaveLength(10)
  await act(async () => {
    fireEvent.click(within(favorites).getAllByRole('button')[0]!)
  })
  expect(within(favorites).getAllByRole('button')[0]).toHaveAccessibleName(
    'Favorite slot 1: Greater Essence of the Body',
  )
  expect(
    document.querySelector(
      '.currency-catalog [data-material-tooltip="Preserved_Rib"]',
    ),
  ).toBeNull()
})
it('opens the left base menu independently from the text dialog', async () => {
  await show()
  await act(async () => {
    fireEvent.click(screen.getByRole('button', { name: 'Select base' }))
  })
  expect(
    await screen.findByRole('dialog', { name: 'Select base' }),
  ).toBeVisible()
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
  await show()
  await act(async () => {
    fireEvent.click(screen.getByRole('tab', { name: 'History' }))
  })
  fireEvent.click(
    await screen.findByRole('button', { name: /^Select session:/ }),
  )
  fireEvent.click(await screen.findByRole('button', { name: /^Rename:/ }))
  const title = screen.getByRole('textbox', { name: 'Session title' })
  title.textContent = 'My craft'
  await act(async () => fireEvent.keyDown(title, { key: 'Enter' }))
  await act(async () => {
    fireEvent.click(screen.getByRole('button', { name: 'Favorite: My craft' }))
  })
  let saved = JSON.parse(localStorage.getItem(historyStorageKey)!)
  expect(saved.films[0]).toMatchObject({
    id: 'one',
    title: 'My craft',
    favorite: true,
  })
  const frames = saved.films[0].frames
  await act(async () => {
    fireEvent.click(screen.getByRole('button', { name: 'Delete: My craft' }))
  })
  await waitFor(() =>
    expect(
      JSON.parse(localStorage.getItem(historyStorageKey)!).films,
    ).toHaveLength(0),
  )
  await act(async () => {
    fireEvent.click(
      screen.getByRole('button', { name: 'Undo deletion: My craft' }),
    )
  })
  saved = JSON.parse(localStorage.getItem(historyStorageKey)!)
  expect(saved.films[0].frames).toEqual(frames)
  expect(saved.films[0]).toMatchObject({
    id: 'one',
    title: 'My craft',
    favorite: true,
  })
})

it('preserves saved sessions when a metadata write fails', async () => {
  const state = concreteInitial(initialFixture)
  const original = JSON.stringify(
    startFilm(emptyFilms(), state, 'one', initialFixture.rulesetIdentity),
  )
  localStorage.setItem(historyStorageKey, original)
  await show()
  await act(async () => {
    fireEvent.click(screen.getByRole('tab', { name: 'History' }))
  })
  const write = vi
    .spyOn(Storage.prototype, 'setItem')
    .mockImplementation(() => {
      throw new Error('Quota exceeded')
    })
  try {
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /^Favorite:/ }))
    })
    expect(localStorage.getItem(historyStorageKey)).toBe(original)
    expect(screen.getByRole('button', { name: /^Favorite:/ })).toHaveAttribute(
      'aria-pressed',
      'false',
    )
  } finally {
    write.mockRestore()
  }
})

it('keeps item controls in the simulation and exposes a separate History tab', async () => {
  await show()
  const canvas = document.querySelector('.stash-canvas')!
  expect(canvas).toContainElement(
    screen.getByRole('tablist', { name: 'Material stash tabs' }),
  )
  expect(screen.queryByRole('checkbox', { name: 'Show tooltips' })).toBeNull()
  expect(document.querySelector('.workbench-favorites-region')).toBeNull()
  const plane = canvas.querySelector('.stash-canvas__content')!
  expect(plane).toContainElement(
    document.querySelector('.workbench-stash__favorites'),
  )
  const controls = plane.querySelector('.workbench-item-controls')!
  for (const name of ['Select base', 'Edit item', 'New craft'])
    expect(controls).toContainElement(screen.getByRole('button', { name }))
  const buttons = document.querySelector('.workbench-film-toolbar')!
  expect(buttons.previousElementSibling).toBeNull()
  expect(buttons).toContainElement(
    screen.getByRole('button', { name: 'Previous crafting step' }),
  )
  expect(buttons).toContainElement(
    screen.getByRole('button', { name: 'Next crafting step' }),
  )
  expect(
    document.querySelector('.bench-item-card .workbench-film-controls'),
  ).toBeNull()
  expect(document.querySelector('.workbench-session-entry')).toBeNull()
  expect(buttons.nextElementSibling).toHaveClass('bench-item-card')
  expect(
    screen.getByRole('tab', { name: 'Crafting Workbench' }),
  ).toHaveAttribute('aria-selected', 'true')
  expect(screen.getByRole('tab', { name: 'History' })).toHaveAttribute(
    'aria-selected',
    'false',
  )
  expect(document.querySelector('.wb-session-library-buttons')).toBeNull()
  expect(document.querySelector('.preview-badge')).toBeNull()
  expect(document.querySelector('.placement-label')).toBeNull()
  expect(document.querySelector('.locale-disclosure')).toBeNull()
  expect(document.querySelector('.craft-footer')).toHaveTextContent('PoE2DB')
  expect(
    document.querySelectorAll('.workbench-film-controls__arrow'),
  ).toHaveLength(2)
})
it('opens the favorites-only session view without rewriting the full saved history', async () => {
  const state = concreteInitial(initialFixture)
  let history = startFilm(
    emptyFilms(),
    state,
    'favorite-one',
    initialFixture.rulesetIdentity,
  )
  history = startFilm(
    history,
    state,
    'ordinary-two',
    initialFixture.rulesetIdentity,
  )
  history = updateFilmMetadata(history, 'favorite-one', {
    title: 'Saved favorite',
    favorite: true,
  })
  localStorage.setItem(historyStorageKey, JSON.stringify(history))
  await show()
  const before = localStorage.getItem(historyStorageKey)
  await act(async () => {
    fireEvent.click(screen.getByRole('tab', { name: 'History' }))
  })
  await act(async () => {
    fireEvent.click(screen.getByRole('button', { name: 'Favorites' }))
  })
  const panel = document.getElementById('workbench-panel-history')!
  expect(
    within(panel).getAllByRole('button', { name: /^Select session:/ }),
  ).toHaveLength(1)
  expect(
    within(panel).getByRole('button', {
      name: /Select session: Saved favorite/,
    }),
  ).toBeVisible()
  expect(localStorage.getItem(historyStorageKey)).toBe(before)
})

it('renders original shelf positions and an unscaled viewport cursor', async () => {
  await show()
  const shelf = document.querySelector<HTMLElement>('.currency-catalog')!
  const chaos = within(shelf).getByRole('button', { name: 'Chaos Orb' })
  expect(chaos.style.left).toBe(`${29 / 9.35}%`)
  expect(chaos.style.top).toBe(`${440 / 5.5}%`)
  fireEvent.contextMenu(chaos, { clientX: 240, clientY: 180 })
  const cursor = document.querySelector<HTMLElement>('.currency-cursor')!
  expect(cursor.parentElement).toBe(document.body)
  expect(cursor.style.left).toBe('254px')
  expect(cursor.style.top).toBe('194px')
  expect(document.querySelector('main')).toHaveClass('craft-page--workbench')
})

it('returns focus to the relocated base and edit buttons after overlay close and input cancellation', async () => {
  await show()
  const base = screen.getByRole('button', { name: 'Select base' })
  base.focus()
  await act(async () => {
    fireEvent.click(base)
  })
  const picker = await screen.findByRole('dialog', { name: 'Select base' })
  expect(base).toHaveAttribute('aria-expanded', 'true')
  expect(base).toHaveAttribute('aria-controls', picker.id)
  fireEvent.keyDown(picker, { key: 'Escape' })
  expect(base).toHaveFocus()
  expect(base).toHaveAttribute('aria-expanded', 'false')
  const edit = screen.getByRole('button', { name: 'Edit item' })
  await waitFor(() => expect(edit).toBeEnabled())
  edit.focus()
  await act(async () => {
    fireEvent.click(edit)
  })
  const input = await screen.findByRole('dialog', { name: 'Edit item' })
  expect(edit).toHaveAttribute('aria-controls', input.id)
  expect(edit).toHaveAttribute('aria-expanded', 'true')
  expect(screen.getByRole('button', { name: 'Close item input' })).toHaveFocus()
  const displayedText = useItemDraft.getState().text
  fireEvent.change(within(input).getByRole('textbox'), {
    target: { value: 'Draft to keep' },
  })
  fireEvent.keyDown(window, { key: 'Escape' })
  expect(edit).toHaveFocus()
  expect(edit).toHaveAttribute('aria-expanded', 'false')
  expect(useItemDraft.getState().text).toBe(displayedText)
  await act(async () => {
    fireEvent.click(edit)
  })
  await act(async () => {
    fireEvent.click(screen.getByRole('button', { name: 'Close item input' }))
  })
  expect(edit).toHaveFocus()
})

it('starts a new craft from the relocated control and preserves the existing film on a failed request', async () => {
  const history = startFilm(
    emptyFilms(),
    concreteInitial(initialFixture),
    'existing',
    initialFixture.rulesetIdentity,
  )
  localStorage.setItem(historyStorageKey, JSON.stringify(history))
  await show()
  const key = ['crafting', 'initial', 'solar', 82, 'manual']
  await waitFor(() => expect(client.getQueryData(key)).toBeDefined())
  const newCraft = screen.getByRole('button', { name: 'New craft' })
  const bytes = localStorage.getItem(historyStorageKey)
  const revision = useItemDraft.getState().baseRevision
  await client.invalidateQueries({ queryKey: key, refetchType: 'none' })
  vi.stubGlobal('fetch', () => Promise.resolve(jsonResponse({}, 500)))
  await act(async () => {
    fireEvent.click(newCraft)
  })
  const picker = await screen.findByRole('dialog', { name: 'Select base' })
  const confirm = within(picker).getByRole('button', { name: 'Select base' })
  await act(async () => fireEvent.click(confirm))
  await waitFor(() =>
    expect(
      document.querySelector('.craft-page > .sr-only[role="status"]'),
    ).toHaveTextContent('Could not load the selected base.'),
  )
  expect(useItemDraft.getState().baseRevision).toBe(revision)
  expect(localStorage.getItem(historyStorageKey)).toBe(bytes)
  vi.stubGlobal('fetch', fixtureFetch)
  await act(async () => fireEvent.click(newCraft))
  const retry = within(
    await screen.findByRole('dialog', { name: 'Select base' }),
  ).getByRole('button', { name: 'Select base' })
  await waitFor(() => expect(retry).toBeEnabled())
  await act(async () => fireEvent.click(retry))
  await waitFor(() =>
    expect(useItemDraft.getState().baseRevision).toBeGreaterThan(revision),
  )
  const saved = JSON.parse(localStorage.getItem(historyStorageKey)!)
  expect(saved.films).toHaveLength(2)
  expect(saved.films[0]).toEqual(history.films[0])
  expect(saved.films[1].frames[0].state).toEqual({
    ...concreteInitial(initialFixture),
    desecrated: [],
  })
})
