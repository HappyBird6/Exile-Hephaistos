import type { ConcreteItem } from '../workbenchApi'

export type Modifier = ConcreteItem['explicits'][number]
export type Slot = {
  revealPool?: string | null
  minimumModifierLevel?: 40 | null
  id: string
  side: 'PREFIX' | 'SUFFIX'
  modifier: Modifier | null
  desecrated: boolean
}
export type AbyssState = {
  item: ConcreteItem
  slots: Slot[]
  offer: { slotId: string; candidates: Modifier[] } | null
}
