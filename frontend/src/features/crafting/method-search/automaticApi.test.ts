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

it('opens historical results across a policy header change without allowing execution', async () => {
  const payload = { ...view(1), reason: 'LEGACY_READ_ONLY', canResume: false }
  vi.stubGlobal(
    'fetch',
    vi.fn(
      async () =>
        new Response(JSON.stringify(payload), {
          headers: { 'X-Crafting-Ruleset': 'new-policy-identity' },
        }),
    ),
  )
  await expect(
    automaticApi.get(id, request.start.provenance.rulesetIdentity),
  ).resolves.toHaveProperty('canResume', false)
  payload.reason = 'SUPPORTED_PLANS_COMPLETE'
  await expect(
    automaticApi.get(id, request.start.provenance.rulesetIdentity),
  ).rejects.toThrow()
})
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

it('accepts concrete aliases above the old state count only with consistent bounded computation accounting', async () => {
  const payload = {
    ...view(0, 129),
    resources: {
      computationStates: 3,
      stateLimit: 64,
      methodExecutions: 0,
      methodLimit: 128,
      concreteStates: 129,
      concreteMethods: 0,
      retainedRecords: 129,
      retainedLimit: 20000,
      primitiveOutcomeBudget: 5000,
      blockers: [],
    },
  }
  await expect(respond(payload)).resolves.toHaveProperty('nodes.length', 129)
  await expect(
    respond({
      ...payload,
      resources: { ...payload.resources, concreteStates: 128 },
    }),
  ).rejects.toThrow('Invalid resource accounting')
  await expect(
    respond({
      ...payload,
      resources: { ...payload.resources, methodExecutions: 257 },
    }),
  ).rejects.toThrow('Invalid resource accounting')
})
