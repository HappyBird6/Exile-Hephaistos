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
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { CraftingPage } from '../../CraftingPage'
import type { AbyssState } from '../../AbyssPanel'
import { useItemDraft } from '../../draft'
import {
  currentFilm,
  currentFrame,
  emptyFilms,
  historyStorageKey,
  LocalFilmRepository,
  startFilm,
} from '../../workbenchHistory'
import {
  currencyButton,
  outerMaterialTab,
} from '../../../../shared/test/workbenchQueries'
import { fixtureFetch } from '../../../../shared/test/craftingFixtures'
import { locales, setLocale } from '../../../../shared/i18n/i18n'
import { chaosRepeatControlMessages } from '../controls'
import type { ChaosRepeatCurrencyId } from '../controls'
import type { ChaosRepeatRequest } from '../api'
import {
  batchResponse,
  envelopeFor,
  repeatInitial,
  requestFor,
} from '../api/fixtures.test-support'
import { repeatItem } from '../api/stateValidation'
import { chaosRepeatResultMessage } from './messages'

let client: QueryClient
let requests: ChaosRepeatRequest[]
let paths: string[]
let manualReply: ((body: string) => Promise<Response>) | undefined
let reply: (
  request: ChaosRepeatRequest,
  body: string,
) => Response | Promise<Response>
beforeEach(() => {
  localStorage.clear()
  setLocale('en')
  useItemDraft.getState().setBase()
  client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  client.setQueryData(
    ['crafting', 'initial', 'solar', 82, 'manual'],
    repeatInitial,
  )
  requests = []
  paths = []
  manualReply = undefined
  reply = (request, body) => batchResponse(positive(request, body))
  vi.stubGlobal('crypto', webcrypto)
  vi.stubGlobal('fetch', (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input)
    paths.push(url)
    if (url.endsWith('/abyss/select') && manualReply)
      return manualReply(init!.body as string)
    if (url.endsWith('/chaos-repeat')) {
      const body = init!.body as string
      const request = JSON.parse(body) as ChaosRepeatRequest
      requests.push(request)
      return Promise.resolve().then(() => reply(request, body))
    }
    if (url.endsWith('/abyss/validate'))
      return Promise.resolve(
        batchResponse(JSON.parse(init!.body as string).state),
      )
    if (url.endsWith('/abyss/eligible'))
      return Promise.resolve(batchResponse(['s']))
    return fixtureFetch(input, init)
  })
  seed()
})
afterEach(() => {
  client.clear()
  vi.restoreAllMocks()
  vi.unstubAllGlobals()
})
function seed(kind: 'CONCRETE' | 'ABYSS' = 'CONCRETE') {
  const request = requestFor(kind)
  const history = startFilm(
    emptyFilms(),
    repeatItem(request.state),
    'original',
    repeatInitial.rulesetIdentity,
  )
  if (request.state.kind === 'ABYSS')
    history.films[0]!.frames[0]!.abyss = request.state.abyss
  new LocalFilmRepository(localStorage).save(history)
  useItemDraft.getState().setActiveOmens(request.activeOmens)
  return history
}
function positive(request: ChaosRepeatRequest, body: string, uses = 3) {
  const value = envelopeFor(request, body)
  const consumed = request.activeOmens.includes('Omen_of_Sinistral_Erasure')
    ? ['Omen_of_Sinistral_Erasure']
    : []
  value.actualUses = uses
  value.consumedOmens = Object.fromEntries(consumed.map((id) => [id, 1]))
  value.remainingOmens = request.activeOmens.filter(
    (id) => !consumed.includes(id),
  )
  value.lastAppliedWitness!.remainingOmens = [...value.remainingOmens]
  value.lastAppliedWitness!.consumedOmens = uses === 1 ? consumed : []
  return value
}
function show() {
  return render(
    <QueryClientProvider client={client}>
      <CraftingPage />
    </QueryClientProvider>,
  )
}
const slot = () => document.querySelector<HTMLButtonElement>('.item-slot')!
const controlRegion = () =>
  document.querySelector<HTMLElement>('.chaos-repeat-controls')!
