import {
  currencyButton,
  outerMaterialTab,
} from '../../shared/test/workbenchQueries'
import { webcrypto } from 'node:crypto'
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
import { CraftingPage } from './CraftingPage'
import { useItemDraft } from './draft'
import { fixtureFetch } from '../../shared/test/craftingFixtures'
import type { Item, TextLine } from './itemModels'
import {
  currentFilm,
  currentFrame,
  emptyFilms,
  historyStorageKey,
  LocalFilmRepository,
  startFilm,
} from './workbenchHistory'
import {
  batchResponse,
  envelopeFor,
  repeatInitial,
  requestFor,
} from './chaos-repeat/api/fixtures.test-support'
import { repeatItem } from './chaos-repeat/api/stateValidation'
import type { ChaosRepeatRequest } from './chaos-repeat/api'

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
    headers: {
      'X-Crafting-Ruleset': 'fixture-ruleset',
      'Content-Type': 'application/json',
    },
  })
let client: QueryClient
let fetchMock: ReturnType<typeof vi.fn<typeof fetch>>
beforeEach(() => {
  useItemDraft.getState().setBase()
  client = new QueryClient({ defaultOptions: { mutations: { retry: false } } })
  fetchMock = vi.fn<typeof fetch>().mockResolvedValue(json(result()))
  vi.stubGlobal('fetch', (input: RequestInfo | URL, init?: RequestInit) =>
    String(input).includes('/api/v1/crafting/')
      ? fixtureFetch(input, init)
      : fetchMock(input, init),
  )
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
      <CraftingPage />
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
  fireEvent.change(
    screen.getByRole('textbox', { name: 'Item text copied from the game' }),
    { target: { value: text } },
  )
}
function submit() {
  fireEvent.click(screen.getByRole('button', { name: /Analyze item/ }))
}

function deferParse() {
  let resolve!: (response: Response) => void
  let reject!: (error: Error) => void
  fetchMock.mockImplementationOnce(
    () =>
      new Promise<Response>((done, fail) => {
        resolve = done
        reject = fail
      }),
  )
  return {
    finish: async (outcome: 'success' | 'error') => {
      await act(async () => {
        if (outcome === 'success') resolve(json(result()))
        else reject(new Error('old parse failed'))
        await new Promise((done) => setTimeout(done, 0))
      })
    },
  }
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
    expect(
      document.querySelector('.craft-page > .sr-only[role="status"]'),
    ).toHaveTextContent('Analyzing item')
    await act(async () => {
      reject(new TypeError('offline'))
    })
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Could not connect',
    )
    expect(screen.getByRole('alert')).toHaveTextContent('Could not connect')
    fireEvent.change(
      screen.getByRole('textbox', { name: 'Item text copied from the game' }),
      {
        target: { value: raw + 'changed' },
      },
    )
    fireEvent.click(currencyButton('Orb of Transmutation'))
    expect(
      document.querySelector('.craft-page > .sr-only[role="status"]'),
    ).toHaveTextContent('Orb of Transmutation selected')
    fireEvent.keyDown(window, { key: 'Escape' })
    expect(
      document.querySelector('.craft-page > .sr-only[role="status"]'),
    ).toHaveTextContent('Currency selection cleared')
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
    fireEvent.click(currencyButton('Chaos Orb'))
    fireEvent.click(
      screen.getByRole('button', {
        name: 'Use selected currency on the central item',
      }),
    )
    expect(
      document.querySelector('.craft-page > .sr-only[role="status"]'),
    ).toHaveTextContent('Pasted items are display-only.')
    expect(useItemDraft.getState().text).toBe(raw)
    expect(useItemDraft.getState().currentText.text).toBe(raw)
  })
  it('rejects blank and oversized UTF-8 input before requests', () => {
    show()
    enter(' ')
    submit()
    expect(screen.getByRole('alert')).toHaveTextContent('Enter item text')
    fireEvent.change(
      screen.getByRole('textbox', { name: 'Item text copied from the game' }),
      {
        target: { value: '가'.repeat(5500) },
      },
    )
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
    expect(
      document.querySelector('.craft-page > .sr-only[role="status"]'),
    ).toHaveTextContent('Analyzing item')
    const signal = fetchMock.mock.calls[0]?.[1]?.signal
    const next = raw + 'new input'
    fireEvent.change(
      screen.getByRole('textbox', { name: 'Item text copied from the game' }),
      { target: { value: next } },
    )
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
    fireEvent.click(screen.getByRole('button', { name: 'Close item input' }))
    fireEvent.click(screen.getByRole('button', { name: 'Select base' }))
    expect(first?.aborted).toBe(true)
    const revision = useItemDraft.getState().baseRevision
    fireEvent.click(
      within(screen.getByRole('dialog', { name: 'Select base' })).getByRole(
        'button',
        { name: 'Select base' },
      ),
    )
    await waitFor(() =>
      expect(useItemDraft.getState().baseRevision).toBe(revision + 1),
    )
    expect(useItemDraft.getState().source).toBe('base')
    enter()
    submit()
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2))
    const second = fetchMock.mock.calls[1]?.[1]?.signal
    view.unmount()
    expect(second?.aborted).toBe(true)
  })
  it.each([
    ['button', 'success'],
    ['button', 'error'],
    ['escape', 'success'],
    ['escape', 'error'],
    ['cancel', 'success'],
    ['cancel', 'error'],
    ['backdrop', 'success'],
    ['backdrop', 'error'],
  ] as const)(
    'allows %s cancellation while parsing and discards the old %s outcome',
    async (method, outcome) => {
      const pending = deferParse()
      show()
      enter()
      const original = useItemDraft.getState().currentText
      const bytes = localStorage.getItem(historyStorageKey)
      submit()
      await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1))
      const signal = fetchMock.mock.calls[0]![1]!.signal
      const modal = screen.getByRole('dialog', { name: 'Starting item' })
      const close = within(modal).getByRole('button', {
        name: 'Close item input',
      })
      expect(close).toBeEnabled()
      expect(
        screen.getByRole('textbox', { name: 'Item text copied from the game' }),
      ).toBeEnabled()
      expect(
        within(modal).getByRole('button', { name: /Analyzing item/ }),
      ).toBeDisabled()
      expect(
        screen.getByRole('button', {
          name: 'Use selected currency on the central item',
        }),
      ).toBeDisabled()
      if (method === 'button') fireEvent.click(close)
      if (method === 'escape') fireEvent.keyDown(window, { key: 'Escape' })
      if (method === 'cancel')
        fireEvent(modal, new Event('cancel', { cancelable: true }))
      if (method === 'backdrop') fireEvent.click(modal)
      expect(signal?.aborted).toBe(true)
      expect(screen.queryByRole('dialog', { name: 'Starting item' })).toBeNull()
      expect(screen.getByRole('button', { name: 'Edit item' })).toHaveFocus()
      await pending.finish(outcome)
      expect(useItemDraft.getState().currentText).toBe(original)
      expect(useItemDraft.getState().text).toBe(raw)
      expect(useItemDraft.getState().source).toBe('base')
      expect(localStorage.getItem(historyStorageKey)).toBe(bytes)
      expect(client.getQueryData(['workbench', 'current-item'])).toBeUndefined()
      expect(screen.queryByRole('alert')).toBeNull()
    },
  )

  it.each(['selected', 'held'] as const)(
    'keeps a newer %s material when a cancelled parse finishes in the same original item context',
    async (kind) => {
      const pending = deferParse()
      show()
      enter()
      submit()
      await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1))
      fireEvent.click(screen.getByRole('button', { name: 'Close item input' }))
      // Reopen the same raw input: source/text/session equality alone cannot
      // distinguish this cancelled request from its replacement editor lifetime.
      fireEvent.click(screen.getByRole('button', { name: 'Edit item' }))
      let material: HTMLElement
      if (kind === 'held') {
        fireEvent.click(outerMaterialTab('Essence'))
        material = currencyButton('Greater Essence of the Body')
        fireEvent.click(material)
        expect(material).toHaveClass('is-held')
      } else {
        material = currencyButton('Divine Orb')
        fireEvent.contextMenu(material)
        expect(material).toHaveAttribute('aria-pressed', 'true')
      }
      const before = document.querySelector(
        '.workbench-feedback-tooltip',
      )!.textContent
      await pending.finish('success')
      expect(
        document.querySelector('.workbench-feedback-tooltip'),
      ).toHaveTextContent(before!)
      if (kind === 'held') expect(material).toHaveClass('is-held')
      else expect(material).toHaveAttribute('aria-pressed', 'true')
      expect(
        screen.getByRole('dialog', { name: 'Starting item' }),
      ).toBeVisible()
      expect(
        screen.getByRole('textbox', { name: 'Item text copied from the game' }),
      ).toHaveValue(raw)
      expect(
        screen.queryByRole('heading', { name: 'Synthetic Name' }),
      ).toBeNull()
    },
  )

  it.each([
    ['during', 'success'],
    ['during', 'error'],
    ['after', 'success'],
    ['after', 'error'],
  ] as const)(
    'preserves Chaos state when a cancelled parse returns %s the batch with %s',
    async (when, outcome) => {
      const root = requestFor()
      const history = startFilm(
        emptyFilms(),
        repeatItem(root.state),
        'parse-cancel-root',
        repeatInitial.rulesetIdentity,
      )
      new LocalFilmRepository(localStorage).save(history)
      useItemDraft.getState().setActiveOmens(root.activeOmens)
      client.setQueryData(
        ['crafting', 'initial', 'solar', 82, 'manual'],
        repeatInitial,
      )
      vi.stubGlobal('crypto', webcrypto)
      const pending = deferParse()
      let batch!: {
        request: ChaosRepeatRequest
        body: string
        signal: AbortSignal
      }
      let complete!: (response: Response) => void
      let batchCalls = 0
      const originalFetch = globalThis.fetch
      vi.stubGlobal('fetch', (input: RequestInfo | URL, init?: RequestInit) => {
        if (!String(input).endsWith('/chaos-repeat'))
          return originalFetch(input, init)
        batchCalls++
        const body = init!.body as string
        batch = {
          body,
          request: JSON.parse(body) as ChaosRepeatRequest,
          signal: init!.signal as AbortSignal,
        }
        return new Promise<Response>((resolve) => {
          complete = resolve
        })
      })
      show()
      enter()
      submit()
      await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1))
      const parseSignal = fetchMock.mock.calls[0]![1]!.signal
      fireEvent.click(screen.getByRole('button', { name: 'Close item input' }))
      expect(parseSignal?.aborted).toBe(true)
      fireEvent.click(screen.getByRole('button', { name: 'Edit item' }))
      fireEvent.click(
        screen.getByRole('button', { name: '1000 x Chaos Orb Use' }),
      )
      fireEvent.change(screen.getByRole('textbox', { name: 'Pattern' }), {
        target: { value: '.' },
      })
      const chaos = currencyButton('Chaos Orb')
      fireEvent.contextMenu(chaos)
      const bytes = localStorage.getItem(historyStorageKey)
      const original = useItemDraft.getState().currentText
      fireEvent.click(
        screen.getByRole('button', {
          name: 'Use selected currency on the central item',
        }),
        { shiftKey: true },
      )
      await waitFor(() => expect(batchCalls).toBe(1))
      const modal = screen.getByRole('dialog', { name: 'Starting item' })
      const text = within(modal).getByRole('textbox', {
        name: 'Item text copied from the game',
      })
      const close = within(modal).getByRole('button', {
        name: 'Close item input',
      })
      expect(text).toBeDisabled()
      expect(close).toBeDisabled()
      fireEvent.change(text, { target: { value: 'blocked edit' } })
      fireEvent.click(close)
      fireEvent(modal, new Event('cancel', { cancelable: true }))
      fireEvent.keyDown(window, { key: 'Escape' })
      expect(modal).toHaveAttribute('open')
      expect(text).toHaveValue(raw)
      expect(batch.signal.aborted).toBe(false)
      if (when === 'during') {
        await pending.finish(outcome)
        expect(chaos).toHaveAttribute('aria-pressed', 'true')
        expect(
          document.querySelector('.workbench-feedback-tooltip'),
        ).toHaveTextContent('Chaos Orb')
        expect(localStorage.getItem(historyStorageKey)).toBe(bytes)
        expect(useItemDraft.getState().activeOmens).toEqual(root.activeOmens)
      }
      complete(batchResponse(envelopeFor(batch.request, batch.body)))
      await waitFor(() =>
        expect(
          currentFrame(new LocalFilmRepository(localStorage).load())
            ?.chaosRepeat?.uses,
        ).toBe(3),
      )
      await waitFor(() => expect(text).toBeEnabled())
      const completed = localStorage.getItem(historyStorageKey)
      if (when === 'after') await pending.finish(outcome)
      expect(localStorage.getItem(historyStorageKey)).toBe(completed)
      expect(
        currentFilm(new LocalFilmRepository(localStorage).load())!.frames,
      ).toHaveLength(2)
      expect(chaos).toHaveAttribute('aria-pressed', 'true')
      expect(
        document.querySelector('.workbench-feedback-tooltip'),
      ).toHaveTextContent('Chaos Orb')
      expect(useItemDraft.getState().currentText).toBe(original)
      expect(useItemDraft.getState().activeOmens).toEqual([
        'Omen_of_Sinistral_Necromancy',
      ])
      expect(modal).toHaveAttribute('open')
      expect(text).toHaveValue(raw)
      expect(batchCalls).toBe(1)
      expect(client.getQueryData(['workbench', 'current-item'])).toBeUndefined()
    },
  )
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
      screen.getByRole('button', { name: 'Language: English' }),
    ).toHaveAttribute('aria-expanded', 'false')
    expect(document.documentElement.lang).toBe('en')
  })
})
