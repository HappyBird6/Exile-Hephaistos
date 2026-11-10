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
import { CraftingPage } from './CraftingPage'
import { useItemDraft } from './draft'
import {
  initialFixture,
  jsonResponse,
} from '../../shared/test/craftingFixtures'
import { concreteInitial } from './workbenchApi'
import { emptyFilms, historyStorageKey, startFilm } from './workbenchHistory'
import {
  editorFixtureFetch,
  parsedTextFixture,
} from './item-editor/fixtures.test-support'
import { locales, setLocale, translate } from '../../shared/i18n/i18n'
let client: QueryClient
const root = concreteInitial(initialFixture)
const history = () => JSON.parse(localStorage.getItem(historyStorageKey)!)
beforeEach(() => {
  localStorage.clear()
  useItemDraft.getState().setBase()
  useItemDraft.getState().resetText('')
  localStorage.setItem(
    historyStorageKey,
    JSON.stringify(
      startFilm(
        emptyFilms(),
        root,
        'original',
        initialFixture.rulesetIdentity,
        'Original imported document',
      ),
    ),
  )
  client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  vi.stubGlobal('fetch', editorFixtureFetch)
})
afterEach(() => {
  client.clear()
  vi.unstubAllGlobals()
})
async function open() {
  render(
    <QueryClientProvider client={client}>
      <CraftingPage />
    </QueryClientProvider>,
  )
  await waitFor(() =>
    expect(screen.getByRole('button', { name: 'Edit item' })).toBeEnabled(),
  )
  fireEvent.click(screen.getByRole('button', { name: 'Edit item' }))
  return screen.getByRole('textbox', { name: 'Item text copied from the game' })
}
const apply = () =>
  fireEvent.click(screen.getByRole('button', { name: 'Apply' }))
it('uses one action to validate and apply while preserving the previous session', async () => {
  const input = await open()
  const before = history().films[0]
  fireEvent.change(input, {
    target: {
      value: useItemDraft
        .getState()
        .text.replace('Item Level: 82', 'Item Level: 81'),
    },
  })
  apply()
  await waitFor(() => expect(history().films).toHaveLength(2))
  expect(history().films[0]).toEqual(before)
  expect(history().films[1].frames[0].state.itemLevel).toBe(81)
  expect(
    screen.queryByRole('button', {
      name: /Analyze item|Load current item text|Enable crafting/,
    }),
  ).toBeNull()
  expect(document.querySelector('#item-input-panel')).not.toHaveAttribute(
    'open',
  )
  expect(document.querySelector('.bench-item-card')).not.toHaveTextContent(
    'Original imported document',
  )
})
it.each(['button', 'escape', 'backdrop'])(
  'discards an unapplied draft on %s close and reloads the displayed item',
  async (way) => {
    const input = await open(),
      original = useItemDraft.getState().text,
      before = localStorage.getItem(historyStorageKey)
    fireEvent.change(input, { target: { value: 'Unapplied draft' } })
    if (way === 'button')
      fireEvent.click(screen.getByRole('button', { name: 'Close item input' }))
    else if (way === 'escape') fireEvent.keyDown(input, { key: 'Escape' })
    else fireEvent.click(document.querySelector('#item-input-panel')!)
    expect(localStorage.getItem(historyStorageKey)).toBe(before)
    fireEvent.click(screen.getByRole('button', { name: 'Edit item' }))
    expect(
      screen.getByRole('textbox', { name: 'Item text copied from the game' }),
    ).toHaveValue(original)
    expect(screen.queryByRole('alert')).toBeNull()
  },
)
it('only displays rejected source lines in the feedback and keeps the current item', async () => {
  const input = await open()
  const source = useItemDraft.getState().text + '\nUnknown future modifier'
  const number = source.split('\n').length
  const before = localStorage.getItem(historyStorageKey)
  vi.stubGlobal('fetch', (url: RequestInfo | URL, init?: RequestInit) =>
    String(url).endsWith('/edit-text')
      ? Promise.resolve(
          jsonResponse({
            mapped: false,
            state: null,
            issues: [
              {
                lineNumber: number,
                message: 'Unparsed line retained; crafting mapping is blocked.',
              },
            ],
          }),
        )
      : editorFixtureFetch(url, init),
  )
  fireEvent.change(input, { target: { value: source } })
  apply()
  await screen.findByRole('alert')
  const feedback = within(screen.getByRole('alert'))
  expect(feedback.getByText('Unknown future modifier')).toBeVisible()
  expect(feedback.queryByText('Item Level: 82')).toBeNull()
  expect(localStorage.getItem(historyStorageKey)).toBe(before)
  expect(input).toHaveValue(source)
})
it('does not apply a late response after closing the editor', async () => {
  const input = await open()
  const original = useItemDraft.getState().text
  let finish!: (r: Response) => void
  const request = vi.fn((url: RequestInfo | URL, init?: RequestInit) =>
    String(url).endsWith('/edit-text')
      ? new Promise<Response>((resolve) => {
          finish = resolve
        })
      : editorFixtureFetch(url, init),
  )
  vi.stubGlobal('fetch', request)
  fireEvent.change(input, {
    target: { value: original.replace('Item Level: 82', 'Item Level: 81') },
  })
  apply()
  await waitFor(() => expect(finish).toBeDefined())
  fireEvent.click(screen.getByRole('button', { name: 'Close item input' }))
  await act(async () => {
    finish(
      jsonResponse({
        mapped: true,
        state: { ...root, itemLevel: 81 },
        issues: [],
      }),
    )
  })
  expect(history().films).toHaveLength(1)
  fireEvent.click(screen.getByRole('button', { name: 'Edit item' }))
  expect(
    screen.getByRole('textbox', { name: 'Item text copied from the game' }),
  ).toHaveValue(original)
})
it('cancels pending validation when the draft changes', async () => {
  const input = await open()
  let finish!: (r: Response) => void
  vi.stubGlobal('fetch', (url: RequestInfo | URL, init?: RequestInit) =>
    String(url).endsWith('/edit-text')
      ? new Promise<Response>((resolve) => {
          finish = resolve
        })
      : editorFixtureFetch(url, init),
  )
  fireEvent.change(input, {
    target: { value: useItemDraft.getState().text + '\n' },
  })
  apply()
  await waitFor(() => expect(finish).toBeDefined())
  fireEvent.change(input, { target: { value: 'newer input' } })
  await act(async () => {
    finish(jsonResponse({ mapped: true, state: root, issues: [] }))
  })
  expect(input).toHaveValue('newer input')
  expect(history().films).toHaveLength(1)
})
it.each([{ type: 'FUTURE' }, { tier: '1' }, { affix: 'UNVERIFIED' }])(
  'rejects an unverified parsed response %j',
  async (override) => {
    const input = await open()
    const source = useItemDraft.getState().text + '\n'
    const parsed = parsedTextFixture(source)
    vi.stubGlobal('fetch', (url: RequestInfo | URL, init?: RequestInit) =>
      String(url).endsWith('/items/parse')
        ? Promise.resolve(
            jsonResponse({
              ...parsed,
              modifiers: [
                {
                  id: 'bad',
                  type: 'EXPLICIT',
                  text: 'Bad',
                  source: parsed.text.lines[0],
                  metadata: null,
                  tier: null,
                  affix: null,
                  affixName: null,
                  ...override,
                },
              ],
            }),
          )
        : editorFixtureFetch(url, init),
    )
    fireEvent.change(input, { target: { value: source } })
    apply()
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Could not connect',
    )
    expect(history().films).toHaveLength(1)
  },
)
it.each(locales)(
  'localizes validation feedback and preserves the current item in %s',
  async (locale) => {
    await open()
    act(() => setLocale(locale))
    const input = screen.getByRole('textbox', {
      name: translate('ui.item_text_copied_from_the_game'),
    })
    fireEvent.change(input, { target: { value: ' ' } })
    fireEvent.click(
      screen.getByRole('button', { name: translate('input.apply') }),
    )
    expect(await screen.findByRole('alert')).toHaveTextContent(
      translate('input.empty'),
    )
    expect(history().films).toHaveLength(1)
  },
)
it('rejects oversized text without calling the parser', async () => {
  const input = await open(),
    fetcher = vi.fn(editorFixtureFetch)
  vi.stubGlobal('fetch', fetcher)
  fireEvent.change(input, { target: { value: '한'.repeat(6000) } })
  apply()
  expect(await screen.findByRole('alert')).toHaveTextContent('16 KiB')
  expect(
    fetcher.mock.calls.some(([url]) => String(url).endsWith('/items/parse')),
  ).toBe(false)
})
it('does not create a duplicate session for unchanged text', async () => {
  await open()
  apply()
  expect(history().films).toHaveLength(1)
  expect(document.querySelector('#item-input-panel')).not.toHaveAttribute(
    'open',
  )
})

