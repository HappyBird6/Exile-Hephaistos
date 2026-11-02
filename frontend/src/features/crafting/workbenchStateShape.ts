import { observationsShape } from './itemObservations'
import { basePolicy, baseRarityAllowed } from './baseRegistry'
// Preserve future/unsupported item data instead of accepting a lossy affix-only projection.
import { catalystBase, qualityShape } from './catalystQuality'
const itemFields = new Set([
  'snapshotId',
  'baseItemId',
  'itemLevel',
  'rarity',
  'implicits',
  'explicits',
  'conditions',
  'modifierIds',
  'augmentSockets',
  'catalystQuality',
  'desecrated',
  'equipmentQuality',
  'observations',
])
const modifierFields = new Set(['modifierId', 'values', 'fractured'])
export function supportsConcreteStateShape(value: unknown): boolean {
  if (
    !value ||
    typeof value !== 'object' ||
    Array.isArray(value) ||
    !Object.keys(value).every((key) => itemFields.has(key))
  )
    return false
  const item = value as Record<string, unknown>
  if (
    typeof item.baseItemId === 'string' &&
    !baseRarityAllowed(item.baseItemId, String(item.rarity))
  )
    return false
  if (!observationsShape(item.observations)) return false
  if (
    item.equipmentQuality != null &&
    (!Number.isInteger(item.equipmentQuality) ||
      Number(item.equipmentQuality) < 0 ||
      Number(item.equipmentQuality) > 100 ||
      typeof item.baseItemId !== 'string' ||
      !basePolicy(item.baseItemId)?.qualityLimit ||
      catalystBase(item.baseItemId))
  )
    return false
  if (
    item.desecrated !== undefined &&
    (!Array.isArray(item.desecrated) ||
      item.desecrated.length > 6 ||
      !item.desecrated.every((slot: unknown) => {
        if (!slot || typeof slot !== 'object' || Array.isArray(slot))
          return false
        const s = slot as Record<string, unknown>
        return (
          Object.keys(s).every((key) =>
            [
              'id',
              'side',
              'modifierId',
              'minimumModifierLevel',
              'revealPool',
            ].includes(key),
          ) &&
          (s.minimumModifierLevel == null || s.minimumModifierLevel === 40) &&
          (s.revealPool == null ||
            ['ALTERED', 'AMANAMU', 'KURGAL', 'ULAMAN'].includes(
              String(s.revealPool),
            )) &&
          typeof s.id === 'string' &&
          s.id.length > 0 &&
          (s.side === 'PREFIX' || s.side === 'SUFFIX') &&
          (s.modifierId === null || typeof s.modifierId === 'string')
        )
      }) ||
      new Set(item.desecrated.map((s: { id: string }) => s.id)).size !==
        item.desecrated.length)
  )
    return false
  if (
    !qualityShape(item.catalystQuality) ||
    (item.catalystQuality != null &&
      (typeof item.baseItemId !== 'string' || !catalystBase(item.baseItemId)))
  )
    return false
  if (
    item.augmentSockets !== undefined &&
    item.augmentSockets !== null &&
    (typeof item.baseItemId !== 'string' ||
      basePolicy(item.baseItemId)?.socketExecutionMaximum == null ||
      !Number.isInteger(item.augmentSockets) ||
      Number(item.augmentSockets) < 0 ||
      Number(item.augmentSockets) >
        basePolicy(item.baseItemId)!.socketExecutionMaximum!)
  )
    return false
  for (const layer of ['implicits', 'explicits'] as const) {
    if (!(layer in item)) return false
    const instances = item[layer]
    if (
      !Array.isArray(instances) ||
      !instances.every(
        (instance) =>
          instance &&
          typeof instance === 'object' &&
          !Array.isArray(instance) &&
          Object.keys(instance).every((key) => modifierFields.has(key)),
      )
    )
      return false
  }
  return true
}
