import type {
  Availability,
  Bucket,
  Initial,
  Outcome,
} from '../../features/crafting/craftingApi'
import { actions } from '../../features/crafting/craftingApi'
import { requestFixture } from '../../features/crafting/path-tree/fixtures.test-support'

export const rootBucket: Bucket = {
  snapshotId: 'fixture-v1',
  baseItemId: 'solar',
  itemLevel: 82,
  rarity: 'NORMAL',
  implicits: [
    { modifierId: 'implicit', values: { base_spirit_from_equipment: 15 } },
  ],
  modifierIds: [],
  conditions: [],
}
export function fixtureActions(state: Bucket): Availability[] {
  return actions.map((action) => ({
    action,
    available:
      state.rarity === 'NORMAL'
        ? action === 'TRANSMUTATION'
        : state.rarity === 'MAGIC'
          ? (action === 'AUGMENTATION' && state.modifierIds.length < 2) ||
            action === 'REGAL' ||
            action === 'ANNULMENT'
          : action === 'EXALTED' ||
            action === 'CHAOS' ||
            action === 'ANNULMENT',
    reason: 'This currency is unavailable for this state.',
  }))
}
export const initialFixture: Initial = {
  baseRules: {
    magicPrefixes: 1,
    magicSuffixes: 1,
    rarePrefixes: 3,
    rareSuffixes: 3,
  },
  rulesetIdentity: 'fixture-ruleset',
  ruleVersion: 'fixture-rules',
  metadata: {
    snapshotId: 'fixture-v1',
    retrievedAt: '2026-09-29T00:00:00Z',
    sourceUrl: 'https://poe2db.tw/us/Amulets#ModifiersCalc',
  },
  id: 'root',
  state: rootBucket,
  modifiers: {
    implicit: {
      id: 'implicit',
      name: 'Spirit',
      text: '+(10—15) to Spirit',
      tier: 0,
      affixType: 'NONE',
      familyIds: ['Spirit'],
      stats: [{ id: 'base_spirit_from_equipment', min: 10, max: 15 }],
    },
    p: {
      id: 'p',
      name: 'Healthy',
      text: '+(10—20) to maximum Life',
      tier: 1,
      affixType: 'PREFIX',
      familyIds: ['Life'],
      stats: [{ id: 'life', min: 10, max: 20 }],
    },
    s: {
      id: 's',
      name: 'of Strength',
      text: '+(5—8) to Strength',
      tier: 1,
      affixType: 'SUFFIX',
      familyIds: ['Strength'],
      stats: [{ id: 'strength', min: 5, max: 8 }],
    },
  },
  actions: fixtureActions(rootBucket),
}
export const firstOutcomes: Outcome[] = [
  {
    id: 'magic-p',
    state: { ...rootBucket, rarity: 'MAGIC', modifierIds: ['p'] },
    probability: 0.6,
  },
  {
    id: 'magic-s',
    state: { ...rootBucket, rarity: 'MAGIC', modifierIds: ['s'] },
    probability: 0.4,
  },
]
export const jsonResponse = (value: unknown, status = 200) =>
  new Response(JSON.stringify(value), {
    status,
    headers: {
      'Content-Type': 'application/json',
      'X-Crafting-Ruleset': 'fixture-ruleset',
    },
  })
export function fixtureFetch(
  input: RequestInfo | URL,
  init?: RequestInit,
): Promise<Response> {
  const url = String(input)
  if (url.endsWith('/basic-paths/provenance'))
    return Promise.resolve(
      jsonResponse({
        ...requestFixture().start.provenance,
        rulesetIdentity: initialFixture.rulesetIdentity,
      }),
    )
  if (url.includes('/goal-filters/catalog?')) {
    const params = new URL(url, 'http://localhost').searchParams
    return Promise.resolve(
      jsonResponse({
        version: 1,
        catalogVersion: 'test-catalog',
        context: {
          snapshotId: params.get('snapshotId'),
          baseItemId: params.get('baseItemId'),
          itemLevel: Number(params.get('itemLevel')),
        },
        stats: [],
        groupTypes: [],
        issues: [],
      }),
    )
  }
  if (url.endsWith('/goal-filters/validate'))
    return Promise.resolve(
      jsonResponse({
        version: 1,
        valid: true,
        issues: [],
        capabilities: { evaluation: 'SUPPORTED', probability: 'UNSUPPORTED' },
      }),
    )
  if (url.includes('/crafting/initial')) {
    const level = Number(
      new URL(url, 'http://localhost').searchParams.get('itemLevel') ?? 82,
    )
    return Promise.resolve(
      jsonResponse({
        ...initialFixture,
        state: { ...rootBucket, itemLevel: level },
      }),
    )
  }
  const body = JSON.parse(String(init?.body ?? '{}')) as {
    action: string
    state: Bucket
  }
  if (url.endsWith('/workbench/apply')) {
    const request = JSON.parse(String(init?.body)) as {
      state: import('../../features/crafting/workbenchApi').ConcreteItem
      action: string
      activeOmens?: string[]
    }
    const applied =
      request.action === 'TRANSMUTATION' && request.state.rarity === 'NORMAL'
    return Promise.resolve(
      jsonResponse({
        ruleVersion: 'solar-workbench-six-v1',
        rulesetIdentity: 'fixture-ruleset',
        ledgerVersion: 'solar-uniform-assumptions-v1',
        snapshotId: request.state.snapshotId,
        action: request.action,
        applied,
        consumedOmens: [],
        remainingOmens: request.activeOmens ?? [],
        reason: applied ? '' : 'This currency cannot be used on this rarity.',
        state: applied
          ? {
              ...request.state,
              rarity: 'MAGIC',
              explicits: [{ modifierId: 'p', values: { life: 17 } }],
            }
          : request.state,
        events: applied
          ? [
              {
                kind: 'ADD',
                modifierId: 'p',
                values: { life: 17 },
                selectionProbability: 0.6,
              },
            ]
          : [],
        assumptions: applied
          ? [
              {
                id: 'uniform-integer-roll-v1',
                candidateUnit: 'life',
                n: 11,
                candidates: [],
                min: 10,
                max: 20,
                sourceUrl: 'https://poe2db.tw/us/Amulets#ModifiersCalc',
                reason: 'No published numeric roll weights.',
              },
            ]
          : [],
      }),
    )
  }
  if (url.endsWith('/actions'))
    return Promise.resolve(
      jsonResponse(fixtureActions(body as unknown as Bucket)),
    )
  if (url.endsWith('/transitions'))
    return Promise.resolve(
      jsonResponse({
        fromId: 'root',
        action: body.action,
        available: true,
        reason: '',
        outcomes: body.state.modifierIds.length
          ? [
              {
                id: 'magic-ps',
                state: { ...body.state, modifierIds: ['p', 's'] },
                probability: 1,
              },
            ]
          : firstOutcomes.map((o) => ({
              ...o,
              state: { ...o.state, itemLevel: body.state.itemLevel },
            })),
      }),
    )
  if (url.endsWith('/explore'))
    return Promise.resolve(
      jsonResponse({
        nodes: { root: body.state },
        edges: [],
        terminals: [
          { id: 'root', step: 0, status: 'DEFERRED', probability: 1 },
        ],
        completedProbability: 0,
        blockedProbability: 0,
        unexploredProbability: 1,
        complete: false,
      }),
    )
  return Promise.reject(new Error(`Unexpected test request: ${url}`))
}
