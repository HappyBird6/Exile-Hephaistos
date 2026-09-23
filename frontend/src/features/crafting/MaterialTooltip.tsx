import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import descriptions from './materialTooltips.json'

type TooltipData = { name: string; lines: string[]; sourceUrl: string }
const catalog: Record<string, TooltipData> = descriptions
export function MaterialTooltip() {
  const tooltip = useRef<HTMLElement>(null)
  const [target, setTarget] = useState<{
    id: string
    x: number
    y: number
    element: HTMLElement
  } | null>(null)
  useEffect(() => {
    let timer: ReturnType<typeof setTimeout> | undefined
    const hide = () => setTarget(null)
    const inspect = (event: Event) => {
      clearTimeout(timer)
      const element = event.target instanceof Element ? event.target : null
      if (element?.closest('.material-tooltip')) return
      const button = element?.closest<HTMLElement>('[data-material-tooltip]')
      const id = button?.dataset.materialTooltip
      if (!button || !id || !catalog[id]) {
        timer = setTimeout(hide, 150)
        return
      }
      const rect = button.getBoundingClientRect()
      setTarget({
        id,
        element: button,
        x: rect.right + 8,
        y: rect.top,
      })
    }
    const escape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') hide()
    }
    document.addEventListener('pointerover', inspect)
    document.addEventListener('focusin', inspect)
    document.addEventListener('keydown', escape)
    window.addEventListener('blur', hide)
    window.addEventListener('resize', hide)
    const scroll = (event: Event) => {
      if (!(
        event.target instanceof Element &&
        event.target.closest('.material-tooltip')
      ))
        hide()
    }
    document.addEventListener('scroll', scroll, true)
    return () => {
      clearTimeout(timer)
      document.removeEventListener('pointerover', inspect)
      document.removeEventListener('focusin', inspect)
      document.removeEventListener('keydown', escape)
      window.removeEventListener('blur', hide)
      window.removeEventListener('resize', hide)
      document.removeEventListener('scroll', scroll, true)
    }
  }, [])
  useEffect(() => {
    if (!target) return
    target.element.setAttribute('aria-describedby', 'material-description')
    return () => target.element.removeAttribute('aria-describedby')
  }, [target])
  useLayoutEffect(() => {
    const element = tooltip.current
    if (!target || !element) return
    const rect = element.getBoundingClientRect()
    element.style.left = `${Math.max(8, Math.min(target.x, window.innerWidth - rect.width - 8))}px`
    element.style.top = `${Math.max(8, Math.min(target.y, window.innerHeight - rect.height - 8))}px`
  }, [target])
  const data = target && catalog[target.id]
  if (!data || !target) return null
  return (
    <aside
      ref={tooltip}
      className="material-tooltip"
      role="tooltip"
      id="material-description"
      style={{ left: target.x, top: target.y }}
    >
      <strong>{data.name}</strong>
      {data.lines
        .filter((line) => !line.startsWith('Stack Size:'))
        .map((line, i) => (
          <p key={i}>{line}</p>
        ))}
      <a href={data.sourceUrl} target="_blank" rel="noreferrer">
        PoE2DB
      </a>
    </aside>
  )
}
