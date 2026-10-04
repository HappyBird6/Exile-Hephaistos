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
  it('renders all 290 compound bindings at min/middle/max in all locales with canonical values preserved', () => {
    const compound = Object.entries(bindings).filter(
      ([, b]) =>
        b.valueStats &&
        b.stats.length > 1 &&
        b.stats.some((s) => s.min !== s.max),
    )
    // Preserve all 244 historical bindings and cover 46 new evasion bindings.
    expect(compound).toHaveLength(290)
    for (const [id, b] of compound) {
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
    const single = Object.entries(bindings).filter(
      ([, b]) => b.valueStats && b.stats.length === 1,
    )
    // 126 historical bindings plus 45 new source-verified single-stat bindings.
    expect(single).toHaveLength(171)
    for (const [id, b] of single) {
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
