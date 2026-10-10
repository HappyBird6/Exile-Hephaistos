import type { Definition } from '../craftingApi'
import type { ConcreteItem } from '../workbenchApi'

/** Keep canonical rolls by stat identity, clamping only to the selected tier. */
export function changeModifierTier(
  modifier: ConcreteItem['explicits'][number],
  next: Definition,
): ConcreteItem['explicits'][number] {
  return {
    ...modifier,
    modifierId: next.id,
    values: Object.fromEntries(
      (next.stats ?? []).map((stat) => [
        stat.id,
        Math.min(
          stat.max,
          Math.max(stat.min, modifier.values[stat.id] ?? stat.min),
        ),
      ]),
    ),
  }
}

export type ModifierGroup = { id: string; tiers: Definition[] }

// Call with one selected catalog's eligible definitions. Text and translations
// are display data, never identity. A conflict family can contain distinct
// effects, so retain the affix/layer and complete stat identity as well.
export function groupStartModifiers(
  definitions: Definition[],
): ModifierGroup[] {
  const groups = new Map<string, Definition[]>()
  for (const definition of definitions) {
    const id = JSON.stringify([
      definition.affixType,
      definition.layer ?? 'EXPLICIT',
      [...definition.familyIds].sort(),
      definition.stats?.map((stat) => stat.id).sort() ?? [],
      definition.familyIds.length && definition.stats?.length
        ? null
        : definition.id,
    ])
    const tiers = groups.get(id) ?? []
    tiers.push(definition)
    groups.set(id, tiers)
  }
  return [...groups].map(([id, tiers]) => ({
    id,
    // Same ranking as SupportGoals: lower tier number is the higher tier.
    // Eligibility is resolved by the caller before the first tier is selected.
    tiers: tiers.sort((a, b) => a.tier - b.tier || a.id.localeCompare(b.id)),
  }))
}
