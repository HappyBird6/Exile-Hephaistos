import { expect, it } from 'vitest'
import { gameTerm, locales } from '../../../shared/i18n/i18n'
import { matchesMaterial } from './materialSearch'
import descriptions from '../materialTooltips.json'

const body = {
  id: 'Greater_Essence_of_the_Body',
  name: 'Greater Essence of the Body',
}

it.each(locales)(
  'searches the displayed description and localized name in %s',
  (locale) => {
    const term = gameTerm(body.id, locale).data!
    const line = term.lines.find((text) => !text.startsWith('Stack Size:'))!
    expect(
      matchesMaterial(body, `  ${line.toLocaleUpperCase(locale)}  `, locale),
    ).toBe(true)
    expect(matchesMaterial(body, term.name, locale)).toBe(true)
    expect(matchesMaterial(body, body.name.toLowerCase(), locale)).toBe(true)
    expect(
      matchesMaterial(body, 'no_matching_description_or_name', locale),
    ).toBe(false)
    expect(matchesMaterial(body, 'Stack Size:', locale)).toBe(false)
  },
)

it('searches English fallback descriptions and keeps empty name searches', () => {
  const entry = { id: 'display-only-test-id', name: 'Fallback material' }
  // Use a sourced description with no game-term entry to exercise the same fallback as feedback.
  const catalog = descriptions as Record<string, { lines: string[] }>
  catalog[entry.id] = {
    lines: ['Stack Size: 1 / 10', 'Fallback description for testing'],
  }
  try {
    expect(matchesMaterial(entry, 'description for testing', 'ko')).toBe(true)
    expect(matchesMaterial(entry, '', 'ko')).toBe(true)
    expect(matchesMaterial(entry, 'Stack Size:', 'ko')).toBe(false)
  } finally {
    delete catalog[entry.id]
  }
})

it('includes socket Augment effect descriptions', () => {
  expect(
    matchesMaterial(
      { id: 'Lesser_Desert_Rune', name: 'Lesser Desert Rune' },
      'Fire Resistance',
      'en',
    ),
  ).toBe(true)
})

it('searches the grouped Rune feedback labels in the selected language', () => {
  const rune = { id: 'Perfect_Desert_Rune', name: 'Perfect Desert Rune' }
  expect(matchesMaterial(rune, '무도 무기', 'ko')).toBe(true)
  expect(matchesMaterial(rune, '마법봉 또는 지팡이', 'ko')).toBe(true)
  expect(matchesMaterial(rune, '방어구', 'ko')).toBe(true)
  expect(matchesMaterial(rune, 'Martial Weapons', 'en')).toBe(true)
  expect(matchesMaterial(rune, '장신구', 'ko')).toBe(false)
})
