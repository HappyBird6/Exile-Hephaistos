import { randomUUID } from 'node:crypto'
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { expect, it } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createElement } from 'react'
import {
  render,
  screen,
  waitFor,
  within,
  fireEvent,
} from '@testing-library/react'
import { PathTree } from '../src/features/crafting/path-tree/PathTree'
import { createItemPresentation } from '../src/features/crafting/path-tree/presentation'
import { setLocale } from '../src/shared/i18n/i18n'
import { PathSearchSession } from '../src/features/crafting/path-tree/session'
import { createHttpPathSearchAdapter } from '../src/features/crafting/path-tree/api'
import {
  mergeGraph,
  canonical,
} from '../src/features/crafting/path-tree/validation'
import {
  ancestors,
  stateLayers,
} from '../src/features/crafting/path-tree/graph'
import type {
  CreateRequest,
  JobSnapshot,
} from '../src/features/crafting/path-tree/types'

// This acceptance command requires the test-only Java HTTP harness. There is no fixture fallback.
const base = 'http://127.0.0.1:19091'
let cookie = ''
const transport: typeof fetch = async (url, init) => {
  const response = await fetch(base + url, {
    ...init,
    headers: { ...init?.headers, ...(cookie ? { Cookie: cookie } : {}) },
  })
  const next = response.headers.get('set-cookie')
  if (next) cookie = next.split(';')[0]!
  return response
}
const api = createHttpPathSearchAdapter(transport)
const signal = new AbortController().signal
async function finish(job: JobSnapshot) {
  for (let i = 0; i < 400; i++) {
    if (
      job.status === 'FAILED' ||
      (job.status === 'PAUSED' && !job.resumable)
    ) {
      throw new Error(`Backend stopped: ${job.status}/${job.reasonCode}`)
    }
    if (job.status === 'COMPLETED' || job.status === 'UNSUPPORTED') return job
    if (job.resumable)
      job = await api.mutate(
        job.jobId,
        {
          version: 1,
          operation: 'RESUME',
          commandId: randomUUID(),
          expectedRevision: job.revision,
        },
        job.provenance.transition.rulesetIdentity,
        signal,
      )
    await new Promise((resolve) => setTimeout(resolve, 30))
    job = await api.read(job.jobId, signal)
  }
  throw new Error(
    'The actual backend did not complete within the acceptance budget',
  )
}
it('accepts real Solar HTTP results, cancellation/resumption, pages, renewal and separate recovery', async () => {
  const request = (await (
    await fetch(base + '/qa/source')
  ).json()) as CreateRequest
  request.clientRequestId = randomUUID()
  const baseline = await finish(await api.create(request, signal))
  expect(baseline.status).toBe('COMPLETED')
  expect(baseline.recommendations).toHaveLength(6)
  const chaos = baseline.recommendations.find(
    (r) => r.policy.id === 'solar-policy-1',
  )!
  for (const n of [0, 1, 2, 100, 300, 500]) {
    const point = chaos.points.find((p) => p.attempts === String(n))!
    const d = 21107n ** BigInt(n),
      numerator = d - 20457n ** BigInt(n)
    expect(BigInt(point.lower.numerator) * d).toBe(
      numerator * BigInt(point.lower.denominator),
    )
    expect(point.status).toBe('COMPLETE')
  }
  let interrupted = await api.create(
    { ...request, clientRequestId: randomUUID() },
    signal,
  )
  while (['QUEUED', 'RUNNING'].includes(interrupted.status)) {
    await new Promise((resolve) => setTimeout(resolve, 30))
    interrupted = await api.read(interrupted.jobId, signal)
  }
  expect(interrupted.status).toBe('PAUSED')
  let stopped: JobSnapshot
  try {
    stopped = await api.mutate(
      interrupted.jobId,
      {
        version: 1,
        operation: 'CANCEL',
        commandId: randomUUID(),
        expectedRevision: interrupted.revision,
      },
      request.start.provenance.rulesetIdentity,
      signal,
    )
  } catch {
    const current = await api.read(interrupted.jobId, signal)
    stopped = await api.mutate(
      current.jobId,
      {
        version: 1,
        operation: 'CANCEL',
        commandId: randomUUID(),
        expectedRevision: current.revision,
      },
      request.start.provenance.rulesetIdentity,
      signal,
    )
  }
  expect(stopped.status).toBe('CANCELLED')
  const resumed = await finish(stopped)
  expect(resumed.recommendations).toEqual(baseline.recommendations)
  expect(resumed.rankings).toEqual(baseline.rankings)
  // Delay an actual HTTP read after it has arrived; a cancel acknowledgement must win.
  let release: (() => void) | undefined
  let captured: (() => void) | undefined
  const arrived = new Promise<void>((resolve) => {
    captured = resolve
  })
  let delayNextRead = false
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  const session = new PathSearchSession(client, {
    ...api,
    read: async (id, readSignal) => {
      const value = await api.read(id, readSignal)
      if (delayNextRead) {
        delayNextRead = false
        await new Promise<void>((resolve) => {
          release = resolve
          captured!()
        })
      }
      return value
    },
  })
  await session.start({ ...request, clientRequestId: randomUUID() })
  while (['QUEUED', 'RUNNING'].includes(session.job?.status ?? '')) {
    await new Promise((resolve) => setTimeout(resolve, 30))
    await session.refresh()
  }
  expect(session.job?.status).toBe('PAUSED')
  delayNextRead = true
  const lateRead = session.refresh()
  await arrived
  await session.command('CANCEL')
  expect(session.job?.status).toBe('CANCELLED')
  const cancelledRevision = session.job!.revision
  release!()
  await lateRead
  expect(session.job?.status).toBe('CANCELLED')
  expect(session.job?.revision).toBe(cancelledRevision)
  session.invalidate()
  client.clear()
  let graph = baseline.graph,
    pages = 1
  while (graph.nextCursor) {
    graph = mergeGraph(
      graph,
      await api.graph(
        baseline.jobId,
        baseline.revision,
        graph.nextCursor,
        signal,
      ),
    )
    pages++
  }
  expect(pages).toBeGreaterThan(1)
  await expect(
    api.graph(
      baseline.jobId,
      baseline.revision + 1000,
      baseline.graph.nextCursor!,
      signal,
    ),
  ).rejects.toMatchObject({ code: 'REVISION_EXPIRED' })
  expect(graph.edges.some((e) => e.kind === 'REPEAT')).toBe(true)
  expect(stateLayers(graph, 'solar-policy-4').length).toBeGreaterThan(1)
  const failure = graph.executions.find(
    (e) =>
      e.policyId === 'solar-policy-1' &&
      graph.nodes.find((n) => n.id === e.stateId)?.goalStatus === 'NO_MATCH' &&
      ancestors(graph, e.id).some((id) => id !== e.stateId),
  )!
  expect(failure).toBeDefined()
  const checkpoint = ancestors(graph, failure.id).find(
    (id) => id !== failure.stateId,
  )!
  const recovery = await finish(
    await api.recover(
      baseline.jobId,
      {
        version: 1,
        clientRequestId: randomUUID(),
        parentRevision: baseline.revision,
        failureExecutionId: failure.id,
        checkpointStateId: checkpoint,
        observations: ['100', '300', '500'],
      },
      request.start.provenance.rulesetIdentity,
      signal,
    ),
  )
  expect(recovery.recovery).toMatchObject({
    conditional: true,
    includedInMain: false,
  })
  expect(canonical(await api.read(baseline.jobId, signal))).toBe(
    canonical(baseline),
  )
  for (const [field, code] of [
    ['catalogVersion', 'CATALOG_VERSION_MISMATCH'],
    ['statId', 'UNKNOWN_STAT'],
  ] as const) {
    const bad = structuredClone(request)
    bad.clientRequestId = randomUUID()
    if (field === 'catalogVersion') bad.goal.catalogVersion = 'deleted-version'
    else bad.goal.groups[0]!.entries[0]!.statId = 'deleted-stat'
    await expect(api.create(bad, signal)).rejects.toMatchObject({ code })
  }
  const deletedModifier = structuredClone(request)
  deletedModifier.clientRequestId = randomUUID()
  deletedModifier.start.item.explicits[0]!.modifierId = 'deleted-modifier'
  await expect(api.create(deletedModifier, signal)).rejects.toMatchObject({
    code: 'INVALID_ITEM',
  })
  const source = JSON.parse(
    readFileSync(
      '../contracts/crafting-paths-v1/solar-source-fixture.json',
      'utf8',
    ),
  )
  const original = canonical(source.startItem)
  const oldSource = {
    ...request,
    clientRequestId: randomUUID(),
    start: { ...request.start, item: source.startItem },
  }
  await expect(api.create(oldSource, signal)).rejects.toMatchObject({
    code: 'INVALID_ITEM',
  })
  expect(canonical(source.startItem)).toBe(original)
  const ordinary = JSON.parse(
    readFileSync(
      '../backend/src/main/resources/catalog/solar-amulet/catalog.json',
      'utf8',
    ),
  )
  const definitions = Object.fromEntries(
    ordinary.modifiers.map((m: { id: string }) => [m.id, m]),
  )
  setLocale('en')
  const uiClient = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  })
  const rendered = render(
    createElement(
      QueryClientProvider,
      { client: uiClient },
      createElement(PathTree, {
        request,
        inputGeneration: 1,
        adapter: api,
        onRecalculate: () => {},
        presentItem: createItemPresentation(
          { name: 'Solar Amulet', itemClass: 'Amulets' },
          definitions,
        ),
      }),
    ),
  )
  await waitFor(() =>
    expect(
      within(screen.getByLabelText('Compared paths')).getAllByRole('button'),
    ).toHaveLength(5),
  )
  for (const n of ['100', '300', '500']) {
    fireEvent.click(screen.getByRole('radio', { name: n }))
    expect(
      (screen.getByRole('radio', { name: n }) as HTMLInputElement).checked,
    ).toBe(true)
    expect(
      screen.getByText(`Chance of reaching the goal within ${n} currency uses`),
    ).toBeDefined()
  }
  expect(screen.queryByText(/globally optimal/i)).toBeNull()
  rendered.unmount()
  uiClient.clear()
  mkdirSync('/tmp/path-search-runtime', { recursive: true })
  writeFileSync(
    '/tmp/path-search-runtime/result.json',
    JSON.stringify({ baseline, graph, stopped, resumed, recovery }),
  )
  console.log(
    `Actual HTTP adapter: ${baseline.recommendations.length} candidates, ${graph.nodes.length} states, ${graph.executions.length} executions, ${pages} pages; Solar 650/21107, cancel/resume and separate recovery passed`,
  )
})
