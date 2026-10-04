import { create } from 'zustand'

export type ItemTextDocument = { id: string; text: string }
const baseText = 'Item Class: Amulets\nRarity: Normal\nSolar Amulet'
const baseTexts = {
  soldier: 'Item Class: Body Armours\nRarity: Normal\nSoldier Cuirass',
  imperial: 'Item Class: Helmets\nRarity: Normal\nImperial Greathelm',
  massive: 'Item Class: Gloves\nRarity: Normal\nMassive Mitts',
  sirenscale: 'Item Class: Gloves\nRarity: Normal\nSirenscale Gloves',
  adherent: 'Item Class: Gloves\nRarity: Normal\nAdherent Cuffs',
  slipstrike: 'Item Class: Body Armours\nRarity: Normal\nSlipstrike Vest',
  'death-mail': 'Item Class: Body Armours\nRarity: Normal\nDeath Mail',
  sleek: 'Item Class: Body Armours\nRarity: Normal\nSleek Jacket',
  vile: 'Item Class: Body Armours\nRarity: Normal\nVile Robe',
  wolfskin: 'Item Class: Body Armours\nRarity: Normal\nWolfskin Mantle',
  ancestral: 'Item Class: Helmets\nRarity: Normal\nAncestral Tiara',
  cryptic: 'Item Class: Helmets\nRarity: Normal\nCryptic Crown',
  freebooter: 'Item Class: Helmets\nRarity: Normal\nFreebooter Cap',
  gladiatorial: 'Item Class: Helmets\nRarity: Normal\nGladiatorial Helm',
  grinning: 'Item Class: Helmets\nRarity: Normal\nGrinning Mask',
  polished: 'Item Class: Gloves\nRarity: Normal\nPolished Bracers',
  'blacksteel-gloves':
    'Item Class: Gloves\nRarity: Normal\nBlacksteel Gauntlets',
  'war-wraps': 'Item Class: Gloves\nRarity: Normal\nWar Wraps',
  'time-lost-ruby': 'Item Class: Jewels\nRarity: Normal\nTime-Lost Ruby',
  'time-lost-emerald': 'Item Class: Jewels\nRarity: Normal\nTime-Lost Emerald',
  'time-lost-sapphire':
    'Item Class: Jewels\nRarity: Normal\nTime-Lost Sapphire',
  'time-lost-diamond': 'Item Class: Jewels\nRarity: Normal\nTime-Lost Diamond',
  ruby: 'Item Class: Jewels\nRarity: Normal\nRuby',
  emerald: 'Item Class: Jewels\nRarity: Normal\nEmerald',
  diamond: 'Item Class: Jewels\nRarity: Normal\nDiamond',
  sapphire: 'Item Class: Jewels\nRarity: Magic\nSapphire',
  solar: baseText,
  ring: 'Item Class: Rings\nRarity: Normal\nIron Ring',
  helmet: 'Item Class: Helmets\nRarity: Normal\nRusted Greathelm',
  belt: 'Item Class: Belts\nRarity: Normal\nRawhide Belt',
  sceptre: 'Item Class: Sceptres\nRarity: Normal\nRattling Sceptre',
  body: 'Item Class: Body Armours\nRarity: Normal\nRusted Cuirass',
  stocky: 'Item Class: Gloves\nRarity: Normal\nStocky Mitts',
  wand: 'Item Class: Wands\nRarity: Normal\nAttuned Wand',
  bow: 'Item Class: Bows\nRarity: Normal\nCrude Bow',
}
type Draft = {
  source: 'base' | 'text'
  text: string
  currentText: ItemTextDocument
  base:
    | 'time-lost-ruby'
    | 'time-lost-emerald'
    | 'time-lost-sapphire'
    | 'time-lost-diamond'
    | 'solar'
    | 'stocky'
    | 'bow'
    | 'wand'
    | 'body'
    | 'soldier'
    | 'imperial'
    | 'massive'
    | 'sirenscale'
    | 'adherent'
    | 'slipstrike'
    | 'death-mail'
    | 'sleek'
    | 'vile'
    | 'wolfskin'
    | 'ancestral'
    | 'cryptic'
    | 'freebooter'
    | 'gladiatorial'
    | 'grinning'
    | 'polished'
    | 'blacksteel-gloves'
    | 'war-wraps'
    | 'sceptre'
    | 'belt'
    | 'helmet'
    | 'ring'
    | 'ruby'
    | 'emerald'
    | 'diamond'
    | 'sapphire'
  baseItemLevel: number
  baseRevision: number
  activeOmens: string[]
  setActiveOmens: (ids: string[]) => void
  setBase: (
    itemLevel?: number,
    base?:
      | 'time-lost-ruby'
      | 'time-lost-emerald'
      | 'time-lost-sapphire'
      | 'time-lost-diamond'
      | 'solar'
      | 'stocky'
      | 'bow'
      | 'wand'
      | 'body'
      | 'soldier'
      | 'imperial'
      | 'massive'
      | 'sirenscale'
      | 'adherent'
      | 'slipstrike'
      | 'death-mail'
      | 'sleek'
      | 'vile'
      | 'wolfskin'
      | 'ancestral'
      | 'cryptic'
      | 'freebooter'
      | 'gladiatorial'
      | 'grinning'
      | 'polished'
      | 'blacksteel-gloves'
      | 'war-wraps'
      | 'sceptre'
      | 'belt'
      | 'helmet'
      | 'ring'
      | 'ruby'
      | 'emerald'
      | 'diamond'
      | 'sapphire',
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
