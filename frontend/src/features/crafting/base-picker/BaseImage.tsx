import { useState } from 'react'
import { baseImageSource } from './baseImageSources'

export function BaseImage({
  baseItemId,
  label,
  image,
}: {
  baseItemId: string
  label: string
  image?: string | undefined
}) {
  const source = image ?? baseImageSource(baseItemId)
  const [failed, setFailed] = useState<string | undefined>()
  return source && failed !== source ? (
    <img
      className="wb-base-image"
      src={source}
      alt={label}
      loading="lazy"
      referrerPolicy="no-referrer"
      onError={() => setFailed(source)}
    />
  ) : (
    <span
      className="wb-base-image wb-base-image--missing"
      role="img"
      aria-label={label}
    >
      ◇
    </span>
  )
}
