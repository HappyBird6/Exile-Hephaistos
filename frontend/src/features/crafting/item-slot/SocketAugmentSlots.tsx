import { useState } from 'react'
import {
  augmentEffect,
  augmentName,
  augments,
  canInstallAugment,
  socketText,
} from '../socketAugments'
import { stashDragType } from '../stash/stashDragType'
import type { ConcreteItem } from '../workbenchApi'

// Sibling buttons overlay the item art; they never nest inside the item button.
export function SocketAugmentSlots({
  item,
  augmentId,
  draggedAugmentId,
  ready,
  onInstall,
}: {
  item: ConcreteItem
  augmentId?: string | undefined
  draggedAugmentId?: string | undefined
  ready: boolean
  onInstall: (slot: number, id: string) => void
}) {
  const [dropTarget, setDropTarget] = useState<number | null>(null)
  return (
    <div className="wb-sockets" aria-label={socketText().tab}>
      {Array.from({ length: item.augmentSockets ?? 0 }, (_, slot) => {
        const installed = item.installedAugments?.find(
          (a) => a.slotIndex === slot,
        )
        const slotReady = ready && canInstallAugment(item, augmentId, slot)
        const canDrop =
          slotReady && draggedAugmentId === augmentId && Boolean(augmentId)
        return (
          <button
            key={slot}
            type="button"
            className={`wb-socket ${installed ? 'is-filled' : ''} ${slotReady ? 'is-available' : ''} ${canDrop && dropTarget === slot ? 'is-drop-target' : ''}`}
            disabled={!slotReady}
            title={
              socketText().policy +
              ' ' +
              (installed
                ? augmentName(installed.augmentId) +
                  ': ' +
                  augmentEffect(installed.augmentId, item.baseItemId)
                : socketText().empty)
            }
            aria-label={`${socketText().socket} ${slot + 1}: ${installed ? augmentName(installed.augmentId) : socketText().empty}`}
            onClick={() => {
              if (slotReady && augmentId) onInstall(slot, augmentId)
            }}
            onDragOver={(event) => {
              if (!canDrop || !event.dataTransfer.types.includes(stashDragType))
                return
              event.preventDefault()
              event.dataTransfer.dropEffect = 'copy'
              setDropTarget(slot)
            }}
            onDragLeave={(event) => {
              if (
                !(event.relatedTarget instanceof Node) ||
                !event.currentTarget.contains(event.relatedTarget)
              )
                setDropTarget(null)
            }}
            onDrop={(event) => {
              event.preventDefault()
              event.stopPropagation()
              setDropTarget(null)
              if (
                canDrop &&
                augmentId &&
                event.dataTransfer.types.includes(stashDragType) &&
                event.dataTransfer.getData(stashDragType) === augmentId
              )
                onInstall(slot, augmentId)
            }}
          >
            {installed && (
              <img
                src={
                  augments.find((a) => a.id === installed.augmentId)?.image ??
                  '/assets/augments/rune.svg'
                }
                alt={augmentName(installed.augmentId)}
              />
            )}
          </button>
        )
      })}
    </div>
  )
}
