export type OverlayRect = {
  left: number
  top: number
  right: number
  bottom: number
}

/** Viewport coordinates already include the workbench zoom. */
export function overlayPlacement(
  canvas: OverlayRect,
  opener: OverlayRect,
  viewport: { width: number; height: number },
  gap = 8,
) {
  const left = Math.max(gap, Math.min(canvas.left + gap, viewport.width - gap))
  const right = Math.max(
    left,
    Math.min(canvas.right - gap, viewport.width - gap),
  )
  const top = Math.max(gap, Math.min(canvas.top + gap, viewport.height - gap))
  const bottom = Math.max(
    top,
    Math.min(opener.bottom - gap, viewport.height - gap),
  )
  return { left, top, width: right - left, height: bottom - top }
}
