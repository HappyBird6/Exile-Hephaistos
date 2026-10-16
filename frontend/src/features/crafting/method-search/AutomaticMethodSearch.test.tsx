import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { AutomaticMethodSearch } from './AutomaticMethodSearch'
import { acceptsAutomatic } from './automaticApi'
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
it('restores the same job and selection after remount without creating or advancing another job', async () => {
  const request = requestFixture()
  const server = { ...initial(), status: 'READY' as const }
  server.nodes.push({ ...server.nodes[0]!, id: 1, depth: 1 })
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
  expect(
    screen.getByRole('heading', { name: 'Item state 1' }),
  ).toBeInTheDocument()
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
  fireEvent.click(screen.getByRole('button', { name: 'Edit this state' }))
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
