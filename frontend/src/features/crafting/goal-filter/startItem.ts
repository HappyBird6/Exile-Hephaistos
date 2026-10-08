import type { Definition, Initial } from '../craftingApi'
import type { ConcreteItem } from '../workbenchApi'
import { rolledText } from '../workbenchApi'
import type { ItemCardData } from '../itemCardData'
import { supportsConcreteStateShape } from '../workbenchStateShape'
import {
  isWorkbenchJewel,
  jewelCapacity,
  reviewedBasicJewel,
} from '../basicJewel'

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
// Exact aliases already accepted by the item parser/mapper contracts. No name guessing.
export function startClassMatches(expected: string, actual: string): boolean {
  return expected === actual || (expected === 'Amulets' && actual === 'Amulet')
}

export function startItemIssues(
  initial: Initial,
  item: ConcreteItem,
): string[] {
  const issues: string[] = []
  if (
    !supportsConcreteStateShape(item) ||
    item.conditions.length ||
    item.catalystQuality != null ||
    [...item.implicits, ...item.explicits].some((m) => m.fractured)
  )
    issues.push(
      'Special conditions, quality and fractured modifiers are not supported by this editor.',
    )
  if (
    item.baseItemId !== initial.state.baseItemId ||
    item.snapshotId !== initial.state.snapshotId
  )
    issues.push(
      'Item and selected catalog must have the same base and snapshot.',
    )
  if (
    !Number.isInteger(item.itemLevel) ||
    item.itemLevel < 1 ||
    item.itemLevel > 100
  )
    issues.push('Item level must be an integer from 1 to 100.')
  if (
    item.implicits.length !== initial.state.implicits.length ||
    item.implicits.some(
      (m, i) => m.modifierId !== initial.state.implicits[i]?.modifierId,
    )
  )
    issues.push('Implicits must match the selected server base.')
  const families = new Set<string>(),
    ids = new Set<string>()
  for (const [layer, rows] of [
    ['IMPLICIT', item.implicits],
    ['EXPLICIT', item.explicits],
  ] as const) {
    for (const row of rows) {
      const definition = initial.modifiers[row.modifierId]
      if (
        !definition ||
        !definition.stats ||
        (definition.layer && definition.layer !== layer)
      ) {
        issues.push(
          `Modifier is not supported in this catalog layer: ${row.modifierId}`,
        )
        continue
      }
      if (
        Object.keys(row.values).length !== definition.stats.length ||
        !definition.stats.every(
          (s) =>
            Number.isSafeInteger(row.values[s.id]) &&
            row.values[s.id]! >= s.min &&
            row.values[s.id]! <= s.max,
        )
      )
        issues.push(
          `Modifier values must match the catalog stat set and ranges: ${row.modifierId}`,
        )
      if (layer === 'EXPLICIT') {
        if (
          ids.has(row.modifierId) ||
          definition.familyIds.some((f) => families.has(f))
        )
          issues.push(
            'Explicit modifiers cannot duplicate IDs or overlap families.',
          )
        ids.add(row.modifierId)
        definition.familyIds.forEach((f) => families.add(f))
        if (
          definition.affixType !== 'PREFIX' &&
          definition.affixType !== 'SUFFIX'
        )
          issues.push(
            'An explicit modifier requires a reviewed prefix or suffix.',
          )
      }
    }
  }
  if (isWorkbenchJewel(item.baseItemId)) {
    if (!reviewedBasicJewel(item))
      issues.push(
        'Jewel state violates reviewed affix, Crafted, implicit or stat rules.',
      )
  } else {
    // Existing nine equipment catalogs: Normal 0/0, Magic 1/1, Rare 3/3.
    // backend/src/main/resources/catalog/*/catalog.json; ItemStateValidator.slots.
    const cap = item.rarity === 'NORMAL' ? 0 : item.rarity === 'MAGIC' ? 1 : 3
    if (
      ['PREFIX', 'SUFFIX'].some(
        (side) =>
          item.explicits.filter(
            (m) => initial.modifiers[m.modifierId]?.affixType === side,
          ).length > cap,
      )
    )
      issues.push('Equipment affix capacity exceeded (Magic 1/1; Rare 3/3).')
  }
  return [...new Set(issues)]
}
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
  if (startItemIssues(initial, item).length) return []
  const rare: ConcreteItem = { ...item, rarity: 'RARE' }
  const used = item.explicits.flatMap(
    (m) => initial.modifiers[m.modifierId]?.familyIds ?? [],
  )
  return Object.values(initial.modifiers).filter(
    (m) =>
      (m.affixType === 'PREFIX' || m.affixType === 'SUFFIX') &&
      m.stats?.length &&
      (!m.layer || m.layer === 'EXPLICIT') &&
      (m.requiredItemLevel ?? 1) <= item.itemLevel &&
      item.explicits.filter(
        (e) => initial.modifiers[e.modifierId]?.affixType === m.affixType,
      ).length <
        (isWorkbenchJewel(item.baseItemId)
          ? jewelCapacity(rare, m.affixType)
          : 3) &&
      !m.familyIds.some((f) => used.includes(f)) &&
      !startItemIssues(initial, {
        ...rare,
        explicits: [
          ...rare.explicits,
          {
            modifierId: m.id,
            values: Object.fromEntries(m.stats.map((s) => [s.id, s.min])),
          },
        ],
      }).length,
  )
}
