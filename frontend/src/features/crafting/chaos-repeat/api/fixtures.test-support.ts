import { createHash } from 'node:crypto'
import { initialFixture } from '../../../../shared/test/craftingFixtures'
import { concreteInitial } from '../../workbenchApi'
import type { Initial } from '../../craftingApi'
import type { ChaosRepeatAction } from '../chaosRepeatReceipt'
import type {
  ChaosRepeatRequest,
  ChaosRepeatState,
  ChaosRepeatWitness,
} from './types'
import { chaosRepeatMatcherVersion } from './validateChaosRepeatResponse'

export const repeatInitial: Initial = {
  ...initialFixture,
  modifiers: {
    ...initialFixture.modifiers,
    s: { ...initialFixture.modifiers.s!, requiredItemLevel: 50 },
  },
}
export function requestFor(
  kind: ChaosRepeatState['kind'] = 'CONCRETE',
  action: ChaosRepeatAction = 'CHAOS',
): ChaosRepeatRequest {
  const item = {
    ...concreteInitial(repeatInitial),
    rarity: 'RARE' as const,
    explicits: [{ modifierId: 'p', values: { life: 10 } }],
  }
  const state: ChaosRepeatState =
    kind === 'CONCRETE'
      ? { kind, item }
      : {
          kind,
          abyss: {
            item: { ...item, explicits: [] },
            slots: [
              {
                id: 'prefix',
                side: 'PREFIX',
                modifier: item.explicits[0]!,
                desecrated: false,
              },
              {
                id: 'hidden',
                side: 'SUFFIX',
                modifier: null,
                desecrated: true,
                minimumModifierLevel: 40,
              },
            ],
            offer: null,
          },
        }
  return {
    requestId: '123e4567-e89b-12d3-a456-426614174000',
    state,
    action,
    activeOmens: ['Omen_of_Sinistral_Erasure', 'Omen_of_Sinistral_Necromancy'],
    pattern: '생명력\\s+17\n續行',
    locale: 'ko',
    maximumUses: 3,
  }
}

export function envelopeFor(
  request: ChaosRepeatRequest,
  body = JSON.stringify(request),
) {
  const finalState = structuredClone(request.state)
  if (finalState.kind === 'CONCRETE')
    finalState.item.explicits[0]!.values.life = 17
  else finalState.abyss.slots[0]!.modifier!.values.life = 17
  const common = {
    kind: request.state.kind,
    action: request.action,
    applied: true as const,
    reason: 'Synthetic final success',
    consumedOmens: [] as string[],
    remainingOmens: ['Omen_of_Sinistral_Necromancy'],
  }
  const lastAppliedWitness: ChaosRepeatWitness =
    request.state.kind === 'CONCRETE'
      ? {
          ...common,
          kind: 'CONCRETE',
          ruleVersion: 'solar-workbench-six-v1',
          ledgerVersion: 'solar-uniform-assumptions-v1',
          snapshotId: 'fixture-v1',
          events: [
            {
              kind: 'REMOVE',
              modifierId: 'p',
              values: {},
              selectionProbability: 1,
            },
            {
              kind: 'ADD',
              modifierId: 'p',
              values: { life: 17 },
              selectionProbability: 1,
            },
          ],
          assumptions: [],
        }
      : { ...common, kind: 'ABYSS' }
  return {
    schemaVersion: 1,
    kind: 'CHAOS_REPEAT',
    rulesetIdentity: repeatInitial.rulesetIdentity,
    requestBinding: {
      requestId: request.requestId,
      requestSha256: createHash('sha256').update(body, 'utf8').digest('hex'),
      action: request.action,
      locale: request.locale,
      maximumUses: request.maximumUses ?? 100,
      matcherVersion: chaosRepeatMatcherVersion,
    },
    finalState,
    actualUses: 3,
    consumedOmens: { Omen_of_Sinistral_Erasure: 1 } as Record<string, number>,
    remainingOmens: ['Omen_of_Sinistral_Necromancy'],
    lastAppliedWitness: lastAppliedWitness as ChaosRepeatWitness | null,
    termination: 'MATCH',
    lastRejection: null as { code: string; reason: string } | null,
    failure: null as { code: string; stage: string } | null,
  }
}
export type TestEnvelope = ReturnType<typeof envelopeFor>
export function batchResponse(value: unknown, status = 200) {
  return new Response(JSON.stringify(value), {
    status,
    headers: {
      'Content-Type': 'application/json',
      'X-Crafting-Ruleset': repeatInitial.rulesetIdentity,
    },
  })
}
