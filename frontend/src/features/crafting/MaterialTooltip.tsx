import { useI18n, gameTerm } from '../../shared/i18n/i18n'
import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import descriptions from './materialTooltips.json'
import { legacyHomogenisingIds, legacyFiveIds } from './workbenchApi'

type TooltipData = { name: string; lines: string[]; sourceUrl: string }
const catalog: Record<string, TooltipData> = descriptions
export function MaterialTooltip() {
  const { t, locale, name } = useI18n()
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
    element.style.maxHeight = ''
    const rect = element.getBoundingClientRect()
    const anchor = target.element.getBoundingClientRect()
    const right = anchor.right + 8
    const left = anchor.left - rect.width - 8
    if (right + rect.width <= window.innerWidth - 8 || left >= 8) {
      element.style.left = `${right + rect.width <= window.innerWidth - 8 ? right : left}px`
      element.style.top = `${Math.max(8, Math.min(anchor.top, window.innerHeight - rect.height - 8))}px`
    } else {
      // A full-width tooltip must sit above or below its trigger, never intercept its click.
      const below = window.innerHeight - anchor.bottom - 16
      const above = anchor.top - 16
      const useBelow = below >= above
      const height = Math.min(
        rect.height,
        Math.max(0, useBelow ? below : above),
      )
      element.style.maxHeight = `${height}px`
      element.style.left = `${Math.max(8, Math.min(anchor.left, window.innerWidth - rect.width - 8))}px`
      element.style.top = `${useBelow ? anchor.bottom + 8 : Math.max(8, anchor.top - height - 8)}px`
    }
  }, [target])
  const data = target && catalog[target.id]
  if (!data || !target) return null
  const translated = gameTerm(target.id, locale)
  const hasDescription = Boolean(translated.data?.lines.length)
  const lines = hasDescription ? translated.data!.lines : data.lines
  return (
    <aside
      ref={tooltip}
      className="material-tooltip"
      role="tooltip"
      id="material-description"
      style={{ left: target.x, top: target.y }}
    >
      <strong>{name(target.id, data.name)}</strong>
      {locale !== 'en' && (!hasDescription || translated.fallback) && (
        <small className="translation-fallback">
          {t('translation.english')}
        </small>
      )}
      {legacyFiveIds.includes(target.id) && <p>{t('notice.legacy_five')}</p>}
      {legacyFiveIds.includes(target.id) && (
        <a
          href="https://www.pathofexile.com/forum/view-thread/3826682"
          target="_blank"
          rel="noreferrer"
        >
          {t('ui.official_legacy_availability')}
        </a>
      )}
      {legacyHomogenisingIds.includes(target.id) && (
        <p>{t('notice.legacy_homogenising')}</p>
      )}
      {lines
        .filter((line) => !line.startsWith('Stack Size:'))
        .map((line, i) => (
          <p key={i} lang={hasDescription ? translated.language : 'en'}>
            {line}
          </p>
        ))}
      <a
        href={translated.data?.sourceUrl ?? data.sourceUrl}
        target="_blank"
        rel="noreferrer"
      >
        PoE2DB
      </a>
      {legacyHomogenisingIds.includes(target.id) && (
        <a
          href="https://www.pathofexile.com/forum/view-thread/3883495/filter-account-type/staff"
          target="_blank"
          rel="noreferrer"
        >
          {t('ui.official_legacy_availability')}
        </a>
      )}
    </aside>
  )
}
