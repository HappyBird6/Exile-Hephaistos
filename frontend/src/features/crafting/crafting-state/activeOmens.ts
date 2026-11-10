export const desecrationOmenIds = [
  'Omen_of_Sinistral_Necromancy',
  'Omen_of_Dextral_Necromancy',
  'Omen_of_the_Liege',
  'Omen_of_the_Sovereign',
  'Omen_of_the_Blackblooded',
  'Omen_of_Putrefaction',
] as const
export const boneConsumedOmens = (activeOmens: readonly string[]) =>
  activeOmens.filter((id) => desecrationOmenIds.some((omen) => omen === id))
export const boneRemainingOmens = (activeOmens: readonly string[]) =>
  activeOmens.filter((id) => !desecrationOmenIds.some((omen) => omen === id))

/** Desecration omens stay active until a bone action; normal currencies must preserve them. */
export function currencyOmens(active: readonly string[]) {
  const dormant = boneConsumedOmens(active)
  const requested = boneRemainingOmens(active)
  return {
    requested,
    restore(remaining: string[]) {
      return [...remaining, ...dormant]
    },
    valid(consumed: unknown, remaining: unknown): consumed is string[] {
      if (!Array.isArray(consumed) || !Array.isArray(remaining)) return false
      const all = [...consumed, ...remaining]
      return (
        all.every((id) => typeof id === 'string') &&
        all.length === requested.length &&
        new Set(all).size === requested.length &&
        requested.every((id) => all.includes(id))
      )
    },
  }
}
