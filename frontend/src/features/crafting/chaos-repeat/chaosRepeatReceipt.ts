import { workbenchOmens } from '../workbenchApi'
import type { AppliedItem, ConcreteItem } from '../workbenchApi'
import type { Frame } from '../workbenchHistory'
import { supportsConcreteStateShape } from '../workbenchStateShape'
import { isRulesetIdentity } from '../rulesetIdentity'
import type { AbyssState } from '../AbyssPanel'
import { equivalent } from '../sanctification/equivalence'
import { isChaosRepeatWitness, knownOmens } from './api/witnessShape'
import type { ChaosRepeatWitness } from './api/types'

export type ChaosRepeatAction = 'CHAOS' | 'GREATER_CHAOS' | 'PERFECT_CHAOS'
export type ChaosRepeatMetadata = {
  version: 1
  action: ChaosRepeatAction
  uses: number
}

export function isChaosRepeatMetadata(
  value: unknown,
): value is ChaosRepeatMetadata {
  if (!value || typeof value !== 'object') return false
  const metadata = value as ChaosRepeatMetadata
  return (
    metadata.version === 1 &&
    ['CHAOS', 'GREATER_CHAOS', 'PERFECT_CHAOS'].includes(metadata.action) &&
    Number.isSafeInteger(metadata.uses) &&
    metadata.uses > 0
  )
}

const omenIds = new Set(workbenchOmens.map((omen) => omen.id))
function confirmedOmens(value: unknown): value is string[] {
  return (
    Array.isArray(value) &&
    Array.from(value).every((id) => typeof id === 'string' && omenIds.has(id))
  )
}

/** An explicitly present but damaged extension must never become a single use. */
export function validChaosRepeatFrame(frame: Frame): boolean {
  if (!Object.hasOwn(frame, 'chaosRepeat'))
    return !Object.hasOwn(frame, 'chaosRepeatWitness')
  const witness = frame.chaosRepeatWitness
  if (
    Object.hasOwn(frame, 'chaosRepeatWitness') &&
    (!isChaosRepeatWitness(witness) || witness.action !== frame.action)
  )
    return false
  if (witness?.kind === 'ABYSS')
    return (
      isChaosRepeatMetadata(frame.chaosRepeat) &&
      frame.action === frame.chaosRepeat.action &&
      !!frame.abyss &&
      !frame.evidence &&
      isRulesetIdentity(frame.rulesetIdentity) &&
      frame.manual?.action === frame.action &&
      frame.manual.rulesetIdentity === frame.rulesetIdentity &&
      confirmedOmens(frame.manual.consumedOmens) &&
      witness.consumedOmens.every((id) =>
        frame.manual!.consumedOmens!.includes(id),
      )
    )
  const evidence = frame.evidence
  return (
    isChaosRepeatMetadata(frame.chaosRepeat) &&
    frame.action === frame.chaosRepeat.action &&
    !frame.manual &&
    evidence?.action === frame.action &&
    isRulesetIdentity(frame.rulesetIdentity) &&
    evidence.rulesetIdentity === frame.rulesetIdentity &&
    typeof evidence.snapshotId === 'string' &&
    evidence.snapshotId.trim().length > 0 &&
    typeof evidence.ruleVersion === 'string' &&
    evidence.ruleVersion.trim().length > 0 &&
    typeof evidence.ledgerVersion === 'string' &&
    evidence.ledgerVersion.trim().length > 0 &&
    Array.isArray(evidence.events) &&
    Array.isArray(evidence.assumptions) &&
    confirmedOmens(evidence.consumedOmens) &&
    (witness === undefined ||
      (witness.kind === 'CONCRETE' &&
        witness.snapshotId === evidence.snapshotId &&
        witness.ruleVersion === evidence.ruleVersion &&
        witness.ledgerVersion === evidence.ledgerVersion &&
        equivalent(witness.events, evidence.events) &&
        equivalent(witness.assumptions, evidence.assumptions) &&
        equivalent(
          witness.qualityLimit ?? null,
          evidence.qualityLimit ?? null,
        ) &&
        witness.consumedOmens.every((id) =>
          evidence.consumedOmens.includes(id),
        )))
  )
}

export type ChaosRepeatReceiptInput = {
  /** State before the entire batch, not the final single application. */
  before: ConcreteItem
  action: ChaosRepeatAction
  uses: number
  /** Final successful application only: its events/assumptions are not a trace. */
  finalWitness: AppliedItem
  /** Confirmed aggregate over the batch, with one ID per actual consumption. */
  consumedOmens: string[]
  /** Actual state-free API witness; absent only on the earlier concrete foundation. */
  witness?: Extract<ChaosRepeatWitness, { kind: 'CONCRETE' }>
}