const stored = () => new LocalFilmRepository(localStorage).load()
const notation = () =>
  document.querySelector<HTMLElement>('.wb-session-notation')!
const shared = () =>
  document.querySelector<HTMLElement>('.workbench-feedback-tooltip')!
function configure(id: ChaosRepeatCurrencyId = 'Chaos_Orb', maximum = '100') {
  const toggle = controlRegion().querySelector<HTMLButtonElement>('button')!
  if (toggle.getAttribute('aria-pressed') === 'false') fireEvent.click(toggle)
  const inputs = controlRegion().querySelectorAll('input')
  fireEvent.change(inputs[0]!, { target: { value: '.' } })
  fireEvent.change(inputs[1]!, { target: { value: maximum } })
  fireEvent.contextMenu(
    document.querySelector(
      `.currency-catalog [data-material-tooltip="${id}"]`,
    )!,
  )
}
async function finished(uses = 3) {
  await waitFor(() =>
    expect(currentFrame(stored())?.chaosRepeat?.uses).toBe(uses),
  )
  await waitFor(() => expect(slot()).toBeEnabled())
}

it.each([
  ['Chaos_Orb', 'CHAOS', 'CONCRETE'],
  ['Greater_Chaos_Orb', 'GREATER_CHAOS', 'CONCRETE'],
  ['Perfect_Chaos_Orb', 'PERFECT_CHAOS', 'CONCRETE'],
  ['Chaos_Orb', 'CHAOS', 'ABYSS'],
  ['Greater_Chaos_Orb', 'GREATER_CHAOS', 'ABYSS'],
  ['Perfect_Chaos_Orb', 'PERFECT_CHAOS', 'ABYSS'],
] as const)(
  'applies one %s %s batch to %s and saves one final frame with confirmed omens',
  async (id, action, kind) => {
    const before = seed(kind)
    show()
    if (kind === 'ABYSS')
      await waitFor(() =>
        expect(screen.getByRole('button', { name: 'Reveal' })).toBeEnabled(),
      )
    configure(id)
    fireEvent.click(slot())
    await finished()
    expect(requests).toHaveLength(1)
    expect(requests[0]).toMatchObject({
      action,
      state: { kind },
      maximumUses: 100,
      pattern: '.',
      locale: 'en',
    })
    const frames = currentFilm(stored())!.frames
    expect(frames).toHaveLength(2)
    expect(frames[0]).toEqual(currentFrame(before))
    expect(frames[1]!.chaosRepeat).toEqual({ version: 1, action, uses: 3 })
    expect(frames[1]!.state.explicits[0]!.values.life).toBe(17)
    expect(useItemDraft.getState().activeOmens).toEqual([
      'Omen_of_Sinistral_Necromancy',
    ])
    expect(within(notation()).getAllByRole('listitem')).toHaveLength(1)
    expect(notation()).toHaveTextContent('3 uses')
    expect(document.querySelector('.workbench-feedback-panel')).toHaveAttribute(
      'role',
      'status',
    )
    if (kind === 'ABYSS') {
      expect(frames[1]!.abyss?.slots.find((s) => s.id === 'hidden')).toEqual(
        frames[0]!.abyss?.slots[1],
      )
      expect(frames[1]!.evidence).toBeUndefined()
      expect(frames[1]!.chaosRepeatWitness).not.toHaveProperty('events')
    }
  },
)

it.each(locales)(
  'uses the captured %s locale and still applies a first use when the starting text matches',
  async (locale) => {
    reply = (request, body) => batchResponse(positive(request, body, 1))
    act(() => setLocale(locale))
    show()
    configure()
    fireEvent.click(slot())
    await finished(1)
    expect(requests).toHaveLength(1)
    expect(requests[0]!.locale).toBe(locale)
    expect(
      document.querySelector('.workbench-feedback-panel'),
    ).toHaveTextContent(chaosRepeatResultMessage(locale, 'match', 1))
    expect(document.querySelector('.chaos-repeat-controls__spinner')).toBeNull()
  },
)

