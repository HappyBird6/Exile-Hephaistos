import { describe, expect, it } from 'vitest'
import catalog from '../../shared/i18n/modifierTemplates.json'
import sapphire from '../../shared/test/sapphire-catalog.json'
import timeLostRuby from '../../shared/test/time-lost-ruby-catalog.json'
import timeLostEmerald from '../../shared/test/time-lost-emerald-catalog.json'
import timeLostSapphire from '../../shared/test/time-lost-sapphire-catalog.json'
import timeLostDiamond from '../../shared/test/time-lost-diamond-catalog.json'
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
    // API JSON property order does not affect stat identity; array order does.
    stats: binding.stats.map(({ id, min, max }) => ({ id, min, max })),
    tier: 1,
    affixType: 'PREFIX',
    familyIds: ['test-family'],
  }
}

describe('verified modifier display templates', () => {
  it('covers every ordinary catalog definition in all six locales', () => {
    const allIds = Object.keys(catalog.definitions)
    const added = (id: string) => /^(staves|maji-talisman):/.test(id)
    const ids = allIds.filter((id) => !added(id))
    // 131 source-new Staff ordinary definitions, two special targets, and Maji implicit.
    expect(allIds.filter(added)).toHaveLength(134)
    expect(
      Object.keys(catalog.definitions)
        .filter((id) =>
          /^(stellar|amber|bloodstone|lunar|azure|crimson|pearlescent):implicit:/.test(
            id,
          ),
        )
        .sort(),
    ).toEqual([
      'amber:implicit:strength',
      'azure:implicit:manaregeneration',
      'bloodstone:implicit:increasedlife',
      'crimson:implicit:liferegeneration',
      'lunar:implicit:increasedenergyshield',
      'pearlescent:implicit:allresistances',
      'stellar:implicit:allattributes',
    ])
    expect(ids.filter((id) => /^(quiver|.+-quiver):/.test(id))).toHaveLength(41)
    const jewelIds = ids.filter((id) => id.startsWith('sapphire:'))
    const crossbowIds = ids.filter((id) => /^(crossbow|.+-crossbow):/.test(id))
    // Source-backed additions: 62 ordinary, 5 special and 4 implicit bindings.
    expect(crossbowIds).toHaveLength(71)
    expect(ids.filter((id) => id.startsWith('offhand:'))).toHaveLength(31)
    expect(
      ids.filter(
        (id) =>
          !/^(ruby|emerald|sapphire|diamond|crossbow|.+-crossbow|offhand|quiver|.+-quiver|one-hand-maces|two-hand-maces|fortified-hammer|strife-pick|akoyan-club|ruination-maul|fanatic-greathammer|tawhoan-greatclub|quarterstaves|spears|aegis-quarterstaff|bolting-quarterstaff|dreaming-quarterstaff|grand-spear|flying-spear|akoyan-spear):/.test(
            id,
          ) && !id.startsWith('time-lost-'),
      ),
    ).toHaveLength(1812)
    expect(
      ids.filter(
        (id) =>
          !/^(ruby|emerald|sapphire|diamond|offhand|quiver|.+-quiver|one-hand-maces|two-hand-maces|fortified-hammer|strife-pick|akoyan-club|ruination-maul|fanatic-greathammer|tawhoan-greatclub|quarterstaves|spears|aegis-quarterstaff|bolting-quarterstaff|dreaming-quarterstaff|grand-spear|flying-spear|akoyan-spear):/.test(
            id,
          ) && !id.startsWith('time-lost-'),
      ),
      // Preserve all 1,812 historical equipment bindings and add 71 Crossbow bindings.
    ).toHaveLength(1883)
    expect(
      ids.filter(
        (id) =>
          !/^(ruby|emerald|sapphire|diamond):/.test(id) &&
          !id.startsWith('time-lost-'),
      ),
    ).toHaveLength(1989)
    expect(
      ids
        .filter(
          (id) =>
            /^(kinetic|vitalic|mnemonic|pearl|amethyst|prismatic|ruby-ring|two-stone-fire-cold):implicit:/.test(
              id,
            ) || id === 'ring:suffix:essence-hysteria-mana-regeneration',
        )
        .sort(),
    ).toEqual([
      'amethyst:implicit:chaosresistance',
      'kinetic:implicit:physicaldamage',
      'mnemonic:implicit:maximummanaincreasepercent',
      'pearl:implicit:increasedcastspeed',
      'prismatic:implicit:allresistances',
      'ring:suffix:essence-hysteria-mana-regeneration',
      'ruby-ring:implicit:fireresistance',
      'two-stone-fire-cold:implicit:fireandcoldresistance',
      'vitalic:implicit:increasedlife',
    ])
    expect(
      ids
        .filter((id) =>
          /^(guardian|gemini|fanatic|obliterator)-bow:implicit:/.test(id),
        )
        .sort(),
    ).toEqual([
      'fanatic-bow:implicit:weaponimplicitdamagetype',
      'gemini-bow:implicit:additionalarrows',
      'guardian-bow:implicit:chain',
      'obliterator-bow:implicit:projectilerange',
    ])
    expect(
      ids.filter((id) => /^(ruby|emerald|sapphire|diamond):/.test(id)),
    ).toHaveLength(392)
    expect(jewelIds).toHaveLength(73)
    const timeLostIds = ids.filter((id) => id.startsWith('time-lost-'))
    expect(timeLostIds).toHaveLength(400)
    expect([...timeLostIds].sort()).toEqual(
      [timeLostRuby, timeLostEmerald, timeLostSapphire, timeLostDiamond]
        .flatMap((c) => c.modifiers.map((d) => d.id))
        .sort(),
    )
    expect([...jewelIds].sort()).toEqual(
      sapphire.modifiers.map((entry) => entry.id).sort(),
    )
    expect(sapphire.modifiers.filter((entry) => entry.weight > 0)).toHaveLength(
      58,
    )
    expect(
      sapphire.modifiers.filter((entry) => entry.tags.includes('crafted')),
    ).toHaveLength(15)
    const source = sapphire.modifiers.find(
      (entry) => entry.id === 'sapphire:suffix:of-enchanting',
    )!
    if (!source) throw new Error('Source-backed Sapphire modifier is missing')
    const binding = catalog.definitions['sapphire:suffix:of-enchanting']
    expect(binding.englishText).toBe(source.text)
    expect(binding.stats).toEqual(source.stats)
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

  it('renders source-correlated compound values in display order without exposing stat IDs', () => {
    const d = definition('stocky-mitts:prefix:oyster-s')
    const values = Object.fromEntries(
      d.stats!.map((stat) => [stat.id, stat.min]),
    )
    const before = JSON.stringify({ d, values })
    const translated = localizedModifierText(d, values, 'ko')
    expect(translated).toContain('방어도 6% 증가')
    expect(translated).toContain('생명력 최대치 +7')
    expect(translated).not.toContain('source units')
    for (const stat of d.stats!) expect(translated).not.toContain(stat.id)
    expect(JSON.stringify({ d, values })).toBe(before)
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
