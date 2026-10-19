import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { AutomaticMethodSearch } from './AutomaticMethodSearch'
import { StrictMode } from 'react'
import { capacityMessages } from './capacityMessages'
import { automaticApi, AutomaticError, acceptsAutomatic } from './automaticApi'
import type { AutomaticView } from './automaticApi'
import { requestFixture } from '../path-tree/fixtures.test-support'
import { createItemPresentation } from '../path-tree/presentation'
import { setLocale } from '../../../shared/i18n/i18n'
import { saveSupport } from './supportStorage'

const id = '11111111-1111-4111-8111-111111111111'
function initial(): AutomaticView {
  return {
    version: 'automatic-method-job-v1',
    id,
    revision: 1,
    generation: 1,
    status: 'READY',
    reason: '',
    complete: false,
    operations: 0,
    retainedStates: 1,
    nodes: [
      {
        id: 0,
        depth: 0,
        quality40: false,
        item: requestFixture().start.item,
        progress: { goal: 'NO_MATCH', entries: ['NO_MATCH'] },
        discoveryFinished: false,
        directFinished: false,
      },
    ],
    methods: [],
    recommendations: [],
  }
}
const present = createItemPresentation(
  { name: 'Solar Amulet', itemClass: 'Amulet' },
  {},
)
it('shows the selected target rarity and blocks resume at the maximum resource limit', async () => {
  setLocale('en')
  const request = requestFixture()
  request.goal.general.rarities = ['RARE']
  const server = {
    ...initial(),
    status: 'PAUSED',
    canResume: false,
    reason: 'RESOURCE_BUDGET',
  }
  const fetcher = vi.fn(
    async () =>
      new Response(JSON.stringify(server), {
        headers: {
          'X-Crafting-Ruleset': request.start.provenance.rulesetIdentity,
        },
      }),
  )
  vi.stubGlobal('fetch', fetcher)
  render(
    <AutomaticMethodSearch
      request={request}
      inputGeneration={1}
      presentItem={present}
      onRecalculate={vi.fn()}
    />,
  )
  expect(
    await screen.findByRole('button', { name: 'Continue calculation' }),
  ).toBeDisabled()
  expect(screen.getByText('Target rarities: RARE')).toBeInTheDocument()
  expect(screen.getByText(/Maximum resource limit reached/)).toBeInTheDocument()
  expect(fetcher).toHaveBeenCalledTimes(1)
})

