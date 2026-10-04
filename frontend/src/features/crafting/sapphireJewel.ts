import type { ConcreteItem } from './workbenchApi'
import { reviewedBasicJewel } from './basicJewel'

export const sapphireBase = 'Metadata/Items/Jewels/JewelInt'
export const sapphireCastSpeed = 'sapphire:suffix:of-enchanting'
export function reviewedSapphire(state: ConcreteItem): boolean {
  return state.baseItemId === sapphireBase && reviewedBasicJewel(state)
}
