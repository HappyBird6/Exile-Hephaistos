import type { CreateRequest } from '../path-tree/types'
import type { AutomaticAction } from '../method-search/automaticApi'

export type Availability = {
  enabled: boolean
  configured: boolean
  model: string
  corpusVersion: string
}
export type Report = {
  model: string
  corpusVersion: string
  proposalStatus: string
  candidateMatch: boolean
  executionMatch: boolean
  reference: unknown[]
  missing: number[]
  extra: number[]
  validationErrors: string[]
  documents: string[]
  inputTokens: number
  outputTokens: number
  cached: boolean
  elapsedMillis: number
  strategies: {
    initial: AutomaticAction[]
    cycle: AutomaticAction[]
    documents: string[]
    explanation: string
    preparationSuccess: string
  }[]
  executions: {
    referenceIndex: number
    proposedIndex: number
    status: string
  }[]
}
export class RagApiError extends Error {
  constructor(
    public code: string,
    public status: number,
  ) {
    super(code)
  }
}
const base = '/api/support-test/rag'
async function read<T>(
  path: string,
  signal: AbortSignal,
  body?: unknown,
): Promise<T> {
  const response = await fetch(base + path, {
    method: body ? 'POST' : 'GET',
    cache: 'no-store',
    signal,
    headers: {
      ...(body ? { 'Content-Type': 'application/json' } : {}),
    },
    ...(body ? { body: JSON.stringify(body) } : {}),
  })
  if (!response.ok) {
    const problem = (await response.json().catch(() => null)) as {
      code?: string
    } | null
    throw new RagApiError(
      problem?.code ?? 'RAG_REQUEST_FAILED',
      response.status,
    )
  }
  return response.json() as Promise<T>
}
export const ragAvailability = (signal: AbortSignal) =>
  read<Availability>('', signal)
export function compareStrategies(
  request: CreateRequest,
  mode: 'ALL' | 'RETRIEVED',
  initialStage: boolean,
  abstractQuality: number,
  locale: string,
  signal: AbortSignal,
) {
  return read<Report>('/compare', signal, {
    request: {
      version: 2,
      clientRequestId: request.clientRequestId,
      start: request.start,
      goal: request.goal,
      activeOmens: request.activeOmens,
    },
    mode,
    initialStage,
    abstractQuality,
    locale,
  })
}