it('keeps an unresolved target connection visible as a dashed edge', async () => {
  setLocale('en')
  const request = requestFixture()
  const server = { ...initial(), status: 'PARTIAL' as const }
  server.nodes.push({ ...server.nodes[0]!, id: 1, depth: 1 })
  const bound = {
    lower: { numerator: '0', denominator: '1' },
    upper: { numerator: '1', denominator: '1' },
    arithmeticWidth: { numerator: '1', denominator: '1' },
    exact: false,
    confirmedPercent: null,
  }
  server.methods.push({
    id: 'uncertain',
    source: 0,
    initial: [],
    cycle: [{ currency: 'CHAOS', omens: [], purpose: 'GOAL_ACQUISITION' }],
    points: [
      {
        cycles: 100,
        complete: false,
        outcomes: [{ target: 1, kind: 'UNRESOLVED', probability: bound }],
        initialUses: {},
        cycleUses: {},
      },
    ],
    reasons: ['UNRESOLVED_MODEL'],
    finished: true,
  })
  vi.stubGlobal(
    'fetch',
    vi.fn(
      async () =>
        new Response(JSON.stringify(server), {
          headers: {
            'X-Crafting-Ruleset': request.start.provenance.rulesetIdentity,
          },
        }),
    ),
  )
  const mounted = render(
    <AutomaticMethodSearch
      request={request}
      inputGeneration={1}
      presentItem={present}
      onRecalculate={vi.fn()}
    />,
  )
  expect(await screen.findByLabelText('Other results')).toHaveValue('0')
  await waitFor(() =>
    expect(
      mounted.container.querySelector('path.automatic-edge'),
    ).toHaveAttribute('stroke-dasharray', '5 4'),
  )
})
it('restores the same job and selection after remount without creating or advancing another job', async () => {
  const request = requestFixture()
  const server = { ...initial(), status: 'READY' as const }
  server.nodes.push({ ...server.nodes[0]!, id: 1, depth: 1 })
  server.methods.push({
    id: 'restored-chaos',
    source: 1,
    executionMode: 'REPEATABLE',
    initial: [],
    cycle: [{ currency: 'CHAOS', omens: [], purpose: 'GOAL_ACQUISITION' }],
    points: [100, 300, 500].map((cycles) => ({
      cycles,
      complete: true,
      outcomes: [],
      initialUses: {},
      cycleUses: {},
    })),
    reasons: [],
    finished: true,
  })
  saveSupport('run', {
    request: request.clientRequestId,
    job: id,
    selected: 1,
    cycles: 300,
    route: [],
  })
  const fetcher = vi.fn(
    async () =>
      new Response(JSON.stringify(server), {
        headers: {
          'X-Crafting-Ruleset': request.start.provenance.rulesetIdentity,
        },
      }),
  )
  vi.stubGlobal('fetch', fetcher)
  const props = {
    request,
    inputGeneration: 1,
    presentItem: present,
    onRecalculate: vi.fn(),
  }
  const mounted = render(<AutomaticMethodSearch {...props} />)
  await screen.findByRole('button', { name: 'Continue calculation' })
  expect(screen.getByLabelText('Method repetitions')).toHaveValue('300')
  expect(screen.getByLabelText('Other results')).toHaveValue('1')
  mounted.rerender(<AutomaticMethodSearch {...props} active={false} />)
  mounted.rerender(<AutomaticMethodSearch {...props} active />)
  await act(async () => {
    await new Promise((resolve) => setTimeout(resolve, 300))
  })
  expect(fetcher).toHaveBeenCalledTimes(1)
  mounted.unmount()
  render(<AutomaticMethodSearch {...props} />)
  await screen.findByRole('button', { name: 'Continue calculation' })
  expect(fetcher).toHaveBeenCalledTimes(2)
  expect(
    fetcher.mock.calls.every(
      (call) =>
        call.length === 0 ||
        !((call as unknown as [unknown, RequestInit])[1]?.method === 'POST'),
    ),
  ).toBe(true)
})
it('preserves input and asks for explicit recalculation for an expired job', async () => {
  const request = requestFixture(),
    recalculate = vi.fn()
  saveSupport('run', {
    request: request.clientRequestId,
    job: id,
    selected: 0,
    cycles: 100,
    route: [],
  })
  const fetcher = vi.fn(async () => new Response('', { status: 404 }))
  vi.stubGlobal('fetch', fetcher)
  render(
    <AutomaticMethodSearch
      request={request}
      inputGeneration={1}
      presentItem={present}
      onRecalculate={recalculate}
    />,
  )
  await screen.findByText(/saved search is unavailable/i)
  expect(fetcher).toHaveBeenCalledTimes(1)
  fireEvent.click(screen.getByRole('button', { name: 'Recalculate' }))
  expect(recalculate).toHaveBeenCalledOnce()
})
afterEach(() => {
  vi.unstubAllGlobals()
  sessionStorage.clear()
})
it('shows stop pending until server confirmation rather than claiming it has stopped', async () => {
  const request = requestFixture()
  let server = { ...initial(), status: 'PAUSED' as const } as AutomaticView
  let confirm: ((value: Response) => void) | undefined
  const send = (v: AutomaticView) =>
    new Response(JSON.stringify(v), {
      headers: {
        'X-Crafting-Ruleset': request.start.provenance.rulesetIdentity,
      },
    })
  vi.stubGlobal(
    'fetch',
    vi.fn(async (_url: unknown, options?: RequestInit) => {
      const body = options?.body ? JSON.parse(String(options.body)) : null
      if (body?.operation === 'CANCEL')
        return new Promise<Response>((resolve) => {
          confirm = resolve
        })
      return send(server)
    }),
  )
  render(
    <AutomaticMethodSearch
      request={request}
      inputGeneration={1}
      presentItem={present}
      onRecalculate={vi.fn()}
    />,
  )
  await screen.findByText('Paused — calculation budget reached')
  fireEvent.click(screen.getByRole('button', { name: 'Stop' }))
  await waitFor(() => expect(confirm).toBeDefined())
  expect(
    screen.getByRole('button', { name: 'Stopping… waiting for confirmation' }),
  ).toBeDisabled()
  expect(screen.queryByText('Calculation stopped.')).not.toBeInTheDocument()
  server = { ...server, status: 'CANCELLED', revision: 2, generation: 2 }
  await act(async () => confirm!(send(server)))
  expect(await screen.findByText('Calculation stopped.')).toBeInTheDocument()
})
it('rejects stale revisions, generations and other jobs', () => {
  const current = initial()
  expect(
    acceptsAutomatic(
      { ...current, revision: 4, generation: 2 },
      { ...current, revision: 5 },
    ),
  ).toBe(false)
  expect(
    acceptsAutomatic({ ...current, revision: 4 }, { ...current, revision: 3 }),
  ).toBe(false)
  expect(
    acceptsAutomatic(current, { ...current, id: crypto.randomUUID() }),
  ).toBe(false)
})
it('ignores a late slice after cancellation and copies the selected state into editing', async () => {
  setLocale('en')
  let server = initial()
  let finish: ((response: Response) => void) | undefined
  const send = (view: AutomaticView) =>
    new Response(JSON.stringify(view), {
      headers: {
        'X-Crafting-Ruleset': requestFixture().start.provenance.rulesetIdentity,
      },
    })
  vi.stubGlobal(
    'fetch',
    vi.fn(async (_url: unknown, options?: RequestInit) => {
      if (options?.method !== 'POST') return send(server)
      const body = JSON.parse(String(options.body)) as { operation?: string }
      if (!body.operation) return send(server)
      if (body.operation === 'ADVANCE') {
        server = { ...server, revision: 2, operations: 1 }
        const stale = structuredClone(server)
        return new Promise<Response>((resolve) => {
          finish = () => resolve(send(stale))
        })
      }
      server = {
        ...server,
        revision: server.revision + 1,
        generation: server.generation + 1,
        status: 'CANCELLED',
      }
      return send(server)
    }),
  )
  const edit = vi.fn()
  const rendered = render(
    <AutomaticMethodSearch
      request={requestFixture()}
      inputGeneration={1}
      presentItem={present}
      onRecalculate={vi.fn()}
      onEditState={edit}
    />,
  )
  await waitFor(() => expect(finish).toBeDefined())
  fireEvent.click(screen.getByRole('button', { name: 'Stop' }))
  await screen.findByText('Calculation stopped.')
  await act(async () => finish?.(new Response()))
  expect(screen.getByText('Calculation stopped.')).toBeInTheDocument()
  fireEvent.click(screen.getByRole('button', { name: 'Use as starting item' }))
  expect(edit).toHaveBeenCalledWith(server.nodes[0]!.item, {
    abstractQuality40: false,
  })
  expect(edit.mock.calls[0]![0]).not.toBe(server.nodes[0]!.item)
  rendered.unmount()
})
it('resumes a paused job and starts a fresh generation after an edited request', async () => {
  setLocale('en')
  let server = { ...initial(), status: 'PAUSED' as const } as AutomaticView
  const creates: unknown[] = []
  vi.stubGlobal(
    'fetch',
    vi.fn(async (_url: unknown, options?: RequestInit) => {
      if (options?.method === 'POST') {
        const body = JSON.parse(String(options.body)) as {
          operation?: string
          clientRequestId?: string
        }
        if (!body.operation) {
          creates.push(body)
          server = {
            ...initial(),
            id:
              creates.length === 1
                ? id
                : '22222222-2222-4222-8222-222222222222',
            status: 'PAUSED',
          }
        } else
          server = {
            ...server,
            revision: server.revision + 1,
            generation:
              server.generation + (body.operation === 'ADVANCE' ? 0 : 1),
            status:
              body.operation === 'RESUME'
                ? 'READY'
                : body.operation === 'ADVANCE'
                  ? 'PAUSED'
                  : 'CANCELLED',
          }
      }
      return new Response(JSON.stringify(server), {
        headers: {
          'X-Crafting-Ruleset':
            requestFixture().start.provenance.rulesetIdentity,
        },
      })
    }),
  )
  const request = requestFixture()
  const rendered = render(
    <AutomaticMethodSearch
      request={request}
      inputGeneration={1}
      presentItem={present}
      onRecalculate={vi.fn()}
    />,
  )
  fireEvent.click(
    await screen.findByRole('button', { name: 'Continue calculation' }),
  )
  await waitFor(() => expect(server.revision).toBeGreaterThan(2))
  const next = structuredClone(request)
  next.clientRequestId = 'edited-request'
  next.start.item.itemLevel = 81
  rendered.rerender(
    <AutomaticMethodSearch
      request={next}
      inputGeneration={2}
      presentItem={present}
      onRecalculate={vi.fn()}
    />,
  )
  await waitFor(() => expect(creates).toHaveLength(2))
  expect(creates[1]).toMatchObject({
    request: { goal: request.goal, start: { item: { itemLevel: 81 } } },
    abstractQuality40: false,
  })
  rendered.unmount()
})

