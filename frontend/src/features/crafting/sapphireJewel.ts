import type { ConcreteItem } from './workbenchApi'

export const sapphireBase = 'Metadata/Items/Jewels/JewelInt'
export const sapphireCastSpeed = 'sapphire:suffix:of-enchanting'
// Editor scope only; full Jewel capacity, generation and special Jewel variants are unreviewed.
export function reviewedSapphire(state: ConcreteItem): boolean {
  return (
    ['MAGIC', 'RARE'].includes(state.rarity) &&
    state.augmentSockets == null &&
    state.implicits.length === 0 &&
    state.explicits.length <= 1 &&
    state.explicits.every(
      (m) =>
        m.modifierId === sapphireCastSpeed &&
        !m.fractured &&
        Object.keys(m.values).length === 1 &&
        Number.isInteger(m.values.display_cast_speed_percent) &&
        m.values.display_cast_speed_percent! >= 2 &&
        m.values.display_cast_speed_percent! <= 4,
    )
  )
}
