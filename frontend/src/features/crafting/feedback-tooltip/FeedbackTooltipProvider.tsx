import { useContext, useEffect, useMemo, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { MaterialDescription } from '../MaterialTooltip'
import descriptions from '../materialTooltips.json'
import { FeedbackContext, FeedbackDescriptionContext } from './feedbackContext'
import type { FeedbackDescription } from './feedbackContext'

export function FeedbackTooltipProvider({
  enabled,
  priorityMaterialId = null,
  priorityAnchor = null,
  contextKey = '',
  children,
}: {
  enabled: boolean
  priorityMaterialId?: string | null
  priorityAnchor?: HTMLElement | null
  contextKey?: string
  children: ReactNode
}) {
  const [description, setDescription] = useState<FeedbackDescription | null>(
    null,
  )
  const priority = useRef<string | null>(null)
  const current = useRef<FeedbackDescription | null>(null)
  const api = useMemo(() => {
    const publish = (next: FeedbackDescription) => {
      current.current = next
      setDescription(next)
    }
    const show = (next: FeedbackDescription) => {
      if (priority.current) return
      publish(next)
    }
    const dismiss = () => {
      current.current = null
      setDescription(null)
    }
    const reset = (
      materialId: string | null,
      source: HTMLElement | null = null,
    ) => {
      const previous = current.current
      priority.current = materialId
      dismiss()
      if (!materialId || !Object.hasOwn(descriptions, materialId)) return
      const valid = (
        element: HTMLElement | null | undefined,
      ): element is HTMLElement =>
        Boolean(
          element?.isConnected &&
          element.dataset.materialTooltip === materialId &&
          !element.closest('[hidden]') &&
          !element.matches(':disabled'),
        )
      const anchor = valid(source)
        ? source
        : valid(previous?.anchor)
          ? previous.anchor
          : ([
              ...document.querySelectorAll<HTMLElement>(
                '[data-material-tooltip]',
              ),
            ].find((element) => valid(element)) ?? null)
      publish({
        anchor,
        materialId,
        id: 'material-description',
        content: <MaterialDescription materialId={materialId} />,
      })
    }
    return { show, dismiss, reset, current: () => current.current }
  }, [])

  useEffect(() => {
    if (!enabled) return
    api.reset(priorityMaterialId, priorityAnchor)
    function inspect(event: Event) {
      const element = event.target instanceof Element ? event.target : null
      // A floating base ItemCard has its own hover/focus lifetime. It never
      // produces a second copy in the shared feedback area.
      if (
        element?.closest(
          '.workbench-feedback-region, .wb-base-picker__option, .wb-base-picker__preview',
        )
      )
        return
      const materialAnchor = element?.closest<HTMLElement>(
        '[data-material-tooltip]',
      )
      const textAnchor =
        element?.closest<HTMLElement>('[data-feedback-tooltip]') ??
        materialAnchor?.querySelector<HTMLElement>(
          '.currency-missing[data-feedback-tooltip]',
        )
      const materialId = materialAnchor?.dataset.materialTooltip
      if (textAnchor?.dataset.feedbackTooltip) {
        api.show({
          anchor: materialAnchor ?? textAnchor,
          materialId,
          id: materialAnchor ? 'material-description' : 'workbench-description',
          content: (
            <>
              <p>{textAnchor.dataset.feedbackTooltip}</p>
              {materialId && Object.hasOwn(descriptions, materialId) && (
                <MaterialDescription materialId={materialId} />
              )}
            </>
          ),
        })
      } else if (
        materialAnchor &&
        materialId &&
        Object.hasOwn(descriptions, materialId)
      ) {
        api.show({
          anchor: materialAnchor,
          materialId,
          id: 'material-description',
          content: <MaterialDescription materialId={materialId} />,
        })
      }
    }
    function escape(event: KeyboardEvent) {
      if (event.key !== 'Escape' || !api.current()) return
      if (
        event.target instanceof Element &&
        event.target.closest(
          '.wb-base-picker__option, .wb-base-picker__preview',
        )
      )
        return
      const anchor = api.current()?.anchor
      const fromPanel = document
        .querySelector('.workbench-feedback-tooltip')
        ?.contains(document.activeElement)
      if (fromPanel && anchor?.isConnected) anchor.focus()
      api.dismiss()
      // Page's window listener must still cancel held/selected state.
    }
    for (const type of [
      'pointerover',
      'mouseover',
      'focusin',
      'pointerdown',
      'click',
      'contextmenu',
    ])
      document.addEventListener(type, inspect)
    document.addEventListener('keydown', escape, true)
    return () => {
      api.reset(null)
      for (const type of [
        'pointerover',
        'mouseover',
        'focusin',
        'pointerdown',
        'click',
        'contextmenu',
      ])
        document.removeEventListener(type, inspect)
      document.removeEventListener('keydown', escape, true)
    }
  }, [enabled, priorityMaterialId, priorityAnchor, contextKey, api])

  useEffect(() => {
    const anchor = description?.anchor
    if (!description || !anchor || !enabled) return
    const { id } = description
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
    <FeedbackContext.Provider value={enabled}>
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
