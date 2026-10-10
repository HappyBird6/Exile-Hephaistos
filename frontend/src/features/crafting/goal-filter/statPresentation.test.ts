import { describe, expect, it } from 'vitest'
import { locales } from '../../../shared/i18n/i18n'
import { localizedModifierText } from '../localizedModifiers'
import type { Definition } from '../craftingApi'
import solar from '../../../../../backend/src/main/resources/catalog/solar-amulet/catalog.json'
import stocky from '../../../../../backend/src/main/resources/catalog/stocky-mitts/catalog.json'
import amber from '../../../../../backend/src/main/resources/catalog/amber-amulet/catalog.json'
import type { Catalog, Stat } from './types'
import { presentGoalStat } from './statPresentation'

describe('starting and goal catalog presentation parity', () => {
  for (const source of [solar, stocky, amber]) {
    const modifiers = source.modifiers as Definition[]
    const catalog = {
      context: { itemLevel: 82 },
      sourceModifiers: Object.fromEntries(modifiers.map((d) => [d.id, d])),
    } as Catalog
    it(`finds every source description and affix name in all six languages: ${source.base.name}`, () => {
      for (const definition of modifiers) {
        for (const raw of definition.stats ?? []) {
          const stat = {
            statId: `hephaistos:v1:${definition.layer!.toLowerCase()}:${raw.id}`,
            kind: definition.layer,
            sourceStatIds: [raw.id],
            label: raw.id,
            support: { evaluation: 'UNSUPPORTED', probability: 'UNSUPPORTED' },
          } as Stat
          for (const locale of locales) {
            const result = presentGoalStat(stat, catalog, locale)
            expect(result.searchText).toContain(
              localizedModifierText(definition, undefined, locale),
            )
            expect(result.searchText).toContain(definition.name)
            expect(result.label).not.toBe(raw.id)
            expect(result.statId).toBe(stat.statId)
            expect(result.support.evaluation).toBe('UNSUPPORTED')
          }
        }
      }
    })
  }
  it('never joins different layers or sources through matching text', () => {
    const stat = {
      kind: 'EXPLICIT',
      sourceStatIds: ['life'],
      label: 'Life',
    } as Stat
    const catalog = {
      context: { itemLevel: 82 },
      sourceModifiers: {
        implicit: {
          layer: 'IMPLICIT',
          stats: [{ id: 'life' }],
          text: 'wrong layer',
        },
        explicit: {
          layer: 'EXPLICIT',
          stats: [{ id: 'other' }],
          text: 'wrong source',
        },
      },
    } as unknown as Catalog
    expect(presentGoalStat(stat, catalog, 'en').searchText).toBe('Life Life')
  })
})