it.each(['JOB_CAPACITY', 'JOB_STORAGE_CAPACITY'])(
  'shows %s without retrying POST in StrictMode',
  async (code) => {
    setLocale('en')
    const fetcher = vi.fn(
      async () =>
        new Response(JSON.stringify({ code }), {
          status: 429,
          headers: { 'Retry-After': '120' },
        }),
    )
    vi.stubGlobal('fetch', fetcher)
    render(
      <StrictMode>
        <AutomaticMethodSearch
          request={requestFixture()}
          inputGeneration={1}
          presentItem={present}
          onRecalculate={vi.fn()}
        />
      </StrictMode>,
    )
    expect(await screen.findByRole('alert')).toHaveTextContent(
      code === 'JOB_CAPACITY'
        ? capacityMessages.en.full
        : capacityMessages.en.storage,
    )
    expect(screen.getByRole('alert')).toHaveTextContent('Try again after')
    await new Promise((resolve) => setTimeout(resolve, 250))
    expect(fetcher).toHaveBeenCalledTimes(1)
    expect(screen.queryByText('Calculation error')).not.toBeInTheDocument()
  },
)
it('shares an in-flight create after local abort and never guesses success on 429', async () => {
  let finish!: (response: Response) => void
  const fetcher = vi.fn(
    () =>
      new Promise<Response>((resolve) => {
        finish = resolve
      }),
  )
  vi.stubGlobal('fetch', fetcher)
  const request = requestFixture(),
    controller = new AbortController()
  const first = automaticApi.create(request, controller.signal)
  controller.abort()
  const second = automaticApi.create(request)
  expect(first).toBe(second)
  await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(1))
  const rejection = expect(first).rejects.toMatchObject({
    status: 429,
    code: 'JOB_CAPACITY',
  })
  finish(
    new Response(JSON.stringify({ code: 'JOB_CAPACITY' }), {
      status: 429,
      headers: { 'Retry-After': '60' },
    }),
  )
  await rejection
  expect(fetcher).toHaveBeenCalledTimes(1)
})
it('does not render private error codes or invent a retry deadline', async () => {
  vi.stubGlobal(
    'fetch',
    vi.fn(
      async () =>
        new Response(JSON.stringify({ code: 'PRIVATE_SERVER_DETAIL' }), {
          status: 429,
          headers: { 'Retry-After': 'bad' },
        }),
    ),
  )
  const err = await automaticApi.create(requestFixture()).catch((e) => e)
  expect(err).toBeInstanceOf(AutomaticError)
  expect(err).toMatchObject({ status: 429, code: '', retryAt: null })
})

