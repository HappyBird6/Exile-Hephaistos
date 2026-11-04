import type { AbyssState } from '../../AbyssPanel'
import { validAbyssResponse } from '../../abyssResponse'
import type { Initial } from '../../craftingApi'
import { equivalent, sameRolls } from '../../sanctification/equivalence'
import type { ConcreteItem } from '../../workbenchApi'
import { storedAbyssItem, verifiedHistoryState } from '../../workbenchHistory'
import { supportsConcreteStateShape } from '../../workbenchStateShape'
import type { ChaosRepeatState } from './types'

export const object = (value: unknown): value is Record<string, unknown> =>
  !!value && typeof value === 'object' && !Array.isArray(value)

export function onlyKeys(
  value: Record<string, unknown>,
  keys: readonly string[],
) {
  return Object.keys(value).every((key) => keys.includes(key))
}

const nullableWireFields = new Set([
  'equipmentQuality',
  'augmentSockets',
  'catalystQuality',
  'sanctification',
  'observations',
  'minimumModifierLevel',
  'revealPool',
])

/** Compare supported wire aliases without changing the returned or stored values. */
export function sameRepeatValue(a: unknown, b: unknown): boolean {
  const normalized = (value: unknown): unknown => {
    if (Array.isArray(value)) return value.map(normalized)
    if (!object(value)) return value
    return Object.fromEntries(
      Object.entries(value)
        .filter(
          ([key, entry]) =>
            !(nullableWireFields.has(key) && entry == null) &&
            !(key === 'fractured' && entry === false) &&
            !(
              key === 'desecrated' &&
              Array.isArray(entry) &&
              entry.length === 0
            ),
        )
        .map(([key, entry]) => [key, normalized(entry)]),
    )
  }
  return equivalent(normalized(a), normalized(b))
}

export function repeatItem(state: ChaosRepeatState): ConcreteItem {
  return state.kind === 'CONCRETE' ? state.item : storedAbyssItem(state.abyss)
}

function modifierShape(value: unknown, initial: Initial): boolean {
  if (
    !object(value) ||
    !onlyKeys(value, ['modifierId', 'values', 'fractured']) ||
    typeof value.modifierId !== 'string' ||
    !object(value.values) ||
    (value.fractured !== undefined && typeof value.fractured !== 'boolean')
  )
    return false
  const definition = initial.modifiers[value.modifierId]
  const values = value.values
  return (
    definition?.id === value.modifierId &&
    !!definition.stats &&
    Object.keys(values).length === definition.stats.length &&
    definition.stats.every((stat) => {
      const amount = values[stat.id]
      return (
        typeof amount === 'number' &&
        Number.isSafeInteger(amount) &&
        amount >= stat.min &&
        amount <= stat.max
      )
    })
  )
}

/** Final-state validation only; no omitted application chain is reconstructed. */
export function validRepeatState(
  value: unknown,
  initial: Initial,
): value is ChaosRepeatState {
  try {
    if (!object(value)) return false
    let item: ConcreteItem
    if (value.kind === 'CONCRETE') {
      if (!onlyKeys(value, ['kind', 'item'])) return false
      item = value.item as ConcreteItem
    } else if (value.kind === 'ABYSS') {
      if (!onlyKeys(value, ['kind', 'abyss']) || !object(value.abyss))
        return false
      const abyss = value.abyss
      if (
        !onlyKeys(abyss, ['item', 'slots', 'offer']) ||
        !supportsConcreteStateShape(abyss.item) ||
        !Array.isArray(abyss.slots) ||
        !Array.from(abyss.slots).every(
          (slot) =>
            object(slot) &&
            onlyKeys(slot, [
              'id',
              'side',
              'modifier',
              'desecrated',
              'minimumModifierLevel',
              'revealPool',
            ]) &&
            (slot.modifier === null || modifierShape(slot.modifier, initial)),
        ) ||
        (abyss.offer !== null &&
          (!object(abyss.offer) ||
            !onlyKeys(abyss.offer, ['slotId', 'candidates']) ||
            !Array.isArray(abyss.offer.candidates) ||
            !Array.from(abyss.offer.candidates).every((candidate) =>
              modifierShape(candidate, initial),
            ))) ||
        !validAbyssResponse(
          'validate',
          abyss,
          abyss.item as ConcreteItem,
          initial,
        )
      )
        return false
      item = storedAbyssItem(abyss as AbyssState)
    } else return false
    return (
      supportsConcreteStateShape(item) &&
      [...item.implicits, ...item.explicits].every((modifier) =>
        modifierShape(modifier, initial),
      ) &&
      verifiedHistoryState(item, initial)
    )
  } catch {
    return false
  }
}

/** Invariants of the existing Chaos rules, rather than a synthetic single-step diff. */
export function chaosInvariants(
  before: ChaosRepeatState,
  after: ChaosRepeatState,
): boolean {
  if (before.kind !== after.kind) return false
  const source = repeatItem(before)
  const target = repeatItem(after)
  if (
    source.snapshotId !== target.snapshotId ||
    source.baseItemId !== target.baseItemId ||
    source.itemLevel !== target.itemLevel ||
    source.rarity !== target.rarity ||
    !sameRolls(source.implicits, target.implicits) ||
    !equivalent([...source.conditions].sort(), [...target.conditions].sort()) ||
    !equivalent(source.observations ?? null, target.observations ?? null) ||
    !equivalent(
      source.catalystQuality ?? null,
      target.catalystQuality ?? null,
    ) ||
    !equivalent(source.sanctification ?? null, target.sanctification ?? null) ||
    (source.equipmentQuality ?? null) !== (target.equipmentQuality ?? null) ||
    (source.augmentSockets ?? null) !== (target.augmentSockets ?? null) ||
    !sameRolls(
      source.explicits.filter((modifier) => modifier.fractured),
      target.explicits.filter((modifier) => modifier.fractured),
    )
  )
    return false
  if (before.kind === 'CONCRETE' && after.kind === 'CONCRETE')
    return (
      source.explicits.length === target.explicits.length &&
      sameRepeatValue(source.desecrated ?? [], target.desecrated ?? [])
    )
  if (before.kind !== 'ABYSS' || after.kind !== 'ABYSS') return false
  return (
    before.abyss.slots.length === after.abyss.slots.length &&
    (after.abyss.offer === null ||
      sameRepeatValue(before.abyss.offer, after.abyss.offer)) &&
    after.abyss.slots.every(
      (slot) =>
        !slot.desecrated ||
        before.abyss.slots.some(
          (original) =>
            original.id === slot.id && sameRepeatValue(original, slot),
        ),
    )
  )
}
