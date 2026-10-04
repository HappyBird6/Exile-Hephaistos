// Product scope only; retained catalogs and saved films are never rewritten.
export const deferredServiceIds = new Set([
  'Blacksmiths_Whetstone',
  'Arcanists_Etcher',
  'Armourers_Scrap',
  'Essence_of_Delirium',
  'Essence_of_Insanity',
  'Scroll_of_Wisdom',
  'Orb_of_Chance',
  'Orb_of_Extraction',
  'Omen_of_Catalysing_Exaltation',
  'Omen_of_Sinistral_Necromancy',
  'Omen_of_Dextral_Necromancy',
])

export function inServiceScope(id: string): boolean {
  return !deferredServiceIds.has(id)
}