it('serializes different creates so the first owner cookie settles before the next request', async () => {
  let finish!: (response: Response) => void
  const fetcher = vi
    .fn()
    .mockImplementationOnce(
      () =>
        new Promise<Response>((resolve) => {
          finish = resolve
        }),
    )
    .mockImplementation(
      async () =>
        new Response(JSON.stringify({ code: 'JOB_CAPACITY' }), { status: 429 }),
    )
  vi.stubGlobal('fetch', fetcher)
  const first = automaticApi.create(requestFixture()).catch((e) => e)
  const next = requestFixture()
  next.clientRequestId = 'different-create'
  const second = automaticApi.create(next).catch((e) => e)
  await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(1))
  finish(
    new Response(JSON.stringify({ code: 'JOB_CAPACITY' }), { status: 429 }),
  )
  expect(await first).toMatchObject({ status: 429 })
  expect(await second).toMatchObject({ status: 429 })
  expect(fetcher).toHaveBeenCalledTimes(2)
  expect(
    JSON.parse(fetcher.mock.calls[1]![1].body).request.clientRequestId,
  ).toBe('different-create')
})

it('automatically resumes an expandable resource slice and finishes without another click', async () => {
  setLocale('en')
  const request = requestFixture()
  let server: AutomaticView = {
    ...initial(),
    status: 'PAUSED',
    reason: 'RESOURCE_BUDGET',
    canResume: true,
  }
  const operations: string[] = []
  vi.stubGlobal(
    'fetch',
    vi.fn(async (_url: unknown, options?: RequestInit) => {
      const body = options?.body ? JSON.parse(String(options.body)) : {}
      if (body.operation) {
        operations.push(body.operation)
        server = {
          ...server,
          revision: server.revision + 1,
          generation: server.generation + (body.operation === 'RESUME' ? 1 : 0),
          status: body.operation === 'RESUME' ? 'READY' : 'COMPLETE',
          reason: body.operation === 'RESUME' ? '' : 'SUPPORTED_PLANS_COMPLETE',
          complete: body.operation === 'ADVANCE',
          canResume: false,
        }
      }
      return new Response(JSON.stringify(server), {
        headers: {
          'X-Crafting-Ruleset': request.start.provenance.rulesetIdentity,
        },
      })
    }),
  )
  render(
    <AutomaticMethodSearch
      request={request}
      inputGeneration={1}
      presentItem={present}
      onRecalculate={vi.fn()}
    />,
  )
  await waitFor(() => expect(operations).toEqual(['RESUME', 'ADVANCE']))
  await new Promise((resolve) => setTimeout(resolve, 350))
  expect(operations).toEqual(['RESUME', 'ADVANCE'])
})

