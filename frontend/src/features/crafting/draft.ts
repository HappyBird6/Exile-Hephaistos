import { create } from 'zustand'

export type ItemTextDocument = { id: string; text: string }
const baseText = 'Item Class: Amulets\nRarity: Normal\nSolar Amulet'
type Draft = {
  source: 'base' | 'text'
  text: string
  currentText: ItemTextDocument
  baseItemLevel: number
  baseRevision: number
  activeOmens: string[]
  setActiveOmens: (ids: string[]) => void
  setBase: (itemLevel?: number) => void
  setText: (text: string) => void
  acceptText: (text: string) => void
}
// Text documents remain replaceable; checkpoint storage and selection are not fixed yet.
// Structured server items are owned only by TanStack Query.
export const useItemDraft = create<Draft>((set) => ({
  source: 'base',
  text: baseText,
  currentText: { id: 'current', text: baseText },
  baseItemLevel: 82,
  baseRevision: 0,
  activeOmens: [],
  setActiveOmens: (activeOmens) => set({ activeOmens }),
  setBase: (itemLevel = 82) =>
    set((old) => ({
      source: 'base',
      text: baseText,
      currentText: { id: 'current', text: baseText },
      baseItemLevel: itemLevel,
      baseRevision: old.baseRevision + 1,
      activeOmens: [],
    })),
  setText: (text) => set({ text }),
  acceptText: (text) =>
    set((old) => ({
      source: 'text',
      text,
      currentText: { id: 'current', text },
      activeOmens: [],
      baseRevision: old.baseRevision + 1,
    })),
}))
