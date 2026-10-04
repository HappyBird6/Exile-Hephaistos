import type { ConcreteItem } from './workbenchApi'
import definitions from './sapphireDefinitions.json'

export const sapphireBase = 'Metadata/Items/Jewels/JewelInt'
export const sapphireCastSpeed = 'sapphire:suffix:of-enchanting'
export function reviewedSapphire(state: ConcreteItem): boolean {
  if (
    !['NORMAL', 'MAGIC', 'RARE'].includes(state.rarity) ||
    state.augmentSockets != null ||
    state.implicits.length !== 0
  )
    return false
  const rows = definitions as Record<
    string,
    {
      affixType: string
      familyIds: string[]
      stats: { id: string; min: number; max: number }[]
      crafted: boolean
    }
  >
  const families = new Set<string>()
  let prefixes = 0,
    suffixes = 0,
    crafted = 0
  for (const m of state.explicits) {
    const d = rows[m.modifierId]
    if (
      !d ||
      m.fractured ||
      Object.keys(m.values).length !== d.stats.length ||
      !d.stats.every(
        (s) =>
          Number.isSafeInteger(m.values[s.id]) &&
          m.values[s.id]! >= s.min &&
          m.values[s.id]! <= s.max,
      ) ||
      d.familyIds.some((f) => families.has(f))
    )
      return false
    d.familyIds.forEach((f) => families.add(f))
    if (d.affixType === 'PREFIX') prefixes++
    else suffixes++
    if (d.crafted) crafted++
  }
  const cap = state.rarity === 'NORMAL' ? 0 : state.rarity === 'MAGIC' ? 1 : 2
  return prefixes <= cap && suffixes <= cap && crafted <= 1
}
