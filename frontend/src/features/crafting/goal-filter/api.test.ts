import { afterEach, describe, expect, it, vi } from 'vitest'
import { createHttpGoalFilterAdapter, GoalFilterApiError } from './api'
import { fixtureContext } from './mock'
import fixture from '../../../../../contracts/support-goal-filter-v1/fixtures.json'
import type { GoalFilter } from './types'
afterEach(() => vi.unstubAllGlobals())
describe('goal filter HTTP adapter', () => {
  it('preserves wire request and AbortSignal', async () => {
    const signal = new AbortController().signal
    const fetchMock = vi.fn().mockImplementation(
      async () =>
        new Response(JSON.stringify(fixture.apiExamples.validateResponse), {
          status: 200,
        }),
    )
    vi.stubGlobal('fetch', fetchMock)
    const adapter = createHttpGoalFilterAdapter()
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
    expect(fetchMock.mock.calls[1]![0]).toContain('itemLevel=82')
  })
  it('does not expose server exception messages', async () => {
    vi.stubGlobal(
      'fetch',
      vi
        .fn()
        .mockImplementation(
          async () => new Response('private stack trace', { status: 503 }),
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
