import { workbenchOmens } from '../../workbenchApi'
import type { ChaosRepeatWitness } from './types'

const omenIds = new Set(workbenchOmens.map((omen) => omen.id))
export function knownOmens(value: unknown): value is string[] {
  return (
    Array.isArray(value) &&
    Array.from(value).every((id) => typeof id === 'string' && omenIds.has(id))
  )
}
export function uniqueOmens(value: unknown): value is string[] {
  return knownOmens(value) && new Set(value).size === value.length
}

/** Wire structure only. Catalog/evidence checks are performed on the final item. */
export function isChaosRepeatWitness(
  value: unknown,
): value is ChaosRepeatWitness {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return false
  const witness = value as Record<string, unknown>
  const common = [
    'kind',
    'action',
    'applied',
    'reason',
    'consumedOmens',
    'remainingOmens',
  ]
  if (
    typeof witness.action !== 'string' ||
    !['CHAOS', 'GREATER_CHAOS', 'PERFECT_CHAOS'].includes(witness.action) ||
    witness.applied !== true ||
    typeof witness.reason !== 'string' ||
    !uniqueOmens(witness.consumedOmens) ||
    !uniqueOmens(witness.remainingOmens)
  )
    return false
  const remaining = witness.remainingOmens
  if (witness.consumedOmens.some((id) => remaining.includes(id))) return false
  if (witness.kind === 'ABYSS')
    return Object.keys(witness).every((key) => common.includes(key))
  return (
    witness.kind === 'CONCRETE' &&
    Object.keys(witness).every((key) =>
      [
        ...common,
        'ruleVersion',
        'ledgerVersion',
        'snapshotId',
        'events',
        'assumptions',
        'qualityLimit',
      ].includes(key),
    ) &&
    ['ruleVersion', 'ledgerVersion', 'snapshotId'].every(
      (key) =>
        typeof witness[key] === 'string' && witness[key].trim().length > 0,
    ) &&
    Array.isArray(witness.events) &&
    Array.isArray(witness.assumptions)
  )
}
