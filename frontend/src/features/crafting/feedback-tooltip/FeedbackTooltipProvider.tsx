import { useContext, useEffect, useMemo, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { MaterialDescription } from '../MaterialTooltip'
import descriptions from '../materialTooltips.json'
import { FeedbackContext, FeedbackDescriptionContext } from './feedbackContext'
import type { FeedbackDescription } from './feedbackContext'

export function FeedbackTooltipProvider({
  enabled,
  children,
}: {
  enabled: boolean
  children: ReactNode
}) {
  const [description, setDescription] = useState<FeedbackDescription | null>(
    null,
  )
  const active = useRef<FeedbackDescription | null>(null)
  const timer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined)
  const api = useMemo(() => {
    const cancel = () => clearTimeout(timer.current)
    const show = (next: FeedbackDescription) => {
      cancel()
      active.current = next
      setDescription(next)
    }
    const dismiss = () => {
      cancel()
      active.current = null
      setDescription(null)
    }
    const release = (anchor: HTMLElement) => {
      cancel()
      const current = active.current
      if (current?.anchor === anchor) {
        const descriptions = new Set(
          anchor.getAttribute('aria-describedby')?.split(/\s+/) ?? [],
        )
        descriptions.add(current.id)
        anchor.setAttribute('aria-describedby', [...descriptions].join(' '))
      }
      timer.current = setTimeout(() => {
        if (active.current?.anchor !== anchor) return
        const panel = document.querySelector('.workbench-feedback-tooltip')
        if (
          panel?.contains(document.activeElement) ||
          panel?.matches(':hover') ||
          anchor.contains(document.activeElement)
        )
          return
        dismiss()
      }, 200)
    }
    return { show, release, dismiss, cancel }
  }, [])

  useEffect(() => {
    if (!enabled) return
    function inspect(event: Event) {
      const element = event.target instanceof Element ? event.target : null
      if (element?.closest('.workbench-feedback-region')) {
        api.cancel()
        return
      }
      const materialAnchor = element?.closest<HTMLElement>(
        '[data-material-tooltip]',
      )
      const textAnchor =
        element?.closest<HTMLElement>('[data-feedback-tooltip]') ??
        materialAnchor?.querySelector<HTMLElement>(
          '.currency-missing[data-feedback-tooltip]',
        )
      const material = materialAnchor?.dataset.materialTooltip
      if (textAnchor?.dataset.feedbackTooltip) {
        api.show({
          anchor: materialAnchor ?? textAnchor,
          id: materialAnchor ? 'material-description' : 'workbench-description',
          content: (
            <>
              <p>{textAnchor.dataset.feedbackTooltip}</p>
              {material && Object.hasOwn(descriptions, material) && (
                <MaterialDescription materialId={material} />
              )}
            </>
          ),
        })
      } else if (
        materialAnchor &&
        material &&
        Object.hasOwn(descriptions, material)
      ) {
        api.show({
          anchor: materialAnchor,
          id: 'material-description',
          content: <MaterialDescription materialId={material} />,
        })
      } else if (active.current?.anchor.contains(element)) {
        api.cancel()
      } else if (active.current) api.release(active.current.anchor)
    }
    function leave(event: Event) {
      const related = (event as MouseEvent | FocusEvent).relatedTarget
      const next = related instanceof Element ? related : null
      if (
        next?.closest('.workbench-feedback-region') ||
        active.current?.anchor.contains(next)
      ) {
        api.cancel()
      } else if (active.current) api.release(active.current.anchor)
    }
    function escape(event: KeyboardEvent) {
      if (event.key !== 'Escape' || !active.current) return
      const anchor = active.current.anchor
      const onDismiss = active.current.onDismiss
      const fromPanel = document
        .querySelector('.workbench-feedback-tooltip')
        ?.contains(document.activeElement)
      if (fromPanel && anchor.isConnected) anchor.focus()
      onDismiss?.()
      api.dismiss()
      if (fromPanel) event.stopPropagation()
    }
    for (const type of ['pointerover', 'mouseover', 'focusin', 'pointerdown'])
      document.addEventListener(type, inspect)
    for (const type of ['pointerout', 'mouseout', 'focusout'])
      document.addEventListener(type, leave)
    document.addEventListener('keydown', escape, true)
    window.addEventListener('blur', api.dismiss)
    return () => {
      api.dismiss()
      for (const type of ['pointerover', 'mouseover', 'focusin', 'pointerdown'])
        document.removeEventListener(type, inspect)
      for (const type of ['pointerout', 'mouseout', 'focusout'])
        document.removeEventListener(type, leave)
      document.removeEventListener('keydown', escape, true)
      window.removeEventListener('blur', api.dismiss)
    }
  }, [enabled, api])

  useEffect(() => {
    if (!description || !enabled) return
    const { anchor, id } = description
    const previous = anchor
      .getAttribute('aria-describedby')
      ?.split(/\s+/)
      .filter((value) => value !== id)
      .join(' ')
    const panel = document.querySelector('.workbench-feedback-tooltip')
    if (panel) panel.scrollTop = 0
    anchor.setAttribute(
      'aria-describedby',
      [...new Set([...(previous?.split(/\s+/) ?? []), id])].join(' '),
    )
    return () => {
      if (previous) anchor.setAttribute('aria-describedby', previous)
      else anchor.removeAttribute('aria-describedby')
    }
  }, [description, enabled])

  return (
    <FeedbackContext.Provider value={enabled ? api : null}>
      <FeedbackDescriptionContext.Provider value={enabled ? description : null}>
        {children}
      </FeedbackDescriptionContext.Provider>
    </FeedbackContext.Provider>
  )
}

export function FeedbackTooltipOutlet() {
  const description = useContext(FeedbackDescriptionContext)
  return description ? (
    <aside
      id={description.id}
      className="workbench-feedback-tooltip wb-scrollbar"
      role="tooltip"
      tabIndex={0}
    >
      {description.content}
    </aside>
  ) : null
}