it('treats the limit as a normal final result with the exact confirmed count', async () => {
  reply = (request, body) =>
    batchResponse({ ...positive(request, body), termination: 'LIMIT' })
  show()
  configure('Chaos_Orb', '3')
  fireEvent.click(slot())
  await finished()
  expect(document.querySelector('.workbench-feedback-panel')).toHaveAttribute(
    'role',
    'status',
  )
  expect(document.querySelector('.workbench-feedback-panel')).toHaveTextContent(
    'Reached the limit after 3 Chaos uses.',
  )
})

it.each(['INAPPLICABLE', 'ERROR'] as const)(
  'applies only the confirmed partial final result for %s',
  async (termination) => {
    reply = (request, body) => {
      const value = positive(request, body, 2)
      value.termination = termination
      if (termination === 'INAPPLICABLE')
        value.lastRejection = {
          code: 'CURRENCY_INAPPLICABLE',
          reason: 'PRIVATE RAW REASON',
        }
      else value.failure = { code: 'REPEAT_EXECUTION_FAILED', stage: 'APPLY' }
      return batchResponse(
        termination === 'ERROR'
          ? { status: 500, partialOutcome: value }
          : value,
        termination === 'ERROR' ? 500 : 200,
      )
    }
    show()
    configure()
    fireEvent.click(slot())
    await finished(2)
    expect(currentFilm(stored())!.frames).toHaveLength(2)
    expect(document.querySelector('.workbench-feedback-panel')).toHaveAttribute(
      'role',
      'alert',
    )
    expect(
      document.querySelector('.workbench-feedback-panel'),
    ).toHaveTextContent('Stopped after 2 confirmed Chaos uses.')
    expect(document.body).not.toHaveTextContent('PRIVATE RAW REASON')
    expect(useItemDraft.getState().activeOmens).toEqual([
      'Omen_of_Sinistral_Necromancy',
    ])
    expect(requests).toHaveLength(1)
  },
)

it.each(['INAPPLICABLE', 'ERROR'] as const)(
  'preserves the item, omens and stored bytes when %s completes zero uses',
  async (termination) => {
    reply = (request, body) => {
      const value = positive(request, body)
      value.actualUses = 0
      value.finalState = structuredClone(request.state)
      value.consumedOmens = {}
      value.remainingOmens = [...request.activeOmens]
      value.lastAppliedWitness = null
      value.termination = termination
      if (termination === 'INAPPLICABLE')
        value.lastRejection = {
          code: 'CURRENCY_INAPPLICABLE',
          reason: 'PRIVATE ZERO REASON',
        }
      else value.failure = { code: 'REPEAT_EXECUTION_FAILED', stage: 'APPLY' }
      return batchResponse(
        termination === 'ERROR'
          ? { status: 500, partialOutcome: value }
          : value,
        termination === 'ERROR' ? 500 : 200,
      )
    }
    const before = localStorage.getItem(historyStorageKey)
    show()
    configure()
    fireEvent.click(slot())
    await waitFor(() =>
      expect(
        document.querySelector('.workbench-feedback-panel'),
      ).toHaveTextContent('No Chaos use completed.'),
    )
    expect(localStorage.getItem(historyStorageKey)).toBe(before)
    expect(useItemDraft.getState().activeOmens).toEqual(
      requestFor().activeOmens,
    )
    expect(document.body).not.toHaveTextContent('PRIVATE ZERO REASON')
    expect(requests).toHaveLength(1)
  },
)

