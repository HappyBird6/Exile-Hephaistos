import { create } from 'zustand'

export type ItemTextDocument = { id: string; text: string }
const baseText = 'Item Class: Amulets\nRarity: Normal\nSolar Amulet'
const baseTexts = {
  'aegis-quarterstaff':
    'Item Class: Quarterstaves\nRarity: Normal\nAegis Quarterstaff',
  'bolting-quarterstaff':
    'Item Class: Quarterstaves\nRarity: Normal\nBolting Quarterstaff',
  'dreaming-quarterstaff':
    'Item Class: Quarterstaves\nRarity: Normal\nDreaming Quarterstaff',
  'grand-spear': 'Item Class: Spears\nRarity: Normal\nGrand Spear',
  'flying-spear': 'Item Class: Spears\nRarity: Normal\nFlying Spear',
  'akoyan-spear': 'Item Class: Spears\nRarity: Normal\nAkoyan Spear',
  'fortified-hammer':
    'Item Class: One Hand Maces\nRarity: Normal\nFortified Hammer',
  'strife-pick': 'Item Class: One Hand Maces\nRarity: Normal\nStrife Pick',
  'akoyan-club': 'Item Class: One Hand Maces\nRarity: Normal\nAkoyan Club',
  'ruination-maul':
    'Item Class: Two Hand Maces\nRarity: Normal\nRuination Maul',
  'fanatic-greathammer':
    'Item Class: Two Hand Maces\nRarity: Normal\nFanatic Greathammer',
  'tawhoan-greatclub':
    'Item Class: Two Hand Maces\nRarity: Normal\nTawhoan Greatclub',
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
  tasalian: 'Item Class: Boots\nRarity: Normal\nTasalian Greaves',
  drakeskin: 'Item Class: Boots\nRarity: Normal\nDrakeskin Boots',
  sekhema: 'Item Class: Boots\nRarity: Normal\nSekhema Sandals',
  'blacksteel-boots': 'Item Class: Boots\nRarity: Normal\nBlacksteel Sabatons',
  faithful: 'Item Class: Boots\nRarity: Normal\nFaithful Leggings',
  daggerfoot: 'Item Class: Boots\nRarity: Normal\nDaggerfoot Shoes',
  warmonger: 'Item Class: Bows\nRarity: Normal\nWarmonger Bow',
  guardian: 'Item Class: Bows\nRarity: Normal\nGuardian Bow',
  gemini: 'Item Class: Bows\nRarity: Normal\nGemini Bow',
  fanatic: 'Item Class: Bows\nRarity: Normal\nFanatic Bow',
  obliterator: 'Item Class: Bows\nRarity: Normal\nObliterator Bow',
  hallowed: 'Item Class: Sceptres\nRarity: Normal\nHallowed Sceptre',
  stoic: 'Item Class: Sceptres\nRarity: Normal\nStoic Sceptre',
  omen: 'Item Class: Sceptres\nRarity: Normal\nOmen Sceptre',
  'shrine-fire': 'Item Class: Sceptres\nRarity: Normal\nShrine Sceptre',
  'shrine-ice': 'Item Class: Sceptres\nRarity: Normal\nShrine Sceptre',
  'shrine-lightning': 'Item Class: Sceptres\nRarity: Normal\nShrine Sceptre',
  clasped: 'Item Class: Sceptres\nRarity: Normal\nClasped Sceptre',
  wrath: 'Item Class: Sceptres\nRarity: Normal\nWrath Sceptre',
  'linen-belt': 'Item Class: Belts\nRarity: Normal\nLinen Belt',
  'wide-belt': 'Item Class: Belts\nRarity: Normal\nWide Belt',
  'long-belt': 'Item Class: Belts\nRarity: Normal\nLong Belt',
  'plate-belt': 'Item Class: Belts\nRarity: Normal\nPlate Belt',
  'ornate-belt': 'Item Class: Belts\nRarity: Normal\nOrnate Belt',
  'mail-belt': 'Item Class: Belts\nRarity: Normal\nMail Belt',
  'double-belt': 'Item Class: Belts\nRarity: Normal\nDouble Belt',
  'heavy-belt': 'Item Class: Belts\nRarity: Normal\nHeavy Belt',
  'utility-belt': 'Item Class: Belts\nRarity: Normal\nUtility Belt',
  'fine-belt': 'Item Class: Belts\nRarity: Normal\nFine Belt',
  'invoking-belt': 'Item Class: Belts\nRarity: Normal\nInvoking Belt',
  'sinew-belt': 'Item Class: Belts\nRarity: Normal\nSinew Belt',
  'forking-belt': 'Item Class: Belts\nRarity: Normal\nForking Belt',
  'siege-crossbow': 'Item Class: Crossbows\nRarity: Normal\nSiege Crossbow',
  'gemini-crossbow': 'Item Class: Crossbows\nRarity: Normal\nGemini Crossbow',
  'elegant-crossbow': 'Item Class: Crossbows\nRarity: Normal\nElegant Crossbow',
  'flexed-crossbow': 'Item Class: Crossbows\nRarity: Normal\nFlexed Crossbow',
  'desolate-crossbow':
    'Item Class: Crossbows\nRarity: Normal\nDesolate Crossbow',
  'engraved-crossbow':
    'Item Class: Crossbows\nRarity: Normal\nEngraved Crossbow',
  'tawhoan-tower-shield':
    'Item Class: Shields\nRarity: Normal\nTawhoan Tower Shield',
  'golden-targe': 'Item Class: Shields\nRarity: Normal\nGolden Targe',
  'blacksteel-crest-shield':
    'Item Class: Shields\nRarity: Normal\nBlacksteel Crest Shield',
  'desert-buckler': 'Item Class: Bucklers\nRarity: Normal\nDesert Buckler',
  'tasalian-focus': 'Item Class: Foci\nRarity: Normal\nTasalian Focus',
  'visceral-quiver': 'Item Class: Quivers\nRarity: Normal\nVisceral Quiver',
  'volant-quiver': 'Item Class: Quivers\nRarity: Normal\nVolant Quiver',
  'penetrating-quiver':
    'Item Class: Quivers\nRarity: Normal\nPenetrating Quiver',
  'primed-quiver': 'Item Class: Quivers\nRarity: Normal\nPrimed Quiver',
  'serrated-quiver': 'Item Class: Quivers\nRarity: Normal\nSerrated Quiver',
  'toxic-quiver': 'Item Class: Quivers\nRarity: Normal\nToxic Quiver',
  'blunt-quiver': 'Item Class: Quivers\nRarity: Normal\nBlunt Quiver',
  'two-point-quiver': 'Item Class: Quivers\nRarity: Normal\nTwo-Point Quiver',
  'sacral-quiver': 'Item Class: Quivers\nRarity: Normal\nSacral Quiver',
  'fire-quiver': 'Item Class: Quivers\nRarity: Normal\nFire Quiver',
  'broadhead-quiver': 'Item Class: Quivers\nRarity: Normal\nBroadhead Quiver',
  bone: 'Item Class: Wands\nRarity: Normal\nBone Wand',
  siphoning: 'Item Class: Wands\nRarity: Normal\nSiphoning Wand',
  volatile: 'Item Class: Wands\nRarity: Normal\nVolatile Wand',
  galvanic: 'Item Class: Wands\nRarity: Normal\nGalvanic Wand',
  acrid: 'Item Class: Wands\nRarity: Normal\nAcrid Wand',
  offering: 'Item Class: Wands\nRarity: Normal\nOffering Wand',
  critical: 'Item Class: Wands\nRarity: Normal\nCritical Wand',
  primordial: 'Item Class: Wands\nRarity: Normal\nPrimordial Wand',
  dueling: 'Item Class: Wands\nRarity: Normal\nDueling Wand',
  stellar: 'Item Class: Amulets\nRarity: Normal\nStellar Amulet',
  amber: 'Item Class: Amulets\nRarity: Normal\nAmber Amulet',
  bloodstone: 'Item Class: Amulets\nRarity: Normal\nBloodstone Amulet',
  lunar: 'Item Class: Amulets\nRarity: Normal\nLunar Amulet',
  azure: 'Item Class: Amulets\nRarity: Normal\nAzure Amulet',
  crimson: 'Item Class: Amulets\nRarity: Normal\nCrimson Amulet',
  pearlescent: 'Item Class: Amulets\nRarity: Normal\nPearlescent Amulet',
  kinetic: 'Item Class: Rings\nRarity: Normal\nKinetic Ring',
  vitalic: 'Item Class: Rings\nRarity: Normal\nVitalic Ring',
  mnemonic: 'Item Class: Rings\nRarity: Normal\nMnemonic Ring',
  pearl: 'Item Class: Rings\nRarity: Normal\nPearl Ring',
  amethyst: 'Item Class: Rings\nRarity: Normal\nAmethyst Ring',
  prismatic: 'Item Class: Rings\nRarity: Normal\nPrismatic Ring',
  'ruby-ring': 'Item Class: Rings\nRarity: Normal\nRuby Ring',
  'two-stone-fire-cold': 'Item Class: Rings\nRarity: Normal\nTwo-Stone Ring',
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
    | 'tasalian'
    | 'drakeskin'
    | 'sekhema'
    | 'blacksteel-boots'
    | 'faithful'
    | 'daggerfoot'
    | 'warmonger'
    | 'guardian'
    | 'gemini'
    | 'fanatic'
    | 'obliterator'
    | 'hallowed'
    | 'stoic'
    | 'omen'
    | 'shrine-fire'
    | 'shrine-ice'
    | 'shrine-lightning'
    | 'clasped'
    | 'wrath'
    | 'linen-belt'
    | 'wide-belt'
    | 'long-belt'
    | 'plate-belt'
    | 'ornate-belt'
    | 'mail-belt'
    | 'double-belt'
    | 'heavy-belt'
    | 'utility-belt'
    | 'fine-belt'
    | 'invoking-belt'
    | 'sinew-belt'
    | 'forking-belt'
    | 'siege-crossbow'
    | 'gemini-crossbow'
    | 'elegant-crossbow'
    | 'flexed-crossbow'
    | 'desolate-crossbow'
    | 'engraved-crossbow'
    | 'tawhoan-tower-shield'
    | 'golden-targe'
    | 'blacksteel-crest-shield'
    | 'desert-buckler'
    | 'tasalian-focus'
    | 'visceral-quiver'
    | 'volant-quiver'
    | 'penetrating-quiver'
    | 'primed-quiver'
    | 'serrated-quiver'
    | 'toxic-quiver'
    | 'blunt-quiver'
    | 'two-point-quiver'
    | 'sacral-quiver'
    | 'fire-quiver'
    | 'broadhead-quiver'
    | 'fortified-hammer'
    | 'strife-pick'
    | 'akoyan-club'
    | 'ruination-maul'
    | 'fanatic-greathammer'
    | 'tawhoan-greatclub'
    | 'aegis-quarterstaff'
    | 'bolting-quarterstaff'
    | 'dreaming-quarterstaff'
    | 'grand-spear'
    | 'flying-spear'
    | 'akoyan-spear'
    | 'bone'
    | 'siphoning'
    | 'volatile'
    | 'galvanic'
    | 'acrid'
    | 'offering'
    | 'critical'
    | 'primordial'
    | 'dueling'
    | 'stellar'
    | 'amber'
    | 'bloodstone'
    | 'lunar'
    | 'azure'
    | 'crimson'
    | 'pearlescent'
    | 'kinetic'
    | 'vitalic'
    | 'mnemonic'
    | 'pearl'
    | 'amethyst'
    | 'prismatic'
    | 'ruby-ring'
    | 'two-stone-fire-cold'
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
      | 'tasalian'
      | 'drakeskin'
      | 'sekhema'
      | 'blacksteel-boots'
      | 'faithful'
      | 'daggerfoot'
      | 'warmonger'
      | 'guardian'
      | 'gemini'
      | 'fanatic'
      | 'obliterator'
      | 'hallowed'
      | 'stoic'
      | 'omen'
      | 'shrine-fire'
      | 'shrine-ice'
      | 'shrine-lightning'
      | 'clasped'
      | 'wrath'
      | 'linen-belt'
      | 'wide-belt'
      | 'long-belt'
      | 'plate-belt'
      | 'ornate-belt'
      | 'mail-belt'
      | 'double-belt'
      | 'heavy-belt'
      | 'utility-belt'
      | 'fine-belt'
      | 'invoking-belt'
      | 'sinew-belt'
      | 'forking-belt'
      | 'siege-crossbow'
      | 'gemini-crossbow'
      | 'elegant-crossbow'
      | 'flexed-crossbow'
      | 'desolate-crossbow'
      | 'engraved-crossbow'
      | 'tawhoan-tower-shield'
      | 'golden-targe'
      | 'blacksteel-crest-shield'
      | 'desert-buckler'
      | 'tasalian-focus'
      | 'visceral-quiver'
      | 'volant-quiver'
      | 'penetrating-quiver'
      | 'primed-quiver'
      | 'serrated-quiver'
      | 'toxic-quiver'
      | 'blunt-quiver'
      | 'two-point-quiver'
      | 'sacral-quiver'
      | 'fire-quiver'
      | 'broadhead-quiver'
      | 'fortified-hammer'
      | 'strife-pick'
      | 'akoyan-club'
      | 'ruination-maul'
      | 'fanatic-greathammer'
      | 'tawhoan-greatclub'
      | 'aegis-quarterstaff'
      | 'bolting-quarterstaff'
      | 'dreaming-quarterstaff'
      | 'grand-spear'
      | 'flying-spear'
      | 'akoyan-spear'
      | 'bone'
      | 'siphoning'
      | 'volatile'
      | 'galvanic'
      | 'acrid'
      | 'offering'
      | 'critical'
      | 'primordial'
      | 'dueling'
      | 'stellar'
      | 'amber'
      | 'bloodstone'
      | 'lunar'
      | 'azure'
      | 'crimson'
      | 'pearlescent'
      | 'kinetic'
      | 'vitalic'
      | 'mnemonic'
      | 'pearl'
      | 'amethyst'
      | 'prismatic'
      | 'ruby-ring'
      | 'two-stone-fire-cold'
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
