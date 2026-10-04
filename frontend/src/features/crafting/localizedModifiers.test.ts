import { describe, expect, it } from 'vitest'
import catalog from '../../shared/i18n/modifierTemplates.json'
import { locales } from '../../shared/i18n/i18n'
import type { Definition } from './craftingApi'
import {
  localizedModifierText,
  modifierTranslationStatus,
} from './localizedModifiers'
import { rolledText } from './workbenchApi'

function definition(id: keyof typeof catalog.definitions): Definition {
  const binding = catalog.definitions[id]
  return {
    id,
    name: catalog.templates.en[
      binding.template as keyof typeof catalog.templates.en
    ].name,
    text: binding.englishText,
    stats: binding.stats,
    tier: 1,
    affixType: 'PREFIX',
    familyIds: ['test-family'],
  }
}

describe('verified modifier display templates', () => {
  it('covers every ordinary catalog definition in all six locales', () => {
    expect(Object.keys(catalog.definitions)).toHaveLength(1485)
    for (const id of Object.keys(catalog.definitions)) {
      const d = definition(id as keyof typeof catalog.definitions)
      for (const locale of locales) {
        expect(modifierTranslationStatus(d, locale)).toBe('VERIFIED')
        expect(localizedModifierText(d, undefined, locale)).not.toMatch(
          /\{v\d+\}/,
        )
      }
    }
  })

  it('renders Korean ranges and rolled numbers without altering the definition or values', () => {
    const d = definition('amulet:prefix:adept-s')
    const values = { 'spell_damage_+%': 10 }
    const before = JSON.stringify({ d, values })
    expect(localizedModifierText(d, undefined, 'ko')).toBe(
      '주문 피해 (8—12)% 증가',
    )
    expect(localizedModifierText(d, values, 'ko')).toBe('주문 피해 10% 증가')
    expect(localizedModifierText(d, values, 'en')).toBe(rolledText(d, values))
    expect(JSON.stringify({ d, values })).toBe(before)
  })

  it('preserves English display and rejects stale or unknown identities', () => {
    const d = definition('amulet:prefix:adept-s')
    for (const changed of [
      { ...d, id: 'unknown:modifier' },
      { ...d, text: 'Different source text' },
      { ...d, stats: [{ id: 'spell_damage_+%', min: 80, max: 120 }] },
    ]) {
      expect(modifierTranslationStatus(changed, 'ko')).toBe('ENGLISH_FALLBACK')
      expect(localizedModifierText(changed, undefined, 'ko')).toBe(changed.text)
    }
  })

  it('keeps raw multi-stat IDs and source values while localizing only the verified affix name', () => {
    const d = definition('stocky-mitts:prefix:oyster-s')
    const values = Object.fromEntries(
      d.stats!.map((stat) => [stat.id, stat.min]),
    )
    const english = rolledText(d, values)
    const translated = localizedModifierText(d, values, 'ko')
    expect(english.startsWith(`${d.name}:`)).toBe(true)
    expect(translated.endsWith(english.slice(d.name.length))).toBe(true)
    for (const stat of d.stats!)
      expect(translated).toContain(stat.id.replaceAll('_', ' '))
    expect(localizedModifierText(d, undefined, 'ko')).toContain('(6—13)')
    expect(localizedModifierText(d, undefined, 'ko')).toContain('(7—10)')
  })

  it('leaves unsupported implicit projection text in its canonical rendering', () => {
    const d: Definition = {
      id: 'iron-ring:implicit:added-physical-damage-to-attacks',
      name: 'Added Physical Damage',
      text: 'Adds 1 to 4 Physical Damage to Attacks',
      tier: 1,
      affixType: 'NONE',
      familyIds: [],
      stats: [
        { id: 'attack_minimum_added_physical_damage', min: 1, max: 1 },
        { id: 'attack_maximum_added_physical_damage', min: 4, max: 4 },
      ],
    }
    const values = {
      attack_minimum_added_physical_damage: 2,
      attack_maximum_added_physical_damage: 5,
    }
    expect(localizedModifierText(d, values, 'ko')).toBe(rolledText(d, values))
    expect(modifierTranslationStatus(d, 'ko')).toBe('ENGLISH_FALLBACK')
  })
})
