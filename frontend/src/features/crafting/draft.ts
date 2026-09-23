import { create } from 'zustand'

export type ItemTextDocument = { id: string; text: string }
const baseText = 'Item Class: Amulets\nRarity: Normal\nSolar Amulet'
type Draft = {
  source: 'base' | 'text'
  text: string
  currentText: ItemTextDocument
  setBase: () => void
  setText: (text: string) => void
  acceptText: (text: string) => void
}
// Text documents remain replaceable; checkpoint storage and selection are not fixed yet.
// Structured server items are owned only by TanStack Query.
export const useItemDraft = create<Draft>((set) => ({
  source: 'base',
  text: baseText,
  currentText: { id: 'current', text: baseText },
  setBase: () =>
    set({
      source: 'base',
      text: baseText,
      currentText: { id: 'current', text: baseText },
    }),
  setText: (text) => set({ text }),
  acceptText: (text) =>
    set({ source: 'text', text, currentText: { id: 'current', text } }),
}))