it('does not auto resume a cancelled or storage-limited calculation', async () => {
  setLocale('en')
  const request = requestFixture()
  for (const state of [
    { status: 'CANCELLED' as const, reason: '' },
    { status: 'PAUSED' as const, reason: 'STORAGE_BUDGET' },
  ]) {
    const fetcher = vi.fn(
      async () =>
        new Response(
          JSON.stringify({ ...initial(), ...state, canResume: true }),
          {
            headers: {
              'X-Crafting-Ruleset': request.start.provenance.rulesetIdentity,
            },
          },
        ),
    )
    vi.stubGlobal('fetch', fetcher)
    sessionStorage.clear()
    const mounted = render(
      <AutomaticMethodSearch
        request={request}
        inputGeneration={1}
        presentItem={present}
        onRecalculate={vi.fn()}
      />,
    )
    await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(1))
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 350))
    })
    expect(fetcher).toHaveBeenCalledTimes(1)
    mounted.unmount()
  }
})

it('does not repeatedly resume the same rejected resource revision', async () => {
  const request = requestFixture()
  const server = {
    ...initial(),
    status: 'PAUSED',
    reason: 'RESOURCE_BUDGET',
    canResume: true,
  }
  let resumes = 0
  vi.stubGlobal(
    'fetch',
    vi.fn(async (_url: unknown, options?: RequestInit) => {
      const body = options?.body ? JSON.parse(String(options.body)) : {}
      if (body.operation === 'RESUME') {
        resumes++
        return new Response('{}', { status: 409 })
      }
      return new Response(JSON.stringify(server), {
        headers: {
          'X-Crafting-Ruleset': request.start.provenance.rulesetIdentity,
        },
      })
    }),
  )
  render(
    <AutomaticMethodSearch
      request={request}
      inputGeneration={1}
      presentItem={present}
      onRecalculate={vi.fn()}
    />,
  )
  await waitFor(() => expect(resumes).toBe(1))
  await act(async () => {
    await new Promise((resolve) => setTimeout(resolve, 500))
  })
  expect(resumes).toBe(1)
})

