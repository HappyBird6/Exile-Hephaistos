import { createContext, useContext } from 'react'
import type { ReactNode } from 'react'

export type FeedbackDescription = {
  anchor: HTMLElement
  id: string
  content: ReactNode
  onDismiss?: (() => void) | undefined
}

export const FeedbackContext = createContext<{
  show: (description: FeedbackDescription) => void
  release: (anchor: HTMLElement) => void
} | null>(null)
export const FeedbackDescriptionContext =
  createContext<FeedbackDescription | null>(null)

export const useFeedbackTooltip = () => useContext(FeedbackContext)

/** Keep native titles for components rendered outside the manual workbench. */
export function useFeedbackTitle(text: string | undefined) {
  const feedback = useFeedbackTooltip()
  return feedback ? { 'data-feedback-tooltip': text } : { title: text }
}
