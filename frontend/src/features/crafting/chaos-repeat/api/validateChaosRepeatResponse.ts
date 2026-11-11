import type { Initial } from '../../craftingApi'
import { equivalent } from '../../sanctification/equivalence'
import { verifiedFrameEvidence } from '../../workbenchHistory'
import { workbenchOmens } from '../../workbenchApi'
import { preservesChaosJewelOverflow } from '../../jewelChaos'
import {
  createChaosRepeatReceipt,
  createAbyssChaosRepeatReceipt,
} from '../chaosRepeatReceipt'
import {
  object,
  onlyKeys,
  validRepeatState,
  chaosInvariants,
  repeatItem,
  sameRepeatValue,
} from './stateValidation'
import { isChaosRepeatWitness, uniqueOmens } from './witnessShape'
import type {
  ChaosRepeatRequest,
  ChaosRepeatResult,
  ChaosRepeatTermination,
  ChaosRepeatFailure,
  ChaosRepeatRejection,
} from './types'

export const chaosRepeatMatcherVersion = 'chaos-repeat-re2j-1.8-find-ci-v1'
const chaosOmenIds = new Set(
  workbenchOmens
    .filter((omen) => omen.trigger === 'CHAOS')
    .map((omen) => omen.id),
)
export type RepeatResponseContext = {
  request: ChaosRepeatRequest
  maximumUses: number
  sha256: string
  initial: Initial
  httpStatus: number
}

