import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from '@testing-library/react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { CraftingPage } from '../../CraftingPage'
import { useItemDraft } from '../../draft'
import { historyStorageKey } from '../../workbenchHistory'
import {
  fixtureFetch,
  initialFixture,
} from '../../../../shared/test/craftingFixtures'
import {
  currencyButton,
  outerMaterialTab,
} from '../../../../shared/test/workbenchQueries'
import { setLocale } from '../../../../shared/i18n/i18n'
import { chaosRepeatControlMessages } from '../controls'

let client: QueryClient
let fetchMock: ReturnType<typeof vi.fn<typeof fixtureFetch>>
beforeEach(() => {
  localStorage.clear()
  setLocale('en')
  useItemDraft.getState().setBase()
  client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  client.setQueryData(
    ['crafting', 'initial', 'solar', 82, 'manual'],
    initialFixture,
  )
  fetchMock = vi.fn(fixtureFetch)
  vi.stubGlobal('fetch', fetchMock)
})
afterEach(() => {
  client.clear()
  vi.unstubAllGlobals()
})
function show() {
  return render(
    <QueryClientProvider client={client}>
      <CraftingPage />
    </QueryClientProvider>,
  )
}
const controls = () => screen.getByRole('group', { name: 'Chaos repeat mode' })
const toggle = () =>
  within(controls()).getByRole('button', { name: /1000 x.*Use/ })
const slot = () =>
  screen.getByRole('button', {
    name: 'Use selected currency on the central item',
  })
const shared = () =>
  document.querySelector<HTMLElement>('.workbench-feedback-tooltip')!
const craftingCalls = () =>
  fetchMock.mock.calls.filter(
    ([url]) =>
      String(url).includes('/workbench/') &&
      !String(url).includes('/modifier-pools'),
  )

it('edits the pattern and toggles mode without using currency or exposing a count editor', () => {
  show()
  const before = localStorage.getItem(historyStorageKey)
  fireEvent.change(screen.getByRole('textbox', { name: 'Pattern' }), {
    target: { value: '^.+Life$' },
  })
  expect(within(controls()).getAllByRole('textbox')).toHaveLength(1)
  expect(toggle()).toHaveAttribute('aria-pressed', 'false')
  expect(toggle()).toHaveTextContent('1000 x')
  fireEvent.click(toggle())
  expect(toggle()).toHaveAttribute('aria-pressed', 'true')
  fireEvent.click(toggle())
  expect(toggle()).toHaveAttribute('aria-pressed', 'false')
  expect(localStorage.getItem(historyStorageKey)).toBe(before)
  expect(craftingCalls()).toHaveLength(0)
})

it('keeps the central item on its existing single-application path', async () => {
  show()
  fireEvent.click(toggle())
  fireEvent.contextMenu(currencyButton('Orb of Transmutation'))
  fireEvent.click(slot())
  await waitFor(() =>
    expect(localStorage.getItem(historyStorageKey)).toContain('TRANSMUTATION'),
  )
  expect(craftingCalls()).toHaveLength(1)
  expect(String(craftingCalls()[0]![0])).toMatch(/\/workbench\/apply$/)
  expect(toggle()).toHaveAttribute('aria-pressed', 'true')
})

it('adds the repeat hint to the same selected Chaos description and retains its source anchor', () => {
  show()
  const chaos = currencyButton('Greater Chaos Orb')
  fireEvent.contextMenu(chaos)
  fireEvent.focusIn(screen.getByRole('textbox', { name: 'Pattern' }))
  expect(shared()).toHaveTextContent('Greater Chaos Orb')
  expect(shared()).toHaveTextContent(chaosRepeatControlMessages.en.hint)
  expect(chaos).toHaveAttribute('aria-describedby', 'material-description')
  const link = within(shared()).getByRole('link', { name: 'PoE2DB' })
  act(() => link.focus())
  fireEvent.keyDown(link, { key: 'Escape' })
  expect(shared()).toBeNull()
  expect(chaos).toHaveAttribute('aria-pressed', 'false')
  expect(chaos).toHaveFocus()
})

it('preserves an actually held other material when repeat controls show their hint', () => {
  show()
  fireEvent.click(outerMaterialTab('Essence'))
  const essence = currencyButton('Greater Essence of the Body')
  fireEvent.click(essence)
  fireEvent.pointerEnter(controls())
  fireEvent.click(toggle())
  expect(essence).toHaveClass('is-held')
  expect(shared()).toHaveTextContent('Greater Essence of the Body')
  expect(shared()).not.toHaveTextContent(chaosRepeatControlMessages.en.hint)
  expect(
    shared().querySelector('.material-description__name'),
  ).not.toHaveTextContent('Chaos')
  expect(craftingCalls()).toHaveLength(0)
})
