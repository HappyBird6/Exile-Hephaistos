import { QueryClient } from '@tanstack/react-query'
import { createStore } from 'zustand/vanilla'
import type { PathSearchAdapter } from './api'
import type {
  CreateRequest,
  GraphPage,
  JobSnapshot,
  RecoveryRequest,
} from './types'
import {
  assertProvenance,
  canonical,
  mergeGraph,
  PathSearchError,
  validateCreated,
  validateJob,
  validateRequest,
} from './validation'

export type SearchView = {
  generation: number
  pending: boolean
  error: string | null
  operation: boolean
}
export class PathSearchSession {
  readonly view = createStore<SearchView>(() => ({
    generation: 0,
    pending: false,
    error: null,
    operation: false,
  }))
  private abort = new AbortController()
  private request: CreateRequest | null = null
  private blockedRevision = -1
  private consumed = new Set<string>()
  private paging = false
  private epoch = 0
  constructor(
    readonly client: QueryClient,
    readonly adapter: PathSearchAdapter,
    readonly id = crypto.randomUUID(),
  ) {}
  key(generation = this.view.getState().generation) {
    return ['path-tree', this.adapter.id, this.id, generation] as const
  }
  graphKey() {
    return [...this.key(), 'graph'] as const
  }
  get job() {
    return this.client.getQueryData<JobSnapshot>(this.key())
  }
  get graph() {
    return this.client.getQueryData<GraphPage>(this.graphKey())
  }
  private current(generation: number) {
    return generation === this.view.getState().generation
  }
  private fail(error: unknown, generation: number) {
    if (
      !this.current(generation) ||
      (error instanceof Error && error.name === 'AbortError')
    )
      return
    const code =
      error instanceof PathSearchError ? error.code : 'REQUEST_FAILED'
    this.view.setState({ error: code, pending: false, operation: false })
    // Never keep a result actionable after expiration, a version change or malformed data.
    if (
      [
        'JOB_EXPIRED',
        'REVISION_EXPIRED',
        'VERSION_CHANGED',
        'CATALOG_VERSION_MISMATCH',
        'RULESET_IDENTITY_MISMATCH',
        'SNAPSHOT_MISMATCH',
        'UNKNOWN_STAT',
        'INVALID_ITEM',
        'INVALID_RESPONSE',
      ].includes(code)
    ) {
      this.abort.abort()
      this.client.removeQueries({ queryKey: this.key() })
    }
  }
  private accept(
    value: JobSnapshot,
    generation: number,
    allowResume = false,
  ): JobSnapshot | undefined {
    if (!this.current(generation)) return
    validateJob(value)
    const old = this.job
    if (old) {
      if (
        value.jobId !== old.jobId ||
        value.clientRequestId !== old.clientRequestId ||
        value.requestFingerprint !== old.requestFingerprint ||
        canonical(value.recovery) !== canonical(old.recovery)
      )
        throw new PathSearchError('INVALID_RESPONSE')
      assertProvenance(value.provenance, old.provenance)
      if (value.revision < old.revision) return old
      if (
        value.revision === old.revision &&
        canonical(value) !== canonical(old)
      )
        throw new PathSearchError('INVALID_RESPONSE')
      if (
        !allowResume &&
        this.blockedRevision >= 0 &&
        ['RUNNING', 'QUEUED'].includes(value.status)
      )
        return old
    }
    if (value.status === 'EXPIRED') throw new PathSearchError('JOB_EXPIRED')
    if (!old || value.revision > old.revision) {
      const empty = {
        ...value.graph,
        nodes: [],
        executions: [],
        edges: [],
        expansions: [],
      }
      this.client.setQueryData(this.graphKey(), mergeGraph(empty, value.graph))
      this.consumed.clear()
    }
    this.client.setQueryData(this.key(), value)
    this.view.setState({ pending: false, error: null })
    return value
  }
  async start(request: CreateRequest) {
    this.invalidate()
    const generation = this.view.getState().generation
    this.request = structuredClone(request)
    this.view.setState({ pending: true, error: null })
    try {
      validateRequest(request)
      // Creation is allowed to acknowledge after local invalidation so its server job can be cancelled.
      const value = await this.adapter.create(
        this.request,
        new AbortController().signal,
      )
      validateCreated(value, request)
      if (!this.current(generation)) {
        await this.cancelDetached(value)
        return
      }
      this.accept(value, generation)
    } catch (error) {
      this.fail(error, generation)
    }
  }
  invalidate() {
    const old = this.job
    this.abort.abort()
    this.epoch++
    this.abort = new AbortController()
    this.view.setState((s) => ({
      generation: s.generation + 1,
      pending: false,
      error: null,
      operation: false,
    }))
    this.blockedRevision = -1
    this.consumed.clear()
    this.paging = false
    if (old && ['RUNNING', 'QUEUED', 'PAUSED'].includes(old.status))
      void this.cancelDetached(old).catch(() => {
        /* Old results remain detached even if server cancellation fails. */
      })
  }
  private async cancelDetached(job: JobSnapshot) {
    const signal = new AbortController().signal
    try {
      await this.adapter.mutate(
        job.jobId,
        {
          version: 1,
          operation: 'CANCEL',
          commandId: crypto.randomUUID(),
          expectedRevision: job.revision,
        },
        job.provenance.transition.rulesetIdentity,
        signal,
      )
    } catch (error) {
      if (
        !(error instanceof PathSearchError) ||
        error.code !== 'REVISION_CONFLICT'
      )
        throw error
      const current = await this.adapter.read(job.jobId, signal)
      if (current.jobId !== job.jobId)
        throw new PathSearchError('INVALID_RESPONSE')
      assertProvenance(current.provenance, job.provenance)
      if (['RUNNING', 'QUEUED', 'PAUSED'].includes(current.status))
        await this.adapter.mutate(
          current.jobId,
          {
            version: 1,
            operation: 'CANCEL',
            commandId: crypto.randomUUID(),
            expectedRevision: current.revision,
          },
          current.provenance.transition.rulesetIdentity,
          signal,
        )
    }
  }
  async refresh(): Promise<JobSnapshot> {
    const generation = this.view.getState().generation,
      old = this.job,
      epoch = this.epoch
    if (!old) throw new PathSearchError('NO_JOB')
    try {
      const value = await this.adapter.read(old.jobId, this.abort.signal)
      if (epoch !== this.epoch) return this.job ?? old
      return this.accept(value, generation) ?? old
    } catch (error) {
      if (epoch !== this.epoch) return this.job ?? old
      this.fail(error, generation)
      throw error
    }
  }
  async command(operation: 'CANCEL' | 'RESUME') {
    const generation = this.view.getState().generation
    if (this.view.getState().operation || !this.job) return
    this.view.setState({ operation: true, error: null })
    // Drop outstanding reads, but send a real server command with a fresh signal.
    this.abort.abort()
    this.abort = new AbortController()
    this.epoch++
    try {
      for (let retry = 0; retry < 2; retry++) {
        const old = this.job
        if (!old || !this.current(generation)) return
        if (operation === 'RESUME' && !old.resumable) return
        try {
          const value = await this.adapter.mutate(
            old.jobId,
            {
              version: 1,
              operation,
              commandId: crypto.randomUUID(),
              expectedRevision: old.revision,
            },
            old.provenance.transition.rulesetIdentity,
            this.abort.signal,
          )
          const accepted = this.accept(
            value,
            generation,
            operation === 'RESUME',
          )
          if (accepted && this.current(generation))
            this.blockedRevision =
              operation === 'CANCEL' ? accepted.revision : -1
          return
        } catch (error) {
          if (
            retry ||
            !(error instanceof PathSearchError) ||
            error.code !== 'REVISION_CONFLICT'
          )
            throw error
          await this.refresh()
          if (operation === 'CANCEL' && this.job?.status === 'COMPLETED') return
        }
      }
    } catch (error) {
      this.fail(error, generation)
    } finally {
      if (this.current(generation)) this.view.setState({ operation: false })
    }
  }
  async more() {
    const generation = this.view.getState().generation,
      old = this.graph
    if (!old?.nextCursor || this.paging || this.consumed.has(old.nextCursor))
      return
    const cursor = old.nextCursor
    this.paging = true
    try {
      const page = await this.adapter.graph(
        old.jobId,
        old.revision,
        cursor,
        this.abort.signal,
      )
      if (!this.current(generation) || this.job?.revision !== old.revision)
        return
      if (
        page.nextCursor === cursor ||
        (page.nextCursor && this.consumed.has(page.nextCursor))
      )
        throw new PathSearchError('INVALID_RESPONSE')
      const merged = mergeGraph(this.graph!, page)
      this.consumed.add(cursor)
      this.client.setQueryData(this.graphKey(), merged)
    } catch (error) {
      this.fail(error, generation)
    } finally {
      if (this.current(generation)) this.paging = false
    }
  }
  async recover(parent: JobSnapshot, request: RecoveryRequest) {
    this.invalidate()
    const generation = this.view.getState().generation
    this.view.setState({ pending: true, error: null })
    try {
      const job = await this.adapter.recover(
        parent.jobId,
        request,
        parent.provenance.transition.rulesetIdentity,
        new AbortController().signal,
      )
      validateJob(job)
      const expected = {
        parentJobId: parent.jobId,
        parentRevision: request.parentRevision,
        failureExecutionId: request.failureExecutionId,
        checkpointStateId: request.checkpointStateId,
        conditional: true,
        includedInMain: false,
      }
      if (
        job.clientRequestId !== request.clientRequestId ||
        canonical(job.recovery) !== canonical(expected)
      )
        throw new PathSearchError('INVALID_RESPONSE')
      assertProvenance(job.provenance, parent.provenance)
      if (!this.current(generation)) {
        await this.cancelDetached(job)
        return
      }
      this.accept(job, generation)
    } catch (error) {
      this.fail(error, generation)
    }
  }
}
