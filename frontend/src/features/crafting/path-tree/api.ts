import { rulesetHeaders, verifyRulesetResponse } from '../rulesetIdentity'
import type {
  CreateRequest,
  GraphPage,
  JobSnapshot,
  MutationRequest,
  RecoveryRequest,
} from './types'
import {
  assertShape,
  PathSearchError,
  validateCreated,
  validateGraph,
  validateJob,
  validateRequest,
} from './validation'

export interface PathSearchAdapter {
  readonly id: string
  create(request: CreateRequest, signal: AbortSignal): Promise<JobSnapshot>
  read(jobId: string, signal: AbortSignal): Promise<JobSnapshot>
  graph(
    jobId: string,
    revision: number,
    cursor: string,
    signal: AbortSignal,
  ): Promise<GraphPage>
  mutate(
    jobId: string,
    request: MutationRequest,
    ruleset: string,
    signal: AbortSignal,
  ): Promise<JobSnapshot>
  recover(
    jobId: string,
    request: RecoveryRequest,
    ruleset: string,
    signal: AbortSignal,
  ): Promise<JobSnapshot>
}
export function createHttpPathSearchAdapter(
  transport: typeof fetch = fetch,
): PathSearchAdapter {
  const root = '/api/v1/crafting/path-searches'
  async function call(
    path: string,
    signal: AbortSignal,
    body?: unknown,
    ruleset?: string,
  ) {
    const init: RequestInit = {
      signal,
      ...(body === undefined
        ? {}
        : {
            method: 'POST',
            headers: rulesetHeaders(ruleset),
            body: JSON.stringify(body),
          }),
    }
    let response: Response
    try {
      response = await transport(root + path, init)
    } catch (error) {
      // A lost acknowledgement must reuse the exact request/command ID and body.
      if (signal.aborted || !(error instanceof TypeError)) throw error
      response = await transport(root + path, init)
    }
    const data: unknown = await response.json()
    if (!response.ok) {
      assertShape('problem', data)
      throw new PathSearchError(
        (data as { code: string }).code,
        response.status,
      )
    }
    if (ruleset) verifyRulesetResponse(response, ruleset)
    return data
  }
  async function job(
    path: string,
    signal: AbortSignal,
    body?: unknown,
    ruleset?: string,
  ) {
    const value = (await call(path, signal, body, ruleset)) as JobSnapshot
    validateJob(value)
    return value
  }
  return {
    id: 'path-search-v1-http',
    async create(request, signal) {
      validateRequest(request)
      const value = await job(
        '',
        signal,
        request,
        request.start.provenance.rulesetIdentity,
      )
      validateCreated(value, request)
      return value
    },
    read: (id, signal) => job('/' + encodeURIComponent(id), signal),
    async graph(id, revision, cursor, signal) {
      const value = (await call(
        `/${encodeURIComponent(id)}/graph?${new URLSearchParams({ revision: String(revision), cursor })}`,
        signal,
      )) as GraphPage
      validateGraph(value)
      return value
    },
    mutate: (id, request, ruleset, signal) => {
      assertShape('mutationRequest', request)
      return job(
        `/${encodeURIComponent(id)}/${request.operation.toLowerCase()}`,
        signal,
        request,
        ruleset,
      )
    },
    recover: (id, request, ruleset, signal) => {
      assertShape('recoveryRequest', request)
      return job(
        `/${encodeURIComponent(id)}/recoveries`,
        signal,
        request,
        ruleset,
      )
    },
  }
}
