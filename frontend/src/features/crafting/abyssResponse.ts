import { sameObservations } from './itemObservations'
import type { Initial } from './craftingApi'
import type { ConcreteItem } from './workbenchApi'

const object = (v: unknown): v is Record<string, unknown> =>
  typeof v === 'object' && v !== null && !Array.isArray(v)

export function validAbyssResponse(
  operation: string,
  value: unknown,
  source: ConcreteItem,
  initial: Initial,
): boolean {
  const modifier = (v: unknown): boolean => {
    if (!object(v) || typeof v.modifierId !== 'string' || !object(v.values))
      return false
    const d = initial.modifiers[v.modifierId]
    if (!d || (v.fractured !== undefined && typeof v.fractured !== 'boolean'))
      return false
    const values = v.values
    return (
      !!d.stats &&
      Object.keys(values).length === d.stats.length &&
      d.stats.every((s) => {
        const amount = values[s.id]
        return (
          typeof amount === 'number' &&
          Number.isSafeInteger(amount) &&
          amount >= s.min &&
          amount <= s.max
        )
      })
    )
  }
  const chance = (v: unknown): boolean =>
    object(v) &&
    typeof v.targetWeight === 'number' &&
    typeof v.totalWeight === 'number' &&
    Number.isSafeInteger(v.targetWeight) &&
    Number.isSafeInteger(v.totalWeight) &&
    v.totalWeight > 0 &&
    v.targetWeight >= 0 &&
    v.targetWeight <= v.totalWeight
  if (operation === 'compare')
    return (
      value === null ||
      (object(value) &&
        chance(value.baseline) &&
        chance(value.afterBlocking) &&
        typeof value.blockerModifierId === 'string' &&
        !!initial.modifiers[value.blockerModifierId])
    )
  if (operation === 'plan')
    return (
      object(value) &&
      typeof value.available === 'boolean' &&
      Array.isArray(value.branches) &&
      value.branches.every(
        (b) =>
          object(b) &&
          (b.removedSlotId === null || typeof b.removedSlotId === 'string') &&
          typeof b.removalProbability === 'number' &&
          Number.isFinite(b.removalProbability) &&
          b.removalProbability > 0 &&
          b.removalProbability <= 1,
      ) &&
      (value.available
        ? value.branches.length > 0 &&
          Math.abs(
            value.branches.reduce(
              (n, b: Record<string, unknown>) =>
                n + (b.removalProbability as number),
              0,
            ) - 1,
          ) < 1e-9
        : value.branches.length === 0)
    )
  if (!object(value) || !object(value.item) || !Array.isArray(value.slots))
    return false
  const item = value.item
  if (
    item.snapshotId !== source.snapshotId ||
    item.baseItemId !== source.baseItemId ||
    item.itemLevel !== source.itemLevel ||
    item.rarity !== source.rarity ||
    !sameObservations(item.observations, source.observations) ||
    (item.equipmentQuality ?? null) !== (source.equipmentQuality ?? null) ||
    (item.augmentSockets ?? null) !== (source.augmentSockets ?? null) ||
    JSON.stringify(item.catalystQuality ?? null) !==
      JSON.stringify(source.catalystQuality ?? null) ||
    !Array.isArray(item.explicits) ||
    item.explicits.length !== 0 ||
    !Array.isArray(item.implicits) ||
    !item.implicits.every(modifier) ||
    item.implicits.length !== source.implicits.length ||
    !Array.isArray(item.conditions) ||
    JSON.stringify(item.conditions) !== JSON.stringify(source.conditions)
  )
    return false
  if (value.slots.length > 6 || value.slots.some((s) => !object(s)))
    return false
  const slots = value.slots as Record<string, unknown>[]
  if (new Set(slots.map((s) => s.id)).size !== slots.length) return false
  if (
    !slots.every(
      (s) =>
        typeof s.id === 'string' &&
        s.id.length > 0 &&
        (s.side === 'PREFIX' || s.side === 'SUFFIX') &&
        typeof s.desecrated === 'boolean' &&
        (s.minimumModifierLevel == null ||
          (s.desecrated && s.minimumModifierLevel === 40)) &&
        (s.modifier === null
          ? s.desecrated
          : modifier(s.modifier) &&
            object(s.modifier) &&
            initial.modifiers[s.modifier.modifierId as string]?.affixType ===
              s.side),
    )
  )
    return false
  if (
    slots.filter((s) => s.side === 'PREFIX').length >
      (initial.baseRules?.rarePrefixes ?? 3) ||
    slots.filter((s) => s.side === 'SUFFIX').length >
      (initial.baseRules?.rareSuffixes ?? 3)
  )
    return false
  if (value.offer === null) return true
  return (
    object(value.offer) &&
    typeof value.offer.slotId === 'string' &&
    slots.some(
      (s) =>
        s.id === (value.offer as Record<string, unknown>).slotId &&
        s.desecrated,
    ) &&
    Array.isArray(value.offer.candidates) &&
    value.offer.candidates.length === 3 &&
    value.offer.candidates.every(modifier)
  )
}
