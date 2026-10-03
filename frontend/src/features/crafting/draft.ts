import { create } from 'zustand'

export type ItemTextDocument = { id: string; text: string }
const baseText = 'Item Class: Amulets\nRarity: Normal\nSolar Amulet'
const baseTexts = {
  solar: baseText,
  body: 'Item Class: Body Armours\nRarity: Normal\nRusted Cuirass',
  stocky: 'Item Class: Gloves\nRarity: Normal\nStocky Mitts',
  wand: 'Item Class: Wands\nRarity: Normal\nAttuned Wand',
  bow: 'Item Class: Bows\nRarity: Normal\nCrude Bow',
}
type Draft = {
  source: 'base' | 'text'
  text: string
  currentText: ItemTextDocument
  base: 'solar' | 'stocky' | 'bow' | 'wand' | 'body'
  baseItemLevel: number
  baseRevision: number
  activeOmens: string[]
  setActiveOmens: (ids: string[]) => void
  setBase: (
    itemLevel?: number,
    base?: 'solar' | 'stocky' | 'bow' | 'wand' | 'body',
  ) => void
  setText: (text: string) => void
  acceptText: (text: string) => void
}
// Text documents remain replaceable; checkpoint storage and selection are not fixed yet.
// Structured server items are owned only by TanStack Query.
export const useItemDraft = create<Draft>((set) => ({
  source: 'base',
  text: baseText,
  currentText: { id: 'current', text: baseText },
  base: 'solar',
  baseItemLevel: 82,
  baseRevision: 0,
  activeOmens: [],
  setActiveOmens: (activeOmens) => set({ activeOmens }),
  setBase: (itemLevel = 82, base = 'solar') =>
    set((old) => ({
      source: 'base',
      base,
      text: baseTexts[base],
      currentText: { id: 'current', text: baseTexts[base] },
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
