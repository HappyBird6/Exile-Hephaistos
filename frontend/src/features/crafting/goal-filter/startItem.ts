import type { Definition, Initial } from '../craftingApi'
import type { ConcreteItem } from '../workbenchApi'
import { rolledText } from '../workbenchApi'
import type { ItemCardData } from '../itemCardData'

// Names match the existing Workbench's reviewed bases; this is not a registry.
export const startBases = [
  ['solar', 'Amulets', 'Solar Amulet'],
  ['stocky', 'Gloves', 'Stocky Mitts'],
  ['bow', 'Bows', 'Crude Bow'],
  ['wand', 'Wands', 'Attuned Wand'],
  ['body', 'Body Armours', 'Rusted Cuirass'],
  ['sceptre', 'Sceptres', 'Rattling Sceptre'],
  ['belt', 'Belts', 'Rawhide Belt'],
  ['helmet', 'Helmets', 'Rusted Greathelm'],
  ['ring', 'Rings', 'Iron Ring'],
  ['ruby', 'Jewels', 'Ruby'],
  ['emerald', 'Jewels', 'Emerald'],
  ['sapphire', 'Jewels', 'Sapphire'],
  ['diamond', 'Jewels', 'Diamond'],
  ['time-lost-ruby', 'Jewels', 'Time-Lost Ruby'],
  ['time-lost-emerald', 'Jewels', 'Time-Lost Emerald'],
  ['time-lost-sapphire', 'Jewels', 'Time-Lost Sapphire'],
  ['time-lost-diamond', 'Jewels', 'Time-Lost Diamond'],
] as const
export type StartBase = (typeof startBases)[number]
export function startCard(
  base: StartBase,
  item: ConcreteItem,
  definitions: Initial['modifiers'],
): ItemCardData {
  return {
    name: base[2],
    base: base[2],
    itemClass: base[1],
    rarity: item.rarity,
    itemLevel: item.itemLevel,
    properties: [],
    requirements: [],
    flags: [],
    modifiers: [
      ...item.implicits.map((m) => ({ ...m, kind: 'implicit' as const })),
      ...item.explicits.map((m) => ({ ...m, kind: 'explicit' as const })),
    ].map((m) => ({
      id: m.modifierId,
      kind: m.kind,
      text: definitions[m.modifierId]
        ? rolledText(definitions[m.modifierId]!, m.values)
        : m.modifierId,
    })),
  }
}
export function startText(
  base: StartBase,
  item: ConcreteItem,
  definitions: Initial['modifiers'],
) {
  const card = startCard(base, item, definitions)
  return [
    `Item Class: ${base[1]}`,
    `Rarity: ${item.rarity === 'NORMAL' ? 'Normal' : item.rarity === 'MAGIC' ? 'Magic' : 'Rare'}`,
    ...(item.rarity === 'RARE' ? ['Crafting Preview'] : []),
    base[2],
    '--------',
    `Item Level: ${item.itemLevel}`,
    '--------',
    ...card.modifiers
      .filter((m) => m.kind === 'implicit')
      .map((m) => `${m.text} (implicit)`),
    ...(item.implicits.length && item.explicits.length ? ['--------'] : []),
    ...card.modifiers.filter((m) => m.kind === 'explicit').map((m) => m.text),
  ].join('\n')
}
export function eligibleStartModifiers(
  initial: Initial,
  item: ConcreteItem,
): Definition[] {
  // Manual concrete editing is restricted to the already reviewed Solar model.
  if (
    initial.state.baseItemId !== 'Metadata/Items/Amulets/FourAmulet9' ||
    item.explicits.length >= 6
  )
    return []
  const used = item.explicits.flatMap(
    (m) => initial.modifiers[m.modifierId]?.familyIds ?? [],
  )
  return Object.values(initial.modifiers).filter(
    (m) =>
      (m.affixType === 'PREFIX' || m.affixType === 'SUFFIX') &&
      m.stats?.length &&
      (m.requiredItemLevel ?? 1) <= item.itemLevel &&
      item.explicits.filter(
        (e) => initial.modifiers[e.modifierId]?.affixType === m.affixType,
      ).length < 3 &&
      !m.familyIds.some((f) => used.includes(f)),
  )
}
