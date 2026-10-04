import definitions from './basicJewelDefinitions.json'
import type { ConcreteItem } from './workbenchApi'

export const basicJewelBases = {
  ruby: 'Metadata/Items/Jewels/JewelStr',
  emerald: 'Metadata/Items/Jewels/JewelDex',
  sapphire: 'Metadata/Items/Jewels/JewelInt',
  diamond: 'Metadata/Items/Jewels/JewelDiamond',
} as const
export function isBasicJewel(base: string): boolean {
  return Object.values(basicJewelBases).some((id) => id === base)
}
export function jewelExtra(state: ConcreteItem, side: string): number {
  const code = side === 'PREFIX' ? 'Prefix' : 'Suffix'
  return state.explicits.some((m) =>
    m.modifierId.endsWith(`:crafted:CraftedJewelAdditional${code}Allowed`),
  )
    ? 1
    : 0
}
export function jewelCapacity(state: ConcreteItem, side: string): number {
  return state.rarity === 'NORMAL'
    ? 0
    : state.rarity === 'MAGIC'
      ? 1
      : 2 + jewelExtra(state, side)
}
export function reviewedBasicJewel(state: ConcreteItem): boolean {
  if (
    !isBasicJewel(state.baseItemId) ||
    !['NORMAL', 'MAGIC', 'RARE'].includes(state.rarity) ||
    state.augmentSockets != null ||
    state.implicits.length !== 0
  )
    return false
  const rows = definitions as Record<
    string,
    {
      baseItemId: string
      affixType: string
      familyIds: string[]
      stats: { id: string; min: number; max: number }[]
      crafted: boolean
    }
  >
  const families = new Set<string>()
  let p = 0,
    s = 0,
    crafted = 0
  for (const m of state.explicits) {
    const d = rows[m.modifierId]
    if (
      !d ||
      d.baseItemId !== state.baseItemId ||
      m.fractured ||
      Object.keys(m.values).length !== d.stats.length ||
      !d.stats.every(
        (r) =>
          Number.isSafeInteger(m.values[r.id]) &&
          m.values[r.id]! >= r.min &&
          m.values[r.id]! <= r.max,
      ) ||
      d.familyIds.some((f) => families.has(f))
    )
      return false
    d.familyIds.forEach((f) => families.add(f))
    if (d.affixType === 'PREFIX') p++
    else s++
    if (d.crafted) crafted++
  }
  if (crafted > 1) return false
  return state.rarity === 'RARE' &&
    jewelExtra(state, 'PREFIX') + jewelExtra(state, 'SUFFIX') === 0
    ? p <= 3 && s <= 3 && p + s <= 4
    : p <= jewelCapacity(state, 'PREFIX') && s <= jewelCapacity(state, 'SUFFIX')
}
export function jewelEffect(
  state: ConcreteItem | undefined,
  side: string,
): number {
  const code = side === 'PREFIX' ? 'Prefix' : 'Suffix'
  return (
    state?.explicits.find((m) =>
      m.modifierId.endsWith(`:crafted:CraftedJewel${code}Effect`),
    )?.values.display_source_value ?? 0
  )
}