it.each(['network', 'damaged', 'rejected'] as const)(
  'does not estimate a count, retry or fall back after a %s result',
  async (kind) => {
    reply = (request, body) => {
      if (kind === 'network')
        return Promise.reject(new Error('PRIVATE NETWORK'))
      if (kind === 'rejected')
        return batchResponse({ detail: 'PRIVATE SERVER REASON' }, 422)
      const value = positive(request, body)
      value.requestBinding.requestSha256 = 'wrong'
      return batchResponse(value)
    }
    const before = localStorage.getItem(historyStorageKey)
    show()
    configure()
    fireEvent.click(slot())
    const message = chaosRepeatResultMessage(
      'en',
      kind === 'network'
        ? 'uncertain'
        : kind === 'damaged'
          ? 'invalid'
          : 'rejected',
    )
    await waitFor(() =>
      expect(
        document.querySelector('.workbench-feedback-panel'),
      ).toHaveTextContent(message),
    )
    expect(localStorage.getItem(historyStorageKey)).toBe(before)
    expect(useItemDraft.getState().activeOmens).toEqual(
      requestFor().activeOmens,
    )
    expect(currentFilm(stored())!.frames).toHaveLength(1)
    expect(requests).toHaveLength(1)
    expect(document.body).not.toHaveTextContent('PRIVATE')
  },
)

it('undoes the whole batch and preserves the original future when branching from its before state', async () => {
  show()
  configure()
  fireEvent.click(slot())
  await finished()
  const original = structuredClone(currentFilm(stored())!)
  fireEvent.click(
    screen.getByRole('button', { name: 'Previous crafting step' }),
  )
  expect(currentFrame(stored())?.state.explicits[0]!.values.life).toBe(10)
  expect(currentFilm(stored())!.frames).toHaveLength(2)
  configure('Perfect_Chaos_Orb')
  fireEvent.click(slot())
  await waitFor(() => expect(stored().films).toHaveLength(2))
  expect(stored().films.find((film) => film.id === original.id)).toEqual(
    original,
  )
  expect(currentFilm(stored())!.frames).toHaveLength(2)
  expect(currentFrame(stored())!.chaosRepeat).toMatchObject({
    action: 'PERFECT_CHAOS',
    uses: 3,
  })
  expect(within(notation()).getAllByRole('listitem')).toHaveLength(1)
})

it('locks mutations synchronously, retains the selected tooltip and animates only during the request', async () => {
  let complete!: (response: Response) => void
  let captured!: { request: ChaosRepeatRequest; body: string }
  reply = (request, body) => {
    captured = { request, body }
    return new Promise<Response>((resolve) => {
      complete = resolve
    })
  }
  show()
  fireEvent.click(screen.getByRole('button', { name: 'Select base' }))
  const level = screen.getByRole('spinbutton', { name: 'Item level' })
  configure()
  const before = localStorage.getItem(historyStorageKey)
  const omens = [...useItemDraft.getState().activeOmens]
  const chaos = currencyButton('Chaos Orb')
  const target = slot()
  act(() => {
    target.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    target.dispatchEvent(new MouseEvent('click', { bubbles: true }))
  })
  await waitFor(() => expect(requests).toHaveLength(1))
  expect(controlRegion()).toHaveAttribute('aria-busy', 'true')
  expect(
    document.querySelector('.chaos-repeat-controls__spinner'),
  ).toBeInTheDocument()
  for (const name of [
    'Edit item',
    'New craft',
    'Select base',
    'History',
    'Language: English',
  ])
    for (const button of screen.getAllByRole('button', { name }))
      expect(button).toBeDisabled()
  expect(level).toBeDisabled()
  expect(outerMaterialTab('Essence')).toBeDisabled()
  expect(
    screen.getByRole('button', { name: 'Favorite slot 1: empty' }),
  ).toBeDisabled()
  fireEvent.change(level, { target: { value: '40' } })
  expect(level).toHaveValue(82)
  fireEvent.contextMenu(currencyButton('Divine Orb'))
  fireEvent.click(outerMaterialTab('Essence'))
  fireEvent.keyDown(outerMaterialTab('Currency'), { key: 'ArrowRight' })
  fireEvent.keyDown(document, { key: 'Escape' })
  const link = within(shared()).getByRole('link', { name: 'PoE2DB' })
  act(() => link.focus())
  fireEvent.keyDown(link, { key: 'Escape' })
  expect(link).toHaveFocus()
  expect(chaos).toHaveAttribute('aria-pressed', 'true')
  expect(shared()).toHaveTextContent(chaosRepeatControlMessages.en.hint)
  expect(localStorage.getItem(historyStorageKey)).toBe(before)
  expect(useItemDraft.getState().activeOmens).toEqual(omens)
  expect(requests).toHaveLength(1)
  complete(batchResponse(positive(captured.request, captured.body)))
  await finished()
  expect(document.querySelector('.chaos-repeat-controls__spinner')).toBeNull()
})