it('imports Sanctified current values as a saved locked item without reconstructing old rolls', async () => {
  const input = await open()
  const original = history().films[0]
  const source =
    'Item Class: Amulets\nRarity: Rare\nImported Item\nSolar Amulet\n--------\nItem Level: 82\n--------\n+18 to Spirit (implicit)\n--------\n+24 to maximum Life\n--------\nSanctified'
  const frozen = {
    ...root,
    rarity: 'RARE' as const,
    implicits: [
      { modifierId: 'implicit', values: { base_spirit_from_equipment: 18 } },
    ],
    explicits: [{ modifierId: 'p', values: { life: 24 } }],
    conditions: ['SANCTIFIED' as const],
    sanctification: null,
  }
  const fetcher = vi.fn((url: RequestInfo | URL, init?: RequestInit) => {
    if (String(url).endsWith('/items/parse')) {
      const parsed = parsedTextFixture(source)
      return Promise.resolve(
        jsonResponse({ ...parsed, flags: [parsed.text.lines.at(-1)!] }),
      )
    }
    if (String(url).endsWith('/map-text'))
      return Promise.resolve(
        jsonResponse({ mapped: true, state: frozen, issues: [] }),
      )
    return editorFixtureFetch(url, init)
  })
  vi.stubGlobal('fetch', fetcher)
  fireEvent.change(input, { target: { value: source } })
  apply()
  await waitFor(() => expect(history().films).toHaveLength(2))
  expect(history().films[0]).toEqual(original)
  expect(history().films[1].frames[0].state).toEqual(frozen)
  expect(document.querySelector('.bench-item-card')).toHaveTextContent(
    '+24 to maximum Life',
  )
  expect(document.querySelector('.bench-item-card')).toHaveTextContent(
    'Sanctified',
  )
  expect(document.querySelector('.bench-item-card')).not.toHaveTextContent(
    'original text',
  )
  expect(
    fetcher.mock.calls.some(([url]) => String(url).endsWith('/edit-text')),
  ).toBe(false)
  fireEvent.click(screen.getByRole('button', { name: 'Edit item' }))
  expect(
    (
      screen.getByRole('textbox', {
        name: 'Item text copied from the game',
      }) as HTMLTextAreaElement
    ).value,
  ).toContain('+24 to maximum Life')
})
