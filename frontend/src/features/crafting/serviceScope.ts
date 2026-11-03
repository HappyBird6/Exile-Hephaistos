// Product scope only; retained catalogs and saved films are never rewritten.
export const deferredServiceIds = new Set([
  'Arcanists_Etcher',
  'Essence_of_Delirium',
  'Essence_of_Insanity',
  'Scroll_of_Wisdom',
  'Orb_of_Chance',
  'Orb_of_Extraction',
  'Omen_of_Catalysing_Exaltation',
])

export function inServiceScope(id: string): boolean {
  return !deferredServiceIds.has(id)
}
