import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { LocaleProvider } from '../../shared/i18n/LocaleProvider'
import { CraftingPage } from './CraftingPage'
import { useItemDraft } from './draft'
import type { Item, TextLine } from './itemModels'

const raw =
  'Item Class: Synthetic Amulets\nRarity: Rare\nSynthetic Name\nSynthetic Base\n--------\nItem Level: 42\n<script>alert(1)</script>\n'
const line = (number: number, raw: string): TextLine => ({
  number,
  raw,
  section: 1,
  kind: 'CONTENT',
})
function result(text = raw): Item {
  return {
    text: { originalText: text, lines: [line(1, text)] },
    locale: 'en',
    itemClass: 'Synthetic Amulets',
    rarity: 'RARE',
    rarityText: 'Rare',
    nameLines: [line(3, 'Synthetic Name'), line(4, 'Synthetic Base')],
    displayName: 'Synthetic Name',
    displayBase: 'Synthetic Base',
    itemLevel: 42,
    properties: [
      { key: 'Item Level', value: '42', source: line(6, 'Item Level: 42') },
    ],
    requirements: [{ key: 'Level', value: '10', source: line(9, 'Level: 10') }],
    modifiers: [
      {
        text: 'Synthetic effect',
        type: 'IMPLICIT',
        source: line(10, 'Synthetic effect (implicit)'),
        metadata: null,
        affix: null,
        tier: null,
        affixName: null,
      },
    ],
    markedModifiers: [line(10, 'Synthetic effect (implicit)')],
    flags: [line(11, 'Unidentified')],
    unparsedLines: [line(7, '<script>alert(1)</script>')],
    warnings: [
      { code: 'CATALOG_VALIDATION_REQUIRED', lineNumber: 0 },
      { code: 'UNPARSED_LINES', lineNumber: 7 },
      { code: 'FUTURE_WARNING', lineNumber: 0 },
    ],
  }
}
const json = (value: unknown, status = 200) =>
  new Response(JSON.stringify(value), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
let client: QueryClient
let fetchMock: ReturnType<typeof vi.fn<typeof fetch>>
beforeEach(() => {
  useItemDraft.getState().setBase()
  client = new QueryClient({ defaultOptions: { mutations: { retry: false } } })
  fetchMock = vi.fn<typeof fetch>().mockResolvedValue(json(result()))
  vi.stubGlobal('fetch', fetchMock)
  localStorage.clear()
})
afterEach(() => {
  client.clear()
  vi.unstubAllGlobals()
  localStorage.clear()
})
function show() {
  return render(
    <QueryClientProvider client={client}>
      <LocaleProvider initialLanguage="en">
        <CraftingPage />
      </LocaleProvider>
    </QueryClientProvider>,
  )
}
function enter(text = raw) {
  if (
    screen
      .getByRole('button', { name: 'Edit item' })
      .getAttribute('aria-expanded') === 'false'
  ) {
    fireEvent.click(screen.getByRole('button', { name: 'Edit item' }))
  }
  fireEvent.click(screen.getByRole('button', { name: 'Item text' }))
  fireEvent.change(screen.getByRole('textbox'), { target: { value: text } })
}
function submit() {
  fireEvent.click(screen.getByRole('button', { name: /Analyze item/ }))
}

describe('Item text import', () => {
  it.each([
    { type: 'FUTURE' },
    { tier: '1' },
    { affix: 'UNVERIFIED' },
    { metadata: { number: 0, section: 0, kind: 'CONTENT', raw: 'invalid' } },
  ])('rejects invalid structured modifier response %j', async (override) => {
    const data = result()
    fetchMock.mockResolvedValueOnce(
      json({ ...data, modifiers: [{ ...data.modifiers[0], ...override }] }),
    )
    show()
    enter()
    submit()
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Could not connect',
    )
    expect(screen.getByRole('article')).toHaveTextContent('Solar Amulet')
  })
  it('shows English progress, failures and currency notices', async () => {
    let reject: (error: Error) => void = () => {
      throw new Error('not started')
    }
    fetchMock.mockImplementationOnce(
      () =>
        new Promise((_resolve, fail) => {
          reject = fail
        }),
    )
    show()
    enter()
    submit()
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1))
    expect(screen.getByRole('status')).toHaveTextContent('Analyzing item')
    await act(async () => {
      reject(new TypeError('offline'))
    })
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Could not connect',
    )
    expect(screen.getByRole('alert')).toHaveTextContent('Could not connect')
    fireEvent.change(screen.getByRole('textbox'), {
      target: { value: raw + 'changed' },
    })
    fireEvent.click(
      screen.getByRole('button', { name: 'Orb of Transmutation' }),
    )
    expect(screen.getByRole('status')).toHaveTextContent(
      'Orb of Transmutation selected',
    )
    fireEvent.keyDown(window, { key: 'Escape' })
    expect(screen.getByRole('status')).toHaveTextContent(
      'Currency selection cleared',
    )
  })
  it('shows API data and preserves evidence without executing HTML', async () => {
    show()
    enter()
    submit()
    expect(
      await screen.findByRole('heading', { name: 'Synthetic Name' }),
    ).toBeVisible()
    expect(screen.getByText('42')).toBeVisible()
    expect(
      within(screen.getByRole('article')).getByText('Level: 10'),
    ).toBeVisible()
    expect(
      within(screen.getByRole('article')).getByText('Synthetic effect'),
    ).toBeVisible()
    expect(
      within(screen.getByRole('article')).getByText('Unidentified'),
    ).toBeVisible()
    expect(screen.queryByText(/FUTURE_WARNING/)).not.toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(document.querySelector('.item-card script')).toBeNull()
    expect(useItemDraft.getState().currentText.text).toBe(raw)
    expect(useItemDraft.getState().text).toBe(raw)
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/items/parse',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ text: raw }),
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    fireEvent.click(screen.getByRole('button', { name: 'Chaos Orb' }))
    fireEvent.click(
      screen.getByRole('button', {
        name: 'Use selected currency on the central item',
      }),
    )
    expect(screen.getByRole('status')).toHaveTextContent(
      'The item has not changed',
    )
  })
  it('rejects blank and oversized UTF-8 input before requests', () => {
    show()
    enter(' ')
    submit()
    expect(screen.getByRole('alert')).toHaveTextContent('Enter item text')
    fireEvent.change(screen.getByRole('textbox'), {
      target: { value: '가'.repeat(5500) },
    })
    submit()
    expect(screen.getByRole('alert')).toHaveTextContent('16 KiB')
    expect(fetchMock).not.toHaveBeenCalled()
  })
  it.each([
    ['INVALID_ITEM_TEXT', 422, 'rarity'],
    ['ITEM_TEXT_TOO_LARGE', 413, '16 KiB'],
    ['MALFORMED_REQUEST', 400, 'request format'],
    ['PRIVATE_SERVER_DETAIL', 500, 'Could not connect'],
  ])('shows safe error %s and allows retry', async (code, status, expected) => {
    fetchMock.mockResolvedValueOnce(
      json({ code, detail: 'secret internal details' }, status),
    )
    show()
    enter()
    submit()
    expect(await screen.findByRole('alert')).toHaveTextContent(expected)
    expect(screen.queryByText(/secret internal/)).not.toBeInTheDocument()
    expect(useItemDraft.getState().text).toBe(raw)
    submit()
    expect(
      await screen.findByRole('heading', { name: 'Synthetic Name' }),
    ).toBeVisible()
  })
  it('does not treat network failures or invalid responses as success', async () => {
    fetchMock
      .mockRejectedValueOnce(new TypeError('offline'))
      .mockResolvedValueOnce(json({ ...result(), itemLevel: '42' }))
    show()
    enter()
    submit()
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Could not connect',
    )
    submit()
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2))
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Could not connect',
    )
    expect(
      screen.queryByRole('heading', { name: 'Synthetic Name' }),
    ).not.toBeInTheDocument()
  })
  it('aborts old requests and ignores late responses after edits', async () => {
    let resolveOld: (response: Response) => void = () => {
      throw new Error('not started')
    }
    fetchMock.mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          resolveOld = resolve
        }),
    )
    show()
    enter()
    submit()
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1))
    expect(screen.getByRole('status')).toHaveTextContent('Analyzing item')
    const signal = fetchMock.mock.calls[0]?.[1]?.signal
    const next = raw + 'new input'
    fireEvent.change(screen.getByRole('textbox'), { target: { value: next } })
    expect(signal?.aborted).toBe(true)
    fetchMock.mockResolvedValueOnce(
      json({ ...result(next), displayName: 'Latest Name' }),
    )
    submit()
    expect(
      await screen.findByRole('heading', { name: 'Latest Name' }),
    ).toBeVisible()
    await act(async () => {
      resolveOld(json(result()))
    })
    expect(screen.getByRole('heading', { name: 'Latest Name' })).toBeVisible()
    expect(
      screen.queryByRole('heading', { name: 'Synthetic Name' }),
    ).not.toBeInTheDocument()
  })
  it('preserves the placed item and current text when new input fails', async () => {
    show()
    enter()
    submit()
    await screen.findByRole('heading', { name: 'Synthetic Name' })
    enter('bad')
    fetchMock.mockResolvedValueOnce(json({ code: 'INVALID_ITEM_TEXT' }, 422))
    submit()
    await screen.findByRole('alert')
    expect(
      screen.queryByRole('heading', { name: 'Synthetic Name' }),
    ).toBeVisible()
    expect(useItemDraft.getState().currentText.text).toBe(raw)
  })
  it('aborts pending requests on base selection and unmount', async () => {
    fetchMock.mockImplementation(() => new Promise(() => {}))
    const view = show()
    enter()
    submit()
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1))
    const first = fetchMock.mock.calls[0]?.[1]?.signal
    fireEvent.click(screen.getByRole('button', { name: 'Select base' }))
    expect(first?.aborted).toBe(true)
    fireEvent.click(screen.getByRole('button', { name: /Place base/ }))
    expect(useItemDraft.getState().source).toBe('base')
    enter()
    submit()
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2))
    const second = fetchMock.mock.calls[1]?.[1]?.signal
    view.unmount()
    expect(second?.aborted).toBe(true)
  })
  it('uses English after a previous Korean locale was saved', async () => {
    localStorage.setItem('exile-hephaistos.locale', 'ko')
    show()
    enter()
    submit()
    await screen.findByRole('heading', { name: 'Synthetic Name' })
    fireEvent.click(screen.getByRole('button', { name: 'Edit item' }))
    expect(screen.getByLabelText('Item text copied from the game')).toHaveValue(
      raw,
    )
    expect(
      screen.queryByRole('combobox', { name: 'Language' }),
    ).not.toBeInTheDocument()
    expect(document.documentElement.lang).toBe('en')
  })
})
