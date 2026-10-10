import { QueryClient } from '@tanstack/react-query'
import { describe, expect, it, vi } from 'vitest'
import type { PathSearchAdapter } from './api'
import { createHttpPathSearchAdapter } from './api'
import { PathSearchSession } from './session'
import { PathSearchError } from './validation'
import { fixture, jobFixture, requestFixture } from './fixtures.test-support'
import type { JobSnapshot } from './types'
import solarSource from '../../../../../contracts/crafting-paths-v1/solar-source-fixture.json'
import { createPathSearchRequest } from './request'
import type { Item } from './types'
function deferred<T>() {
  let resolve!: (v: T) => void
  const promise = new Promise<T>((r) => {
    resolve = r
  })
  return { promise, resolve }
}
function setup() {
  const base = jobFixture()
  const adapter: PathSearchAdapter = {
    id: 'fixture-test',
    create: vi.fn(async () => base),
    read: vi.fn(async () => base),
    graph: vi.fn(async () => base.graph),
    mutate: vi.fn(async () => base),
    recover: vi.fn(async () => fixture<JobSnapshot>('conditional-recovery')),
  }
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  })
  return { adapter, session: new PathSearchSession(client, adapter), client }
}
describe('job lifecycle', () => {
  it('preserves the pinned Solar source snapshot when the current server rejects it', async () => {
    const original = structuredClone(solarSource.startItem)
    const input = requestFixture()
    const request = createPathSearchRequest(
      solarSource.startItem as Item,
      input.goal,
      input.start.provenance,
    )
    const transport = vi.fn<typeof fetch>().mockResolvedValue(
      new Response(
        JSON.stringify({
          type: 'about:blank',
          title: 'Snapshot mismatch',
          status: 422,
          code: 'SNAPSHOT_MISMATCH',
        }),
        { status: 422 },
      ),
    )
    await expect(
      createHttpPathSearchAdapter(transport).create(
        request,
        new AbortController().signal,
      ),
    ).rejects.toMatchObject({ code: 'SNAPSHOT_MISMATCH' })
    expect(
      JSON.parse(transport.mock.calls[0]![1]?.body as string).start.item,
    ).toEqual(original)
    expect(solarSource.startItem).toEqual(original)
    expect(transport).toHaveBeenCalledTimes(1)
  })
  it('retries a lost mutation acknowledgement with the identical command body', async () => {
    const request = {
      version: 1 as const,
      operation: 'CANCEL' as const,
      commandId: 'same-command',
      expectedRevision: 2,
    }
    const ruleset = requestFixture().start.provenance.rulesetIdentity
    const transport = vi
      .fn<typeof fetch>()
      .mockRejectedValueOnce(new TypeError('network'))
      .mockResolvedValueOnce(
        new Response(JSON.stringify(jobFixture()), {
          headers: { 'X-Crafting-Ruleset': ruleset },
        }),
      )
    await createHttpPathSearchAdapter(transport).mutate(
      'synthetic:job1',
      request,
      ruleset,
      new AbortController().signal,
    )
    expect(transport.mock.calls[0]![1]?.body).toBe(
      transport.mock.calls[1]![1]?.body,
    )
    expect(
      JSON.parse(transport.mock.calls[1]![1]?.body as string).commandId,
    ).toBe('same-command')
  })
  it('cancels a late create on the server without publishing it into the next input', async () => {
    const { adapter, session } = setup(),
      late = deferred<JobSnapshot>()
    vi.mocked(adapter.create).mockReturnValueOnce(late.promise)
    const old = session.start(requestFixture())
    session.invalidate()
    late.resolve(jobFixture())
    await old
    expect(session.job).toBeUndefined()
    expect(adapter.mutate).toHaveBeenCalledWith(
      'synthetic:job1',
      expect.objectContaining({ operation: 'CANCEL' }),
      expect.any(String),
      expect.any(AbortSignal),
    )
  })
  it('rejects revision regressions and same revision mutations', async () => {
    const { adapter, session } = setup()
    await session.start(requestFixture())
    const older = jobFixture()
    older.revision--
    older.graph.revision--
    vi.mocked(adapter.read).mockResolvedValueOnce(older)
    await session.refresh()
    expect(session.job?.revision).toBe(2)
    const changed = jobFixture()
    changed.status = 'RUNNING'
    vi.mocked(adapter.read).mockResolvedValueOnce(changed)
    await expect(session.refresh()).rejects.toThrow()
    expect(session.view.getState().error).toBe('INVALID_RESPONSE')
  })
  it('refreshes a conflict and retries cancel with a new command ID', async () => {
    const { adapter, session } = setup()
    await session.start(requestFixture())
    const current = jobFixture()
    current.revision = 3
    current.graph.revision = 3
    current.status = 'RUNNING'
    vi.mocked(adapter.read).mockResolvedValueOnce(current)
    const cancelled = {
      ...current,
      status: 'CANCELLED' as const,
      revision: 4,
      graph: { ...current.graph, revision: 4 },
      resumable: true,
    }
    vi.mocked(adapter.mutate)
      .mockRejectedValueOnce(new PathSearchError('REVISION_CONFLICT', 409))
      .mockResolvedValueOnce(cancelled)
    await session.command('CANCEL')
    const calls = vi.mocked(adapter.mutate).mock.calls
    expect(calls[0]![1].commandId).not.toBe(calls[1]![1].commandId)
    expect(calls[1]![1].expectedRevision).toBe(3)
    expect(session.job?.status).toBe('CANCELLED')
  })
  it('never publishes an old read after cancel acknowledgement, even when abort is ignored', async () => {
    const { adapter, session } = setup()
    await session.start(requestFixture())
    const late = deferred<JobSnapshot>()
    vi.mocked(adapter.read).mockReturnValueOnce(late.promise)
    const reading = session.refresh()
    const stopped = jobFixture()
    stopped.revision = 3
    stopped.graph.revision = 3
    stopped.status = 'CANCELLED'
    stopped.resumable = true
    vi.mocked(adapter.mutate).mockResolvedValueOnce(stopped)
    await session.command('CANCEL')
    const stale = jobFixture()
    stale.revision = 4
    stale.graph.revision = 4
    stale.status = 'RUNNING'
    late.resolve(stale)
    await reading
    expect(session.job?.status).toBe('CANCELLED')
  })
  it('resumes the same job without adding previous probability mass', async () => {
    const { adapter, session } = setup()
    const stopped = jobFixture()
    stopped.status = 'CANCELLED'
    stopped.resumable = true
    vi.mocked(adapter.create).mockResolvedValueOnce(stopped)
    await session.start(requestFixture())
    const resumed = jobFixture()
    resumed.revision = 3
    resumed.graph.revision = 3
    vi.mocked(adapter.mutate).mockResolvedValueOnce(resumed)
    await session.command('RESUME')
    expect(session.job?.recommendations).toEqual(resumed.recommendations)
    expect(session.graph?.edges.length).toBe(resumed.graph.edges.length)
  })
  for (const code of [
    'JOB_EXPIRED',
    'REVISION_EXPIRED',
    'CATALOG_VERSION_MISMATCH',
    'UNKNOWN_STAT',
    'SNAPSHOT_MISMATCH',
  ])
    it(`invalidates ${code} without recalculating`, async () => {
      const { adapter, session } = setup()
      await session.start(requestFixture())
      vi.mocked(adapter.read).mockRejectedValueOnce(
        new PathSearchError(code, 410),
      )
      await expect(session.refresh()).rejects.toThrow()
      expect(session.job).toBeUndefined()
      expect(adapter.create).toHaveBeenCalledTimes(1)
    })
  it('invalidates all provenance changes', async () => {
    const { adapter, session } = setup()
    await session.start(requestFixture())
    const changed = jobFixture()
    changed.provenance.searchVersion = 'new'
    vi.mocked(adapter.read).mockResolvedValueOnce(changed)
    await expect(session.refresh()).rejects.toThrow()
    expect(session.job).toBeUndefined()
  })
  it('keeps conditional recovery in an independent job cache', async () => {
    const { adapter, session, client } = setup()
    await session.start(requestFixture())
    const main = structuredClone(session.job)
    const child = new PathSearchSession(client, adapter)
    const recovery = fixture<JobSnapshot>('conditional-recovery'),
      parent = fixture<JobSnapshot>('recovery-parent')
    await child.recover(parent, {
      version: 1,
      clientRequestId: recovery.clientRequestId,
      parentRevision: recovery.recovery!.parentRevision,
      failureExecutionId: recovery.recovery!.failureExecutionId,
      checkpointStateId: recovery.recovery!.checkpointStateId,
      observations: ['100', '300', '500'],
    })
    expect(child.view.getState().error).toBeNull()
    expect(child.job?.recovery?.includedInMain).toBe(false)
    expect(session.job).toEqual(main)
  })
  it('POST uses a real ruleset header and propagates AbortSignal', async () => {
    const request = requestFixture(),
      signal = new AbortController().signal
    const transport = vi.fn<typeof fetch>().mockResolvedValue(
      new Response(JSON.stringify(jobFixture()), {
        status: 202,
        headers: {
          'X-Crafting-Ruleset': request.start.provenance.rulesetIdentity,
        },
      }),
    )
    await createHttpPathSearchAdapter(transport).create(request, signal)
    expect(transport).toHaveBeenCalledWith(
      '/api/v1/crafting/path-searches',
      expect.objectContaining({
        signal,
        method: 'POST',
        headers: expect.objectContaining({
          'X-Crafting-Ruleset': request.start.provenance.rulesetIdentity,
        }),
      }),
    )
  })
})
