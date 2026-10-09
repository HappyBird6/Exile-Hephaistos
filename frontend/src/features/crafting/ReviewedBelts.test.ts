import { describe, expect, it } from 'vitest'
import manifest from './topBases.json'
import catalog from '../../shared/i18n/modifierTemplates.json'
import { localizedModifierText } from './localizedModifiers'
import { maximumQuality } from './qualityLimit'
import { catalystBase } from './catalystQuality'
import type { Definition } from './craftingApi'
import type { ConcreteItem } from './workbenchApi'

const keys = [
  'linen-belt',
  'wide-belt',
  'long-belt',
  'plate-belt',
  'ornate-belt',
  'mail-belt',
  'double-belt',
  'heavy-belt',
  'utility-belt',
  'fine-belt',
  'invoking-belt',
  'sinew-belt',
  'forking-belt',
] as const
describe('source-reviewed distinct Belt implicits', () => {
  it.each(keys)(
    '%s retains canonical units and six source translations',
    (key) => {
      const b = manifest[key]
      const binding =
        catalog.definitions[
          b.implicitModifierId as keyof typeof catalog.definitions
        ]
      const d: Definition = {
        id: b.implicitModifierId,
        name: b.name,
        text: binding.englishText,
        stats: binding.stats,
        affixType: 'NONE',
        familyIds: ['source'],
        tier: 0,
        layer: 'IMPLICIT',
      }
      expect(b.family).toBe('belts')
      expect(catalystBase(b.id)).toBe(false)
      const values = Object.fromEntries(binding.stats.map((s) => [s.id, s.max]))
      const state = {
        baseItemId: b.id,
        explicits: [],
      } as unknown as ConcreteItem
      expect(maximumQuality(state, {})).toBeNull()
      for (const locale of [
        'en',
        'ko',
        'ja',
        'zh-CN',
        'zh-TW',
        'es',
      ] as const) {
        const before = JSON.stringify(values)
        expect(localizedModifierText(d, values, locale)).not.toContain('{v')
        expect(JSON.stringify(values)).toBe(before)
        expect(b.sourceProperties[locale]).toHaveLength(1)
      }
      if (key === 'fine-belt') {
        expect(values.generate_x_charges_for_any_flask_per_minute).toBe(10)
        expect(localizedModifierText(d, values, 'en')).toContain('0.17')
      }
      if (['invoking-belt', 'sinew-belt', 'forking-belt'].includes(key))
        expect(values.local_charm_slots).toBe(1)
      if (['ornate-belt', 'mail-belt'].includes(key))
        expect(localizedModifierText(d, values, 'en')).toContain('10% reduced')
    },
  )
})
