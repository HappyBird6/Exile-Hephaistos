import type { CreateRequest } from '../path-tree/types'
import type { AutomaticAction } from '../method-search/automaticApi'

export type Availability = {
  enabled: boolean
  configured: boolean
  model: string
  corpusVersion: string
}
export type PromptPreview = {
  promptId: string
  prompt: { system: string; user: string }
}
export type StrategyResponse = {
  status: string
  strategies: {
    initial: AutomaticAction[]
    cycle: AutomaticAction[]
    documents: string[]
    explanation: string
    preparationSuccess: string
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
  signal: AbortSignal | undefined,
  body?: unknown,
): Promise<T> {
  const response = await fetch(base + path, {
    method: body ? 'POST' : 'GET',
    cache: 'no-store',
    signal: signal ?? null,
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
export function preparePrompt(
  request: CreateRequest,
  abstractQuality: number,
  locale: string,
) {
  return read<PromptPreview>('/prompt', undefined, {
    request: {
      version: 2,
      clientRequestId: request.clientRequestId,
      start: request.start,
      goal: request.goal,
      activeOmens: request.activeOmens,
    },
    mode: 'RETRIEVED',
    initialStage: true,
    abstractQuality,
    locale,
  })
}
export function generateStrategies(promptId: string, signal: AbortSignal) {
  return read<StrategyResponse>('/generate', signal, { promptId })
}
