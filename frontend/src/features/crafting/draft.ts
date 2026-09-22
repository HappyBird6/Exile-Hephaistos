import { create } from 'zustand'

type Draft = {
  source: 'base' | 'text'
  text: string
  setBase: () => void
  setText: (text: string) => void
}

// Input only. A pasted item remains unvalidated until the item parser is connected.
export const useItemDraft = create<Draft>((set) => ({
  source: 'base',
  text: '',
  setBase: () => set({ source: 'base', text: '' }),
  setText: (text) => set({ source: 'text', text }),
}))
