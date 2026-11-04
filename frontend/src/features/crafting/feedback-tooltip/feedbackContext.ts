import { createContext, useContext } from 'react'
import type { ReactNode } from 'react'

export type FeedbackDescription = {
  anchor: HTMLElement | null
  id: string
  content: ReactNode
  materialId?: string | undefined
}

export const FeedbackContext = createContext(false)
export const FeedbackDescriptionContext =
  createContext<FeedbackDescription | null>(null)

export const useFeedbackTooltip = () => useContext(FeedbackContext)

/** Keep native titles for components rendered outside the manual workbench. */
export function useFeedbackTitle(text: string | undefined) {
  const feedback = useFeedbackTooltip()
  return feedback ? { 'data-feedback-tooltip': text } : { title: text }
}
