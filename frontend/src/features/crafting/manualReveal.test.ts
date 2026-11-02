import { expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import reveal from './manualRevealDefinitions.json'
import templates from '../../shared/i18n/modifierTemplates.json'
import {
  localizedModifierText,
  modifierRollDivisor,
  storedModifierRoll,
} from './localizedModifiers'
import { locales, formatNumber } from '../../shared/i18n/i18n'
import type { Definition } from './craftingApi'

it('keeps manual definitions identical to the sealed backend catalog with no sampling weights', () => {
  const backend = JSON.parse(
    readFileSync(
      '../backend/src/main/resources/catalog/manual-reveal.json',
      'utf8',
    ),
  )
  expect(reveal.bases).toEqual(backend.bases)
  expect(Object.keys(reveal.bases)).toHaveLength(134)
  expect(Object.values(reveal.definitions)).toEqual(backend.definitions)
  expect(backend.definitions).toHaveLength(277)
  for (const definition of backend.definitions) {
    expect(definition.weight).toBe(0)
    expect(definition.tier).toBe(0)
    expect(definition.tags).toContain('reveal_only')
    expect(definition.tags).toContain('tier_unknown')
  }
})

it('renders every manual roll at min/middle/max including fractional source units without changing stored values', () => {
  const bindings = templates.definitions as Record<
    string,
    { valueStats?: { id: string; divisor: number }[] }
  >
  for (const raw of Object.values(reveal.definitions)) {
    const definition = raw as Definition
    for (const point of [0, 0.5, 1]) {
      const values = Object.fromEntries(
        definition.stats!.map((s) => [
          s.id,
          Math.round(s.min + (s.max - s.min) * point),
        ]),
      )
      const before = JSON.stringify(values)
      for (const locale of locales) {
        const text = localizedModifierText(definition, values, locale)
        expect(text).not.toMatch(/\{v\d+\}/)
        for (const stat of bindings[definition.id]!.valueStats ?? []) {
          expect(modifierRollDivisor(definition, stat.id)).toBe(stat.divisor)
          expect(text).toContain(
            formatNumber(
              values[stat.id]! / stat.divisor,
              { maximumFractionDigits: 20 },
              locale,
            ),
          )
        }
      }
      expect(JSON.stringify(values)).toBe(before)
    }
  }
})

it('does not silently round unsupported fractional input into an allowed roll', () => {
  expect(storedModifierRoll(0.07, 100)).toBe(7)
  expect(storedModifierRoll(0.031, 100)).toBe(3.1)
  expect(storedModifierRoll(1.9, 1)).toBe(1.9)
})
