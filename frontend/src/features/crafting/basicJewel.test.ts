import { expect, it } from 'vitest'
import ruby from '../../shared/test/ruby-catalog.json'
import emerald from '../../shared/test/emerald-catalog.json'
import diamond from '../../shared/test/diamond-catalog.json'
import sapphire from '../../shared/test/sapphire-catalog.json'
import type { Definition } from './craftingApi'
import type { ConcreteItem } from './workbenchApi'
import { reviewedBasicJewel, jewelCapacity } from './basicJewel'
import { catalystProjection } from './catalystQuality'
import {
  localizedModifierText,
  modifierTranslationStatus,
} from './localizedModifiers'
import { craftProbabilityEvidence } from './craftProbabilityEvidence'
import { verifiedHistoryState } from './workbenchHistory'
import type { Initial } from './craftingApi'

const catalogs = { ruby, emerald, diamond, sapphire }
function state(c: typeof ruby, ids: string[]): ConcreteItem {
  return {
    snapshotId: c.metadata.snapshotId,
    baseItemId: c.base.id,
    itemLevel: 82,
    rarity: 'RARE',
    implicits: [],
    conditions: [],
    explicits: ids.map((id) => ({
      modifierId: id,
      values: Object.fromEntries(
        c.modifiers.find((d) => d.id === id)!.stats.map((s) => [s.id, s.min]),
      ),
    })),
    catalystQuality: { type: 'CARAPACE', amount: 20 },
  }
}
it('binds every targeted definition in all six languages and discloses uniform generation', () => {
  for (const c of Object.values(catalogs)) {
    for (const d of c.modifiers)
      for (const l of ['en', 'ko', 'zh-CN', 'zh-TW', 'ja', 'es'] as const) {
        expect(modifierTranslationStatus(d as Definition, l)).toBe('VERIFIED')
        expect(
          localizedModifierText(
            d as Definition,
            Object.fromEntries(d.stats.map((s) => [s.id, s.min])),
            l,
          ),
        ).not.toContain('undefined')
      }
    expect(
      craftProbabilityEvidence({
        snapshotId: c.metadata.snapshotId,
        action: 'EXALTED',
        events: [
          { kind: 'ADD', modifierId: 'x', values: {}, selectionProbability: 1 },
        ],
      }).weighted,
    ).toBe(false)
  }
})
it('validates per-base Crafted identity, expanded slots, preserved overflow and saved films', () => {
  for (const [base, c] of Object.entries(catalogs)) {
    const families = new Set<string>(),
      ids: string[] = []
    for (const d of c.modifiers)
      if (
        d.weight > 0 &&
        d.affixType === 'PREFIX' &&
        !d.familyIds.some((f) => families.has(f)) &&
        ids.length < 3
      ) {
        ids.push(d.id)
        d.familyIds.forEach((f) => families.add(f))
      }
    const suffix = c.modifiers.find(
      (d) =>
        d.weight > 0 &&
        d.affixType === 'SUFFIX' &&
        !d.familyIds.some((f) => families.has(f)),
    )!
    const expanded = state(c, [
      ...ids,
      suffix.id,
      base + ':crafted:CraftedJewelAdditionalPrefixAllowed',
    ])
    expect(reviewedBasicJewel(expanded)).toBe(true)
    expect(jewelCapacity(expanded, 'PREFIX')).toBe(3)
    const overflow = {
      ...expanded,
      explicits: expanded.explicits.filter(
        (m) => !m.modifierId.includes(':crafted:'),
      ),
    }
    expect(reviewedBasicJewel(overflow)).toBe(true)
    expect(jewelCapacity(overflow, 'PREFIX')).toBe(2)
    const initial = {
      metadata: c.metadata,
      state: overflow,
      modifiers: Object.fromEntries(c.modifiers.map((d) => [d.id, d])),
    } as unknown as Initial
    expect(verifiedHistoryState(overflow, initial)).toBe(true)
    expect(
      reviewedBasicJewel({
        ...expanded,
        baseItemId: ruby.base.id === c.base.id ? emerald.base.id : ruby.base.id,
      }),
    ).toBe(false)
    expect(
      reviewedBasicJewel(
        state(c, [
          base + ':crafted:CraftedJewelPrefixEffect',
          base + ':crafted:CraftedJewelAdditionalSuffixAllowed',
        ]),
      ),
    ).toBe(false)
  }
})
it('projects opposite-side Ferocity and matching quality from original values with one rounding', () => {
  for (const [base, c] of Object.entries(catalogs)) {
    const d = c.modifiers.find(
      (d) =>
        d.weight > 0 && d.affixType === 'PREFIX' && d.tags.includes('defences'),
    )!
    const item = state(c, [d.id, base + ':crafted:CraftedJewelPrefixEffect'])
    item.explicits[1]!.values.display_source_value = 50
    const values = item.explicits[0]!.values,
      original = { ...values }
    const result = catalystProjection(
      d as Definition,
      values,
      item.catalystQuality,
      item,
    )
    expect(result.values[d.stats[0]!.id]).toBe(
      Math.round(values[d.stats[0]!.id]! * 1.2 * 1.5),
    )
    expect(values).toEqual(original)
    expect(
      catalystProjection(
        c.modifiers.find(
          (d) => d.id === item.explicits[1]!.modifierId,
        )! as Definition,
        item.explicits[1]!.values,
        item.catalystQuality,
        item,
      ).values,
    ).toEqual({ display_source_value: 50 })
    expect(
      localizedModifierText(
        c.modifiers.find(
          (d) => d.id === item.explicits[1]!.modifierId,
        )! as Definition,
        { display_source_value: 50 },
        'es',
      ),
    ).toContain('50')
  }
})