it.each(['unmount', 'base', 'locale', 'catalog', 'omens'] as const)(
  'discards a late result after %s changes',
  async (change) => {
    let complete!: (response: Response) => void
    let captured!: { request: ChaosRepeatRequest; body: string }
    reply = (request, body) => {
      captured = { request, body }
      return new Promise<Response>((resolve) => {
        complete = resolve
      })
    }
    const mounted = show()
    configure()
    fireEvent.click(slot())
    await waitFor(() => expect(requests).toHaveLength(1))
    const before = localStorage.getItem(historyStorageKey)
    if (change === 'unmount') mounted.unmount()
    else
      act(() => {
        if (change === 'base') useItemDraft.getState().setBase()
        if (change === 'locale') setLocale('ja')
        if (change === 'omens') useItemDraft.getState().setActiveOmens([])
        if (change === 'catalog')
          client.setQueryData(['crafting', 'initial', 'solar', 82, 'manual'], {
            ...repeatInitial,
            modifiers: {
              ...repeatInitial.modifiers,
              p: {
                ...repeatInitial.modifiers.p!,
                stats: [{ id: 'life', min: 18, max: 20 }],
              },
            },
          })
      })
    const activeOmens = [...useItemDraft.getState().activeOmens]
    await act(async () => {
      complete(batchResponse(positive(captured.request, captured.body)))
      await new Promise((resolve) => setTimeout(resolve, 20))
    })
    expect(localStorage.getItem(historyStorageKey)).toBe(before)
    expect(useItemDraft.getState().activeOmens).toEqual(activeOmens)
    if (change !== 'unmount')
      expect(
        document.querySelector('.chaos-repeat-controls__spinner'),
      ).toBeNull()
  },
)

it('blocks Abyss manual selection during a batch while retaining the complete before state', async () => {
  seed('ABYSS')
  let complete!: (response: Response) => void
  let captured!: { request: ChaosRepeatRequest; body: string }
  reply = (request, body) => {
    captured = { request, body }
    return new Promise<Response>((resolve) => {
      complete = resolve
    })
  }
  show()
  const reveal = await screen.findByRole('button', { name: 'Reveal' })
  await waitFor(() => expect(reveal).toBeEnabled())
  fireEvent.click(reveal)
  const candidate = screen.getByRole('combobox', { name: 'Candidate 1' })
  await within(candidate).findByRole('option', { name: /Strength/ })
  fireEvent.change(candidate, { target: { value: 's' } })
  configure()
  const before = localStorage.getItem(historyStorageKey)
  fireEvent.click(slot())
  await waitFor(() => expect(requests).toHaveLength(1))
  expect(reveal).toBeDisabled()
  expect(candidate).toBeDisabled()
  const apply = screen.getByRole('button', { name: 'Apply chosen modifier' })
  expect(apply).toBeDisabled()
  fireEvent.click(apply)
  expect(paths.some((path) => path.endsWith('/abyss/select'))).toBe(false)
  expect(localStorage.getItem(historyStorageKey)).toBe(before)
  complete(batchResponse(positive(captured.request, captured.body)))
  await finished()
  expect(currentFrame(stored())!.abyss?.slots[1]!.modifier).toBeNull()
})

