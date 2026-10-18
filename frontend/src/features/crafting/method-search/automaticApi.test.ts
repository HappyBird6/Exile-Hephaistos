import { afterEach, expect, it, vi } from 'vitest'
import { automaticApi } from './automaticApi'
import { requestFixture } from '../path-tree/fixtures.test-support'

const id = '11111111-1111-4111-8111-111111111111'
const request = requestFixture()
function view(goalCount: number, openCount = 1) {
  return {
    version: 'automatic-method-job-v1',
    id,
    revision: 1,
    generation: 1,
    status: 'COMPLETE',
    reason: 'SUPPORTED_PLANS_COMPLETE',
    complete: true,
    operations: 26,
    retainedStates: goalCount + openCount,
    nodes: Array.from({ length: goalCount + openCount }, (_, index) => ({
      id: index,
      depth: index === 0 ? 0 : 1,
      quality40: false,
      item: request.start.item,
      progress: { goal: index < openCount ? 'NO_MATCH' : 'MATCH', entries: [] },
      discoveryFinished: true,
      directFinished: true,
    })),
    methods: [],
    recommendations: [],
  }
}
function respond(payload: unknown) {
  vi.stubGlobal(
    'fetch',
    vi.fn(
      async () =>
        new Response(JSON.stringify(payload), {
          headers: {
            'X-Crafting-Ruleset': request.start.provenance.rulesetIdentity,
          },
        }),
    ),
  )
  return automaticApi.get(id, request.start.provenance.rulesetIdentity)
}
afterEach(() => vi.unstubAllGlobals())
it('accepts completed terminal outcomes beyond the expandable-state limit', async () => {
  await expect(respond(view(200))).resolves.toHaveProperty('nodes.length', 201)
})
it('still rejects too many expandable states', async () => {
  await expect(respond(view(0, 129))).rejects.toThrow('Invalid automatic graph')
})
it('still bounds total retained response nodes', async () => {
  await expect(
    respond({ ...view(0), nodes: Array(50001).fill(null) }),
  ).rejects.toThrow('Invalid automatic graph')
})
