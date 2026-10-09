import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { BasicPaths } from './BasicPaths'
import type { PathRequest, Provenance } from './api'
import {
  initialFixture,
  jsonResponse,
} from '../../../shared/test/craftingFixtures'
import { concreteInitial } from '../workbenchApi'
import { setLocale } from '../../../shared/i18n/i18n'

const provenance: Provenance = {
  rulesetIdentity: initialFixture.rulesetIdentity,
  catalogDigest: 'a'.repeat(64),
  modelVersion: 'model',
  ruleVersion: 'rules',
  ledgerVersion: 'ledger',
  weightPolicy: 'POE2DB_AS_PUBLISHED',
  sourceUrl: 'https://poe2db.tw/us/Amulets',
  retrievedAt: '2026-10-09',
  rawSha256: 'b'.repeat(64),
  detailsSha256: 'c'.repeat(64),
}
const initial = {
  ...initialFixture,
  state: {
    ...initialFixture.state,
    baseItemId: 'Metadata/Items/Amulets/FourAmulet9',
  },
  modifiers: {
    ...initialFixture.modifiers,
    p: { ...initialFixture.modifiers.p!, layer: 'EXPLICIT' },
    s: { ...initialFixture.modifiers.s!, layer: 'EXPLICIT' },
  },
}
const item = concreteInitial(initial)
const zero = { numerator: '0', denominator: '1' },
  one = { numerator: '1', denominator: '1' }
