/** Prefer the event's device on hybrid laptops; coarse-pointer media covers older browsers. */
export function isTouchActivation(event: {
  detail: number
  nativeEvent: Event
}): boolean {
  if (event.detail === 0) return false
  if ('pointerType' in event.nativeEvent && event.nativeEvent.pointerType)
    return event.nativeEvent.pointerType === 'touch'
  return (
    window.matchMedia?.('(hover: none) and (pointer: coarse)').matches ?? false
  )
}
