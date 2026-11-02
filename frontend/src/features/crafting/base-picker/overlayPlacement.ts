export type OverlayRect = {
  left: number
  top: number
  right: number
  bottom: number
}

/** Covers only a compact slice to the left of its trigger; never changes shelf layout. */
export function overlayPlacement(
  anchor: OverlayRect,
  viewport: { width: number; height: number },
  preferredWidth = 520,
  shelf?: OverlayRect,
) {
  const width = Math.min(
    preferredWidth,
    Math.max(0, viewport.width - 16),
    Math.max(240, anchor.left - 8),
  )
  const left = Math.max(
    8,
    Math.min(anchor.left - width - 6, viewport.width - width - 8),
  )
  const ceiling = Math.max(8, shelf?.top ?? 8)
  const floor = Math.min(
    viewport.height - 8,
    shelf?.bottom ?? viewport.height - 8,
  )
  const maxHeight = Math.max(0, Math.min(560, floor - ceiling))
  const top = Math.max(ceiling, Math.min(anchor.top, floor - maxHeight))
  return { left, top, width, maxHeight }
}
