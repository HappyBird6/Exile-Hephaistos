import { useLayoutEffect, useRef, type ReactNode } from 'react'
export function MeasuredGraphBox({
  children,
  onSize,
  className,
}: {
  children: ReactNode
  onSize: (height: number) => void
  className?: string
}) {
  const ref = useRef<HTMLDivElement>(null)
  useLayoutEffect(() => {
    const el = ref.current
    if (!el) return
    const measure = () => {
      const h = Math.ceil(el.offsetHeight)
      if (h > 0) onSize(h)
    }
    measure()
    if (typeof ResizeObserver === 'undefined') return
    const observer = new ResizeObserver(measure)
    observer.observe(el)
    return () => observer.disconnect()
  }, [onSize])
  return (
    <div ref={ref} className={className}>
      {children}
    </div>
  )
}
