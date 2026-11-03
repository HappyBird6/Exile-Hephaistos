import { BaseImage } from '../base-picker/BaseImage'
import './item-slot-image.css'

/** The parent supplies the current verified base ID, including restored/imported items. */
export function ItemSlotImage({
  baseItemId,
  label,
}: {
  baseItemId: string
  label: string
}) {
  return (
    <span
      className="wb-item-slot-art"
      onDragStart={(event) => event.preventDefault()}
    >
      <BaseImage key={baseItemId} baseItemId={baseItemId} label={label} />
    </span>
  )
}