/** Server-reported MATCH is not re-executed using JavaScript's different regex engine. */
export function validateChaosRepeatResponse(
  raw: unknown,
  context: RepeatResponseContext,
): ChaosRepeatResult | null {
  try {
    const { request, maximumUses, sha256, initial, httpStatus } = context
    if (
      !object(raw) ||
      !onlyKeys(raw, [
        'schemaVersion',
        'kind',
        'rulesetIdentity',
        'requestBinding',
        'finalState',
        'actualUses',
        'consumedOmens',
        'remainingOmens',
        'lastAppliedWitness',
        'termination',
        'lastRejection',
        'failure',
      ]) ||
      raw.schemaVersion !== 1 ||
      raw.kind !== 'CHAOS_REPEAT' ||
      raw.rulesetIdentity !== initial.rulesetIdentity ||
      !object(raw.requestBinding)
    )
      return null
    const binding = raw.requestBinding
    if (
      !onlyKeys(binding, [
        'requestId',
        'requestSha256',
        'action',
        'locale',
        'maximumUses',
        'matcherVersion',
      ]) ||
      binding.requestId !== request.requestId ||
      binding.requestSha256 !== sha256 ||
      binding.action !== request.action ||
      binding.locale !== request.locale ||
      binding.maximumUses !== maximumUses ||
      binding.matcherVersion !== chaosRepeatMatcherVersion ||
      !validRepeatState(raw.finalState, initial) ||
      !chaosInvariants(request.state, raw.finalState) ||
      !preservesChaosJewelOverflow(
        repeatItem(request.state),
        repeatItem(raw.finalState),
        initial.modifiers,
      ) ||
      typeof raw.actualUses !== 'number' ||
      !Number.isSafeInteger(raw.actualUses) ||
      raw.actualUses < 0 ||
      raw.actualUses > maximumUses ||
      !object(raw.consumedOmens) ||
      !uniqueOmens(raw.remainingOmens) ||
      typeof raw.termination !== 'string' ||
      !['MATCH', 'LIMIT', 'INAPPLICABLE', 'ERROR'].includes(raw.termination)
    )
      return null
    const uses = raw.actualUses
    const termination = raw.termination as ChaosRepeatTermination
    const remaining = raw.remainingOmens
    const counts = raw.consumedOmens
    const consumed: string[] = []
    // Omens cannot be re-armed during this batch. Preserve the actual multiset partition.
    for (const [id, count] of Object.entries(counts)) {
      if (
        !request.activeOmens.includes(id) ||
        !chaosOmenIds.has(id) ||
        typeof count !== 'number' ||
        !Number.isSafeInteger(count) ||
        count <= 0 ||
        count > uses ||
        count + remaining.filter((entry) => entry === id).length !==
          request.activeOmens.filter((entry) => entry === id).length
      )
        return null
    }
    if (remaining.some((id) => !request.activeOmens.includes(id))) return null
    for (const id of request.activeOmens) {
      const count = Object.hasOwn(counts, id) ? (counts[id] as number) : 0
      if (count + remaining.filter((entry) => entry === id).length !== 1)
        return null
      for (let index = 0; index < count; index++) consumed.push(id)
    }
    const rejection = raw.lastRejection
    const failure = raw.failure
    if (termination === 'INAPPLICABLE') {
      if (
        uses >= maximumUses ||
        failure !== null ||
        !object(rejection) ||
        !onlyKeys(rejection, ['code', 'reason']) ||
        rejection.code !== 'CURRENCY_INAPPLICABLE' ||
        typeof rejection.reason !== 'string'
      )
        return null
    } else if (rejection !== null) return null
    if (termination === 'ERROR') {
      if (
        httpStatus !== 500 ||
        !object(failure) ||
        !onlyKeys(failure, ['code', 'stage']) ||
        failure.code !== 'REPEAT_EXECUTION_FAILED' ||
        typeof failure.stage !== 'string' ||
        !['APPLY', 'MATCHER', 'STOP_SIGNAL'].includes(failure.stage) ||
        (failure.stage === 'MATCHER' && uses === 0) ||
        (failure.stage === 'APPLY' && uses >= maximumUses)
      )
        return null
    } else if (failure !== null || httpStatus !== 200) return null
    if (
      (termination === 'MATCH' && uses === 0) ||
      (termination === 'LIMIT' && uses !== maximumUses)
    )
      return null
    const common = {
      requestId: request.requestId,
      termination,
      failure: failure as ChaosRepeatFailure | null,
      lastRejection: rejection as ChaosRepeatRejection | null,
    }
    if (uses === 0) {
      if (
        (termination !== 'ERROR' && termination !== 'INAPPLICABLE') ||
        raw.lastAppliedWitness !== null ||
        consumed.length > 0 ||
        !sameRepeatValue(request.state, raw.finalState)
      )
        return null
      return { ...common, kind: 'no-use', termination }
    }
    const witness = raw.lastAppliedWitness
    if (
      !isChaosRepeatWitness(witness) ||
      witness.action !== request.action ||
      witness.kind !== raw.finalState.kind ||
      !equivalent([...witness.remainingOmens].sort(), [...remaining].sort()) ||
      witness.consumedOmens.some((id) => !consumed.includes(id)) ||
      (uses === 1 &&
        !equivalent([...witness.consumedOmens].sort(), [...consumed].sort()))
    )
      return null
    let receipt
    if (witness.kind === 'CONCRETE' && raw.finalState.kind === 'CONCRETE') {
      const result = {
        ...witness,
        state: raw.finalState.item,
        rulesetIdentity: initial.rulesetIdentity,
      }
      if (
        witness.snapshotId !== result.state.snapshotId ||
        witness.events.length !== 2 ||
        witness.events.filter((event) => event.kind === 'ADD').length !== 1 ||
        witness.events.filter((event) => event.kind === 'REMOVE').length !==
          1 ||
        !witness.events.every(
          (event) =>
            Number.isFinite(event.selectionProbability) &&
            event.selectionProbability > 0 &&
            event.selectionProbability <= 1 &&
            (event.kind === 'ADD' || event.kind === 'REMOVE'),
        ) ||
        !verifiedFrameEvidence(
          {
            state: result.state,
            action: request.action,
            rulesetIdentity: initial.rulesetIdentity,
            evidence: result,
          },
          initial,
        )
      )
        return null
      const input = {
        before: repeatItem(request.state),
        action: request.action,
        uses,
        finalWitness: result,
        consumedOmens: consumed,
        witness,
      }
      receipt = createChaosRepeatReceipt(input, (candidate) =>
        equivalent(candidate, input),
      )
    } else if (
      witness.kind === 'ABYSS' &&
      raw.finalState.kind === 'ABYSS' &&
      request.state.kind === 'ABYSS'
    ) {
      const input = {
        before: request.state.abyss,
        finalState: raw.finalState.abyss,
        rulesetIdentity: initial.rulesetIdentity,
        action: request.action,
        uses,
        finalWitness: witness,
        consumedOmens: consumed,
      }
      receipt = createAbyssChaosRepeatReceipt(input, (candidate) =>
        equivalent(candidate, input),
      )
    } else return null
    if (!receipt) return null
    return {
      ...common,
      kind:
        termination === 'MATCH' || termination === 'LIMIT'
          ? 'completed'
          : 'partial',
      finalState: structuredClone(raw.finalState),
      actualUses: uses,
      consumedOmens: consumed,
      remainingOmens: [...remaining],
      receipt,
    }
  } catch {
    return null
  }
}