it('locks the saved-session editor and rejects forced submit while a batch is pending', async () => {
  let complete!: (response: Response) => void
  let captured!: { request: ChaosRepeatRequest; body: string }
  reply = (request, body) => {
    captured = { request, body }
    return new Promise<Response>((resolve) => {
      complete = resolve
    })
  }
  show()
  fireEvent.click(screen.getByRole('button', { name: 'History' }))
  fireEvent.click(screen.getByRole('button', { name: /^Rename:/ }))
  const title = screen.getByRole('textbox', { name: /^Session title:/ })
  fireEvent.change(title, { target: { value: 'Uncommitted title' } })
  configure()
  const before = localStorage.getItem(historyStorageKey)
  fireEvent.click(slot())
  await waitFor(() => expect(requests).toHaveLength(1))
  expect(title).toBeDisabled()
  expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled()
  fireEvent.submit(title.closest('form')!)
  expect(localStorage.getItem(historyStorageKey)).toBe(before)
  complete(batchResponse(positive(captured.request, captured.body)))
  await finished()
  expect(currentFilm(stored())!.title).toBeUndefined()
})

it('blocks a same-event Chaos request while Abyss selection owns the mutation, then retains the revealed slot', async () => {
  seed('ABYSS')
  let complete!: (response: Response) => void
  let manual!: {
    state: AbyssState
    slotId: string
    candidate: AbyssState['slots'][number]['modifier']
  }
  manualReply = (body) => {
    manual = JSON.parse(body) as typeof manual
    return new Promise<Response>((resolve) => {
      complete = resolve
    })
  }
  show()
  const reveal = await screen.findByRole('button', { name: 'Reveal' })
  await waitFor(() => expect(reveal).toBeEnabled())
  fireEvent.click(reveal)
  const candidate = screen.getByRole('combobox', { name: 'Candidate 1' })
  await within(candidate).findByRole('option', { name: /Strength/ })
  fireEvent.change(candidate, { target: { value: 's' } })
  configure()
  const manualButton = screen.getByRole('button', {
    name: 'Apply chosen modifier',
  })
  const target = slot()
  act(() => {
    manualButton.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    target.dispatchEvent(new MouseEvent('click', { bubbles: true }))
  })
  await waitFor(() =>
    expect(paths.filter((path) => path.endsWith('/abyss/select'))).toHaveLength(
      1,
    ),
  )
  expect(requests).toHaveLength(0)
  expect(slot()).toBeDisabled()
  complete(
    batchResponse({
      ...manual.state,
      offer: null,
      slots: manual.state.slots.map((slot) =>
        slot.id === manual.slotId
          ? { ...slot, modifier: manual.candidate }
          : slot,
      ),
    }),
  )
  await waitFor(() =>
    expect(currentFrame(stored())!.action).toBe('ABYSS_SELECT'),
  )
  await waitFor(() => expect(slot()).toBeEnabled())
  fireEvent.click(slot())
  await finished()
  expect(requests).toHaveLength(1)
  expect(currentFilm(stored())!.frames).toHaveLength(3)
  expect(currentFrame(stored())!.abyss?.slots[1]!.modifier).toEqual(
    manual.candidate,
  )
})

it('does not publish the same successful requestId twice', async () => {
  vi.spyOn(globalThis.crypto, 'randomUUID').mockReturnValue(
    '123e4567-e89b-12d3-a456-426614174000',
  )
  show()
  configure()
  fireEvent.click(slot())
  await finished()
  const before = localStorage.getItem(historyStorageKey)
  configure()
  fireEvent.click(slot())
  await waitFor(() => expect(requests).toHaveLength(2))
  await waitFor(() => expect(slot()).toBeEnabled())
  expect(localStorage.getItem(historyStorageKey)).toBe(before)
  expect(currentFilm(stored())!.frames).toHaveLength(2)
})
