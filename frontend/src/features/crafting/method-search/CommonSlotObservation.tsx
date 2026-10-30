import { useQuery } from '@tanstack/react-query'
import { AbyssPanel, type AbyssState } from '../AbyssPanel'
import { loadInitial } from '../craftingApi'
import type { WorkbenchBaseKey } from '../baseRegistry'
import type { ConcreteItem } from '../workbenchApi'
import type { Item } from '../path-tree/types'
import type { FlowTransition } from './craftingFlow'

/** Only observed input belongs here. All currency and desecration transitions use graph methods. */
export function CommonSlotObservation({
  item,
  base,
  identity,
  onTransition,
  onContinue,
}: {
  item: Item
  base: string
  identity: string
  onTransition: (state: AbyssState, event: FlowTransition) => void
  onContinue: (state: AbyssState, item: Item) => void
}) {
  const initial = useQuery({
    queryKey: ['slot-observation-catalog', base, identity, item.itemLevel],
    queryFn: ({ signal }) =>
      loadInitial(item.itemLevel, signal, base as WorkbenchBaseKey),
    staleTime: Infinity,
  })
  if (!initial.data || initial.data.rulesetIdentity !== identity) return null
  const slots: AbyssState['slots'] = item.explicits.map((modifier, index) => {
    const marked = item.desecrated?.find(
      (s) => s.modifierId === modifier.modifierId,
    )
    return {
      id: marked?.id ?? `explicit-${index}`,
      side: initial.data.modifiers[modifier.modifierId]!.affixType as
        'PREFIX' | 'SUFFIX',
      modifier,
      desecrated: !!marked,
    }
  })
  for (const slot of item.desecrated ?? [])
    if (slot.modifierId === null)
      slots.push({
        id: slot.id,
        side: slot.side,
        modifier: null,
        desecrated: true,
      })
  const prepared: AbyssState = {
    item: { ...item, explicits: [], desecrated: [] } as ConcreteItem,
    slots,
    offer: null,
  }
  return (
    <AbyssPanel
      key={JSON.stringify(item)}
      source={item as ConcreteItem}
      initial={initial.data}
      prepared={prepared}
      embedded
      observationOnly
      onTransition={(state, event) => {
        if (event.operation !== 'restore') onTransition(state, event)
      }}
      onContinue={onContinue}
    />
  )
}
