import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import terms from '../../shared/i18n/gameTerms.json'
import catalog from '../../shared/i18n/modifierTemplates.json'
import {
  locales,
  formatNumber,
  gameName,
  matchesGameName,
  setLocale,
} from '../../shared/i18n/i18n'
import { localizedModifierText } from './localizedModifiers'
import type { Definition } from './craftingApi'
import { ServiceMessage } from './ServiceMessage'
import { serviceText } from './serviceMessages'

type DisplayBinding = {
  englishText: string
  stats: NonNullable<Definition['stats']>
  template: string
  valueStats?: { id: string; divisor: number }[]
}
const bindings: Record<string, DisplayBinding> = catalog.definitions
describe('remaining display coverage', () => {
  it('preserves historical compounds and renders new Staff bindings at min/middle/max in all locales', () => {
    const allCompound = Object.entries(bindings).filter(
      ([, b]) =>
        b.valueStats &&
        b.stats.length > 1 &&
        b.stats.some((s) => s.min !== s.max),
    )
    const compound = allCompound.filter(
      ([id]) => !/^(staves|maji-talisman):/.test(id),
    )
    expect(
      allCompound.filter(([id]) => /^(staves|maji-talisman):/.test(id)),
    ).toHaveLength(10)
    // Preserve all historical bindings and cover all source-new Staff compounds below.
    expect(
      compound.filter(
        ([id]) =>
          !/^(crossbow|.+-crossbow|offhand|one-hand-maces|two-hand-maces|fortified-hammer|strife-pick|akoyan-club|ruination-maul|fanatic-greathammer|tawhoan-greatclub|quarterstaves|spears|aegis-quarterstaff|bolting-quarterstaff|dreaming-quarterstaff|grand-spear|flying-spear|akoyan-spear):/.test(
            id,
          ),
      ),
    ).toHaveLength(382)
    expect(
      compound.filter(([id]) => /^(crossbow|.+-crossbow):/.test(id)),
    ).toHaveLength(50)
    expect(compound.filter(([id]) => id.startsWith('offhand:'))).toHaveLength(4)
    expect(
      compound.filter(([id]) =>
        /^(one-hand-maces|two-hand-maces|fortified-hammer|strife-pick|akoyan-club|ruination-maul|fanatic-greathammer|tawhoan-greatclub):/.test(
          id,
        ),
      ),
    ).toHaveLength(0)
    expect(
      compound.filter(([id]) =>
        /^(quarterstaves|spears|aegis-quarterstaff|bolting-quarterstaff|dreaming-quarterstaff|grand-spear|flying-spear|akoyan-spear):/.test(
          id,
        ),
      ),
    ).toHaveLength(1)
    expect(compound).toHaveLength(437)
    expect(compound.filter(([id]) => id.startsWith('kinetic:'))).toEqual([
      [
        'kinetic:implicit:physicaldamage',
        bindings['kinetic:implicit:physicaldamage'],
      ],
    ])
    for (const [id, b] of allCompound) {
      const d: Definition = {
        id,
        name: 'Canonical source',
        text: b.englishText,
        stats: b.stats.map(({ id, min, max }) => ({ id, min, max })),
        tier: 1,
        affixType: 'PREFIX',
        familyIds: [],
      }
      for (const point of [0, 0.5, 1]) {
        const values = Object.fromEntries(
          b.stats.map((s) => [
            s.id,
            Math.round(s.min + (s.max - s.min) * point),
          ]),
        )
        const before = JSON.stringify({ d, values })
        for (const locale of locales) {
          const text = localizedModifierText(d, values, locale)
          expect(text, `${id}:${locale}`).not.toMatch(
            /source units|\{v\d+\}| = /,
          )
          for (const s of b.valueStats!)
            expect(text).toContain(
              formatNumber(
                values[s.id]! / s.divisor,
                { maximumFractionDigits: 20 },
                locale,
              ),
            )
        }
        expect(JSON.stringify({ d, values })).toBe(before)
      }
    }
  })
  it('projects verified per-minute, permyriad and negative source units without rewriting rolls', () => {
    const allSingle = Object.entries(bindings).filter(
      ([, b]) => b.valueStats && b.stats.length === 1,
    )
    const single = allSingle.filter(
      ([id]) => !/^(staves|maji-talisman):/.test(id),
    )
    expect(
      allSingle.filter(([id]) => /^(staves|maji-talisman):/.test(id)),
    ).toHaveLength(124)
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
    // Preserve all historical bindings and cover 18 new numeric Crossbow bindings.
    expect(
      single.filter(
        ([id]) =>
          !/^(crossbow|.+-crossbow|offhand|quiver|.+-quiver|one-hand-maces|two-hand-maces|fortified-hammer|strife-pick|akoyan-club|ruination-maul|fanatic-greathammer|tawhoan-greatclub|quarterstaves|spears|aegis-quarterstaff|bolting-quarterstaff|dreaming-quarterstaff|grand-spear|flying-spear|akoyan-spear):/.test(
            id,
          ),
      ),
    ).toHaveLength(234)
    expect(
      single.filter(([id]) => /^(crossbow|.+-crossbow):/.test(id)),
    ).toHaveLength(18)
    expect(single.filter(([id]) => id.startsWith('offhand:'))).toHaveLength(27)
    expect(
      single.filter(([id]) => /^(quiver|.+-quiver):/.test(id)),
    ).toHaveLength(38)
    expect(
      single.filter(([id]) =>
        /^(one-hand-maces|two-hand-maces|fortified-hammer|strife-pick|akoyan-club|ruination-maul|fanatic-greathammer|tawhoan-greatclub):/.test(
          id,
        ),
      ),
    ).toHaveLength(24)
    expect(
      single.filter(([id]) =>
        /^(quarterstaves|spears|aegis-quarterstaff|bolting-quarterstaff|dreaming-quarterstaff|grand-spear|flying-spear|akoyan-spear):/.test(
          id,
        ),
      ),
    ).toHaveLength(1)
    expect(single).toHaveLength(342)
    expect(
      single
        .filter(([id]) =>
          /^(vitalic|mnemonic|pearl|amethyst|prismatic|ruby-ring|two-stone-fire-cold):implicit:/.test(
            id,
          ),
        )
        .map(([id]) => id)
        .sort(),
    ).toEqual([
      'amethyst:implicit:chaosresistance',
      'mnemonic:implicit:maximummanaincreasepercent',
      'pearl:implicit:increasedcastspeed',
      'prismatic:implicit:allresistances',
      'ruby-ring:implicit:fireresistance',
      'two-stone-fire-cold:implicit:fireandcoldresistance',
      'vitalic:implicit:increasedlife',
    ])
    expect(
      single
        .filter(([id]) =>
          /^(guardian|gemini|obliterator)-bow:implicit:/.test(id),
        )
        .map(([id]) => id)
        .sort(),
    ).toEqual([
      'gemini-bow:implicit:additionalarrows',
      'guardian-bow:implicit:chain',
      'obliterator-bow:implicit:projectilerange',
    ])
    for (const [id, b] of allSingle) {
      const d: Definition = {
        id,
        name: 'Canonical source',
        text: b.englishText,
        stats: b.stats.map(({ id, min, max }) => ({ id, min, max })),
        tier: 1,
        affixType: 'SUFFIX',
        familyIds: [],
      }
      const stat = b.stats[0]!
      for (const value of [
        stat.min,
        Math.round((stat.min + stat.max) / 2),
        stat.max,
      ]) {
        const values = { [stat.id]: value },
          before = JSON.stringify(values)
        for (const locale of locales) {
          const text = localizedModifierText(d, values, locale)
          expect(text).not.toMatch(/source units|\{v\d+\}| = /)
          expect(text).toContain(
            formatNumber(
              value / b.valueStats![0]!.divisor,
              { maximumFractionDigits: 20 },
              locale,
            ),
          )
        }
        expect(JSON.stringify(values)).toBe(before)
      }
    }
  })
  it('provides six-language Liquid tooltips and localized base search', () => {
    const ids = Object.keys(terms.en).filter(
      (id) => id.includes('Liquid') && id !== 'Liquid_Verisium',
    )
    expect(ids).toHaveLength(26)
    for (const locale of locales) {
      const dictionary: Record<
        string,
        { name: string; lines: string[]; itemKey: string }
      > = terms[locale]
      for (const id of ids) {
        expect(dictionary[id]?.name).toBeTruthy()
        expect(dictionary[id]?.lines.length).toBeGreaterThan(0)
      }
      for (const id of ['Ruby', 'Emerald', 'Diamond']) {
        expect(gameName(id, id, locale)).toBe(dictionary[id]!.name)
        expect(matchesGameName(id, id, dictionary[id]!.name, locale)).toBe(true)
      }
    }
  })
  it('retains raw radius [1000] while localizing its display meaning', () => {
    for (const [id, b] of Object.entries(bindings).filter(([id]) =>
      id.endsWith(':implicit:base-radius'),
    )) {
      const d: Definition = {
        id,
        name: 'Base radius',
        text: b.englishText,
        stats: b.stats.map(({ id, min, max }) => ({ id, min, max })),
        tier: 0,
        affixType: 'NONE',
        familyIds: [],
      }
      for (const locale of locales) {
        const text = localizedModifierText(
          d,
          { local_jewel_effect_base_radius: 1000 },
          locale,
        )
        expect(text).toContain('[1000]')
        expect(text).not.toMatch(/Very Large|Medium|local jewel effect/)
      }
    }
  })
  it('uses stable service adapters and labels unknown source text without altering it', () => {
    expect(
      serviceText('This currency cannot be used on this rarity.', 'ko'),
    ).toContain('희귀도')
    expect(
      serviceText(
        'Could not verify the coupled roll model. Your item is unchanged. Please retry.',
        'es',
      ),
    ).toContain('valores vinculados')
    const raw = 'Unknown source ID: Data/Mods/A; https://example.test/?x=1'
    setLocale('ko')
    render(<ServiceMessage text={raw} />)
    expect(screen.getByText(raw)).toHaveAttribute('lang', 'en')
    expect(screen.getByText(/서버·출처 원문/)).toBeVisible()
    expect(serviceText(raw, 'ja')).toBe(raw)
  })
})