function response(request: PathRequest, recovery = false, partial = false) {
  return jsonResponse({
    purpose: recovery ? 'CONDITIONAL_RECOVERY' : 'MAIN_FIRST_HIT',
    request,
    provenance,
    interpretation: 'Declared simulator model',
    points: request.observations.map((attempts) => ({
      attempts: String(attempts),
      lower: partial ? zero : { numerator: '3', denominator: '4' },
      upper: partial ? one : { numerator: '3', denominator: '4' },
      active: partial ? zero : { numerator: '1', denominator: '4' },
      dead: zero,
      unresolved: partial ? one : zero,
      status: partial ? 'UNKNOWN' : 'COMPLETE',
      reachability: partial ? 'UNKNOWN' : 'REACHABLE_WITHIN_OBSERVATION',
    })),
    blockers: [],
    blockersTruncated: false,
    recoveryIncludedInMain: false,
    reason: partial ? 'TRANSITION_UNRESOLVED' : '',
    computationLimits: {},
    evaluations: 0,
    peakFrontier: 1,
    peakFractionBits: 1,
    fractionMetricScope: 'STORED',
    renewalProof: null,
  })
}
function view(revision = 'root', customItem = item) {
  return (
    <BasicPaths
      active
      item={customItem}
      initial={initial}
      valid
      revision={revision}
      activeOmens={[]}
    />
  )
}
function mount() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  )
  return { ...render(view(), { wrapper }), client }
}
async function open() {
  const summary = screen.getByText('Experimental basic-currency probability')
  const details = summary.closest('details')!
  details.open = true
  fireEvent(details, new Event('toggle'))
  await waitFor(() =>
    expect(
      screen.getByRole('button', {
        name: 'Save current item as failure state',
      }),
    ).toBeEnabled(),
  )
  fireEvent.change(
    within(
      screen.getByRole('region', { name: 'Main first-hit probability' }),
    ).getByLabelText('Exact modifier tier'),
    { target: { value: 'p' } },
  )
}
beforeEach(() => {
  setLocale('en')
  vi.stubGlobal(
    'fetch',
    vi.fn(async (url: string, init?: RequestInit) => {
      if (url.endsWith('/provenance')) return jsonResponse(provenance)
      return response(
        JSON.parse(String(init?.body)) as PathRequest,
        url.endsWith('/recovery'),
      )
    }),
  )
})
afterEach(() => vi.unstubAllGlobals())
describe('experimental basic paths', () => {
  it('returns keyboard focus to the action selector after removing an action', async () => {
    mount()
    await open()
    const main = screen.getByRole('region', {
      name: 'Main first-hit probability',
    })
    fireEvent.click(
      within(main).getByRole('button', { name: 'Remove action 1' }),
    )
    await waitFor(() =>
      expect(
        within(main).getByLabelText('Append action (16 supported actions)'),
      ).toHaveFocus(),
    )
    expect(
      screen.getByRole('button', { name: 'Calculate main probability' }),
    ).toBeDisabled()
  })
  it('blocks duplicate requests and permits an explicit retry after a service failure', async () => {
    let failed = true
    vi.mocked(fetch).mockImplementation(async (url, init) => {
      if (String(url).endsWith('/provenance')) return jsonResponse(provenance)
      if (failed) return jsonResponse({}, 503)
      return response(JSON.parse(String(init?.body)) as PathRequest)
    })
    mount()
    await open()
    const calculate = screen.getByRole('button', {
      name: 'Calculate main probability',
    })
    fireEvent.click(calculate)
    fireEvent.click(calculate)
    expect(await screen.findByRole('alert')).toHaveTextContent('503')
    expect(
      vi
        .mocked(fetch)
        .mock.calls.filter(([url]) => String(url).endsWith('/first-hit')),
    ).toHaveLength(1)
    failed = false
    fireEvent.click(calculate)
    expect(await screen.findByText('100 — COMPLETE')).toBeVisible()
  })
  it('discards old results on provenance refetch and never resurrects them when provenance returns', async () => {
    mount()
    await open()
    fireEvent.click(
      screen.getByRole('button', { name: 'Calculate main probability' }),
    )
    await screen.findByText('100 — COMPLETE')
    vi.mocked(fetch).mockImplementation(async () =>
      jsonResponse({ ...provenance, rulesetIdentity: 'changed-rules' }),
    )
    fireEvent.click(
      screen.getByRole('button', { name: 'Refresh probability provenance' }),
    )
    await waitFor(() =>
      expect(
        screen.getByRole('button', { name: 'Calculate main probability' }),
      ).toBeDisabled(),
    )
    expect(screen.queryByText('100 — COMPLETE')).toBeNull()
    vi.mocked(fetch).mockImplementation(async () => jsonResponse(provenance))
    fireEvent.click(
      screen.getByRole('button', { name: 'Refresh probability provenance' }),
    )
    await waitFor(() =>
      expect(
        screen.getByRole('button', { name: 'Calculate main probability' }),
      ).toBeEnabled(),
    )
    expect(screen.queryByText('100 — COMPLETE')).toBeNull()
  })
  it('keeps other bases unsupported rather than displaying a zero probability', async () => {
    const mounted = mount()
    mounted.rerender(
      view('ring', { ...item, baseItemId: 'Metadata/Items/Rings/FourRing1' }),
    )
    const details = screen
      .getByText('Experimental basic-currency probability')
      .closest('details')!
    details.open = true
    fireEvent(details, new Event('toggle'))
    expect(
      await screen.findByText(/Other bases are unsupported; this is not 0%/),
    ).toBeVisible()
    expect(
      screen.getByRole('button', { name: 'Calculate main probability' }),
    ).toBeDisabled()
    expect(
      vi
        .mocked(fetch)
        .mock.calls.every(([url]) => String(url).endsWith('/provenance')),
    ).toBe(true)
  })
  it('retains main and recovery in separate result panels when both explicit requests are evaluated', async () => {
    mount()
    await open()
    fireEvent.click(
      screen.getByRole('button', {
        name: 'Save current item as failure state',
      }),
    )
    const recovery = screen.getByRole('region', {
      name: 'Separate conditional recovery probability',
    })
    fireEvent.change(within(recovery).getByLabelText('Exact modifier tier'), {
      target: { value: 's' },
    })
    fireEvent.click(
      screen.getByRole('button', { name: 'Calculate main probability' }),
    )
    await screen.findByText('100 — COMPLETE')
    fireEvent.click(
      screen.getByRole('button', { name: 'Calculate conditional recovery' }),
    )
    await waitFor(() =>
      expect(
        screen.getAllByRole('region', { name: 'Probability results' }),
      ).toHaveLength(2),
    )
    expect(screen.getAllByText('500 — COMPLETE')).toHaveLength(2)
  })
  it('shows PARTIAL bounds and displays large fractions safely in the rendered result', async () => {
    vi.mocked(fetch).mockImplementation(async (url, init) => {
      if (String(url).endsWith('/provenance')) return jsonResponse(provenance)
      const data = await response(
        JSON.parse(String(init?.body)) as PathRequest,
      ).json()
      const denominator = 10n ** 5000n
      data.points = data.points.map((point: Record<string, unknown>) => ({
        ...point,
        lower: {
          numerator: String(denominator / 2n),
          denominator: String(denominator),
        },
        upper: one,
        active: zero,
        unresolved: { numerator: '1', denominator: '2' },
        status: 'PARTIAL',
      }))
      return jsonResponse(data)
    })
    mount()
    await open()
    fireEvent.click(
      screen.getByRole('button', { name: 'Calculate main probability' }),
    )
    expect(await screen.findByText('100 — PARTIAL')).toBeVisible()
    expect(screen.getAllByText('≈50%')).toHaveLength(6)
    expect(screen.queryByText('Exact success probability')).toBeNull()
  })
  it('uses the unchanged concrete item and exact catalog target, with observation points and repeated policy', async () => {
    mount()
    await open()
    fireEvent.click(
      screen.getByRole('button', { name: 'Calculate main probability' }),
    )
    expect(await screen.findByText('100 — COMPLETE')).toBeVisible()
    expect(screen.getByText('500 — COMPLETE')).toBeVisible()
    expect(screen.getAllByText('≈75%')).toHaveLength(3)
    const calls = vi.mocked(fetch).mock.calls
    const request = JSON.parse(
      String(
        calls.find(([url]) => String(url).endsWith('/first-hit'))?.[1]?.body,
      ),
    ) as PathRequest
    expect(request.start.item).toEqual(item)
    expect(request.target).toEqual({
      checkpoint: null,
      explicitModifierIds: ['p'],
    })
    expect(request.policy).toEqual({ actions: ['CHAOS'], mode: 'REPEAT_CYCLE' })
    expect(request.observations).toEqual([100, 300, 500])
  })
  it('keeps conditional recovery separate and requires explicit failure and checkpoint selection', async () => {
    mount()
    await open()
    const calculate = screen.getByRole('button', {
      name: 'Calculate conditional recovery',
    })
    expect(calculate).toBeDisabled()
    fireEvent.click(
      screen.getByRole('button', {
        name: 'Save current item as recovery checkpoint',
      }),
    )
    fireEvent.click(
      screen.getByRole('button', {
        name: 'Save current item as failure state',
      }),
    )
    fireEvent.change(screen.getByLabelText('Recovery target type'), {
      target: { value: 'checkpoint' },
    })
    fireEvent.click(calculate)
    expect(await screen.findByText('100 — COMPLETE')).toBeVisible()
    const request = JSON.parse(
      String(
        vi
          .mocked(fetch)
          .mock.calls.find(([url]) => String(url).endsWith('/recovery'))?.[1]
          ?.body,
      ),
    ) as PathRequest
    expect(request.start.item).toEqual(item)
    expect(request.target.checkpoint?.item).toEqual(item)
    expect(request.policy.mode).toBe('SINGLE_PASS')
    expect(
      screen.getByText('100 — COMPLETE').closest('section'),
    ).toHaveAccessibleName('Probability results')
  })
  it('shows unknown bounds and unresolved evidence without claiming exact zero', async () => {
    vi.mocked(fetch).mockImplementation(async (url, init) =>
      String(url).endsWith('/provenance')
        ? jsonResponse(provenance)
        : response(JSON.parse(String(init?.body)) as PathRequest, false, true),
    )
    mount()
    await open()
    fireEvent.click(
      screen.getByRole('button', { name: 'Calculate main probability' }),
    )
    expect(await screen.findByText('100 — UNKNOWN')).toBeVisible()
    expect(screen.getAllByText('Success lower bound')).toHaveLength(3)
    expect(screen.getAllByText('Success upper bound')).toHaveLength(3)
    expect(screen.queryByText('Exact success probability')).toBeNull()
  })
  it('isolates a late response when the starting-item history changes', async () => {
    let finish: ((value: Response) => void) | undefined
    let request: PathRequest | undefined
    vi.mocked(fetch).mockImplementation(async (url, init) => {
      if (String(url).endsWith('/provenance')) return jsonResponse(provenance)
      request = JSON.parse(String(init?.body)) as PathRequest
      return new Promise<Response>((resolve) => {
        finish = resolve
      })
    })
    const mounted = mount()
    await open()
    fireEvent.click(
      screen.getByRole('button', { name: 'Calculate main probability' }),
    )
    await waitFor(() => expect(finish).toBeDefined())
    const signal = vi
      .mocked(fetch)
      .mock.calls.find(([url]) =>
        String(url).endsWith('/first-hit'),
      )?.[1]?.signal
    mounted.rerender(view('history-changed'))
    await waitFor(() => expect(signal?.aborted).toBe(true))
    finish!(response(request!))
    await waitFor(() => expect(screen.queryByText('100 — COMPLETE')).toBeNull())
  })
  it('clears pending work on close and returns focus to the disclosure', async () => {
    vi.mocked(fetch).mockImplementation(async (url) =>
      String(url).endsWith('/provenance')
        ? jsonResponse(provenance)
        : new Promise<Response>(() => {}),
    )
    mount()
    await open()
    fireEvent.click(
      screen.getByRole('button', { name: 'Calculate main probability' }),
    )
    await waitFor(() =>
      expect(screen.getByText('Calculating explicit policy…')).toBeVisible(),
    )
    fireEvent.click(screen.getByRole('button', { name: 'Close calculator' }))
    expect(
      screen.getByText('Experimental basic-currency probability'),
    ).toHaveFocus()
    expect(screen.queryByText('Calculating explicit policy…')).toBeNull()
  })
})
