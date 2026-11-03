import { describe, expect, it } from 'vitest'
import { inServiceScope, deferredServiceIds } from './serviceScope'
import { materials, specialEssences, essenceRows } from './materials'
import { legacyFiveIds, legacyOmenIds, workbenchOmens } from './workbenchApi'

describe('current service scope', () => {
  it('retains all 26 catalysts while enabling equipment quality and excluding seven deferred identities', () => {
    const catalysts = materials.filter((m) => m.category === 'Catalysts')
    expect(catalysts).toHaveLength(26)
    expect(deferredServiceIds.size).toBe(7)
    expect(catalysts.every((m) => inServiceScope(m.id))).toBe(true)
    expect(specialEssences.every((m) => inServiceScope(m.id))).toBe(true)
    expect(
      essenceRows('')
        .flat()
        .filter(Boolean)
        .every((m) => inServiceScope(m!.id)),
    ).toBe(true)
    expect(inServiceScope('Orb_of_Transmutation')).toBe(true)
    expect(inServiceScope('Essence_of_Hysteria')).toBe(true)
    expect(inServiceScope('Armourers_Scrap')).toBe(true)
    expect(inServiceScope('Blacksmiths_Whetstone')).toBe(true)
  })
  it('offers five scoped legacy effects behind the legacy identity set', () => {
    expect(legacyFiveIds).toHaveLength(5)
    expect(legacyOmenIds).toHaveLength(7)
    expect(
      legacyFiveIds.every((id) => workbenchOmens.some((o) => o.id === id)),
    ).toBe(true)
  })
})
