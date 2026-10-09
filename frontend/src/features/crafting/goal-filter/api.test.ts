import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  createHttpGoalFilterAdapter,
  GoalFilterApiError,
  readGoalFilterResponse,
} from './api'
import { fixtureContext, mockGoalFilterAdapter } from './mock'
import fixture from '../../../../../contracts/support-goal-filter-v1/fixtures.json'
import type { GoalFilter } from './types'
afterEach(() => vi.unstubAllGlobals())
describe('goal filter HTTP adapter', () => {
  it('preserves wire request and AbortSignal', async () => {
    const signal = new AbortController().signal
    const catalogResponse = await mockGoalFilterAdapter.catalog(
      fixtureContext,
      signal,
    )
    const fetchMock = vi.fn().mockImplementation(
      async (url: string) =>
        new Response(
          JSON.stringify(
            url.includes('/catalog')
              ? catalogResponse
              : fixture.apiExamples.validateResponse,
          ),
          {
            headers: { 'X-Crafting-Ruleset': 'fixture-ruleset' },
            status: 200,
          },
        ),
    )
    vi.stubGlobal('fetch', fetchMock)
    const adapter = createHttpGoalFilterAdapter()
    await adapter.catalog(fixtureContext, signal)
    await adapter.validate(
      fixtureContext,
      fixture.apiExamples.validateRequest.goal as GoalFilter,
      signal,
    )
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/crafting/support/goal-filters/validate',
      expect.objectContaining({
        signal,
        method: 'POST',
        body: JSON.stringify(fixture.apiExamples.validateRequest),
      }),
    )
    await adapter.catalog(fixtureContext, signal)
    expect(fetchMock.mock.calls[0]![0]).toContain('itemLevel=82')
  })
  it('does not expose server exception messages', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockImplementation(
        async () =>
          new Response('private stack trace', {
            headers: { 'X-Crafting-Ruleset': 'fixture-ruleset' },
            status: 503,
          }),
      ),
    )
    await expect(
      createHttpGoalFilterAdapter().catalog(
        fixtureContext,
        new AbortController().signal,
      ),
    ).rejects.toEqual(new GoalFilterApiError(503))
  })
})

describe('goal filter Problem Details', () => {
  it('retains scoped issue codes and pointers without displaying unrelated response details', async () => {
    const issue = {
      code: 'CONTEXT_BASE_MISMATCH',
      path: '/goal/general/baseItemId',
      message: 'Goal and request base must match.',
      severity: 'ERROR',
    }
    try {
      await readGoalFilterResponse(
        new Response(
          JSON.stringify({ detail: 'unrelated detail', issues: [issue] }),
          { headers: { 'X-Crafting-Ruleset': 'fixture-ruleset' }, status: 422 },
        ),
      )
      throw new Error('Expected failure')
    } catch (error) {
      expect(error).toBeInstanceOf(GoalFilterApiError)
      expect((error as GoalFilterApiError).issues).toEqual([issue])
      expect((error as Error).message).not.toContain('unrelated detail')
    }
  })
  it('handles non-JSON service errors with the same safe status message', async () => {
    await expect(
      readGoalFilterResponse(
        new Response('<html>proxy failure</html>', {
          headers: { 'X-Crafting-Ruleset': 'fixture-ruleset' },
          status: 503,
        }),
      ),
    ).rejects.toMatchObject({
      status: 503,
      issues: [],
      message: 'Goal filter request failed (503)',
    })
  })
})
