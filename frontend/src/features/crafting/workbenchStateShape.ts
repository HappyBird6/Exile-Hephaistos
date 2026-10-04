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
    !qualityShape(item.catalystQuality) ||
    (item.catalystQuality != null &&
      (typeof item.baseItemId !== 'string' || !catalystBase(item.baseItemId)))
  )
    return false
  if (
    item.augmentSockets !== undefined &&
    item.augmentSockets !== null &&
    (item.baseItemId !== 'Metadata/Items/Armours/Gloves/FourGlovesStr1' ||
      ![0, 1].includes(item.augmentSockets as number))
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