export type AbyssChaosRepeatReceiptInput = {
  before: AbyssState
  finalState: AbyssState
  rulesetIdentity: string
  action: ChaosRepeatAction
  uses: number
  finalWitness: Extract<ChaosRepeatWitness, { kind: 'ABYSS' }>
  consumedOmens: string[]
}

declare const receiptBrand: unique symbol
export type ChaosRepeatReceipt = { readonly [receiptBrand]: true }
const receipts = new WeakMap<ChaosRepeatReceipt, ChaosRepeatReceiptInput>()
const abyssReceipts = new WeakMap<
  ChaosRepeatReceipt,
  AbyssChaosRepeatReceiptInput
>()

/**
 * Adapter boundary for a future dedicated repeat-response validator. The required
 * callback must verify the batch response, including count and aggregate omens.
 * This module neither implements that protocol nor validates a batch as one roll.
 */
export function createChaosRepeatReceipt(
  input: ChaosRepeatReceiptInput,
  verifyRepeatResponse: (input: ChaosRepeatReceiptInput) => boolean,
): ChaosRepeatReceipt | null {
  try {
    const snapshot = structuredClone(input)
    if (
      !isChaosRepeatMetadata({ ...snapshot, version: 1 }) ||
      snapshot.finalWitness?.applied !== true ||
      snapshot.finalWitness.action !== snapshot.action ||
      !supportsConcreteStateShape(snapshot.before) ||
      !supportsConcreteStateShape(snapshot.finalWitness.state) ||
      !isRulesetIdentity(snapshot.finalWitness.rulesetIdentity) ||
      typeof snapshot.finalWitness.snapshotId !== 'string' ||
      !snapshot.finalWitness.snapshotId.trim() ||
      typeof snapshot.finalWitness.ruleVersion !== 'string' ||
      !snapshot.finalWitness.ruleVersion.trim() ||
      typeof snapshot.finalWitness.ledgerVersion !== 'string' ||
      !snapshot.finalWitness.ledgerVersion.trim() ||
      !Array.isArray(snapshot.finalWitness.events) ||
      !Array.isArray(snapshot.finalWitness.assumptions) ||
      !confirmedOmens(snapshot.consumedOmens) ||
      !confirmedOmens(snapshot.finalWitness.consumedOmens) ||
      !snapshot.finalWitness.consumedOmens.every((id) =>
        snapshot.consumedOmens.includes(id),
      ) ||
      (snapshot.witness !== undefined &&
        (!isChaosRepeatWitness(snapshot.witness) ||
          snapshot.witness.kind !== 'CONCRETE' ||
          snapshot.witness.action !== snapshot.action)) ||
      verifyRepeatResponse(structuredClone(snapshot)) !== true
    )
      return null
    const receipt = Object.freeze({}) as ChaosRepeatReceipt
    receipts.set(receipt, snapshot)
    return receipt
  } catch {
    return null
  }
}

export function createAbyssChaosRepeatReceipt(
  input: AbyssChaosRepeatReceiptInput,
  verifyRepeatResponse: (input: AbyssChaosRepeatReceiptInput) => boolean,
): ChaosRepeatReceipt | null {
  try {
    const snapshot = structuredClone(input)
    if (
      !isChaosRepeatMetadata({
        version: 1,
        action: snapshot.action,
        uses: snapshot.uses,
      }) ||
      !isRulesetIdentity(snapshot.rulesetIdentity) ||
      !snapshot.before ||
      !snapshot.finalState ||
      !supportsConcreteStateShape(snapshot.before.item) ||
      !supportsConcreteStateShape(snapshot.finalState.item) ||
      !Array.isArray(snapshot.before.slots) ||
      !Array.isArray(snapshot.finalState.slots) ||
      !isChaosRepeatWitness(snapshot.finalWitness) ||
      snapshot.finalWitness.kind !== 'ABYSS' ||
      snapshot.finalWitness.action !== snapshot.action ||
      !knownOmens(snapshot.consumedOmens) ||
      !snapshot.finalWitness.consumedOmens.every((id) =>
        snapshot.consumedOmens.includes(id),
      ) ||
      verifyRepeatResponse(structuredClone(snapshot)) !== true
    )
      return null
    const receipt = Object.freeze({}) as ChaosRepeatReceipt
    abyssReceipts.set(receipt, snapshot)
    return receipt
  } catch {
    return null
  }
}

export function readAbyssChaosRepeatReceipt(
  receipt: ChaosRepeatReceipt | null,
): AbyssChaosRepeatReceiptInput | undefined {
  const input = receipt && abyssReceipts.get(receipt)
  return input ? structuredClone(input) : undefined
}

/** Only runtime-branded receipts are accepted; callers receive a detached copy. */
export function readChaosRepeatReceipt(
  receipt: ChaosRepeatReceipt | null,
): ChaosRepeatReceiptInput | undefined {
  const input = receipt && receipts.get(receipt)
  return input ? structuredClone(input) : undefined
}