it('allows Stop while automatic resume is pending and rejects its late READY response', async () => {
  setLocale('en')
  const request = requestFixture()
  let server: AutomaticView = {
    ...initial(),
    status: 'PAUSED',
    reason: 'RESOURCE_BUDGET',
    canResume: true,
  }
  let resume: ((response: Response) => void) | undefined
  let cancel: ((response: Response) => void) | undefined
  let resumed: AutomaticView | undefined
  const operations: string[] = []
  const response = (view: AutomaticView) =>
    new Response(JSON.stringify(view), {
      headers: {
        'X-Crafting-Ruleset': request.start.provenance.rulesetIdentity,
      },
    })
  vi.stubGlobal(
    'fetch',
    vi.fn(async (_url: unknown, options?: RequestInit) => {
      const body = options?.body ? JSON.parse(String(options.body)) : {}
      if (body.operation) operations.push(body.operation)
      if (body.operation === 'RESUME') {
        server = {
          ...server,
          status: 'READY',
          revision: 2,
          generation: 2,
          reason: '',
        }
        resumed = server
        return new Promise<Response>((resolve) => {
          resume = resolve
        })
      }
      if (body.operation === 'CANCEL') {
        server = { ...server, status: 'CANCELLED', revision: 3, generation: 3 }
        return new Promise<Response>((resolve) => {
          cancel = resolve
        })
      }
      return response(server)
    }),
  )
  render(
    <AutomaticMethodSearch
      request={request}
      inputGeneration={1}
      presentItem={present}
      onRecalculate={vi.fn()}
    />,
  )
  await waitFor(() => expect(resume).toBeDefined())
  expect(screen.getByRole('button', { name: 'Stop' })).toBeEnabled()
  fireEvent.click(screen.getByRole('button', { name: 'Stop' }))
  await waitFor(() => expect(cancel).toBeDefined())
  await act(async () => resume!(response(resumed!)))
  expect(screen.getByRole('button', { name: /Stopping/ })).toBeDisabled()
  await act(async () => cancel!(response(server)))
  expect(await screen.findByText('Calculation stopped.')).toBeInTheDocument()
  await act(async () => {
    await new Promise((resolve) => setTimeout(resolve, 350))
  })
  expect(operations).toEqual(['RESUME', 'CANCEL'])
})

it('explains a catalog-proven target outside the four explicit modifier scope without advancing', async () => {
  setLocale('en')
  vi.spyOn(automaticApi, 'create').mockRejectedValue(
    new AutomaticError(422, 'GOAL_EXCEEDS_FOUR_EXPLICIT_SCOPE'),
  )
  const control = vi.spyOn(automaticApi, 'control')
  render(
    <AutomaticMethodSearch
      request={requestFixture()}
      inputGeneration={1}
      presentItem={present}
      onRecalculate={vi.fn()}
    />,
  )
  expect(await screen.findByRole('alert')).toHaveTextContent(
    'requires more than four explicit modifiers',
  )
  expect(control).not.toHaveBeenCalled()
})

it('opens legacy partial results without advance, resume, cancellation or automatic recalculation', async () => {
  setLocale('en')
  const request = requestFixture()
  saveSupport('run', {
    request: request.clientRequestId,
    job: id,
    selected: 0,
    cycles: 100,
    route: [],
  })
  vi.spyOn(automaticApi, 'get').mockResolvedValue({
    ...initial(),
    status: 'PARTIAL',
    reason: 'LEGACY_READ_ONLY',
    canResume: false,
  })
  const control = vi.spyOn(automaticApi, 'control')
  const recalculate = vi.fn()
  const { unmount } = render(
    <AutomaticMethodSearch
      request={request}
      inputGeneration={1}
      presentItem={present}
      onRecalculate={recalculate}
    />,
  )
  expect(
    await screen.findByText(/Saved result from an earlier calculation version/),
  ).toBeInTheDocument()
  expect(
    screen.queryByRole('button', { name: 'Continue calculation' }),
  ).not.toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Stop' })).not.toBeInTheDocument()
  unmount()
  expect(control).not.toHaveBeenCalled()
  expect(recalculate).not.toHaveBeenCalled()
})
