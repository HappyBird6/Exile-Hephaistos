import type { Definition } from './craftingApi'
import type { ConcreteItem } from './workbenchApi'

/** Local preview uses the same unlocked -> prefix -> minimum-level order as the simulator. */
export function whittlingCandidates(
  state: ConcreteItem,
  definitions: Record<string, Definition>,
  activeOmens: readonly string[],
): string[] {
  if (
    !activeOmens.includes('Omen_of_Whittling') ||
    state.rarity !== 'RARE' ||
    state.conditions.length > 0 ||
    state.explicits.some((m) => m.fractured) ||
    (activeOmens.includes('Omen_of_Dextral_Erasure') &&
      activeOmens.includes('Omen_of_Sinistral_Erasure'))
  )
    return []
  let eligible = state.explicits.filter((m) => !m.fractured)
  // Unknown affix metadata must not silently remove a possible candidate.
  if (eligible.some((m) => !definitions[m.modifierId])) return []
  if (activeOmens.includes('Omen_of_Sinistral_Erasure'))
    eligible = eligible.filter(
      (m) => definitions[m.modifierId]!.affixType === 'PREFIX',
    )
  if (activeOmens.includes('Omen_of_Dextral_Erasure'))
    eligible = eligible.filter(
      (m) => definitions[m.modifierId]!.affixType === 'SUFFIX',
    )
  if (
    eligible.some((m) => {
      const level = definitions[m.modifierId]!.requiredItemLevel
      return (
        level == null || !Number.isInteger(level) || level < 1 || level > 100
      )
    })
  )
    return []
  const lowest = Math.min(
    ...eligible.map((m) => definitions[m.modifierId]!.requiredItemLevel!),
  )
  return eligible
    .filter((m) => definitions[m.modifierId]!.requiredItemLevel === lowest)
    .map((m) => m.modifierId)
}
